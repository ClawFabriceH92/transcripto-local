package com.transcripto.local.security

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Chiffrement AES-256/GCM via Android Keystore.
 * Les clés ne quittent jamais le matériel sécurisé (TEE/StrongBox).
 */
class CryptoManager(private val context: Context) {

    private companion object {
        const val KEY_ALIAS = "transcripto_local_master_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_LENGTH = 128 // bits
        const val IV_LENGTH = 12 // bytes
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        if (keyStore.containsAlias(KEY_ALIAS)) {
            return (keyStore.getEntry(KEY_ALIAS, null) as java.security.KeyStore.SecretKeyEntry).secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )

        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setUnlockedDeviceRequired(true)
        }

        keyGenerator.init(builder.build())
        return keyGenerator.generateKey()
    }

    fun encryptFile(inputFile: File, outputFile: File) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv

        FileOutputStream(outputFile).use { output ->
            // Write IV first
            output.write(iv)

            FileInputStream(inputFile).use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    val encrypted = cipher.update(buffer, 0, bytesRead)
                    if (encrypted != null) output.write(encrypted)
                }
                output.write(cipher.doFinal())
            }
        }
    }

    fun decryptFile(encryptedFile: File, outputFile: File) {
        FileInputStream(encryptedFile).use { input ->
            // Read IV
            val iv = ByteArray(IV_LENGTH)
            if (input.read(iv) != IV_LENGTH) {
                throw SecurityException("Invalid encrypted file format")
            }

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), spec)

            FileOutputStream(outputFile).use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    val decrypted = cipher.update(buffer, 0, bytesRead)
                    if (decrypted != null) output.write(decrypted)
                }
                output.write(cipher.doFinal())
            }
        }
    }

    fun secureDelete(file: File) {
        if (!file.exists()) return

        // Overwrite with random data before deletion
        val random = java.security.SecureRandom()
        val length = file.length().toInt().coerceAtMost(1024 * 1024) // 1 MB max
        val buffer = ByteArray(length.coerceAtLeast(1))

        file.outputStream().use { out ->
            for (i in 0 until 3) { // 3 passes
                random.nextBytes(buffer)
                out.write(buffer)
                out.flush()
            }
        }

        file.delete()
    }
}
