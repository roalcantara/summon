# On-device Whisper Chant Check Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Seal each chant line when Whisper base (language Latin), running on the phone, hears it; fall back to a lenient rule on devices that can't run it.

**Architecture:** whisper.cpp v1.9.4 is built from source (git submodule) into a small JNI library; a pure `LineMatcher` scores transcripts against the line; a `LineListener` in the ViewModel buffers 16 kHz audio since the line began, checks after pauses and hands `LineHeard` to the still-pure `RitualEngine` as input. The vowel aligner and chunks of ADR-0022 are removed; the old "seal when every syllable lit" rule becomes the lenient mode.

**Tech Stack:** Kotlin, Jetpack Compose, Android NDK + CMake, whisper.cpp (MIT), Whisper base q5_1 weights (MIT), JUnit 4, kotlinx-coroutines-test, AndroidX Test (instrumented spike).

**Spec:** `docs/superpowers/specs/2026-10-18-whisper-chant-check-design.md`. **Evidence:** ADR-0026.

**Branch:** `docs/chant-check-v2`. Paths are relative to the repo root `/Users/roalcantara/Work/mydemon/mydemon`. `MAIN/` = `app/src/main/java/com/summondemons/safely/`, `TEST/` = `app/src/test/java/com/summondemons/safely/`, `ATEST/` = `app/src/androidTest/java/com/summondemons/safely/`.

**Commit trailer:** every commit message ends with a blank line and `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

**Galaxy serial:** `RXCX10467TP` (the emulator `emulator-5554` is usually connected too; always pass `ANDROID_SERIAL=RXCX10467TP` for device work).

---

## Notes decided while planning

1. **whisper.cpp v1.9.4** is pinned (v1.9.5 was under two weeks old). It is added as a CMake subdirectory (static libraries; its tests and examples are off when it is not the top-level project), so the plan does not depend on the release's internal source list.
2. **Release-mode native code in every build type**: whisper.cpp is unusably slow at `-O0`, so CMake gets `-DCMAKE_BUILD_TYPE=Release` even for debug builds.
3. **The model is not committed** (60 MB). `tools/fetch_whisper_model.sh` downloads it with a pinned sha256 into the git-ignored assets folder, and the build fails with that hint if it is missing.
4. **Half a long line passes at 0.60.** "Ostende, mihi, faciem, tuam." scores 0.46 against line II. That is the measured honest-first threshold (ADR-0026) working as intended: it still needs real Latin from the line. `LineMatcherTest` documents it; logged sessions can tighten `LINE_MATCH_MAX` later (Task 9).
5. **Formant analysis stays** in `VoiceAnalyzer` (it feeds the session log); removing it is out of scope.
6. **Lenient mode reuses the engine's existing path**: before this change, a ritual started without the chant check sealed a line when its last syllable lit. That becomes `CheckMode.Lenient`, so most of `RitualEngineTest` keeps working unchanged.

---

## File map

| Path | Action | Responsibility |
|---|---|---|
| `.gitmodules`, `app/src/main/cpp/whisper.cpp` | Create (submodule) | whisper.cpp v1.9.4 |
| `app/src/main/cpp/CMakeLists.txt` | Create | Builds `libchantwhisper.so` (16 KB aligned) |
| `app/src/main/cpp/chant_whisper.cpp` | Create | JNI bridge |
| `tools/fetch_whisper_model.sh` | Create | Downloads and verifies the model |
| `app/build.gradle.kts`, `gradle/libs.versions.toml`, `.gitignore` | Modify | NDK/CMake, model check, AndroidX Test |
| `MAIN/platform/WhisperNative.kt` | Create | JNI declarations |
| `MAIN/platform/WhisperModelFile.kt` | Create | Copies the model asset to private storage |
| `ATEST/platform/WhisperSpikeTest.kt` | Create | Device spike: load time, check latency, memory |
| `MAIN/ritual/LineMatcher.kt` | Create | Normalisation, loop collapse, best-stretch distance, coverage |
| `MAIN/ritual/LineListener.kt` | Create | Buffers, triggers, coalescing, results |
| `MAIN/ritual/Resampler16k.kt` | Create | Mic PCM → 16 kHz floats |
| `MAIN/platform/WhisperTranscriber.kt` | Create | `Transcriber` over JNI, one at a time |
| `MAIN/platform/DeviceCheck.kt` | Create | Once per install: Whisper or lenient |
| `MAIN/ritual/RitualConfig.kt` | Modify | New constants; vowel-judge constants removed |
| `MAIN/ritual/RitualState.kt`, `RitualEngine.kt` | Modify | `CheckMode`, `LineHeard` input, hysteresis |
| `MAIN/ritual/ChantJudge.kt` → `MAIN/ritual/Vowel.kt` | Replace | Keeps only `Vowel` (used by formant tests) |
| `MAIN/ritual/ChantAligner.kt`, `ChantChunks.kt`, `ChantTemplate.kt` | Delete | Vowel judge (ADR-0022) |
| `MAIN/ritual/RitualViewModel.kt` | Modify | Listener, mode, hold-to-chant, logs |
| `MAIN/ritual/SessionLog.kt` | Modify | Check and heard records |
| `MAIN/SummonApp.kt`, `MAIN/navigation/AppNavHost.kt` | Modify | Run the device check; pass transcriber and mode |
| `MAIN/ui/screens/ChantScreenState.kt`, `RitualStatusBlock.kt`, `ChantScreen.kt`, `RitualScreen.kt`, `ChantIntro.kt` | Modify | Covered words, no rejection, checking pulse |
| `MAIN/ui/components/DebugControls.kt`, `MAIN/platform/RitualFeedback.kt` | Modify | Check readout; effects |
| `app/src/main/res/values/strings.xml` | Modify | Copy |
| `app/src/main/assets/licenses/whisper.txt` | Create | MIT notices |
| Tests: `LineMatcherTest`, `Resampler16kTest`, `LineListenerTest`, `WhisperModeTest`, `TranscriptRescoreTest` | Create | — |
| Tests: `ChantAlignerTest`, `ChantChunksTest`, `ChantTemplateTest`, `ChantJudgeTest`, `ChantCheckTest`, `SyntheticChantTest`, `ChantClipReplayTest`, `ChantReplay.kt` | Delete | Vowel judge |
| Tests: `RitualEngineTest`, `RitualViewModelTest`, `SessionLogTest`, `SessionReplay.kt`, `SessionReplayTest`, `ChantScreenStateTest` | Modify | — |
| `docs/decisions/0027-…`, ADR-0026, privacy, store listing, PRODUCT, DESIGN, AGENTS | Modify/Create | — |

---

## Phase 0 — Device spike

### Task 1: Native build, model, and the speed spike

**Files:**
- Create: `.gitmodules` (via `git submodule add`), `app/src/main/cpp/CMakeLists.txt`, `app/src/main/cpp/chant_whisper.cpp`, `tools/fetch_whisper_model.sh`, `MAIN/platform/WhisperNative.kt`, `MAIN/platform/WhisperModelFile.kt`, `ATEST/platform/WhisperSpikeTest.kt`
- Modify: `app/build.gradle.kts`, `gradle/libs.versions.toml`, `.gitignore`

- [ ] **Step 1: Add whisper.cpp as a pinned submodule**

```bash
git submodule add https://github.com/ggml-org/whisper.cpp app/src/main/cpp/whisper.cpp
git -C app/src/main/cpp/whisper.cpp checkout v1.9.4
git add .gitmodules app/src/main/cpp/whisper.cpp
```

Expected: `git -C app/src/main/cpp/whisper.cpp describe --tags` prints `v1.9.4`.

- [ ] **Step 2: Model download script `tools/fetch_whisper_model.sh`**

```bash
#!/usr/bin/env bash
# Downloads the Whisper base q5_1 model (MIT, ~57 MB) into the git-ignored app assets (ADR-0027).
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
dest="$root/app/src/main/assets/whisper/ggml-base-q5_1.bin"
url="https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base-q5_1.bin"
sha="422f1ae452ade6f30a004d7e5c6a43195e4433bc370bf23fac9cc591f01a8898"
mkdir -p "$(dirname "$dest")"
if [ ! -f "$dest" ] || [ "$(shasum -a 256 "$dest" | cut -d' ' -f1)" != "$sha" ]; then
  curl -fL --retry 3 -o "$dest.part" "$url"
  [ "$(shasum -a 256 "$dest.part" | cut -d' ' -f1)" = "$sha" ] || { echo "checksum mismatch" >&2; rm -f "$dest.part"; exit 1; }
  mv "$dest.part" "$dest"
fi
echo "model ok: $dest"
```

Run: `chmod +x tools/fetch_whisper_model.sh && tools/fetch_whisper_model.sh`
Expected: `model ok: …/app/src/main/assets/whisper/ggml-base-q5_1.bin` (59,707,625 bytes).

Append to `.gitignore`:

```
# Whisper model, downloaded by tools/fetch_whisper_model.sh (ADR-0027)
app/src/main/assets/whisper/
```

- [ ] **Step 3: `app/src/main/cpp/CMakeLists.txt`**

```cmake
cmake_minimum_required(VERSION 3.22.1)
project(chantwhisper C CXX)

set(CMAKE_CXX_STANDARD 17)
# whisper.cpp and ggml as static libraries inside our one shared library (ADR-0027).
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_SERVER OFF CACHE BOOL "" FORCE)
add_subdirectory(whisper.cpp)

add_library(chantwhisper SHARED chant_whisper.cpp)
target_link_libraries(chantwhisper PRIVATE whisper log)
# Google Play requires 16 KB page alignment for native code.
target_link_options(chantwhisper PRIVATE "-Wl,-z,max-page-size=16384")
```

- [ ] **Step 4: `app/src/main/cpp/chant_whisper.cpp`**

```cpp
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
```

- [ ] **Step 5: `MAIN/platform/WhisperNative.kt`**

```kotlin
package com.summondemons.safely.platform

/** JNI bindings to `libchantwhisper.so` (whisper.cpp, ADR-0027). Not thread-safe: callers serialise. */
internal object WhisperNative {
    init {
        System.loadLibrary("chantwhisper")
    }

    /** A context handle, or 0 if the model can't be loaded. */
    external fun init(modelPath: String): Long

    /** Transcribes 16 kHz mono [pcm] in Latin; returns the segment count, or -1 on failure. */
    external fun transcribe(handle: Long, pcm: FloatArray, threads: Int, shrinkContext: Boolean): Int

    /** UTF-8 bytes of segment [index] of the last transcription. */
    external fun segmentText(handle: Long, index: Int): ByteArray

    external fun segmentNoSpeech(handle: Long, index: Int): Float

    external fun free(handle: Long)
}
```

- [ ] **Step 6: `MAIN/platform/WhisperModelFile.kt`**

```kotlin
package com.summondemons.safely.platform

import android.content.Context
import java.io.File

/** The bundled Whisper model, copied once from assets to private storage (whisper.cpp needs a path; ADR-0027). */
object WhisperModelFile {
    private const val ASSET = "whisper/ggml-base-q5_1.bin"
    const val BYTES = 59_707_625L

    fun ensure(context: Context): File {
        val file = File(context.filesDir, ASSET)
        if (file.length() == BYTES) return file
        file.parentFile?.mkdirs()
        val part = File(file.path + ".part")
        context.assets.open(ASSET).use { input -> part.outputStream().use { input.copyTo(it, 1 shl 20) } }
        check(part.length() == BYTES) { "model copy truncated: ${part.length()} bytes" }
        check(part.renameTo(file)) { "could not move the model into place" }
        return file
    }
}
```

- [ ] **Step 7: Gradle wiring**

In `gradle/libs.versions.toml` add under `[versions]`:

```toml
androidxTestRunner = "1.6.2"
androidxTestExtJunit = "1.2.1"
```

and under `[libraries]`:

```toml
androidx-test-runner = { group = "androidx.test", name = "runner", version.ref = "androidxTestRunner" }
androidx-test-ext-junit = { group = "androidx.test.ext", name = "junit", version.ref = "androidxTestExtJunit" }
```

In `app/build.gradle.kts`, inside `android { … defaultConfig { … } }` add:

```kotlin
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
        externalNativeBuild {
            cmake {
                // whisper.cpp is unusably slow at -O0, so native code is always optimised (ADR-0027).
                arguments += listOf("-DCMAKE_BUILD_TYPE=Release", "-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON")
            }
        }
```

inside `android { … }` (top level of the block) add:

```kotlin
    ndkVersion = "28.2.13676358"
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    androidResources {
        noCompress += "bin"
    }
```

after the `android { }` block add:

```kotlin
// The Whisper model is downloaded, not committed (ADR-0027).
val whisperModel = layout.projectDirectory.file("src/main/assets/whisper/ggml-base-q5_1.bin").asFile
val verifyWhisperModel by tasks.registering {
    doLast {
        check(whisperModel.exists()) { "Whisper model missing: run tools/fetch_whisper_model.sh (ADR-0027)" }
    }
}
tasks.named("preBuild") { dependsOn(verifyWhisperModel) }
```

and in `dependencies { }` add:

```kotlin
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
```

If the build reports that NDK `28.2.13676358` or CMake `3.22.1` can't be downloaded, install them from Android Studio → Settings → Languages & Frameworks → Android SDK → SDK Tools (show package details: NDK (Side by side) 28.x and CMake 3.22.1 or newer) and set `ndkVersion` to the installed NDK version.

- [ ] **Step 8: Device spike test `ATEST/platform/WhisperSpikeTest.kt`**

```kotlin
package com.summondemons.safely.platform

