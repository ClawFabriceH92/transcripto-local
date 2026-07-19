package com.transcripto.local.llm

import com.transcripto.local.data.AppLogger

/**
 * Types of analysis prompts the LLM can process.
 */
enum class PromptType {
    SUMMARY,
    KEY_POINTS,
    ACTIONS,
    FREE_QUERY,
}

/**
 * Result of an LLM analysis operation.
 */
sealed class LlmResult {
    data class Success(val text: String) : LlmResult()
    data class Error(val message: String, val exception: Throwable? = null) : LlmResult()
}

/**
 * Configuration for an LLM query.
 */
data class LlmQuery(
    val transcription: String,
    val promptType: PromptType,
    val freeQuery: String = "",
    val systemPrompt: String? = null,
)

/**
 * Interface for local LLM engine.
 */
interface LlmEngine {
    fun analyze(query: LlmQuery): LlmResult
    fun loadModel(modelPath: String): Result<Unit>
    fun unloadModel(): Result<Unit>
    val isLoaded: Boolean
    var parameters: InferenceParams
}

/**
 * Inference / generation parameters for the LLM.
 */
data class InferenceParams(
    val maxTokens: Int = 512,
    val temperature: Float = 0.3f,
    val topK: Int = 40,
    val topP: Float = 0.9f,
    val repeatPenalty: Float = 1.1f,
)

/**
 * Implementation bridging to llama.cpp through JNI.
 *
 * Native library expected: libllama.so
 */
class LlamaLlmEngine : LlmEngine {

    private var nativeHandle: Long = 0L

    override var isLoaded: Boolean = false
        private set

    override var parameters: InferenceParams = InferenceParams()

    override fun analyze(query: LlmQuery): LlmResult {
        if (!isLoaded) {
            return LlmResult.Error("Mod\u00e8le non charg\u00e9. Appelez loadModel() d'abord.")
        }
        return try {
            AppLogger.i("LLM: d\u00e9but analyse (${query.promptType})")
            val prompt = buildPrompt(query)
            val resultJson = nativeGenerate(
                handle = nativeHandle,
                prompt = prompt,
            )
            AppLogger.i("LLM: analyse termin\u00e9e")
            LlmResult.Success(resultJson)
        } catch (e: Exception) {
            AppLogger.e("LLM: erreur : ${e.message}")
            LlmResult.Error("Inference failed: ${e.message}", e)
        }
    }

    override fun loadModel(modelPath: String): Result<Unit> {
        return try {
            AppLogger.i("LLM: chargement du mod\u00e8le $modelPath")
            nativeHandle = nativeLoadModel(modelPath)
            isLoaded = true
            AppLogger.i("LLM: mod\u00e8le charg\u00e9 (handle=$nativeHandle)")
            Result.success(Unit)
        } catch (e: Exception) {
            isLoaded = false
            AppLogger.e("LLM: \u00e9chec chargement : ${e.message}")
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
            AppLogger.i("LLM: mod\u00e8le d\u00e9charg\u00e9")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Failed to unload model: ${e.message}", e))
        }
    }

    // ---- Prompt building ----

    private fun buildPrompt(query: LlmQuery): String {
        val system = query.systemPrompt ?: defaultSystemPrompt(query.promptType)
        val user = when (query.promptType) {
            PromptType.FREE_QUERY -> buildFreeQueryPrompt(query.transcription, query.freeQuery)
            else -> "${system.trimEnd()}\n\nTranscription :\n${query.transcription}"
        }
        // ChatML-style template
        return "<|im_start|>system\n$system<|im_end|>\n<|im_start|>user\n$user<|im_end|>\n<|im_start|>assistant\n"
    }

    private fun defaultSystemPrompt(type: PromptType): String = when (type) {
        PromptType.SUMMARY -> """Tu es un assistant spécialisé dans le résumé de transcriptions audio. Produis un résumé concis et structuré en français. Ne fais pas référence à la transcription elle-même, réponds directement."""
        PromptType.KEY_POINTS -> """Tu es un assistant qui extrait les points clés d'une transcription. Produis une liste à puces des idées principales en français."""
        PromptType.ACTIONS -> """Tu es un assistant qui identifie les actions et prochaines étapes. Liste chaque action avec un responsable implicite si identifiable."""
        PromptType.FREE_QUERY -> """Tu es un assistant utile. Réponds en français à la question posée en te basant sur la transcription fournie."""
    }

    private fun buildFreeQueryPrompt(transcription: String, query: String): String {
        return "Transcription :\n$transcription\n\nQuestion : $query"
    }

    // ---- JNI stubs (native in libllama.so) ----
    private external fun nativeLoadModel(modelPath: String): Long
    private external fun nativeGenerate(handle: Long, prompt: String): String
    private external fun nativeUnloadModel(handle: Long)

    companion object {
        init {
            try {
                System.loadLibrary("llama")
                AppLogger.i("LLM: libllama.so charg\u00e9e avec succ\u00e8s")
            } catch (e: UnsatisfiedLinkError) {
                AppLogger.e("LLM: impossible de charger libllama.so : ${e.message}")
            }
        }
    }
}
