package com.transcripto.local.llm

/**
 * Types of analysis prompts the LLM can process.
 */
enum class PromptType {
    /** Generate a concise summary of the transcription. */
    SUMMARY,

    /** Extract key points / bullet items. */
    KEY_POINTS,

    /** Identify action items and next steps. */
    ACTIONS,

    /** Free-form question / response about the content. */
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
    /** The transcription text (context). */
    val transcription: String,
    /** Type of prompt to build. */
    val promptType: PromptType,
    /** Optional free-form question (used with FREE_QUERY). */
    val freeQuery: String = "",
    /** System prompt overrides (optional). */
    val systemPrompt: String? = null,
)

/**
 * Interface for local LLM engine.
 *
 * The concrete implementation bridges to llama.cpp via JNI.
 */
interface LlmEngine {

    /**
     * Run an analysis on the given transcription.
     *
     * @param query The transcription and prompt configuration.
     * @return LlmResult with the generated text or an error.
     */
    fun analyze(query: LlmQuery): LlmResult

    /**
     * Load the GGUF model from the given path.
     */
    fun loadModel(modelPath: String): Result<Unit>

    /**
     * Unload the current model from memory.
     */
    fun unloadModel(): Result<Unit>

    /** Whether a model is loaded and ready. */
    val isLoaded: Boolean

    /** Current inference parameters. */
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
 * Placeholder implementation bridging to llama.cpp via JNI.
 *
 * Native library expected: libllama_jni.so
 * JNI class: com.transcripto.local.llm.LlamaNative
 */
class LlamaLlmEngine : LlmEngine {

    private var nativeHandle: Long = 0L

    override var isLoaded: Boolean = false
        private set

    override var parameters: InferenceParams = InferenceParams()

    override fun analyze(query: LlmQuery): LlmResult {
        if (!isLoaded) {
            return LlmResult.Error("Model not loaded. Call loadModel() first.")
        }
        return try {
            val prompt = buildPrompt(query)
            val resultJson = nativeInference(
                handle = nativeHandle,
                prompt = prompt,
                maxTokens = parameters.maxTokens,
                temperature = parameters.temperature,
                topK = parameters.topK,
                topP = parameters.topP,
                repeatPenalty = parameters.repeatPenalty,
            )
            parseInferenceResult(resultJson)
        } catch (e: Exception) {
            LlmResult.Error("Inference failed: ${e.message}", e)
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

    // ---- Prompt building ----

    private fun buildPrompt(query: LlmQuery): String {
        val system = query.systemPrompt ?: defaultSystemPrompt(query.promptType)
        val user = when (query.promptType) {
            PromptType.FREE_QUERY -> buildFreeQueryPrompt(query.transcription, query.freeQuery)
            else -> "${system.trimEnd()}\n\nTranscription :\n${query.transcription}"
        }
        // ChatML-style template (adjust to match the model's expected format)
        return "<|im_start|>system\n$system<|im_end|>\n<|im_start|>user\n$user<|im_end|>\n<|im_start|>assistant\n"
    }

    private fun defaultSystemPrompt(type: PromptType): String = when (type) {
        PromptType.SUMMARY -> """
            Tu es un assistant spécialisé dans le résumé de transcriptions audio.
            Produis un résumé concis et structuré en français.
            Ne fais pas référence à la transcription elle-même, réponds directement.
        """.trimIndent()
        PromptType.KEY_POINTS -> """
            Tu es un assistant qui extrait les points clés d'une transcription.
            Produis une liste à puces des idées principales en français.
        """.trimIndent()
        PromptType.ACTIONS -> """
            Tu es un assistant qui identifie les actions et prochaines étapes.
            Liste chaque action avec un responsable implicite si identifiable.
        """.trimIndent()
        PromptType.FREE_QUERY -> """
            Tu es un assistant utile. Réponds en français à la question posée
            en te basant sur la transcription fournie.
        """.trimIndent()
    }

    private fun buildFreeQueryPrompt(transcription: String, query: String): String {
        return "Transcription :\n$transcription\n\nQuestion : $query"
    }

    // ---- JNI stubs (native implementation in libllama_jni.so) ----

    private external fun nativeLoadModel(modelPath: String): Long
    private external fun nativeInference(
        handle: Long,
        prompt: String,
        maxTokens: Int,
        temperature: Float,
        topK: Int,
        topP: Float,
        repeatPenalty: Float,
    ): String
    private external fun nativeUnloadModel(handle: Long)

    // ---- JSON result parsing ----

    private fun parseInferenceResult(json: String): LlmResult {
        // TODO: parse {"text": "...", "tokens_used": N, "stop_reason": "..."}
        return LlmResult.Error("Inference result parsing not yet implemented.")
    }

    companion object {
        init {
            try {
                System.loadLibrary("llama_jni")
            } catch (e: UnsatisfiedLinkError) {
                // Native library not yet bundled
            }
        }
    }
}
