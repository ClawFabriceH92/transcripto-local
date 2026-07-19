package com.transcripto.local.stt

import android.util.Log
import com.transcripto.local.data.AppLogger
import java.io.File

/**
 * A single time-stamped segment produced by the STT engine.
 */
data class SttSegment(
    val startMs: Long,
    val endMs: Long,
    val text: String,
)

/**
 * Full transcription result.
 */
data class TranscriptionResult(
    val fullText: String,
    val segments: List<SttSegment>,
    val language: String = "fr",
    val durationMs: Long = 0L,
)

/**
 * Result wrapper for STT operations.
 */
sealed class SttResult {
    data class Success(val transcription: TranscriptionResult) : SttResult()
    data class Error(val message: String, val exception: Throwable? = null) : SttResult()
}

/**
 * Interface for local Speech-To-Text engine.
 */
interface SttEngine {
    fun transcribe(
        audioFile: File,
        language: String = "auto",
        options: Map<String, Any> = emptyMap(),
    ): SttResult

    fun loadModel(modelPath: String): Result<Unit>
    fun unloadModel(): Result<Unit>
    val isLoaded: Boolean
}

/**
 * Implementation bridging to whisper.cpp through JNI.
 *
 * Native library expected: libwhisper.so
 */
class WhisperSttEngine : SttEngine {

    private var nativeHandle: Long = 0L

    override var isLoaded: Boolean = false
        private set

    override fun transcribe(
        audioFile: File,
        language: String,
        options: Map<String, Any>,
    ): SttResult {
        if (!isLoaded) {
            return SttResult.Error("Mod\u00e8le non charg\u00e9. Appelez loadModel() d'abord.")
        }
        if (!audioFile.exists()) {
            return SttResult.Error("Fichier audio introuvable : ${audioFile.absolutePath}")
        }
        return try {
            AppLogger.i("Whisper: d\u00e9but transcription de ${audioFile.name}")
            val resultJson = nativeTranscribe(nativeHandle, audioFile.absolutePath, language)
            AppLogger.i("Whisper: transcription termin\u00e9e")
            parseTranscriptionResult(resultJson)
        } catch (e: Exception) {
            AppLogger.e("Whisper: erreur transcription : ${e.message}")
            SttResult.Error("Transcription failed: ${e.message}", e)
        }
    }

    override fun loadModel(modelPath: String): Result<Unit> {
        return try {
            AppLogger.i("Whisper: chargement du mod\u00e8le $modelPath")
            nativeHandle = nativeLoadModel(modelPath)
            isLoaded = true
            AppLogger.i("Whisper: mod\u00e8le charg\u00e9 (handle=$nativeHandle)")
            Result.success(Unit)
        } catch (e: Exception) {
            isLoaded = false
            AppLogger.e("Whisper: \u00e9chec chargement mod\u00e8le : ${e.message}")
            Result.failure(IllegalStateException("Failed to load model: ${e.message}", e))
        }
    }

    override fun unloadModel(): Result<Unit> {
        return try {
            if (nativeHandle != 0L) {
                nativeUnloadModel(nativeHandle)
                nativeHandle = 0L
            }
            isLoaded = false
            AppLogger.i("Whisper: mod\u00e8le d\u00e9charg\u00e9")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Failed to unload model: ${e.message}", e))
        }
    }

    // ---- JNI stubs (native in libwhisper.so) ----
    private external fun nativeLoadModel(modelPath: String): Long
    private external fun nativeTranscribe(handle: Long, audioPath: String, language: String): String
    private external fun nativeUnloadModel(handle: Long)

    // ---- JSON parsing ----
    private fun parseTranscriptionResult(json: String): SttResult {
        return try {
            // Parser simple sans dépendance
            val fullText = extractJsonString(json, "full_text") ?: ""
            val language = extractJsonString(json, "language") ?: "fr"

            val segments = mutableListOf<SttSegment>()
            val segStart = json.indexOf("\"segments\"")
            if (segStart >= 0) {
                val arrStart = json.indexOf('[', segStart)
                val arrEnd = json.lastIndexOf(']')
                if (arrStart >= 0 && arrEnd > arrStart) {
                    val arrContent = json.substring(arrStart + 1, arrEnd)
                    var pos = 0
                    while (true) {
                        val objStart = arrContent.indexOf('{', pos)
                        if (objStart < 0) break
                        val objEnd = arrContent.indexOf('}', objStart)
                        if (objEnd < 0) break
                        val obj = arrContent.substring(objStart, objEnd + 1)
                        val sMs = extractJsonLong(obj, "start_ms") ?: 0L
                        val eMs = extractJsonLong(obj, "end_ms") ?: 0L
                        val txt = extractJsonString(obj, "text") ?: ""
                        segments.add(SttSegment(sMs, eMs, txt))
                        pos = objEnd + 1
                    }
                }
            }

            SttResult.Success(
                TranscriptionResult(
                    fullText = fullText,
                    segments = segments,
                    language = language,
                )
            )
        } catch (e: Exception) {
            AppLogger.e("Whisper: erreur parsing JSON : ${e.message}")
            SttResult.Error("Erreur de parsing: ${e.message}", e)
        }
    }

    private fun extractJsonString(json: String, key: String): String? {
        val search = "\"$key\""
        val idx = json.indexOf(search)
        if (idx < 0) return null
        val colon = json.indexOf(':', idx + search.length)
        if (colon < 0) return null
        val quoteStart = json.indexOf('"', colon + 1)
        if (quoteStart < 0) return null
        val quoteEnd = json.indexOf('"', quoteStart + 1)
        if (quoteEnd < 0) return null
        return json.substring(quoteStart + 1, quoteEnd)
    }

    private fun extractJsonLong(json: String, key: String): Long? {
        val search = "\"$key\""
        val idx = json.indexOf(search)
        if (idx < 0) return null
        val colon = json.indexOf(':', idx + search.length)
        if (colon < 0) return null
        val end = json.indexOfAny(charArrayOf(',', '}', ']'), colon + 1)
        if (end < 0) return null
        val numStr = json.substring(colon + 1, end).trim()
        return numStr.toLongOrNull()
    }

    companion object {
        init {
            try {
                System.loadLibrary("whisper")
                AppLogger.i("Whisper: libwhisper.so charg\u00e9e avec succ\u00e8s")
            } catch (e: UnsatisfiedLinkError) {
                AppLogger.e("Whisper: impossible de charger libwhisper.so : ${e.message}")
            }
        }
    }
}
