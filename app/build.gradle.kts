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
        versionCode = 1
        versionName = "1.0.0"
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
}