import android.os.Debug
import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.random.Random

/**
 * Device spike (ADR-0027): model load time, check latency for 3/5/10 s of audio with and without
 * a shrunk encoder context, and native memory. If `spike.wav` (mono PCM16 16 kHz) is pushed to the
 * app's external files dir, it is transcribed too. Read results with `adb logcat -s WhisperSpike`.
 */
@RunWith(AndroidJUnit4::class)
class WhisperSpikeTest {

    private val tag = "WhisperSpike"

    @Test
    fun measure() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val model = WhisperModelFile.ensure(context)
        val threads = Runtime.getRuntime().availableProcessors().coerceIn(2, 4)
        val heapBefore = Debug.getNativeHeapAllocatedSize()
        val loadStart = SystemClock.elapsedRealtime()
        val handle = WhisperNative.init(model.path)
        val loadMs = SystemClock.elapsedRealtime() - loadStart
        assertTrue("model failed to load", handle != 0L)
        Log.i(tag, "load ${loadMs} ms, native heap +${(Debug.getNativeHeapAllocatedSize() - heapBefore) / 1_000_000} MB, threads $threads")
        try {
            for (seconds in listOf(3, 5, 10)) {
                for (shrink in listOf(true, false)) {
                    val pcm = noise(seconds)
                    val t0 = SystemClock.elapsedRealtime()
                    val segments = WhisperNative.transcribe(handle, pcm, threads, shrink)
                    Log.i(tag, "noise ${seconds}s shrink=$shrink: ${SystemClock.elapsedRealtime() - t0} ms, segments $segments")
                }
            }
            val wav = File(context.getExternalFilesDir(null), "spike.wav")
            if (wav.exists()) {
                val pcm = readWav(wav)
                for (shrink in listOf(true, false)) {
                    val t0 = SystemClock.elapsedRealtime()
                    val n = WhisperNative.transcribe(handle, pcm, threads, shrink)
                    val text = (0 until maxOf(n, 0)).joinToString(" ") { String(WhisperNative.segmentText(handle, it), Charsets.UTF_8) }
                    Log.i(tag, "spike.wav ${pcm.size / 16_000.0}s shrink=$shrink: ${SystemClock.elapsedRealtime() - t0} ms: $text")
                }
            }
            Log.i(tag, "native heap peak-ish ${Debug.getNativeHeapAllocatedSize() / 1_000_000} MB")
        } finally {
            WhisperNative.free(handle)
        }
    }

    private fun noise(seconds: Int): FloatArray {
        val random = Random(seconds)
        return FloatArray(seconds * 16_000) { (random.nextFloat() - 0.5f) * 0.02f }
    }

    /** Mono PCM16 WAV → floats; assumes 16 kHz (the debug clip recorder's rate on this phone). */
    private fun readWav(file: File): FloatArray {
        val bytes = ByteBuffer.wrap(file.readBytes()).order(ByteOrder.LITTLE_ENDIAN)
        bytes.position(12)
        while (bytes.remaining() >= 8) {
            val id = ByteArray(4).also { bytes.get(it) }.toString(Charsets.US_ASCII)
            val size = bytes.int
            if (id == "data") return FloatArray(size / 2) { bytes.short / 32_768f }
            bytes.position(bytes.position() + size)
        }
        error("no data chunk")
    }
}
```

- [ ] **Step 9: Build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL; `unzip -l app/build/outputs/apk/debug/app-debug.apk | grep -E 'libchantwhisper|ggml-base'` lists `lib/arm64-v8a/libchantwhisper.so`, `lib/armeabi-v7a/libchantwhisper.so` and `assets/whisper/ggml-base-q5_1.bin`.

Check 16 KB alignment:
Run: `unzip -o -q app/build/outputs/apk/debug/app-debug.apk 'lib/arm64-v8a/*' -d /tmp/apklib && llvm-readelf -l /tmp/apklib/lib/arm64-v8a/libchantwhisper.so | grep LOAD` (llvm-readelf lives in `$ANDROID_HOME/ndk/<version>/toolchains/llvm/prebuilt/darwin-x86_64/bin/`)
Expected: every `LOAD` line ends with alignment `0x4000`.

- [ ] **Step 10: Run the spike on the Galaxy**

```bash
ANDROID_SERIAL=RXCX10467TP adb push app/src/test/chant-clips/asmodeus-chant-v1-20261018-161215.wav /sdcard/Android/data/com.summondemons.safely/files/spike.wav
ANDROID_SERIAL=RXCX10467TP ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.summondemons.safely.platform.WhisperSpikeTest
ANDROID_SERIAL=RXCX10467TP adb logcat -d -s WhisperSpike
```

(`adb` is `$ANDROID_HOME/platform-tools/adb`, or `~/Library/Android/sdk/platform-tools/adb`.)
Expected: lines for load time, the six noise timings and the spike.wav transcript ("Daemonium residente…").

- [ ] **Step 11: Gate**

Record the numbers in a new section "Spike" of `docs/decisions/0027-whisper-chant-check.md` (create the file with just a title line `# 0027 — On-device Whisper chant check` and that section now; Task 10 completes it). Decide:
- **Go** if a 10 s check with `shrink=true` is ≤ 1.5 s and the spike.wav transcript is recognisable Latin. Keep `shrinkContext = true` in later tasks.
- **shrink=true garbles text but shrink=false is fast enough (≤ 1.5 s)**: use `shrinkContext = false` in Task 5 and note it.
- **No-go** if both exceed 3 s: stop and report; do not continue with this plan.

- [ ] **Step 12: Commit**

```bash
git add .gitmodules app/src/main/cpp tools/fetch_whisper_model.sh .gitignore app/build.gradle.kts gradle/libs.versions.toml app/src/main/java/com/summondemons/safely/platform/WhisperNative.kt app/src/main/java/com/summondemons/safely/platform/WhisperModelFile.kt app/src/androidTest docs/decisions/0027-whisper-chant-check.md
git commit -q -m "feat(native): Build whisper.cpp v1.9.4 with a JNI bridge and measure it on device (ADR-0027)

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

## Phase 1 — Pure logic

### Task 2: `LineMatcher`

**Files:**
- Modify: `MAIN/ritual/RitualConfig.kt`
- Create: `MAIN/ritual/LineMatcher.kt`
- Test: `TEST/ritual/LineMatcherTest.kt`

- [ ] **Step 1: Constants in `RitualConfig`** (insert after `const val ROTATION_GRACE_MS = 1_500L`)

```kotlin

    // On-device Whisper chant check (ADR-0027)
    /** Whisper's input rate. */
    const val WHISPER_RATE = 16_000
    /** A line is found when the best-matching stretch of the transcript is within this distance. */
    const val LINE_MATCH_MAX = 0.60
    /** A word is covered (green) when this share of its letters align to equal letters. */
    const val WORD_COVER_MIN = 0.6
    /** Transcript segments Whisper rates as likelier silence than this are dropped. */
    const val NO_SPEECH_MAX = 0.6f
    /** Loops of up to this many words, repeated three times or more, collapse to one. */
    const val LOOP_MAX_WORDS = 6
    const val CHECK_PAUSE_MS = 600L
    const val CHECK_MIN_VOICED_MS = 800L
    const val CHECK_CONTINUOUS_MS = 6_000L
    const val CHECK_WINDOW_MS = 25_000L
    /** Lenient mode when a 5 s device check takes longer than this. */
    const val DEVICE_MAX_CHECK_MS = 4_000L
    /** Once locked, the ritual wavers beyond this and resumes within [DRIFT_EXIT_DEG]. */
    const val DRIFT_ENTER_DEG = 25f
    const val DRIFT_EXIT_DEG = 20f
```

- [ ] **Step 2: Write the failing test `TEST/ritual/LineMatcherTest.kt`**

```kotlin
package com.summondemons.safely.ritual

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Expectations come from the real transcripts of ADR-0026 (Whisper base, Latin). */
class LineMatcherTest {

    private fun heard(vararg text: String) = text.map { TranscriptSegment(it) }

    @Test
    fun lettersFoldCaseAccentsAndLatinSpelling() {
        assertEquals("HOLAMAIUUENIT", LineMatcher.letters("Holāmā, jvvénit!"))
    }

    @Test
    fun honestRealLinesAreFound() {
        listOf(
            "DAEMONIUM RESIDENTE," to "Daemonium residente.",
            "TUI GRATIA LOVIS" to "Tui gracia, lovi, lovi.",
            "GRATIA SIT CURA." to "Gratia, si pi, cura!",
            "SINT BENE OBEDIENTES." to "singt bine, obe, dien, test",
            "HIC EST ME DIGNUS LEGI" to "Ik ezt me di noz legi.",
        ).forEach { (line, text) ->
            val m = LineMatcher.match(line, heard(text))
            assertTrue("$text → ${m.distance}", m.found)
        }
        assertEquals(0.0, LineMatcher.match("DAEMONIUM RESIDENTE,", heard("Daemonium residente.")).distance, 1e-9)
    }

    @Test
    fun repeatsAndRestartsBeforeTheLineCostNothing() {
        val m = LineMatcher.match(
            "OSTENDE MIHI FACIEM TUAM POTENTIAM, VENIT AD ME.",
            heard("Ostende, mihi, faciem, tuam.", "potenti-am", "Ad, vendit, adme."),
        )
        assertTrue(m.found)
        assertEquals((0..7).toSet(), m.covered)
    }

    @Test
    fun nonsenseAndTheOtherDemonsLinesAreNotFound() {
        assertFalse(LineMatcher.match("DAEMONIUM RESIDENTE,", heard("Holamio Miku Holamio Miku Holamio Miku Holamio Miku")).found)
        assertFalse(LineMatcher.match("DAEMONIUM RESIDENTE,", heard("O lamio migu, O lamio migu, O lamio migu")).found)
        assertFalse(LineMatcher.match("TUI GRATIA LOVIS", heard("Daemonium residente.")).found)
    }

    @Test
    fun hallucinatedLoopsCollapseAndFail() {
        val m = LineMatcher.match("DAEMONIUM RESIDENTE,", heard("I'm sorry, I'm sorry, I'm sorry, I'm sorry"))
        assertEquals("IMSORRY", m.heardLetters)
        assertFalse(m.found)
    }

    @Test
    fun likelySilenceIsDropped() {
        val m = LineMatcher.match("DAEMONIUM RESIDENTE,", listOf(TranscriptSegment("Daemonium residente.", noSpeech = 0.9f)))
        assertFalse(m.found)
        assertEquals("", m.heardLetters)
    }

    @Test
    fun loopCollapseKeepsShortRepeats() {
        assertEquals(listOf("HOLAMIO", "MIKU"), LineMatcher.collapseLoops(List(4) { listOf("HOLAMIO", "MIKU") }.flatten()))
        assertEquals(listOf("A", "B"), LineMatcher.collapseLoops(listOf("A", "A", "A", "B")))
        assertEquals(listOf("X", "Y", "X", "Y", "Z"), LineMatcher.collapseLoops(listOf("X", "Y", "X", "Y", "Z")))
    }

    @Test
    fun halfOfALongLinePassesAtTheHonestFirstThreshold() {
        // Documented leniency (plan note 4): real Latin from the line, but only half of it.
        val m = LineMatcher.match("OSTENDE MIHI FACIEM TUAM POTENTIAM, VENIT AD ME.", heard("Ostende, mihi, faciem, tuam."))
        assertTrue(m.found)
        assertEquals(0.462, m.distance, 0.001)
    }
}
```

- [ ] **Step 3: Run it to see it fail**

Run: `./gradlew testDebugUnitTest --tests '*LineMatcherTest*'`
Expected: FAIL, `Unresolved reference: LineMatcher`.

- [ ] **Step 4: Create `MAIN/ritual/LineMatcher.kt`**

```kotlin
package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.LINE_MATCH_MAX
import com.summondemons.safely.ritual.RitualConfig.LOOP_MAX_WORDS
import com.summondemons.safely.ritual.RitualConfig.NO_SPEECH_MAX
import com.summondemons.safely.ritual.RitualConfig.WORD_COVER_MIN
import java.text.Normalizer

/** One piece of a Whisper transcript and how likely Whisper thinks it was silence. */
data class TranscriptSegment(val text: String, val noSpeech: Float = 0f)

/** How a transcript compares with a chant line (ADR-0027). */
data class LineMatch(
    /** Edit distance of the line to the best-matching stretch of the transcript, / the line's letters. */
    val distance: Double,
    val found: Boolean,
    /** Word indices of the line covered by the best stretch. */
    val covered: Set<Int>,
    /** The transcript's letters after filtering and loop collapse. */
    val heardLetters: String,
)

/** Pure transcript scoring for the chant check (ADR-0026, ADR-0027). */
object LineMatcher {
    private val NON_LETTERS = Regex("[^\\p{L}]+")
    private val MARKS = Regex("\\p{Mn}+")

