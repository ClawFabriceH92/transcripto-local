package com.transcripto.local.models

/**
 * Hardware profile for model selection.
 *
 * Maps a device's available RAM to the appropriate STT and LLM model sizes,
 * following the correspondence table (cf. CDC §3.2).
 *
 * @property ramMin  Minimum RAM in MB for this profile (inclusive).
 * @property ramMax  Maximum RAM in MB for this profile (inclusive, -1 = unlimited).
 * @property sttModel Identifier of the recommended STT model (e.g. "ggml-tiny", "ggml-base").
 * @property sttSize  Approximate download size of the STT model in MB.
 * @property llmModel Identifier of the recommended LLM model (e.g. "qwen2-0.5b-q4", "qwen2-1.5b-q4").
 * @property llmSize  Approximate download size of the LLM model in MB.
 * @property label    Human-readable label for the profile.
 */
data class ModelProfile(
    val ramMin: Int,
    val ramMax: Int,
    val sttModel: String,
    val sttFile: String,
    val sttRepo: String,
    val sttSize: Int,
    val llmModel: String,
    val llmFile: String,
    val llmRepo: String,
    val llmSize: Int,
    val label: String,
) {
    /**
     * Whether this profile fits within the given available RAM (in MB).
     */
    fun fits(availableRamMb: Int): Boolean {
        return availableRamMb >= ramMin && (ramMax == -1 || availableRamMb <= ramMax)
    }

    companion object {
        /** Total estimated RAM footprint when both models are loaded. */
        fun totalFootprint(profile: ModelProfile): Int {
            return profile.sttSize + profile.llmSize
        }
    }
}

/**
 * Pre-defined profiles covering devices from 2 GB to 12+ GB RAM.
 *
 * Order matters — iterate from smallest to largest and pick the first
 * that fits, so a device gets the most capable profile it can handle.
 */
enum class ModelProfiles(val profile: ModelProfile) {
    // 2 GB devices — very constrained
    ULTRA_LIGHT(
        ModelProfile(
            ramMin = 1500,
            ramMax = 2500,
            sttModel = "Whisper Tiny",
            sttFile = "ggml-tiny.bin",
            sttRepo = "ggerganov/whisper.cpp",
            sttSize = 75,
            llmModel = "Qwen2.5 0.5B",
            llmFile = "Qwen2.5-0.5B-Instruct-Q4_K_M.gguf",
            llmRepo = "Qwen/Qwen2.5-0.5B-Instruct-GGUF",
            llmSize = 400,
            label = "Ultra-léger (2 Go RAM)",
        )
    ),

    // 3 GB devices
    LIGHT(
        ModelProfile(
            ramMin = 2500,
            ramMax = 3500,
            sttModel = "Whisper Base",
            sttFile = "ggml-base.bin",
            sttRepo = "ggerganov/whisper.cpp",
            sttSize = 140,
            llmModel = "Qwen2.5 0.5B",
            llmFile = "Qwen2.5-0.5B-Instruct-Q4_K_M.gguf",
            llmRepo = "Qwen/Qwen2.5-0.5B-Instruct-GGUF",
            llmSize = 400,
            label = "Léger (3 Go RAM)",
        )
    ),

    // 4 GB devices
    STANDARD(
        ModelProfile(
            ramMin = 3500,
            ramMax = 4500,
            sttModel = "Whisper Small",
            sttFile = "ggml-small.bin",
            sttRepo = "ggerganov/whisper.cpp",
            sttSize = 460,
            llmModel = "Qwen2.5 1.5B",
            llmFile = "Qwen2.5-1.5B-Instruct-Q4_K_M.gguf",
            llmRepo = "Qwen/Qwen2.5-1.5B-Instruct-GGUF",
            llmSize = 950,
            label = "Standard (4 Go RAM)",
        )
    ),

    // 6 GB devices
    MEDIUM(
        ModelProfile(
            ramMin = 4500,
            ramMax = 6500,
            sttModel = "Whisper Medium",
            sttFile = "ggml-medium.bin",
            sttRepo = "ggerganov/whisper.cpp",
            sttSize = 770,
            llmModel = "Qwen2.5 1.5B",
            llmFile = "Qwen2.5-1.5B-Instruct-Q4_K_M.gguf",
            llmRepo = "Qwen/Qwen2.5-1.5B-Instruct-GGUF",
            llmSize = 950,
            label = "Moyen (6 Go RAM)",
        )
    ),

    // 8 GB devices
    HIGH(
        ModelProfile(
            ramMin = 6500,
            ramMax = 8500,
            sttModel = "Whisper Large V3",
            sttFile = "ggml-large-v3.bin",
            sttRepo = "ggerganov/whisper.cpp",
            sttSize = 1550,
            llmModel = "Qwen2.5 7B",
            llmFile = "Qwen2.5-7B-Instruct-Q4_K_M.gguf",
            llmRepo = "Qwen/Qwen2.5-7B-Instruct-GGUF",
            llmSize = 4300,
            label = "Performant (8 Go RAM)",
        )
    ),

    // 12+ GB devices
    ULTRA(
        ModelProfile(
            ramMin = 8500,
            ramMax = -1,
            sttModel = "Whisper Large V3",
            sttFile = "ggml-large-v3.bin",
            sttRepo = "ggerganov/whisper.cpp",
            sttSize = 1550,
            llmModel = "Qwen2.5 7B",
            llmFile = "Qwen2.5-7B-Instruct-Q4_K_M.gguf",
            llmRepo = "Qwen/Qwen2.5-7B-Instruct-GGUF",
            llmSize = 4300,
            label = "Ultra (12 Go+ RAM)",
        )
    ),
}
