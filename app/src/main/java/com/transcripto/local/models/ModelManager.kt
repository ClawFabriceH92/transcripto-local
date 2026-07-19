package com.transcripto.local.models

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import android.util.Log
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Manages model discovery, download, caching, and lifecycle.
 *
 * Uses [ActivityManager.MemoryInfo] to detect available RAM and selects
 * the best-matching [ModelProfiles] entry.
 */
class ModelManager(private val context: Context) {

    private val tag = "ModelManager"

    /** Directory where downloaded models are stored. */
    private val modelsDir: File
        get() = File(context.filesDir, "models").also { it.mkdirs() }

    /**
     * Detect available RAM and return the best-matching profile.
     */
    fun detectProfile(): ModelProfile {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)

        // Use totalMem as the best heuristic for capacity; fall back to
        // memClass (the per-app limit) on older API levels.
        val totalMemMb = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            memInfo.totalMem / (1024L * 1024L)
        } else {
            // Approximate: memClass in MB is already the per-app limit
            am.memoryClass.toLong()
        }

        Log.d(tag, "Detected ~${totalMemMb}MB available RAM")

        // Pick the first profile whose ramMin the device satisfies
        return ModelProfiles.entries
            .firstOrNull { it.profile.fits(totalMemMb.toInt()) }
            ?.profile
            ?: ModelProfiles.ULTRA_LIGHT.profile
    }

    /**
     * Check whether both models for the given profile are already downloaded.
     */
    fun areModelsReady(profile: ModelProfile): Boolean {
        return getSttModelFile(profile).exists() && getLlmModelFile(profile).exists()
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
     * Download a model from the remote repository.
     *
     * @param modelName The model file name (e.g. "ggml-tiny.bin").
     * @param remoteUrl Full download URL.
     * @param progress  Callback with (bytesDownloaded, totalBytes).
     * @return The downloaded [File], or null on failure.
     */
    fun downloadModel(
        modelName: String,
        remoteUrl: String,
        progress: ((Long, Long) -> Unit)? = null,
    ): File? {
        val outputFile = File(modelsDir, modelName)
        if (outputFile.exists()) {
            Log.d(tag, "Model $modelName already exists, skipping download")
            return outputFile
        }

        return try {
            val url = URL(remoteUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 15_000
            connection.readTimeout = 120_000
            connection.connect()

            val responseCode = connection.responseCode
            Log.d(tag, "HTTP $responseCode for $remoteUrl")
            if (responseCode != 200 && responseCode != 206) {
                Log.e(tag, "HTTP $responseCode for $remoteUrl")
                connection.disconnect()
                return null
            }

            val totalBytes = connection.contentLength.toLong()
            val inputStream: InputStream = connection.inputStream
            val outputStream = FileOutputStream(outputFile)

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalRead: Long = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalRead += bytesRead
                progress?.invoke(totalRead, totalBytes)
            }

            outputStream.close()
            inputStream.close()
            connection.disconnect()

            Log.d(tag, "Downloaded model $modelName ($totalRead bytes)")
            outputFile
        } catch (e: Exception) {
            Log.e(tag, "Failed to download model $modelName", e)
            outputFile.delete()
            null
        }
    }

    /**
     * Verify a file's SHA-256 hash.
     *
     * @param file   The file to check.
     * @param expectedHex The expected hex-encoded SHA-256 digest.
     * @return true if the digest matches (or [expectedHex] is empty).
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
     *
     * @return Available space in MB, or -1 if unknown.
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