    /** Letters only, upper case, accents removed, J→I and V→U (Latin spelling varies). */
    fun letters(text: String): String {
        val plain = MARKS.replace(Normalizer.normalize(text, Normalizer.Form.NFD), "").uppercase()
        return buildString {
            for (c in plain) {
                when (c) {
                    'J' -> append('I')
                    'V' -> append('U')
                    in 'A'..'Z' -> append(c)
                }
            }
        }
    }

    fun match(line: String, segments: List<TranscriptSegment>): LineMatch {
        val text = segments.filter { it.noSpeech <= NO_SPEECH_MAX }.joinToString(" ") { it.text }
        val heard = collapseLoops(words(text)).joinToString("")
        val lineWords = words(line)
        val expected = lineWords.joinToString("")
        if (expected.isEmpty()) return LineMatch(0.0, true, lineWords.indices.toSet(), heard)
        val (cost, matched) = bestStretch(expected, heard)
        val distance = cost.toDouble() / expected.length
        val covered = mutableSetOf<Int>()
        var pos = 0
        lineWords.forEachIndexed { w, word ->
            val hits = (pos until pos + word.length).count { matched[it] }
            if (hits >= WORD_COVER_MIN * word.length) covered += w
            pos += word.length
        }
        return LineMatch(distance, distance <= LINE_MATCH_MAX, covered, heard)
    }

    /** A sequence of 1..[LOOP_MAX_WORDS] words repeated three or more times in a row is kept once. */
    fun collapseLoops(words: List<String>): List<String> {
        val out = words.toMutableList()
        var n = 1
        while (n <= LOOP_MAX_WORDS) {
            var i = 0
            var changed = false
            while (i + 3 * n <= out.size) {
                val unit = out.subList(i, i + n).toList()
                if (unit == out.subList(i + n, i + 2 * n) && unit == out.subList(i + 2 * n, i + 3 * n)) {
                    var end = i + 3 * n
                    while (end + n <= out.size && out.subList(end, end + n) == unit) end += n
                    repeat(end - (i + n)) { out.removeAt(i + n) }
                    changed = true
                } else {
                    i++
                }
            }
            n = if (changed) 1 else n + 1
        }
        return out
    }

    private fun words(text: String): List<String> = text.split(NON_LETTERS).map(::letters).filter { it.isNotEmpty() }

    /**
     * Semi-global edit distance: [expected] fully, against any stretch of [heard] (free start and end).
     * Returns the cost and, per expected letter, whether it aligned to an equal heard letter.
     */
    private fun bestStretch(expected: String, heard: String): Pair<Int, BooleanArray> {
        val m = expected.length
        val n = heard.length
        val d = Array(m + 1) { IntArray(n + 1) }
        for (i in 1..m) d[i][0] = i
        for (i in 1..m) {
            for (j in 1..n) {
                val sub = d[i - 1][j - 1] + if (expected[i - 1] == heard[j - 1]) 0 else 1
                d[i][j] = minOf(sub, d[i - 1][j] + 1, d[i][j - 1] + 1)
            }
        }
        var j = (0..n).minBy { d[m][it] }
        val cost = d[m][j]
        val matched = BooleanArray(m)
        var i = m
        while (i > 0 && j > 0) {
            val same = expected[i - 1] == heard[j - 1]
            when {
                d[i][j] == d[i - 1][j - 1] + (if (same) 0 else 1) -> { matched[i - 1] = same; i--; j-- }
                d[i][j] == d[i - 1][j] + 1 -> i--
                else -> j--
            }
        }
        return cost to matched
    }
}
```

- [ ] **Step 5: Run the tests**

Run: `./gradlew testDebugUnitTest --tests '*LineMatcherTest*'`
Expected: PASS (8 tests). These values were checked with a Python twin of the algorithm on 2026-10-18.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/summondemons/safely/ritual/RitualConfig.kt app/src/main/java/com/summondemons/safely/ritual/LineMatcher.kt app/src/test/java/com/summondemons/safely/ritual/LineMatcherTest.kt
git commit -q -m "feat(ritual): Score transcripts against a chant line (ADR-0027)

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

### Task 3: `Resampler16k` and `LineListener`

**Files:**
- Create: `MAIN/ritual/Resampler16k.kt`, `MAIN/ritual/LineListener.kt`
- Test: `TEST/ritual/Resampler16kTest.kt`, `TEST/ritual/LineListenerTest.kt`

- [ ] **Step 1: Write the failing tests**

`TEST/ritual/Resampler16kTest.kt`:

```kotlin
package com.summondemons.safely.ritual

import org.junit.Assert.assertEquals
import org.junit.Test

class Resampler16kTest {

    @Test
    fun sixteenKilohertzPassesThroughScaled() {
        val out = Resampler16k(16_000).convert(shortArrayOf(0, 16_384, -32_768), 3)
        assertEquals(listOf(0f, 0.5f, -1f), out.toList())
    }

    @Test
    fun fortyEightKilohertzKeepsAThirdAndConstantsStayConstant() {
        val r = Resampler16k(48_000)
        val total = (0 until 10).sumOf { r.convert(ShortArray(960) { 8_192 }, 960).also { f -> f.forEach { assertEquals(0.25f, it, 1e-6f) } }.size }
        assertEquals(3_200, total, 2)
    }
}
```

`TEST/ritual/LineListenerTest.kt`:

```kotlin
package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.FRAME_MS
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LineListenerTest {

    private val line = "DAEMONIUM RESIDENTE,"
    private val frame = FloatArray(320)

    /** Answers with [text]; counts calls; optionally waits on [gate] so a check stays in flight. */
    private class FakeTranscriber(var text: String = "Daemonium residente.") : Transcriber {
        var calls = 0
        var gate: CompletableDeferred<Unit>? = null
        override suspend fun transcribe(pcm: FloatArray): List<TranscriptSegment> {
            calls++
            gate?.await()
            return listOf(TranscriptSegment(text))
        }
    }

    private fun LineListener.voiced(ms: Long) = repeat((ms / FRAME_MS).toInt()) { onFrame(frame, voiced = true) }
    private fun LineListener.quiet(ms: Long) = repeat((ms / FRAME_MS).toInt()) { onFrame(frame, voiced = false) }

    private fun TestScope.listener(t: FakeTranscriber) = LineListener(t, this, now = { 0L }).also { it.start(0, line) }

    @Test
    fun aPauseAfterEnoughVoiceRunsACheckAndReportsTheLine() = runTest {
        val t = FakeTranscriber()
        val l = listener(t)
        l.voiced(1_000); l.quiet(600)
        advanceUntilIdle()
        assertEquals(1, t.calls)
        val report = l.poll()!!
        assertTrue(report.heard.found)
        assertEquals(0, report.heard.phrase)
        assertNull(l.poll())
    }

    @Test
    fun tooLittleVoiceRunsNoCheck() = runTest {
        val t = FakeTranscriber()
        val l = listener(t)
        l.voiced(500); l.quiet(1_000)
        advanceUntilIdle()
        assertEquals(0, t.calls)
    }

    @Test
    fun continuousVoicingChecksEverySixSeconds() = runTest {
        val t = FakeTranscriber()
        val l = listener(t)
        l.voiced(6_000)
        advanceUntilIdle()
        assertEquals(1, t.calls)
    }

    @Test
    fun triggersDuringACheckCoalesceIntoOneMore() = runTest {
        val t = FakeTranscriber().apply { gate = CompletableDeferred() }
        val l = listener(t)
        l.voiced(1_000); l.quiet(600)
        advanceUntilIdle()
        assertTrue(l.checking)
        repeat(3) { l.voiced(1_000); l.quiet(600) }
        t.gate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(2, t.calls)
        assertFalse(l.checking)
    }

    @Test
    fun aResultForAnOldLineIsDiscarded() = runTest {
        val t = FakeTranscriber().apply { gate = CompletableDeferred() }
        val l = listener(t)
        l.voiced(1_000); l.quiet(600)
        advanceUntilIdle()
        l.start(1, "GRATIA SIT CURA.")
        t.gate!!.complete(Unit)
        advanceUntilIdle()
        assertNull(l.poll())
    }

    @Test
    fun nonsenseIsReportedAsNotFound() = runTest {
        val t = FakeTranscriber("Holamio Miku Holamio Miku Holamio Miku")
        val l = listener(t)
        l.voiced(1_000); l.quiet(600)
        advanceUntilIdle()
        assertFalse(l.poll()!!.heard.found)
    }
}
```

- [ ] **Step 2: Run them to see them fail**

Run: `./gradlew testDebugUnitTest --tests '*Resampler16kTest*' --tests '*LineListenerTest*'`
Expected: FAIL, unresolved `Resampler16k`, `LineListener`, `Transcriber`.

- [ ] **Step 3: Create `MAIN/ritual/Resampler16k.kt`**

```kotlin
package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.WHISPER_RATE
import kotlin.math.floor

/** Mic PCM16 at [rate] → 16 kHz floats in [-1, 1] for Whisper, by linear interpolation (ADR-0027). */
class Resampler16k(private val rate: Int) {
    private val step = rate.toDouble() / WHISPER_RATE
    /** Position of the next output sample, in input samples relative to the frame being converted. */
    private var pos = 0.0
    private var last = 0f

    fun convert(samples: ShortArray, count: Int): FloatArray {
        if (rate == WHISPER_RATE) return FloatArray(count) { samples[it] / 32_768f }
        if (count <= 0) return FloatArray(0)
        val out = FloatArray(((count - pos) / step).toInt() + 2)
        var k = 0
        while (pos < count - 1 && k < out.size) {
            val i = floor(pos).toInt()
            val f = (pos - i).toFloat()
            val a = if (i < 0) last else samples[i] / 32_768f
            val b = samples[i + 1] / 32_768f
            out[k++] = a + (b - a) * f
            pos += step
        }
        pos -= count
        last = samples[count - 1] / 32_768f
        return out.copyOf(k)
    }
}
```

- [ ] **Step 4: Create `MAIN/ritual/LineListener.kt`**

```kotlin
package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.CHECK_CONTINUOUS_MS
import com.summondemons.safely.ritual.RitualConfig.CHECK_MIN_VOICED_MS
import com.summondemons.safely.ritual.RitualConfig.CHECK_PAUSE_MS
import com.summondemons.safely.ritual.RitualConfig.CHECK_WINDOW_MS
import com.summondemons.safely.ritual.RitualConfig.FRAME_MS
import com.summondemons.safely.ritual.RitualConfig.WHISPER_RATE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Speech to text for the chant check; Whisper on device, fakes in tests (ADR-0027). */
fun interface Transcriber {
    suspend fun transcribe(pcm: FloatArray): List<TranscriptSegment>
}

/** What one check found, as engine input (ADR-0027). */
data class LineHeard(val phrase: Int, val found: Boolean, val distance: Double, val covered: Set<Int>)

/** One finished check, for the engine and the session log. */
data class CheckReport(
    val heard: LineHeard,
    val transcript: String,
    val letters: String,
    val windowMs: Long,
    val voicedMs: Long,
    val checkMs: Long,
)

/**
 * Buffers 16 kHz audio since the current line began and checks it with [transcriber] after
 * pauses (ADR-0027): a pause ≥ CHECK_PAUSE_MS after ≥ CHECK_MIN_VOICED_MS of new voice, or
 * CHECK_CONTINUOUS_MS of continuous voice. One check runs at a time; triggers meanwhile
 * coalesce into one more. Thread-safe: frames arrive on the tick, results on [scope].
 */
