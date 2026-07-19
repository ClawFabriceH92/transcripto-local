package com.transcripto.local.security

import android.content.Context
import android.content.SharedPreferences
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * Gestion du verrouillage de l'application par PIN et/ou biométrie.
 */
class AppLockManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_lock", Context.MODE_PRIVATE)

    private val random = SecureRandom()

    companion object {
        private const val KEY_ENABLED = "lock_enabled"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    }

    val isLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)

    val isBiometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)

    val hasPin: Boolean
        get() = prefs.contains(KEY_PIN_HASH)

    fun enablePinLock(pin: String) {
        val salt = ByteArray(16).also { random.nextBytes(it) }
        val hash = hashPin(pin, salt)
        prefs.edit()
            .putBoolean(KEY_ENABLED, true)
            .putString(KEY_PIN_HASH, hash)
            .putString(KEY_PIN_SALT, Base64.getEncoder().encodeToString(salt))
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val salt = Base64.getDecoder().decode(prefs.getString(KEY_PIN_SALT, ""))
        return hashPin(pin, salt) == storedHash
    }

    fun enableBiometric(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun isBiometricAvailable(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticateWithBiometric(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        if (!isBiometricEnabled) {
            onError("Biométrie non activée")
            return
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Déverrouiller Transcripto Local")
            .setSubtitle("Authentification requise")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        val biometricPrompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    onError("Authentification échouée")
                }
            }
        )

        biometricPrompt.authenticate(promptInfo)
    }

    fun disableLock() {
        prefs.edit()
            .clear()
            .apply()
    }

    private fun hashPin(pin: String, salt: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        return Base64.getEncoder().encodeToString(digest.digest(pin.toByteArray()))
    }
}
