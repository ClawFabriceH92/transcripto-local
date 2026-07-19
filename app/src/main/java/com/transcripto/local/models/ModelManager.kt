package com.transcripto.local.models

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.StatFs
import android.util.Log
import com.transcripto.local.data.AppLogger
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest

/**
 * Manages model discovery, extraction from APK assets, caching, and lifecycle.
 *
 * Models are bundled in the APK under assets/models/ and extracted
 * to internal storage on first run.
 */
class ModelManager(private val context: Context) {

    private val tag = "ModelManager"

    /** Directory where extracted models are stored. */
    val modelsDir: File
        get() = File(context.filesDir, "models").also { it.mkdirs() }

    /**
     * Detect available RAM and return the best-matching profile.
     */
    fun detectProfile(): ModelProfile {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)

        val totalMemMb = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            memInfo.totalMem / (1024L * 1024L)
        } else {
            am.memoryClass.toLong()
        }

        Log.d(tag, "Detected ~${totalMemMb}MB available RAM")

        return ModelProfiles.entries
            .firstOrNull { it.profile.fits(totalMemMb.toInt()) }
            ?.profile
            ?: ModelProfiles.ULTRA_LIGHT.profile
    }

    /**
     * Check whether both models for the given profile are already extracted.
     */
    fun areModelsReady(profile: ModelProfile): Boolean {
        val sttReady = getSttModelFile(profile).exists()
        val llmReady = getLlmModelFile(profile).exists()
        AppLogger.i("areModelsReady: STT=$sttReady (${getSttModelFile(profile).absolutePath}), LLM=$llmReady (${getLlmModelFile(profile).absolutePath})")
        return sttReady && llmReady
    }

    /**
     * Return the expected local file for the STT model.
     */
    fun getSttModelFile(profile: ModelProfile): File {
        return File(modelsDir, profile.sttFile)
    }

    /**
     * Return the expected local file for the LLM model.
     */
    fun getLlmModelFile(profile: ModelProfile): File {
        return File(modelsDir, profile.llmFile)
    }

    /**
     * Extract a bundled model from APK assets to internal storage.
     *
     * @param assetPath Path inside assets/models/ (e.g. "ggml-tiny.bin").
     * @param outputFile Destination file.
     * @param progress  Callback with (bytesCopied, totalBytes).
     * @return The output [File], or null on failure.
     */
    fun extractModel(
        assetPath: String,
        outputFile: File,
        progress: ((Long, Long) -> Unit)? = null,
    ): File? {
        if (outputFile.exists()) {
            AppLogger.i("extractModel: $assetPath déjà extrait → skip")
            return outputFile
        }

        return try {
            AppLogger.i("extractModel: début extraction $assetPath (${outputFile.absolutePath})")
            val fullAssetPath = "models/$assetPath"
            val inputStream: InputStream = context.assets.open(fullAssetPath)
            val totalBytes = inputStream.available().toLong()
            AppLogger.i("extractModel: $assetPath → ${totalBytes / (1024*1024)} Mo à extraire")
            val outputStream = FileOutputStream(outputFile)

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalRead: Long = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalRead += bytesRead
                if (totalBytes > 0) {
                    progress?.invoke(totalRead, totalBytes)
                }
            }

            outputStream.close()
            inputStream.close()

            AppLogger.i("extractModel: $assetPath extrait avec succès ($totalRead bytes)")
            outputFile
        } catch (e: Exception) {
            AppLogger.e("extractModel: échec $assetPath → ${e.message}")
            Log.e(tag, "Failed to extract model $assetPath", e)
            outputFile.delete()
            null
        }
    }

    /**
     * Verify a file's SHA-256 hash.
     */
    fun verifyHash(file: File, expectedHex: String): Boolean {
        if (expectedHex.isBlank()) {
            Log.w(tag, "No expected hash provided for ${file.name}, skipping verification")
            return true
        }
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            val actualHex = digest.digest().joinToString("") { "%02x".format(it) }
            val match = actualHex.equals(expectedHex, ignoreCase = true)
            if (!match) {
                Log.e(tag, "Hash mismatch for ${file.name}: expected=$expectedHex, actual=$actualHex")
            }
            match
        } catch (e: Exception) {
            Log.e(tag, "Hash verification failed for ${file.name}", e)
            false
        }
    }

    /**
     * Check available storage space.
     */
    fun getAvailableStorageMb(): Long {
        return try {
            val path = modelsDir
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                val stat = StatFs(path.absolutePath)
                stat.availableBlocksLong * stat.blockSizeLong / (1024L * 1024L)
            } else {
                @Suppress("DEPRECATION")
                val stat = StatFs(path.absolutePath)
                (stat.availableBlocks.toLong() * stat.blockSize.toLong()) / (1024L * 1024L)
            }
        } catch (e: Exception) {
            -1L
        }
    }

    /**
     * Delete both models for the given profile to free space.
     */
    fun unloadModels(profile: ModelProfile) {
        getSttModelFile(profile).delete()
        getLlmModelFile(profile).delete()
        Log.d(tag, "Models unloaded for profile ${profile.label}")
    }
}