class LineListener(
    private val transcriber: Transcriber,
    private val scope: CoroutineScope,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val capacity = (CHECK_WINDOW_MS * WHISPER_RATE / 1_000).toInt()
    private val audio = FloatArray(capacity)
    private var audioStart = 0
    private var audioSize = 0
    private val voicedFrames = ArrayDeque<Boolean>()
    private val maxFrames = (CHECK_WINDOW_MS / FRAME_MS).toInt()

    private var token = 0
    private var phrase = -1
    private var line = ""
    private var newVoicedMs = 0L
    private var quietMs = 0L
    private var continuousMs = 0L
    private var running = false
    private var again = false
    private val results = ArrayDeque<CheckReport>()

    /** A check is in flight (the UI pulses the current seal). */
    @Volatile
    var checking = false
        private set

    /** Starts listening for [phrase] (its text is [text]); a no-op if that line is already current. */
    @Synchronized
    fun start(phrase: Int, text: String) {
        if (phrase == this.phrase && text == line) return
        token++
        this.phrase = phrase
        line = text
        audioStart = 0
        audioSize = 0
        voicedFrames.clear()
        newVoicedMs = 0L
        quietMs = 0L
        continuousMs = 0L
        again = false
        results.clear()
    }

    /** One 20 ms frame of 16 kHz audio and whether it was voiced. */
    @Synchronized
    fun onFrame(samples: FloatArray, voiced: Boolean) {
        append(samples)
        voicedFrames.addLast(voiced)
        if (voicedFrames.size > maxFrames) voicedFrames.removeFirst()
        if (voiced) {
            newVoicedMs += FRAME_MS
            continuousMs += FRAME_MS
            quietMs = 0L
        } else {
            quietMs += FRAME_MS
            continuousMs = 0L
        }
        val afterPause = quietMs == CHECK_PAUSE_MS && newVoicedMs >= CHECK_MIN_VOICED_MS
        val longVoice = continuousMs >= CHECK_CONTINUOUS_MS
        if (afterPause || longVoice) {
            newVoicedMs = 0L
            continuousMs = 0L
            trigger()
        }
    }

    /** The oldest unread report for the current line, if any. */
    @Synchronized
    fun poll(): CheckReport? = results.removeFirstOrNull()

    private fun trigger() {
        if (voicedFrames.count { it } * FRAME_MS < CHECK_MIN_VOICED_MS) return
        if (running) {
            again = true
            return
        }
        launchCheck()
    }

    private fun launchCheck() {
        running = true
        checking = true
        val myToken = token
        val myPhrase = phrase
        val myLine = line
        val pcm = snapshot()
        val windowMs = pcm.size * 1_000L / WHISPER_RATE
        val voicedMs = voicedFrames.count { it } * FRAME_MS
        scope.launch {
            val started = now()
            val segments = runCatching { transcriber.transcribe(pcm) }.getOrDefault(emptyList())
            val match = LineMatcher.match(myLine, segments)
            val report = CheckReport(
                heard = LineHeard(myPhrase, match.found, match.distance, match.covered),
                transcript = segments.joinToString(" ") { it.text.trim() },
                letters = match.heardLetters,
                windowMs = windowMs,
                voicedMs = voicedMs,
                checkMs = now() - started,
            )
            finished(myToken, report)
        }
    }

    @Synchronized
    private fun finished(myToken: Int, report: CheckReport) {
        running = false
        checking = false
        if (myToken != token) {
            again = false
            return
        }
        results.addLast(report)
        if (again) {
            again = false
            launchCheck()
        }
    }

    private fun append(samples: FloatArray) {
        for (x in samples) {
            if (audioSize < capacity) {
                audio[(audioStart + audioSize) % capacity] = x
                audioSize++
            } else {
                audio[audioStart] = x
                audioStart = (audioStart + 1) % capacity
            }
        }
    }

    private fun snapshot(): FloatArray = FloatArray(audioSize) { audio[(audioStart + it) % capacity] }
}
```

- [ ] **Step 5: Run the tests**

Run: `./gradlew testDebugUnitTest --tests '*Resampler16kTest*' --tests '*LineListenerTest*'`
Expected: PASS (2 + 6 tests).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/summondemons/safely/ritual/Resampler16k.kt app/src/main/java/com/summondemons/safely/ritual/LineListener.kt app/src/test/java/com/summondemons/safely/ritual/Resampler16kTest.kt app/src/test/java/com/summondemons/safely/ritual/LineListenerTest.kt
git commit -q -m "feat(ritual): Listen for a chant line and check it after pauses (ADR-0027)

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

## Phase 2 — Engine

### Task 4: Whisper and lenient modes in the engine; vowel judge removed

**Files:**
- Modify: `MAIN/ritual/RitualState.kt`, `MAIN/ritual/RitualEngine.kt`, `MAIN/ritual/RitualConfig.kt`
- Replace: `MAIN/ritual/ChantJudge.kt` → `MAIN/ritual/Vowel.kt`
- Delete: `MAIN/ritual/ChantAligner.kt`, `MAIN/ritual/ChantChunks.kt`, `MAIN/ritual/ChantTemplate.kt`; tests `ChantAlignerTest`, `ChantChunksTest`, `ChantTemplateTest`, `ChantJudgeTest`, `ChantCheckTest`, `SyntheticChantTest`, `ChantClipReplayTest`, `ChantReplay.kt`
- Create: `TEST/ritual/WhisperModeTest.kt`
- Modify: `TEST/ritual/RitualEngineTest.kt`

This task, Task 5 and Task 6 change shared types used across the module, so the module compiles again only at the end of Task 6, where every test runs. Tasks 4 and 5 are committed as work in progress.

- [ ] **Step 1: Delete the vowel judge and its tests**

```bash
git rm -q app/src/main/java/com/summondemons/safely/ritual/ChantAligner.kt \
  app/src/main/java/com/summondemons/safely/ritual/ChantChunks.kt \
  app/src/main/java/com/summondemons/safely/ritual/ChantTemplate.kt \
  app/src/test/java/com/summondemons/safely/ritual/ChantAlignerTest.kt \
  app/src/test/java/com/summondemons/safely/ritual/ChantChunksTest.kt \
  app/src/test/java/com/summondemons/safely/ritual/ChantTemplateTest.kt \
  app/src/test/java/com/summondemons/safely/ritual/ChantJudgeTest.kt \
  app/src/test/java/com/summondemons/safely/ritual/ChantCheckTest.kt \
  app/src/test/java/com/summondemons/safely/ritual/SyntheticChantTest.kt \
  app/src/test/java/com/summondemons/safely/ritual/ChantClipReplayTest.kt \
  app/src/test/java/com/summondemons/safely/ritual/ChantReplay.kt
git mv app/src/main/java/com/summondemons/safely/ritual/ChantJudge.kt app/src/main/java/com/summondemons/safely/ritual/Vowel.kt
```

Replace the whole content of `MAIN/ritual/Vowel.kt` with:

```kotlin
package com.summondemons.safely.ritual

/** The five Latin vowels with reference formants for an adult male voice (ADR-0018); used by formant tests. */
enum class Vowel(val f1: Double, val f2: Double) {
    A(750.0, 1_250.0),
    E(480.0, 1_850.0),
    I(300.0, 2_250.0),
    O(500.0, 900.0),
    U(320.0, 750.0);

    /** This vowel's formants for a vocal tract [scale] times shorter than the reference. */
    fun formants(scale: Double = 1.0) = Formants(f1 * scale, f2 * scale)
}
```

- [ ] **Step 2: Remove the vowel-judge constants from `RitualConfig`**

Delete the whole `// Chant check (ADR-0018, ADR-0022)` section (from that comment through `const val CHUNK_MIN_SYLLABLES = 3`, which includes `VOWEL_SCALES`, `VOWEL_MAX_DISTANCE`, `CHANT_GAP_COST`, `CHANT_UNMEASURED_COST`, `SEAL_LISTEN_MS`, `NEUTRAL_VOWEL`, every `ALIGN_*`, `BASELINE_*`, `TALK_MAX_SLOT_MS`, `CHUNK_*`), and delete `WORD_DOUBT_DISTANCE` with its comment. Keep `VERDICT_HOLD_MS`.

Run: `grep -rn "VOWEL_SCALES\|VOWEL_MAX_DISTANCE\|CHANT_GAP_COST\|CHANT_UNMEASURED_COST\|SEAL_LISTEN_MS\|ALIGN_\|BASELINE_\|CHUNK_\|WORD_DOUBT" app/src --include='*.kt'`
Expected: no output except in files Task 4–6 rewrite (`RitualEngine.kt`, `SessionLog.kt`).

- [ ] **Step 3: Write the failing engine test `TEST/ritual/WhisperModeTest.kt`**

```kotlin
package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.FRAME_MS
import com.summondemons.safely.ritual.RitualConfig.LOCK_HOLD_MS
import com.summondemons.safely.ritual.RitualConfig.ONSET_REFRACTORY_MS
import com.summondemons.safely.ritual.RitualEffect.Drift
import com.summondemons.safely.ritual.RitualEffect.LineChecked
import com.summondemons.safely.ritual.RitualEffect.Resumed
import com.summondemons.safely.ritual.RitualEffect.Sealed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The engine in Whisper mode (ADR-0027): lines seal on LineHeard, not on lit syllables. */
class WhisperModeTest {

    private val phrases = listOf("DAEMONIUM RESIDENTE,", "GRATIA SIT CURA.", "HIC EST", "UT VOTA")
    private val onset = VoiceFrame(3_000.0, voiced = true, onset = true)
    private val silent = VoiceFrame.SILENT

    private fun run(start: RitualState, frames: Int, azimuth: Float? = 0f, voice: VoiceFrame = silent, heard: LineHeard? = null): Step {
        var state = start
        val effects = mutableListOf<RitualEffect>()
        repeat(frames) { i ->
            val step = RitualEngine.step(state, RitualInput(azimuth, voice, heard = if (i == 0) heard else null))
            state = step.state
            effects += step.effects
        }
        return Step(state, effects)
    }

    private fun locked(mode: CheckMode) = run(RitualState.start(0f, phrases, mode = mode), (LOCK_HOLD_MS / FRAME_MS).toInt()).state

    /** [count] syllables: an onset, then silence for the refractory period. */
    private fun syllables(start: RitualState, count: Int): RitualState {
        var s = start
        repeat(count) {
            s = run(s, 1, voice = onset).state
            s = run(s, (ONSET_REFRACTORY_MS / FRAME_MS).toInt()).state
        }
        return s
    }

    @Test
    fun lightingEverySyllableDoesNotSealInWhisperMode() {
        val s = syllables(locked(CheckMode.Whisper), 12)
        assertEquals(0, s.phrase)
        assertEquals(8, s.lit) // capped at the line's syllables
    }

    @Test
    fun aFoundLineSealsWithAVerdictAndAFullTimerNext() {
        val step = run(locked(CheckMode.Whisper), 1, heard = LineHeard(0, found = true, distance = 0.1, covered = setOf(0, 1)))
        assertTrue(LineChecked(0, true, 0.1) in step.effects)
        assertTrue(Sealed(0) in step.effects)
        assertEquals(1, step.state.phrase)
        assertEquals(step.state.focusBudgetMs, step.state.focusMs)
        assertEquals(emptySet<Int>(), step.state.covered)
        assertEquals(ChantVerdict.Kind.Sealed, step.state.verdict!!.kind)
    }

    @Test
    fun aMissAddsCoverageAndKeepsListening() {
        val step = run(locked(CheckMode.Whisper), 1, heard = LineHeard(0, found = false, distance = 0.7, covered = setOf(1)))
        assertTrue(LineChecked(0, false, 0.7) in step.effects)
        assertEquals(0, step.state.phrase)
        assertEquals(setOf(1), step.state.covered)
    }

    @Test
    fun aResultForAnotherLineIsIgnored() {
        val step = run(locked(CheckMode.Whisper), 1, heard = LineHeard(2, found = true, distance = 0.0, covered = emptySet()))
        assertTrue(step.effects.none { it is LineChecked || it is Sealed })
    }

    @Test
    fun lenientModeSealsWhenEverySyllableLit() {
        val s = syllables(locked(CheckMode.Lenient), 8)
        assertEquals(1, s.phrase)
    }

    @Test
    fun onceLockedTheRitualWaversOnlyBeyondTwentyFiveDegreesAndResumesWithinTwenty() {
        val chanting = locked(CheckMode.Whisper)
        assertEquals(RitualPhase.Chanting, run(chanting, 5, azimuth = 22f).state.phase)
        val wavering = run(chanting, 1, azimuth = 26f)
        assertTrue(Drift in wavering.effects)
        assertEquals(RitualPhase.Wavering, run(wavering.state, 5, azimuth = 22f).state.phase)
        val back = run(wavering.state, 1, azimuth = 19f)
        assertTrue(Resumed in back.effects)
        assertEquals(RitualPhase.Chanting, back.state.phase)
    }
}
```

- [ ] **Step 4: Update `MAIN/ritual/RitualState.kt`**

a) After `enum class RitualPhase …` add:

```kotlin

/** How lines seal (ADR-0027): Whisper hears them, or (fallback) every syllable lit. Fixed per ritual. */
enum class CheckMode { Whisper, Lenient }
```

b) Replace `data class RitualInput(val azimuth: Float?, val voice: VoiceFrame, val inCourt: Boolean = false)` and its KDoc with:

```kotlin
/**
 * One 20 ms tick: the latest heading (null = unknown, which counts as off heading once
 * locked), the analysed mic frame, whether the demon's House is in court (read only at the
 * moment of lock, ADR-0012), and the latest finished line check, if any (ADR-0027).
 */
data class RitualInput(
    val azimuth: Float?,
    val voice: VoiceFrame,
    val inCourt: Boolean = false,
    val heard: LineHeard? = null,
)
```

c) In `RitualEffect`, replace the `Sealed` KDoc with `/** [phrase] sealed: heard (Whisper) or every syllable lit (lenient; ADR-0027). */`, delete `Judged`, `ChunkPassed` and `Rejected` with their KDocs, and add:

```kotlin
    /** A finished check of [phrase] (ADR-0027); [found] seals it. */
    data class LineChecked(val phrase: Int, val found: Boolean, val distance: Double) : RitualEffect
```

d) In `RitualState`, replace the fields from `/** Each phrase's chunks; …` through `val verdict: ChantVerdict? = null,` with:

```kotlin
    /** How lines seal; fixed per ritual (ADR-0027). */
    val mode: CheckMode = CheckMode.Lenient,
    /** Words of the current line covered by checks so far (Whisper mode; ADR-0027). */
    val covered: Set<Int> = emptySet(),
    /** The last verdict, until the next syllable lights (ADR-0019). */
    val verdict: ChantVerdict? = null,
```

