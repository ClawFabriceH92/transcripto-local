#include <jni.h>
#include <string>
#include <whisper.h>

extern "C" {

static struct whisper_context *g_ctx = nullptr;

JNIEXPORT jlong JNICALL
Java_com_transcripto_local_stt_WhisperSttEngine_nativeLoadModel(
    JNIEnv *env, jobject /*thiz*/, jstring model_path) {

    const char *path = env->GetStringUTFChars(model_path, nullptr);
    if (g_ctx) {
        whisper_free(g_ctx);
        g_ctx = nullptr;
    }

    struct whisper_context_params cparams = whisper_context_default_params();
    g_ctx = whisper_init_from_file_with_params(path, cparams);
    env->ReleaseStringUTFChars(model_path, path);

    return reinterpret_cast<jlong>(g_ctx);
}

JNIEXPORT jstring JNICALL
Java_com_transcripto_local_stt_WhisperSttEngine_nativeTranscribe(
    JNIEnv *env, jobject /*thiz*/, jlong handle, jstring audio_path, jstring language) {

    if (!handle) return env->NewStringUTF("{\"error\":\"model not loaded\"}");

    auto *ctx = reinterpret_cast<whisper_context *>(handle);
    const char *path = env->GetStringUTFChars(audio_path, nullptr);
    const char *lang = env->GetStringUTFChars(language, nullptr);

    // Paramètres par défaut
    auto wparams = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    wparams.print_realtime = false;
    wparams.print_progress = false;
    wparams.print_timestamps = false;
    wparams.print_special = false;
    wparams.translate = false;
    wparams.language = lang;
    wparams.n_threads = 4;

    std::string result;
    int ret = whisper_full(ctx, wparams, path, nullptr, nullptr);
    if (ret != 0) {
        result = "{\"error\":\"whisper_full failed\"}";
    } else {
        int n_segments = whisper_full_n_segments(ctx);
        result = "{\"full_text\":\"";
        for (int i = 0; i < n_segments; i++) {
            const char *text = whisper_full_get_segment_text(ctx, i);
            result += text;
        }
        result += "\",\"segments\":[";
        for (int i = 0; i < n_segments; i++) {
            if (i > 0) result += ",";
            int64_t t0 = whisper_full_get_segment_t0(ctx, i);
            int64_t t1 = whisper_full_get_segment_t1(ctx, i);
            const char *text = whisper_full_get_segment_text(ctx, i);
            result += "{\"start_ms\":" + std::to_string(t0 * 10) + ",";
            result += "\"end_ms\":" + std::to_string(t1 * 10) + ",";
            result += "\"text\":\"" + std::string(text) + "\"}";
        }
        result += "],\"language\":\"";
        result += lang;
        result += "\",\"duration_ms\":0}";
    }

    env->ReleaseStringUTFChars(audio_path, path);
    env->ReleaseStringUTFChars(language, lang);

    return env->NewStringUTF(result.c_str());
}

JNIEXPORT void JNICALL
Java_com_transcripto_local_stt_WhisperSttEngine_nativeUnloadModel(
    JNIEnv * /*env*/, jobject /*thiz*/, jlong handle) {

    if (handle) {
        whisper_free(reinterpret_cast<whisper_context *>(handle));
    }
}

} // extern "C"
