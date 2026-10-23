import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Upload key (ADR-0017): environment variables first, then local.properties; never in git.
val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.isFile }?.inputStream()?.use { load(it) }
}
fun signingValue(key: String): String? =
    providers.environmentVariable(key).orNull ?: localProperties.getProperty(key)
val uploadStoreFile = signingValue("SUMMON_UPLOAD_STORE_FILE")
val hasUploadKey = listOf(
    "SUMMON_UPLOAD_STORE_FILE", "SUMMON_UPLOAD_STORE_PASSWORD",
    "SUMMON_UPLOAD_KEY_ALIAS", "SUMMON_UPLOAD_KEY_PASSWORD",
).all { !signingValue(it).isNullOrBlank() }

android {
    namespace = "com.summondemons.safely"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.summondemons.safely"
        minSdk = 24
        targetSdk = 37
        versionCode = 2
        versionName = "1.0.0"
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
    }
    signingConfigs {
        if (hasUploadKey) {
            create("release") {
                storeFile = file(uploadStoreFile!!)
                storePassword = signingValue("SUMMON_UPLOAD_STORE_PASSWORD")
                keyAlias = signingValue("SUMMON_UPLOAD_KEY_ALIAS")
                keyPassword = signingValue("SUMMON_UPLOAD_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            // English (XA) and Arabic (XB) in the device language list, for layout checks (i18n spec §5.2)
            isPseudoLocalesEnabled = true
        }
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
            if (hasUploadKey) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                logger.warn("Release is UNSIGNED: set SUMMON_UPLOAD_* (env or local.properties), see ADR-0017.")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        // java.time (GameClock, progress days) on API 24–25.
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    ndkVersion = "28.2.13676358"
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    androidResources {
        noCompress += "bin"
        // Lists the shipped languages for Android 13+ per-app language settings (i18n spec §4)
        generateLocaleConfig = true
    }
    lint {
        error += "MissingTranslation"
    }
}

// The Whisper model is downloaded, not committed (ADR-0027).
abstract class VerifyWhisperModelTask : DefaultTask() {
    @get:InputFile
    abstract val modelFile: RegularFileProperty

    @TaskAction
    fun verify() {
        val file = modelFile.get().asFile
        check(file.exists()) { "Whisper model missing: run tools/fetch_whisper_model.sh (ADR-0027)" }
    }
}

val verifyWhisperModel = tasks.register<VerifyWhisperModelTask>("verifyWhisperModel") {
    modelFile.set(layout.projectDirectory.file("src/main/assets/whisper/ggml-base-q5_1.bin"))
}
tasks.named("preBuild") { dependsOn(verifyWhisperModel) }

// Calibration harness switches (ADR-0018, ADR-0025): -PchantClips=…, -PchantSource=real|generated, -PchantDump=1, -Psessions=…
tasks.withType<Test>().configureEach {
    listOf("chantClips", "chantSource", "chantDump", "sessions").forEach { key ->
        project.findProperty(key)?.let { systemProperty(key, it.toString()) }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.core.splashscreen)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
}