e) Delete the init line `require(chunks == null || chunks.size == PHRASE_COUNT) { … }`.

f) Replace the `start(...)` function with:

```kotlin
        /** The app passes the device's mode (ADR-0027); most engine tests use the lenient default. */
        fun start(
            target: Float,
            phrases: List<String>,
            mode: CheckMode = CheckMode.Lenient,
            phase: RitualPhase = RitualPhase.Seeking,
        ): RitualState = RitualState(
            target = target,
            syllables = phrases.map { LatinSyllables.count(it).coerceAtLeast(1) },
            phase = phase,
            mode = mode,
        )
```

g) Replace `ChantVerdict` with:

```kotlin
/**
 * The last verdict (ADR-0019, ADR-0027): shown until the next syllable lights. While it is
 * fresh ([holdsClock]) the timer waits.
 */
data class ChantVerdict(val kind: Kind, val phrase: Int, val heldMs: Long = 0L) {
    enum class Kind { Sealed, Cracked }

    val holdsClock: Boolean get() = heldMs < VERDICT_HOLD_MS
}
```

- [ ] **Step 5: Rewrite `MAIN/ritual/RitualEngine.kt`**

Replace the whole file with:

```kotlin
package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.CLIMAX_MS
import com.summondemons.safely.ritual.RitualConfig.COURT_BONUS
import com.summondemons.safely.ritual.RitualConfig.DRIFT_ENTER_DEG
import com.summondemons.safely.ritual.RitualConfig.DRIFT_EXIT_DEG
import com.summondemons.safely.ritual.RitualConfig.FOCUS_DRIFT_MULTIPLIER
import com.summondemons.safely.ritual.RitualConfig.FRAME_MS
import com.summondemons.safely.ritual.RitualConfig.LOCK_HOLD_MS
import com.summondemons.safely.ritual.RitualConfig.ONSET_REFRACTORY_MS
import com.summondemons.safely.ritual.RitualConfig.PHRASE_COUNT
import com.summondemons.safely.ritual.RitualConfig.PROXIMITY_RANGE_DEG
import com.summondemons.safely.ritual.RitualConfig.PROXIMITY_STEP_DEG
import com.summondemons.safely.ritual.RitualConfig.SUSTAIN_SYLLABLE_MS

/**
 * Pure ritual rules (ADR-0008, the timer of ADR-0010/ADR-0025, verdicts of ADR-0019, the
 * on-device chant check of ADR-0027): one call per 20 ms tick.
 */
object RitualEngine {

    fun step(state: RitualState, input: RitualInput): Step {
        if (state.phase == RitualPhase.Done) return Step(state)
        val s = state.copy(azimuth = input.azimuth)
        return when (s.phase) {
            RitualPhase.Seeking, RitualPhase.Locking -> seek(s, input.inCourt)
            RitualPhase.Chanting, RitualPhase.Wavering -> chant(s, input.voice, input.heard)
            RitualPhase.Climax -> climax(s)
            RitualPhase.Done -> Step(s)
        }
    }

    /**
     * Whether a step with [state] and [azimuth] can lock on this tick.
     * Keeps RitualEngine pure: inputs and state only.
     */
    fun canLock(state: RitualState, azimuth: Float?): Boolean {
        if (state.phase != RitualPhase.Seeking && state.phase != RitualPhase.Locking) return false
        val s = state.copy(azimuth = azimuth)
        if (!s.aligned) return false
        val held = (if (s.phase == RitualPhase.Locking) s.lockMs else 0L) + FRAME_MS
        return held >= LOCK_HOLD_MS
    }

    private fun seek(s: RitualState, inCourt: Boolean): Step {
        val effects = mutableListOf<RitualEffect>()
        val bucket = s.azimuth?.let { proximityBucket(angularDistance(it, s.target)) }
        val previous = s.proximityBucket
        if (bucket != null && (previous == null || bucket < previous)) effects += RitualEffect.ProximityTick
        if (!s.aligned) {
            return Step(s.copy(phase = RitualPhase.Seeking, lockMs = 0L, proximityBucket = bucket), effects)
        }
        val held = (if (s.phase == RitualPhase.Locking) s.lockMs else 0L) + FRAME_MS
        if (held < LOCK_HOLD_MS) {
            return Step(s.copy(phase = RitualPhase.Locking, lockMs = held, proximityBucket = bucket), effects)
        }
        effects += RitualEffect.Locked
        val chanting = s.copy(
            phase = RitualPhase.Chanting,
            lockMs = 0L,
            proximityBucket = bucket,
            courtMultiplier = if (inCourt) 1f + COURT_BONUS else 1f,
            sinceSyllableMs = ONSET_REFRACTORY_MS,
            voicedRunMs = 0L,
        )
        return Step(chanting, effects)
    }

    private fun chant(s: RitualState, voice: VoiceFrame, heard: LineHeard?): Step {
        val effects = mutableListOf<RitualEffect>()
        if (!onCourse(s)) {
            // Off heading: nothing counts; once locked, never back to Seeking (ADR-0010).
            if (s.phase == RitualPhase.Chanting) effects += RitualEffect.Drift
            return drain(s.copy(phase = RitualPhase.Wavering, voicedRunMs = 0L), FOCUS_DRIFT_MULTIPLIER, effects)
        }
        if (s.phase == RitualPhase.Wavering) effects += RitualEffect.Resumed
        val since = s.sinceSyllableMs + FRAME_MS
        val run = if (voice.voiced) s.voicedRunMs + FRAME_MS else 0L
        var chanting = s.copy(phase = RitualPhase.Chanting, sinceSyllableMs = since, voicedRunMs = run)

        if (s.mode == CheckMode.Whisper && heard != null && heard.phrase == s.phrase) {
            effects += RitualEffect.LineChecked(heard.phrase, heard.found, heard.distance)
            chanting = chanting.copy(covered = chanting.covered + heard.covered)
            if (heard.found) return seal(chanting.copy(verdict = ChantVerdict(ChantVerdict.Kind.Sealed, s.phrase)), effects)
        }

        val total = s.syllables[s.phrase]
        val lights = voice.voiced && s.lit < total &&
            ((voice.onset && since >= ONSET_REFRACTORY_MS) || run >= SUSTAIN_SYLLABLE_MS)
        if (!lights) return drain(chanting, 1, effects)

        effects += RitualEffect.SyllableLit
        // Chanting again clears the last verdict (ADR-0019).
        val lit = chanting.copy(sinceSyllableMs = 0L, voicedRunMs = 0L, lit = s.lit + 1, verdict = null)
        if (s.mode == CheckMode.Lenient && lit.lit == total) return seal(lit, effects)
        return drain(lit, 1, effects)
    }

    /** Locking needs ±20°; once locked the ritual wavers beyond 25° and resumes within 20° (ADR-0027). */
    private fun onCourse(s: RitualState): Boolean {
        val azimuth = s.azimuth ?: return false
        val limit = if (s.phase == RitualPhase.Chanting) DRIFT_ENTER_DEG else DRIFT_EXIT_DEG
        return angularDistance(azimuth, s.target) <= limit
    }

    /** Seals the current phrase: the next one starts with a full timer, or the 4th starts the climax. */
    private fun seal(s: RitualState, effects: MutableList<RitualEffect>): Step {
        effects += RitualEffect.Sealed(s.phrase)
        val phrase = s.phrase + 1
        if (phrase < PHRASE_COUNT) {
            val budget = focusBudget(phrase, s.courtMultiplier)
            return Step(s.copy(phrase = phrase, lit = 0, covered = emptySet(), focusMs = budget, focusBudgetMs = budget), effects)
        }
        effects += RitualEffect.ClimaxStart(s.flawless)
        val climax = s.copy(
            phase = RitualPhase.Climax, phrase = phrase, lit = 0, covered = emptySet(),
            focusMs = 0L, focusBudgetMs = 0L, climaxMs = 0L,
        )
        return Step(climax, effects)
    }

    /**
     * Drains the timer by [multiplier] frames (phrases II–IV only); at 0 the last seal cracks.
     * While a fresh verdict is on screen the timer waits instead (ADR-0019, ADR-0025).
     */
    private fun drain(s: RitualState, multiplier: Int, effects: MutableList<RitualEffect>): Step {
        if (s.phrase < 1) return Step(s, effects)
        val verdict = s.verdict
        if (verdict != null && verdict.holdsClock) return Step(s.copy(verdict = verdict.copy(heldMs = verdict.heldMs + FRAME_MS)), effects)
        val focus = s.focusMs - FRAME_MS * multiplier
        if (focus > 0L) return Step(s.copy(focusMs = focus), effects)
        return crack(s, effects)
    }

    /** The timer ran out: the last seal cracks and its phrase reopens with a full timer (ADR-0010, ADR-0025). */
    private fun crack(s: RitualState, effects: MutableList<RitualEffect>): Step {
        val phrase = s.phrase - 1
        effects += RitualEffect.Cracked(phrase)
        val budget = focusBudget(phrase, s.courtMultiplier)
        val cracked = if (s.mode == CheckMode.Whisper) ChantVerdict(ChantVerdict.Kind.Cracked, phrase) else null
        return Step(
            s.copy(
                phrase = phrase, lit = 0, covered = emptySet(), focusMs = budget, focusBudgetMs = budget,
                flawless = false, verdict = cracked,
            ),
            effects,
        )
    }

    private fun climax(s: RitualState): Step {
        val elapsed = s.climaxMs + FRAME_MS
        if (elapsed < CLIMAX_MS) return Step(s.copy(climaxMs = elapsed))
        return Step(s.copy(phase = RitualPhase.Done, climaxMs = CLIMAX_MS), listOf(RitualEffect.Summoned))
    }

    private fun proximityBucket(distance: Float): Int? =
        if (distance <= PROXIMITY_RANGE_DEG) (distance / PROXIMITY_STEP_DEG).toInt() else null
}
```

- [ ] **Step 6: Update `TEST/ritual/RitualEngineTest.kt`**

The lenient default keeps it working, with two edits:
- every `RitualState.start(…, judged = true)` (if any) → `RitualState.start(…, mode = CheckMode.Whisper)`;
- the test that lights more syllables than a phrase has while wavering, `voiceIsIgnoredWhileWavering`, is unchanged; any test that relied on a syllable lighting past the phrase end in lenient mode does not exist (lenient seals at the last syllable, as before).

Run: `grep -n "judged" app/src/test/java/com/summondemons/safely/ritual/RitualEngineTest.kt`
Expected: no output after the edit.

- [ ] **Step 7: No test run yet**

The ViewModel, session log, screen state and debug panel still reference removed types; they are rewritten in Tasks 5 and 6. `WhisperModeTest` and `RitualEngineTest` run at Task 6, Step 7.

- [ ] **Step 8: Commit (work in progress; compiles after Task 6)**

```bash
git add -A app/src
git commit -q -m "refactor(ritual): Seal lines on LineHeard (Whisper) or all syllables (lenient); remove the vowel judge (ADR-0027)

The module compiles again at the end of the UI task.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

## Phase 3 — Platform and ViewModel

### Task 5: Transcriber, device check, ViewModel and session log

**Files:**
- Create: `MAIN/platform/WhisperTranscriber.kt`, `MAIN/platform/DeviceCheck.kt`
- Modify: `MAIN/ritual/RitualViewModel.kt`, `MAIN/ritual/SessionLog.kt`, `MAIN/SummonApp.kt`, `MAIN/navigation/AppNavHost.kt`, `MAIN/platform/RitualFeedback.kt`

- [ ] **Step 1: `MAIN/platform/WhisperTranscriber.kt`**

```kotlin
package com.summondemons.safely.platform

