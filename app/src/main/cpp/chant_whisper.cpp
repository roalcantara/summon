// JNI bridge to whisper.cpp for the chant check (ADR-0027). One context per process;
// calls are serialised by the Kotlin side.
#include <jni.h>
#include <cstring>
#include <vector>
#include "whisper.h"

namespace {
constexpr int kRate = 16000;
constexpr int kFullAudioCtx = 1500;  // encoder frames for 30 s
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_summondemons_safely_platform_WhisperNative_init(JNIEnv* env, jobject, jstring path) {
    const char* p = env->GetStringUTFChars(path, nullptr);
    whisper_context_params params = whisper_context_default_params();
    params.use_gpu = false;
    whisper_context* ctx = whisper_init_from_file_with_params(p, params);
    env->ReleaseStringUTFChars(path, p);
    return reinterpret_cast<jlong>(ctx);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_summondemons_safely_platform_WhisperNative_transcribe(
        JNIEnv* env, jobject, jlong handle, jfloatArray pcm, jint threads, jboolean shrinkContext) {
    auto* ctx = reinterpret_cast<whisper_context*>(handle);
    if (ctx == nullptr) return -1;
    const jsize n = env->GetArrayLength(pcm);
    std::vector<float> samples(n);
    env->GetFloatArrayRegion(pcm, 0, n, samples.data());

    whisper_full_params p = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    p.language = "la";
    p.detect_language = false;
    p.translate = false;
    p.n_threads = threads;
    p.no_context = true;
    p.no_timestamps = true;
    p.print_progress = false;
    p.print_realtime = false;
    p.print_special = false;
    p.print_timestamps = false;
    p.suppress_blank = true;
    p.temperature = 0.0f;
    p.temperature_inc = 0.0f;
    if (shrinkContext) {
        // 50 encoder frames per second of audio, plus a margin; full context beyond 30 s.
        const int frames = static_cast<int>(static_cast<double>(n) / kRate * 50.0) + 64;
        p.audio_ctx = frames < kFullAudioCtx ? frames : 0;
    }
    if (whisper_full(ctx, p, samples.data(), n) != 0) return -1;
    return whisper_full_n_segments(ctx);
}

// Returned as bytes: Whisper may emit invalid modified-UTF-8, which NewStringUTF would reject.
extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_summondemons_safely_platform_WhisperNative_segmentText(JNIEnv* env, jobject, jlong handle, jint index) {
    auto* ctx = reinterpret_cast<whisper_context*>(handle);
    const char* text = whisper_full_get_segment_text(ctx, index);
    const jsize len = static_cast<jsize>(std::strlen(text));
    jbyteArray out = env->NewByteArray(len);
    env->SetByteArrayRegion(out, 0, len, reinterpret_cast<const jbyte*>(text));
    return out;
}

extern "C" JNIEXPORT jfloat JNICALL
Java_com_summondemons_safely_platform_WhisperNative_segmentNoSpeech(JNIEnv*, jobject, jlong handle, jint index) {
    return whisper_full_get_segment_no_speech_prob(reinterpret_cast<whisper_context*>(handle), index);
}

extern "C" JNIEXPORT void JNICALL
Java_com_summondemons_safely_platform_WhisperNative_free(JNIEnv*, jobject, jlong handle) {
    whisper_free(reinterpret_cast<whisper_context*>(handle));
}
