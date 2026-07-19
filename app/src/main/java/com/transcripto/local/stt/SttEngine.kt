package com.transcripto.local.stt

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
 *
 * The concrete implementation bridges to whisper.cpp via JNI.
 */
interface SttEngine {

    /**
     * Transcribe the given audio file and return segments with timing.
     *
     * @param audioFile PCM/WAV audio file sampled at 16 kHz mono.
     * @param language  ISO-639-1 language code (e.g. "fr", "en", "auto").
     * @param options   Optional engine-specific flags.
     */
    fun transcribe(
        audioFile: File,
        language: String = "auto",
        options: Map<String, Any> = emptyMap(),
    ): SttResult

    /**
     * Load (or pre-load) the model into memory.
     * @param modelPath Absolute path to the whisper.cpp GGML model file.
     */
    fun loadModel(modelPath: String): Result<Unit>

    /**
     * Unload the current model from memory to free RAM.
     */
    fun unloadModel(): Result<Unit>

    /**
     * Whether a model is currently loaded and ready.
     */
    val isLoaded: Boolean
}

/**
 * Placeholder implementation that bridges to whisper.cpp through JNI.
 *
 * Native library expected: libwhisper_jni.so
 * JNI class: com.transcripto.local.stt.WhisperNative
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
            return SttResult.Error("Model not loaded. Call loadModel() first.")
        }
        if (!audioFile.exists()) {
            return SttResult.Error("Audio file not found: ${audioFile.absolutePath}")
        }
        return try {
            val resultJson = nativeTranscribe(nativeHandle, audioFile.absolutePath, language)
            parseTranscriptionResult(resultJson)
        } catch (e: Exception) {
            SttResult.Error("Transcription failed: ${e.message}", e)
        }
    }

    override fun loadModel(modelPath: String): Result<Unit> {
        return try {
            nativeHandle = nativeLoadModel(modelPath)
            isLoaded = true
            Result.success(Unit)
        } catch (e: Exception) {
            isLoaded = false
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
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Failed to unload model: ${e.message}", e))
        }
    }

    // ---- JNI stubs (native implementation in libwhisper_jni.so) ----

    private external fun nativeLoadModel(modelPath: String): Long
    private external fun nativeTranscribe(handle: Long, audioPath: String, language: String): String
    private external fun nativeUnloadModel(handle: Long)

    // ---- JSON parsing (simple — consider using org.json or kotlinx.serialization) ----

    private fun parseTranscriptionResult(json: String): SttResult {
        // TODO: parse JSON response from native layer.
        // Expected format:
        // {
        //   "full_text": "...",
        //   "language": "fr",
        //   "duration_ms": 12345,
        //   "segments": [
        //     {"start_ms": 0, "end_ms": 1000, "text": "Bonjour"}
        //   ]
        // }
        return SttResult.Error("Transcription result parsing not yet implemented.")
    }

    companion object {
        init {
            try {
                System.loadLibrary("whisper_jni")
            } catch (e: UnsatisfiedLinkError) {
                // Native library not yet bundled — operations will return errors
            }
        }
    }
}