import android.content.Context
import com.summondemons.safely.ritual.Transcriber
import com.summondemons.safely.ritual.TranscriptSegment
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Whisper base in Latin through whisper.cpp (ADR-0027). The model loads once per process. */
class WhisperTranscriber private constructor(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : Transcriber {
    private val threads = Runtime.getRuntime().availableProcessors().coerceIn(2, 4)

    override suspend fun transcribe(pcm: FloatArray): List<TranscriptSegment> = withContext(dispatcher) {
        lock.withLock { transcribeNow(pcm) }
    }

    /** Blocking; callers hold [lock] or run before any ritual (the device check). */
    internal fun transcribeNow(pcm: FloatArray): List<TranscriptSegment> {
        val h = handle()
        val n = WhisperNative.transcribe(h, pcm, threads, SHRINK_CONTEXT)
        if (n <= 0) return emptyList()
        return (0 until n).map { i ->
            TranscriptSegment(String(WhisperNative.segmentText(h, i), Charsets.UTF_8), WhisperNative.segmentNoSpeech(h, i))
        }
    }

    private fun handle(): Long = synchronized(Companion) {
        if (loaded == 0L) {
            loaded = WhisperNative.init(WhisperModelFile.ensure(context).path)
            check(loaded != 0L) { "Whisper model failed to load" }
        }
        loaded
    }

    companion object {
        /** From the device spike (Task 1): true unless the spike showed shrunk context garbles text. */
        private const val SHRINK_CONTEXT = true
        private val lock = Mutex()
        private var loaded = 0L
        @Volatile
        private var instance: WhisperTranscriber? = null

        fun get(context: Context): WhisperTranscriber =
            instance ?: synchronized(this) { instance ?: WhisperTranscriber(context.applicationContext).also { instance = it } }
    }
}
```

If Task 1 decided `shrinkContext = false`, set `SHRINK_CONTEXT = false`.

- [ ] **Step 2: `MAIN/platform/DeviceCheck.kt`**

```kotlin
package com.summondemons.safely.platform

import android.content.Context
import android.os.SystemClock
import com.summondemons.safely.ritual.CheckMode
import com.summondemons.safely.ritual.RitualConfig.DEVICE_MAX_CHECK_MS
import com.summondemons.safely.ritual.RitualConfig.WHISPER_RATE
import kotlin.random.Random

/**
 * Once per install (ADR-0027): can this phone run Whisper fast enough? Loads the model and times
 * a 5 s transcription; the result is cached. Until it has run, rituals use [CheckMode.Lenient].
 */
object DeviceCheck {
    private const val PREFS = "device_check"
    private const val KEY_MODE = "mode_v1"
    private const val KEY_REASON = "reason_v1"

    data class Result(val mode: CheckMode, val reason: String)

    fun cached(context: Context): Result? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val mode = prefs.getString(KEY_MODE, null) ?: return null
        return Result(CheckMode.valueOf(mode), prefs.getString(KEY_REASON, "") ?: "")
    }

    /** Blocking; run off the main thread. */
    fun runOnce(context: Context): Result {
        cached(context)?.let { return it }
        val result = try {
            val transcriber = WhisperTranscriber.get(context)
            val random = Random(5)
            val pcm = FloatArray(5 * WHISPER_RATE) { (random.nextFloat() - 0.5f) * 0.02f }
            val t0 = SystemClock.elapsedRealtime()
            transcriber.transcribeNow(pcm)
            val ms = SystemClock.elapsedRealtime() - t0
            if (ms <= DEVICE_MAX_CHECK_MS) Result(CheckMode.Whisper, "${ms} ms") else Result(CheckMode.Lenient, "slow: ${ms} ms")
        } catch (e: Throwable) {
            Result(CheckMode.Lenient, "load: ${e.javaClass.simpleName}")
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_MODE, result.mode.name).putString(KEY_REASON, result.reason).apply()
        return result
    }
}
```

- [ ] **Step 3: Run the device check at app start (`MAIN/SummonApp.kt`)**

Replace the file with:

```kotlin
package com.summondemons.safely

import android.app.Application
import com.summondemons.safely.platform.DeviceCheck
import kotlin.concurrent.thread

class SummonApp : Application() {
    val graph: AppGraph by lazy { AppGraph(this) }

    override fun onCreate() {
        super.onCreate()
        // Decides once per install whether rituals use Whisper or the lenient check (ADR-0027).
        thread(name = "whisper-device-check", isDaemon = true) { DeviceCheck.runOnce(this) }
    }
}
```

- [ ] **Step 4: Session log records (`MAIN/ritual/SessionLog.kt`)**

a) In `SessionHeader`, replace `val judged: Boolean,` with:

```kotlin
    /** CheckMode name (ADR-0027). */
    val mode: String,
```

b) In `SessionTick`, replace `val judged: JudgedRecord? = null,` with:

```kotlin
    /** The check result fed to the engine on this tick (input; replayed). */
    val heard: HeardRecord? = null,
    /** Details of a check that finished on this tick (debug readout, threshold re-check). */
    val check: CheckRecord? = null,
```

and in `input()`, add `heard = heard?.let { LineHeard(it.phrase, it.found, it.distance, it.covered.toSet()) },` as the last argument of `RitualInput(...)`.

c) Replace the `JudgedRecord` class with:

```kotlin
@Serializable
data class HeardRecord(val phrase: Int, val found: Boolean, val distance: Double, val covered: List<Int>)

/** One Whisper check (ADR-0027). */
@Serializable
data class CheckRecord(
    val phrase: Int,
    val found: Boolean,
    val distance: Double,
    val transcript: String,
    val letters: String,
    val windowMs: Long,
    val voicedMs: Long,
    val checkMs: Long,
)
```

d) In `SessionRecords`, replace `tick(...)`, `effectName(...)` and `record(...)` with:

```kotlin
    fun tick(t: Long, input: RitualInput, step: Step, hold: Boolean, check: CheckReport? = null): SessionTick {
        val s = step.state
        return SessionTick(
            t = t,
            az = input.azimuth,
            court = input.inCourt,
            rms = input.voice.rms,
            v = input.voice.voiced,
            o = input.voice.onset,
            f1 = input.voice.formants?.f1,
            f2 = input.voice.formants?.f2,
            hold = hold,
            ph = s.phase.name,
            p = s.phrase,
            c = s.covered.size,
            lit = s.lit,
            fm = s.focusMs,
            fx = step.effects.map(::effectName),
            heard = input.heard?.let { HeardRecord(it.phrase, it.found, it.distance, it.covered.sorted()) },
            check = check?.let {
                CheckRecord(it.heard.phrase, it.heard.found, it.heard.distance, it.transcript, it.letters, it.windowMs, it.voicedMs, it.checkMs)
            },
        )
    }

    fun effectName(effect: RitualEffect): String = effect.toString()
```

(The field `c` now holds the number of covered words; update its KDoc in `SessionTick` to `/** Words of the current line covered so far (ADR-0027). */`.)

- [ ] **Step 5: ViewModel (`MAIN/ritual/RitualViewModel.kt`)**

a) In `RitualUiState`, replace

```kotlin
    /** The chant check's latest verdict, for the debug readout (ADR-0022). */
    val lastJudged: RitualEffect.Judged? = null,
    /** Phrases rejected this attempt; picks the scoff line (ADR-0018). */
    val rejections: Int = 0,
```

with

```kotlin
    /** The latest Whisper check, for the debug readout (ADR-0027). */
    val lastCheck: CheckReport? = null,
    /** A Whisper check is running (the current seal pulses faster). */
    val checking: Boolean = false,
    /** Why the ritual runs in lenient mode, for the debug readout; empty in Whisper mode. */
    val modeReason: String = "",
```

b) Add constructor parameters after `sessionLog`:

```kotlin
    /** Whisper on device; null in tests and in lenient mode (ADR-0027). */
    private val transcriber: Transcriber? = null,
    /** The device's check mode and its reason, read once when the ritual opens (ADR-0027). */
    private val checkMode: () -> Pair<CheckMode, String> = { CheckMode.Lenient to "" },
```

c) Replace the initial `state` with:

```kotlin
    private val mode = checkMode()
    private val state = MutableStateFlow(
        RitualUiState(
            demon = demon,
            ritual = RitualState.start(demon.ritualHeading, demon.chantPhrases, mode = mode.first),
            headingUnavailable = !headingSource.available,
            introVisible = showIntro,
            modeReason = mode.second,
        ),
    )

    private val listener: LineListener? =
        if (mode.first == CheckMode.Whisper && transcriber != null) LineListener(transcriber, viewModelScope) else null
```

d) Change the voice pipeline to carry 16 kHz audio. Add at the top level of the file (after `RitualUiState`):

```kotlin
/** One analysed mic frame plus its 16 kHz audio for the line listener (null when silent or synthetic). */
class MicFrame(val voice: VoiceFrame, val pcm16k: FloatArray?)
```

Replace `private val voiceFrames: Flow<VoiceFrame>` with `private val voiceFrames: Flow<MicFrame>`; in `micFrames()` change the return type to `Flow<MicFrame>`, create `var resampler: Resampler16k? = null` next to `analyzer`, and replace `emit(current.analyze(frame.samples, frame.count))` with:

```kotlin
            val voice = current.analyze(frame.samples, frame.count)
            val pcm = if (listener != null) {
                val r = resampler?.takeIf { it.rate == frame.sampleRate } ?: Resampler16k(frame.sampleRate).also { resampler = it }
                r.convert(frame.samples, frame.count)
            } else {
                null
            }
            emit(MicFrame(voice, pcm))
```

and in `Resampler16k` (Task 3 file) change `class Resampler16k(private val rate: Int)` to `class Resampler16k(val rate: Int)`.

In `silentFrames()` change the return type to `Flow<MicFrame>` and `emit(VoiceFrame.SILENT)` to `emit(MicFrame(VoiceFrame.SILENT, null))`.

e) Replace `tick(...)` with:

```kotlin
    /** One 20 ms tick. Only this collector changes `ritual`, so read-then-update is safe. */
    private fun tick(mic: MicFrame) {
        val current = state.value
        if (!current.active || awaitingHeading(current)) return
        val voice = when {
            current.introVisible -> VoiceFrame.SILENT // nothing counts while the rules are up
            current.holdToChant -> syntheticVoice(current.ritual)
            else -> mic.voice
        }
        val ritual = current.ritual
        val listening = ritual.phase == RitualPhase.Chanting || ritual.phase == RitualPhase.Wavering
        var report: CheckReport? = null
        if (listener != null && listening && ritual.phrase < RitualConfig.PHRASE_COUNT) {
            listener.start(ritual.phrase, demon.chantPhrases[ritual.phrase])
            val pcm = mic.pcm16k
            if (pcm != null && !current.introVisible && !current.holdToChant) listener.onFrame(pcm, voice.voiced)
            report = listener.poll()
        }
        var heard = report?.heard
        // Debug "Hold to Chant" can't speak: once every syllable is lit, the line counts as heard.
        if (current.holdToChant && ritual.mode == CheckMode.Whisper && ritual.phase == RitualPhase.Chanting &&
            ritual.phrase < RitualConfig.PHRASE_COUNT && ritual.lit >= ritual.syllables[ritual.phrase]
        ) {
            val words = LatinSyllables.wordSizes(demon.chantPhrases[ritual.phrase]).indices.toSet()
            heard = LineHeard(ritual.phrase, found = true, distance = 0.0, covered = words)
        }

        val inCourt = if (RitualEngine.canLock(ritual, current.azimuth)) inCourt() else false
        val input = RitualInput(current.azimuth, voice, inCourt, heard)
        val step = RitualEngine.step(ritual, input)
        sessionLog.write(SessionRecords.tick(ticks++ * RitualConfig.FRAME_MS, input, step, hold = current.holdToChant, check = report))
        // The summon is saved here, not through the lossy effects flow, before the climax plays.
        step.effects.filterIsInstance<RitualEffect.ClimaxStart>().firstOrNull()?.let { commitSummon(it.flawless) }
        state.update {
            it.copy(
                ritual = step.state,
                micLevel = smoothMicLevel(it.micLevel, voice.rms),
                lastCheck = report ?: it.lastCheck,
                checking = listener?.checking ?: false,
            )
        }
        step.effects.forEach { _effects.tryEmit(it) }
    }
```

f) In `syntheticVoice(...)` replace the body with:

```kotlin
        val onset = holdFrames++ % HOLD_TO_CHANT_ONSET_FRAMES == 0
        return VoiceFrame(HOLD_TO_CHANT_RMS, voiced = true, onset = onset)
```

and update its KDoc to `/** Debug "Hold to Chant": continuous voicing with an onset every ~240 ms (ADR-0027). */`.

g) In `init { … }` replace the header with:

```kotlin
        sessionLog.start(SessionHeader(demon.id, demon.ritualHeading, demon.chantPhrases, mode = mode.first.name, startedMs = System.currentTimeMillis()))
        if (mode.second.isNotEmpty()) note("mode:${mode.first}:${mode.second}")
```

h) Update `factory(...)`: add parameters `transcriber: Transcriber? = null` and `checkMode: () -> Pair<CheckMode, String> = { CheckMode.Lenient to "" }`, and construct with `RitualViewModel(demon, headingSource, pcmSource, inCourt, commitSummon, clipRecorder, showIntro, sessionLog = sessionLog, transcriber = transcriber, checkMode = checkMode)`.

- [ ] **Step 6: Wire the app (`MAIN/navigation/AppNavHost.kt`)**

In the `RitualViewModel.factory(...)` call, after `sessionLog = SessionLogs.create(appContext),` add:

```kotlin
            transcriber = WhisperTranscriber.get(appContext),
            checkMode = {
                val result = DeviceCheck.cached(appContext)
                when {
                    result == null -> CheckMode.Lenient to "device check still running"
                    result.mode == CheckMode.Lenient -> CheckMode.Lenient to result.reason
                    else -> CheckMode.Whisper to ""
                }
            },
```

and add the imports `com.summondemons.safely.platform.DeviceCheck`, `com.summondemons.safely.platform.WhisperTranscriber`, `com.summondemons.safely.ritual.CheckMode`.

- [ ] **Step 7: Feedback (`MAIN/platform/RitualFeedback.kt`)**

Replace the three lines

```kotlin
        is RitualEffect.Judged -> Unit
        is RitualEffect.ChunkPassed -> Haptics.tick(context)
        is RitualEffect.Rejected -> Haptics.warn(context)
```

with

```kotlin
        is RitualEffect.LineChecked -> Unit
