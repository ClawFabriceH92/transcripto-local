#include <jni.h>
#include <string>
#include <llama.h>

extern "C" {

static llama_model *g_model = nullptr;
static llama_context *g_ctx = nullptr;

JNIEXPORT jlong JNICALL
Java_com_transcripto_local_llm_LlamaLlmEngine_nativeLoadModel(
    JNIEnv *env, jobject /*thiz*/, jstring model_path) {

    const char *path = env->GetStringUTFChars(model_path, nullptr);

    if (g_model) {
        llama_free_model(g_model);
        g_model = nullptr;
    }
    if (g_ctx) {
        llama_free(g_ctx);
        g_ctx = nullptr;
    }

    llama_model_params model_params = llama_model_default_params();
    g_model = llama_load_model_from_file(path, model_params);
    env->ReleaseStringUTFChars(model_path, path);

    if (!g_model) return 0;

    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = 2048;
    ctx_params.n_threads = 4;
    g_ctx = llama_new_context_with_model(g_model, ctx_params);

    return reinterpret_cast<jlong>(g_ctx);
}

JNIEXPORT jstring JNICALL
Java_com_transcripto_local_llm_LlamaLlmEngine_nativeGenerate(
    JNIEnv *env, jobject /*thiz*/, jlong handle, jstring prompt) {

    if (!handle || !g_model) {
        return env->NewStringUTF("{\"error\":\"model not loaded\"}");
    }

    const char *prompt_str = env->GetStringUTFChars(prompt, nullptr);

    // Tokenisation
    int n_tokens = llama_tokenize(g_model, prompt_str, strlen(prompt_str), nullptr, 0, true, false);
    std::vector<llama_token> tokens(n_tokens);
    llama_tokenize(g_model, prompt_str, strlen(prompt_str), tokens.data(), n_tokens, true, false);
    env->ReleaseStringUTFChars(prompt, prompt_str);

    // Génération
    std::string result;
    int max_tokens = 512;

    for (int i = 0; i < max_tokens; i++) {
        if (llama_decode(g_ctx, llama_batch_get_one(tokens.data(), tokens.size(), 0, 0))) {
            break;
        }

        llama_token new_token = llama_sample_token_greedy(g_ctx, nullptr);

        if (new_token == llama_token_eos(g_model)) break;

        char buf[8];
        int n = llama_token_to_piece(g_model, new_token, buf, sizeof(buf), 0, false);
        if (n > 0) {
            buf[n] = '\0';
            result += buf;
        }

        tokens = {new_token};
    }

    return env->NewStringUTF(result.c_str());
}

JNIEXPORT void JNICALL
Java_com_transcripto_local_llm_LlamaLlmEngine_nativeUnloadModel(
    JNIEnv * /*env*/, jobject /*thiz*/, jlong handle) {

    if (g_ctx) {
        llama_free(g_ctx);
        g_ctx = nullptr;
    }
    if (g_model) {
        llama_free_model(g_model);
        g_model = nullptr;
    }
}

} // extern "C"