```

- [ ] **Step 8: Commit (work in progress, compiles after Task 6)**

```bash
git add -A app/src
git commit -q -m "feat(ritual): Run Whisper checks from the ViewModel; device check and session records (ADR-0027)

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

## Phase 4 — Screen

### Task 6: Chant screen, debug panel, intro copy; tests green again

**Files:**
- Modify: `MAIN/ui/screens/ChantScreenState.kt`, `MAIN/ui/screens/RitualStatusBlock.kt`, `MAIN/ui/screens/ChantScreen.kt`, `MAIN/ui/screens/RitualScreen.kt`, `MAIN/ui/components/DebugControls.kt`, `app/src/main/res/values/strings.xml`
- Modify tests: `TEST/ui/screens/ChantScreenStateTest.kt`, `TEST/ritual/RitualViewModelTest.kt`, `TEST/ritual/SessionLogTest.kt`, `TEST/ritual/SessionReplay.kt`, `TEST/ritual/SessionReplayTest.kt`

- [ ] **Step 1: `ChantScreenState.kt`**

a) Replace `enum class WordLook { Pending, Current, Heard, Doubtful, Rejected }` with `enum class WordLook { Pending, Current, Heard }` and `enum class VerdictLine { None, Listening, ChantAgain, SealCracked }` (and its KDoc) with:

```kotlin
/** The one line under the words (ADR-0027): nothing before the lock, the voice meter, or a crack. */
enum class VerdictLine { None, Listening, SealCracked }
```

b) Replace `data class StatusBlock(val step: StepState, val turn: TurnDirection?, val seals: List<SealState>)` with:

```kotlin
/** The two-row status block under the sigil (ADR-0024). [turn] is null when aligned or done; [checking] pulses the current seal (ADR-0027). */
data class StatusBlock(val step: StepState, val turn: TurnDirection?, val seals: List<SealState>, val checking: Boolean = false)
```

c) Change the signature to `fun chantScreenState(ritual: RitualState, phrases: List<String>, azimuth: Float?, target: Float, checking: Boolean = false): ChantScreenState`, and inside it replace the `val words = when { … }` block with:

```kotlin
    val words = if (ritual.phrase >= PHRASE_COUNT) {
        text.map { ChantWord(it, it.size, WordLook.Heard) }
    } else {
        chantingWords(ritual, text)
    }
```

replace the `val line = when { … }` block with:

```kotlin
    val line = when {
        verdict?.kind == ChantVerdict.Kind.Cracked -> VerdictLine.SealCracked
        ritual.phase == RitualPhase.Chanting || ritual.phase == RitualPhase.Wavering -> VerdictLine.Listening
        else -> VerdictLine.None
    }
```

change `facing = ritual.aligned,` to `facing = ritual.aligned || ritual.phase == RitualPhase.Chanting,` and `status = statusBlock(ritual, azimuth, target, verdict),` to `status = statusBlock(ritual, azimuth, target, verdict, checking),`.

d) Replace `chantingWords(...)` with:

```kotlin
/** While chanting: lit syllables, the current word, and words heard (covered by checks, or fully lit in lenient mode). */
private fun chantingWords(ritual: RitualState, text: List<List<String>>): List<ChantWord> {
    var start = 0
    return text.mapIndexed { i, syllables ->
        val lit = (ritual.lit - start).coerceIn(0, syllables.size)
        val current = ritual.lit in start until start + syllables.size
        start += syllables.size
        val heard = if (ritual.mode == CheckMode.Whisper) i in ritual.covered else lit == syllables.size
        val look = when {
            heard -> WordLook.Heard
            current -> WordLook.Current
            else -> WordLook.Pending
        }
        ChantWord(syllables, lit, look)
    }
}
```

e) Replace `statusBlock(...)` with:

```kotlin
private fun statusBlock(ritual: RitualState, azimuth: Float?, target: Float, verdict: ChantVerdict?, checking: Boolean): StatusBlock {
    val done = ritual.phrase >= PHRASE_COUNT
    val step = when {
        done -> StepState.Done
        ritual.phase == RitualPhase.Wavering -> StepState.Drifting
        ritual.aligned || ritual.phase == RitualPhase.Chanting -> StepState.Aligned
        else -> StepState.Seeking
    }
    val seals = (0 until PHRASE_COUNT).map { p ->
        when {
            verdict?.kind == ChantVerdict.Kind.Cracked && p == verdict.phrase -> SealState(SealLook.Cracked, 1, 0)
            p < ritual.phrase -> SealState(SealLook.Sealed, 1, 1)
            p == ritual.phrase -> SealState(SealLook.Current, 1, 0)
            else -> SealState(SealLook.Pending, 1, 0)
        }
    }
    val turn = if (step == StepState.Aligned || step == StepState.Done) null else turnDirection(azimuth, target)
    return StatusBlock(step, turn, seals, checking && !done)
}
```

f) Remove the imports of `WordMark` and of anything chunk-related; add `import com.summondemons.safely.ritual.CheckMode`.

- [ ] **Step 2: Faster pulse while checking (`RitualStatusBlock.kt`)**

Change the `Seal(...)` call `Seal(ROMAN_NUMERALS[i], seal, accent, dimmed = status.step == StepState.Drifting, Modifier.size(SealSize))` to

```kotlin
                Seal(ROMAN_NUMERALS[i], seal, accent, dimmed = status.step == StepState.Drifting, checking = status.checking, modifier = Modifier.size(SealSize))
```

change the signature `private fun Seal(numeral: String, seal: SealState, accent: Color, dimmed: Boolean, modifier: Modifier)` to `private fun Seal(numeral: String, seal: SealState, accent: Color, dimmed: Boolean, checking: Boolean, modifier: Modifier)`, and in its breathing animation replace `tween(BREATH_HALF_MS)` with `tween(if (checking) BREATH_HALF_MS / 3 else BREATH_HALF_MS)`.

- [ ] **Step 3: `ChantScreen.kt`**

a) At the call `chantScreenState(state.ritual, demon.chantPhrases, state.azimuth, demon.ritualHeading…)` add `checking = state.checking` as the last argument.

b) In `Phrase(...)`, replace

```kotlin
                val color = when {
                    word.look == WordLook.Rejected -> colors.error
                    word.look == WordLook.Heard || word.look == WordLook.Doubtful -> accent
                    si < word.lit -> accent
                    else -> dim
                }
```

with

```kotlin
                val color = if (word.look == WordLook.Heard || si < word.lit) accent else dim
```

and replace the `lineColors` mapping with:

```kotlin
    val lineColors = words.map {
        when (it.look) {
            WordLook.Pending -> colors.outlineVariant
            WordLook.Current -> colors.onSurfaceVariant
            WordLook.Heard -> safe
        }
    }
```

c) In `drawWordLines(...)`, replace the `when (words[i].look) { … }` block with:

```kotlin
        val width = if (words[i].look == WordLook.Pending) thickness * PENDING_LINE_SCALE else thickness
        drawLine(color, Offset(first.left, y), Offset(last.right, y), width, StrokeCap.Round)
```

then delete the now-unused `badgeInk` parameter (and the argument `colors.onError` at its call), the `drawWave` function and the constants `DOUBT_ALPHA`, `WAVE_AMPLITUDE`, `BADGE_FRACTION`.

d) In `VerdictLine(...)`, replace

```kotlin
            VerdictLine.ChantAgain, VerdictLine.SealCracked -> {
                val label = if (screen.verdict == VerdictLine.ChantAgain) R.string.verdict_chant_again else R.string.verdict_seal_cracked
                Text(stringResource(label).upper(), style = style, color = colors.error, maxLines = 1)
```

with

```kotlin
            VerdictLine.SealCracked -> {
                Text(stringResource(R.string.verdict_seal_cracked).upper(), style = style, color = colors.error, maxLines = 1)
```

- [ ] **Step 4: Debug panel**

In `app/src/main/res/values/strings.xml` replace the `debug_chant_score` string with

```xml
    <string name="debug_chant_score">Check %1$s %2$.2f %3$s · %4$d ms · “%5$s”</string>
    <string name="debug_chant_mode">Check mode: lenient (%1$s)</string>
```

and delete `verdict_chant_again`.

In `MAIN/ui/components/DebugControls.kt`, replace the parameter `lastJudged: RitualEffect.Judged?,` (and its KDoc) with

```kotlin
    /** The latest Whisper check (ADR-0027). */
    lastCheck: CheckReport?,
    /** Why the ritual runs in lenient mode; empty in Whisper mode. */
    modeReason: String,
```

replace the import `com.summondemons.safely.ritual.RitualEffect` with `com.summondemons.safely.ritual.CheckReport`, and replace the score `Text(...)` with:

```kotlin
        Text(
            when {
                modeReason.isNotEmpty() -> stringResource(R.string.debug_chant_mode, modeReason)
                lastCheck == null -> stringResource(R.string.debug_chant_score_none)
                else -> stringResource(
                    R.string.debug_chant_score,
                    ROMAN_NUMERALS[lastCheck.heard.phrase],
                    lastCheck.heard.distance,
                    if (lastCheck.heard.found) "✓" else "✗",
                    lastCheck.checkMs,
                    lastCheck.transcript.take(40),
                )
            },
            style = small,
            color = colors.onSurfaceVariant,
        )
```

In `MAIN/ui/screens/RitualScreen.kt` replace `lastJudged = state.lastJudged,` with `lastCheck = state.lastCheck,` and `modeReason = state.modeReason,`, and delete the `is RitualEffect.Rejected -> { … }` branch (three lines) in the effects `when`.

- [ ] **Step 5: Intro copy (`strings.xml`)**

```xml
    <string name="intro_chant_body">Read the line aloud until it seals.</string>
    <string name="intro_lines_body">Green: heard.</string>
    <string name="intro_focus_body">Each line gets a minute; the ring shows what is left.</string>
```

- [ ] **Step 6: Tests**

`TEST/ui/screens/ChantScreenStateTest.kt`:
- replace every `RitualState.start(…, judged = true)` with `RitualState.start(…, mode = CheckMode.Whisper)` and add `import com.summondemons.safely.ritual.CheckMode`;
- delete `aRejectionShowsTheDoubtfulWordsUntilChantingResumes` and `theCurrentSealCountsItsPassedParts`;
- replace `wordsOfPassedChunksAreHeard` with:

```kotlin
    @Test
    fun coveredWordsAreHeardInWhisperMode() {
        val r = chanting.copy(lit = 2, covered = setOf(1))
        val s = state(r)
        assertEquals(listOf(WordLook.Current, WordLook.Heard), s.words.map { it.look })
    }

    @Test
    fun fullyLitWordsAreHeardInLenientMode() {
        val r = RitualState.start(90f, phrases).copy(phase = RitualPhase.Chanting, azimuth = 90f, lit = 5)
        assertEquals(listOf(WordLook.Heard, WordLook.Current), chantScreenState(r, phrases, 90f, 90f).words.map { it.look })
    }

    @Test
    fun aRunningCheckPulsesTheStatusBlock() {
        assertTrue(chantScreenState(chanting, phrases, 90f, 90f, checking = true).status.checking)
    }
```

- remove the imports that no longer resolve (`WordMark`).

`TEST/ritual/RitualViewModelTest.kt`, in the Hold-to-Chant test: replace

```kotlin
            effects.filter { it != SyllableLit && it != ProximityTick && it !is RitualEffect.Judged && it !is RitualEffect.ChunkPassed },
```

with

```kotlin
            effects.filter { it != SyllableLit && it != ProximityTick && it !is RitualEffect.LineChecked },
```

and delete the four lines from `val judged = effects.filterIsInstance<RitualEffect.Judged>()` through `assertEquals(0, vm.uiState.value.rejections)`. (The ViewModel there runs in the default lenient mode, so Hold to Chant seals by lighting.)

`TEST/ritual/SessionReplay.kt`: replace `RitualState.start(header.target, header.phrases, judged = header.judged)` with `RitualState.start(header.target, header.phrases, mode = CheckMode.valueOf(header.mode))`.

`TEST/ritual/SessionLogTest.kt`: replace the header with `SessionHeader("asmodeus", 0f, phrases, mode = "Whisper", startedMs = 1L)`; in `record(...)` start with `RitualState.start(0f, phrases, mode = CheckMode.Whisper)` and pass `RitualInput(0f, voice, heard = heardAt[i])` where `heardAt` is a parameter `heardAt: Map<Int, LineHeard> = emptyMap()`; replace `aLoggedSessionReplaysToTheSameEffects` with:

```kotlin
    @Test
    fun aLoggedSessionReplaysToTheSameEffects() {
        val lockFrames = (LOCK_HOLD_MS / FRAME_MS).toInt()
        val frames = List(lockFrames + 50) { VoiceFrame.SILENT }
        val heardAt = mapOf(
            lockFrames + 10 to LineHeard(0, found = false, distance = 0.7, covered = setOf(1)),
            lockFrames + 20 to LineHeard(0, found = true, distance = 0.1, covered = setOf(0, 1)),
        )
        val (h, ticks) = SessionRecords.decode(record(frames, heardAt))
        val replayed = SessionReplay.replay(h, ticks)
        replayed.forEach { r -> assertEquals(r.logged.fx, r.step.effects.map(SessionRecords::effectName)) }
        assertTrue(ticks.any { "Sealed(phrase=0)" in it.fx })
        assertEquals(listOf(false, true), ticks.mapNotNull { it.heard?.found })
    }
```

and in `aHeaderAndTicksRoundTrip` replace `c = 2` usage as is (it is now the covered count) — no change needed. Delete the helpers `frame`, `sing` if unused.

`TEST/ritual/SessionReplayTest.kt`: replace the block that prints `then`/`now` judged verdicts with:

```kotlin
                replayed.forEach { r ->
                    r.logged.check?.let { c ->
                        appendLine("   %6.1fs %s check %s dist %.2f %d ms “%s”".format(
                            r.logged.t / 1000.0, ROMAN[c.phrase], verdict(c.found), c.distance, c.checkMs, c.transcript.take(60),
                        ))
                    }
                }
                val seals = replayed.count { r -> r.step.effects.any { it is RitualEffect.Sealed } }
                appendLine("   sealed $seals; phase changes ${replayed.zipWithNext().count { (a, b) -> a.logged.ph != b.logged.ph }}")
```

and remove the now-unused `phoneRejects`/`nowRejects` variables and the alignment-path printing.

- [ ] **Step 7: Build and run everything**

Run: `./gradlew testDebugUnitTest lintDebug assembleDebug`
Expected: BUILD SUCCESSFUL; all tests pass (including `WhisperModeTest`, `LineMatcherTest`, `LineListenerTest`, `Resampler16kTest`).

- [ ] **Step 8: Commit**

```bash
git add -A app/src
git commit -q -m "feat(ui): Words turn green as Whisper hears them; no rejection state; check readout (ADR-0027)

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

## Phase 5 — Threshold, docs, verification

### Task 7: Desktop greedy re-run of the threshold

**Files:**
- Modify: `tools/experiments/whisper_latin.py`, `tools/experiments/whisper_real_lines.py`

- [ ] **Step 1: Make decoding selectable**

In both scripts, replace `beam_size=5` with `beam_size=int(__import__("os").environ.get("BEAM", "5"))`.

- [ ] **Step 2: Run with greedy decoding (beam 1)**

```bash
mkdir -p /tmp/real && cp app/src/test/chant-clips/*.wav /tmp/real/
BEAM=1 uv run --no-project --python 3.11 --with faster-whisper --with soundfile --with numpy \
  python -I tools/experiments/whisper_real_lines.py /tmp/real app/src/main/java/com/summondemons/safely/data/DemonCatalog.kt
```

Expected: honest lines within the ADR-0026 ranges; if any honest line now exceeds 0.60 while no wrong line is below 0.65, note it; if honest and wrong overlap more than with beam 5, set the native decoder to beam search (`WHISPER_SAMPLING_BEAM_SEARCH` with `p.beam_search.beam_size = 3`) in `chant_whisper.cpp` and re-measure Task 1's spike.

- [ ] **Step 3: Commit**

```bash
git add tools/experiments
git commit -q -m "chore(tools): Select Whisper beam size in the experiments (ADR-0027)

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

### Task 8: Threshold re-check on logged sessions

**Files:**
- Create: `TEST/ritual/TranscriptRescoreTest.kt`

- [ ] **Step 1: Create the harness**

```kotlin
package com.summondemons.safely.ritual

import com.summondemons.safely.data.DemonCatalog
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * Re-scores every logged Whisper transcript (ADR-0027) at a range of thresholds: how many checks
 * of the line being chanted would pass, and how many would match a different line of either demon.
 * Sessions come from `tools/pull_sessions.sh` into `app/src/test/sessions/` (git-ignored).
 * Run: `./gradlew testDebugUnitTest --tests '*TranscriptRescoreTest*' -i`.
 */
class TranscriptRescoreTest {

    private val dir = File(System.getProperty("sessions") ?: "src/test/sessions")

    @Test
    fun rescoreLoggedTranscripts() {
        val checks = dir.listFiles { f -> f.extension == "jsonl" }.orEmpty().flatMap { file ->
            val (header, ticks) = SessionReplay.read(file)
            ticks.mapNotNull { t -> t.check?.let { header to it } }
        }
        assumeTrue("no logged Whisper checks in ${dir.absolutePath}", checks.isNotEmpty())
        val allLines = DemonCatalog.all.flatMap { d -> d.chantPhrases.map { d.id to it } }
        println("threshold  own-line pass  other-line match   (checks n=${checks.size})")
        for (t in listOf(0.40, 0.45, 0.50, 0.55, 0.60, 0.65, 0.70)) {
            var own = 0
            var other = 0
            checks.forEach { (header, c) ->
                val segments = listOf(TranscriptSegment(c.transcript))
                if (LineMatcher.match(header.phrases[c.phrase], segments).distance <= t) own++
                val otherBest = allLines.filter { it != header.demon to header.phrases[c.phrase] }
                    .minOf { (_, line) -> LineMatcher.match(line, segments).distance }
                if (otherBest <= t) other++
            }
            println("  %.2f       %5.1f %%         %5.1f %%".format(t, own * 100.0 / checks.size, other * 100.0 / checks.size))
        }
    }
}
```

Check that `DemonCatalog.all` exists:
Run: `grep -n "val all" app/src/main/java/com/summondemons/safely/data/DemonCatalog.kt`
Expected: a `val all: List<Demon>` line. If the catalog exposes the list under another name, use that name.

- [ ] **Step 2: Run (skips without sessions)**

Run: `./gradlew testDebugUnitTest --tests '*TranscriptRescoreTest*' -i`
Expected: SKIPPED (no logged Whisper checks yet) or a threshold table.

- [ ] **Step 3: Commit**

```bash
git add app/src/test/java/com/summondemons/safely/ritual/TranscriptRescoreTest.kt
git commit -q -m "test(ritual): Re-score logged Whisper transcripts at several thresholds (ADR-0027)

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

### Task 9: Docs, privacy and licences

**Files:**
- Modify: `docs/decisions/0027-whisper-chant-check.md`, `docs/decisions/0026-whisper-latin-chant-check.md`, `docs/decisions/0018-chant-plausibility-check.md`, `docs/decisions/0022-chant-check-v2.md`, `docs/decisions/0024-status-column.md`, `docs/decisions/0025-play-test-fixes-session-log-corpus.md`, `docs/PRIVACY_POLICY.md`, `docs/STORE_LISTING.md`, `PRODUCT.md`, `DESIGN.md`, `AGENTS.md`
- Create: `app/src/main/assets/licenses/whisper.txt`

- [ ] **Step 1: Licences**

Create `app/src/main/assets/licenses/whisper.txt` with the MIT licence text of whisper.cpp (copy `app/src/main/cpp/whisper.cpp/LICENSE` verbatim) followed by:

```
Whisper model weights (ggml-base-q5_1.bin) © OpenAI, released under the MIT License;
converted to ggml format by the whisper.cpp project.
```

Check that the assets folder is packaged: `git check-ignore app/src/main/assets/licenses/whisper.txt` prints nothing (only `assets/whisper/` is ignored).

- [ ] **Step 2: ADR-0027**

Complete `docs/decisions/0027-whisper-chant-check.md` (it already has the "Spike" section from Task 1) so it reads, above the Spike section:

```markdown
# 0027 — On-device Whisper chant check

- Date: 2026-10-18
- Status: Accepted. Replaces the vowel judge and chunks of ADR-0022; amends ADR-0018 (no words recognised → on-device recognition), ADR-0024 (no part arcs, no rejection state) and ADR-0025 (copy, timer refill on seal only).

## Decision
- Whisper base (q5_1, MIT) runs on the phone through whisper.cpp v1.9.4 (MIT), language Latin, no prompt, greedy decoding, encoder context sized to the audio.
- **Listen until found:** after each pause (≥ 0.6 s after ≥ 0.8 s of new voice) or 6 s of continuous voice, the audio since the line began (≤ 25 s) is transcribed; the line seals when its letters are within 0.60 of the best-matching stretch of the transcript (J→I, V→U; repeats collapsed; likely-silence segments dropped). No rejection state.
- Words turn green as checks cover them; the current seal pulses faster while a check runs.
- Drift hysteresis: lock at ±20°, waver beyond 25°, resume within 20°.
- Devices that fail to load the model or need more than 4 s for a 5 s check use the lenient rule (a line seals when every syllable lit), decided once per install.
- The model ships in the app (≈ 57 MB); `tools/fetch_whisper_model.sh` downloads it for builds.

## Consequences
Speech is recognised on the device; transcripts are discarded at once in release builds and kept
only in debug session logs. Half a long line can pass (plan note 4); `TranscriptRescoreTest`
re-checks the threshold on logged sessions.
```

- [ ] **Step 3: Status lines of older ADRs**

Append to the `Status:` line of:
- ADR-0026: change `Status: Proposed.` to `Status: Accepted; implemented by ADR-0027.`
- ADR-0018: ` Superseded for the verdict by ADR-0027.`
- ADR-0022: ` Superseded by ADR-0027.`
- ADR-0024: ` Amended by ADR-0027 (whole-line seals, no rejection state).`
- ADR-0025: ` Amended by ADR-0027 (judge, copy).`

- [ ] **Step 4: Privacy policy and store listing**

Run: `grep -n -i "recogni\|vowel\|rhythm\|words" docs/PRIVACY_POLICY.md docs/STORE_LISTING.md`

Rewrite every sentence that says no words or speech are recognised, or that only vowel sounds and rhythm are measured, to: "Speech is recognised on your device, only to check that you chant the line. Nothing you say is stored or sent anywhere." Keep "no data collected" in the Data safety notes.

- [ ] **Step 5: PRODUCT.md, DESIGN.md, AGENTS.md**

- PRODUCT.md, **Chant** row: replace everything from "Lines over 8 syllables are judged…" to the end of the row with: "The line seals when it is heard: after each pause the phone transcribes, on the device and in Latin, everything said since the line began, and the line seals as soon as it is found in it (ADR-0027). Repeats and restarts are fine; there is no rejection. Words turn green as they are heard. On phones too slow for this, a line seals when every syllable has lit. |"
- PRODUCT.md, **Chant screen** row: replace "four seals I–IV whose rings show the line's parts" with "four seals I–IV (the current one pulses faster while the phone is listening)" and "the voice meter, or \"CHANT AGAIN ✕\" / \"SEAL CRACKED ✕\"" with "the voice meter, or \"SEAL CRACKED ✕\"".
- DESIGN.md, `RitualStatusBlock` row: replace "ring split per chunk, passed arcs in accent" with "plain ring; the current seal breathes 3× faster while a check runs"; in the `ChantScreen` row delete "doubtful primary 70 %, rejected error wavy + ✕ badge" and "\"CHANT AGAIN ✕\" /".
- AGENTS.md, Commands section, add before the build commands:

```bash
tools/fetch_whisper_model.sh   # once: downloads the Whisper model the build needs (ADR-0027)
git submodule update --init    # once: whisper.cpp sources
```

and in "Current state" add: "Chant check: Whisper base on device (ADR-0027); `TranscriptRescoreTest` re-scores logged transcripts."

- [ ] **Step 6: Commit**

```bash
git add docs PRODUCT.md DESIGN.md AGENTS.md app/src/main/assets/licenses
git commit -q -m "docs: Record the on-device Whisper chant check (ADR-0027), privacy and licences

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

### Task 10: Verification on device

- [ ] **Step 1: Full gate**

Run: `./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease`
Expected: BUILD SUCCESSFUL (release may be unsigned locally; ADR-0017).

- [ ] **Step 2: Install and clear the device check**

```bash
ANDROID_SERIAL=RXCX10467TP ./gradlew installDebug
ANDROID_SERIAL=RXCX10467TP adb shell pm clear com.summondemons.safely
```

Open the app, wait ~10 s on Home (the device check runs once), then start a ritual. The debug panel must **not** show "Check mode: lenient"; if it does, its reason says why.

- [ ] **Step 3: Device checklist (both voices, Record clip: chant on)**

- Asmodeus and Astaroth, read normally: every line seals within a few seconds of finishing it; words turn green as they are heard.
- Read a line in fragments with restarts: it still seals.
- Say "Olá meu amigo, como está hoje?" repeatedly for a whole line: it never seals; the timer runs out after 60 s and cracks (line II+).
- Turn 22° away while chanting: nothing changes; 30° away: "drifting"; back within 20°: resumes.
- The current seal pulses faster while a check runs; the debug panel shows transcript, distance and milliseconds.

- [ ] **Step 4: Re-check the threshold**

```bash
tools/pull_sessions.sh
./gradlew testDebugUnitTest --tests '*TranscriptRescoreTest*' -i
```

Expected: own-line pass ≥ 95 % at 0.60 with other-line matches clearly lower. If not, adjust `LINE_MATCH_MAX` in `RitualConfig`, record the table in ADR-0027 under a "Calibration" section, re-run the gate, and commit:

```bash
git add app/src/main/java/com/summondemons/safely/ritual/RitualConfig.kt docs/decisions/0027-whisper-chant-check.md
git commit -q -m "chore(ritual): Calibrate the line-match threshold on logged sessions (ADR-0027)

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```
