# Compose Port Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the XML "Basic Views Activity" template in `app/` with the Jetpack Compose game described in `PRODUCT.md` / `DESIGN.md`, porting `reference/src/` and fixing its known bugs.

**Architecture:** Single-activity Compose app (`com.summondemons.safely`) with type-safe navigation-compose (Grimoire → Ritual → Profile). An activity-scoped `DemonViewModel` owns the demon catalog and in-memory unlock state; a per-entry `RitualViewModel` runs the ritual as a cold-flow pipeline over injected `HeadingSource` / `VolumeSource` interfaces and a pure `RitualEngine`, exposing one `StateFlow<RitualUiState>`. UI reads only theme tokens (`MaterialTheme` + `DemonColors` + `Spacing`).

**Tech Stack:** AGP 9.4.1 (built-in Kotlin 2.2.10), Compose BOM 2026.09.00 + Material 3, navigation-compose 2.10.2, lifecycle 2.11.0, activity-compose 1.13.0, kotlinx-serialization (via navigation), JUnit 4 + kotlinx-coroutines-test 1.11.0. Spec: `docs/superpowers/specs/2026-10-15-compose-port-design.md`.

**Working directory:** the repo root (the worktree). All paths below are relative to it. Do not edit anything under `reference/` (read-only input).

**Commit trailer:** every commit message ends with a blank line and
`Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>` (the `-m` second
paragraph in the commands below does this).

**Path shorthand used in this plan:**
- `MAIN` = `app/src/main/java/com/summondemons/safely`
- `TEST` = `app/src/test/java/com/summondemons/safely`
- `RES` = `app/src/main/res`

These are just abbreviations for reading; every command spells out the full path.

---

## File map

| Path | Action | Responsibility |
|---|---|---|
| `gradle/libs.versions.toml` | Rewrite | Versions, Compose/nav/lifecycle libs, Kotlin plugins |
| `build.gradle.kts`, `settings.gradle.kts` | Modify | Declare plugins; project name |
| `app/build.gradle.kts` | Rewrite | Compose + serialization, new namespace, deps |
| `app/src/main/AndroidManifest.xml` | Rewrite | Permissions, portrait, theme |
| `RES/values/{colors,strings,themes}.xml` | Rewrite | Window theme, obsidian color, UI copy |
| `RES/drawable-nodpi/*.webp` | Create | Demon art, sigil, logo |
| `RES/drawable/ic_lock.xml`, `ic_launcher_foreground.xml` | Create / Rewrite | Lock glyph, adaptive icon foreground |
| `RES/mipmap-*` | Rewrite | Launcher icons from logo |
| `RES/raw/tada.wav` | Create (generated) | Success fanfare placeholder |
| `tools/gen_tada.py` | Create | Generates `tada.wav` |
| `MAIN/MainActivity.kt` | Create | Edge-to-edge, theme, nav host |
| `MAIN/data/Demon.kt`, `DemonCatalog.kt` | Create | Demon model + the two MVP demons |
| `MAIN/navigation/Routes.kt`, `AppNavHost.kt` | Create | Type-safe routes and graph |
| `MAIN/ritual/RitualConfig.kt` | Create | Tuning constants |
| `MAIN/ritual/HeadingMath.kt` | Create | Angle math, compass labels, smoother |
| `MAIN/ritual/RitualEngine.kt` | Create | Pure progress state machine |
| `MAIN/ritual/Sources.kt` | Create | `HeadingSource`, `VolumeSource`, `rms()` |
| `MAIN/ritual/RitualViewModel.kt` | Create | Ritual pipeline + UI state |
| `MAIN/platform/AndroidHeadingSource.kt` | Create | Sensor-backed azimuth flow |
| `MAIN/platform/AndroidVolumeSource.kt` | Create | AudioRecord-backed RMS flow |
| `MAIN/platform/Haptics.kt`, `TadaPlayer.kt` | Create | Vibration, sound |
| `MAIN/ui/DemonViewModel.kt` | Create | Catalog + unlock state |
| `MAIN/ui/theme/*.kt` | Create | Color, Type, Shape, Spacing, DemonColors, Theme |
| `MAIN/ui/components/*.kt` | Create | Six DESIGN.md components + DebugControls |
| `MAIN/ui/screens/*.kt` | Create | Grimoire, Ritual, Profile |
| `TEST/**` | Create | Unit tests |
| `PRODUCT.md`, `DESIGN.md`, `AGENTS.md`, `docs/decisions/0007-*.md` | Modify / Create | Docs |
| Template files (`com/example/**`, layouts, menu, nav_graph, `values-*`, dimens) | Delete | — |

---

### Task 1: Build setup, package rename, template removal

**Files:**
- Rewrite: `gradle/libs.versions.toml`, `build.gradle.kts`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/main/res/values/colors.xml`, `app/src/main/res/values/strings.xml`, `app/src/main/res/values/themes.xml`
- Modify: `settings.gradle.kts` (last-but-one line)
- Create: `app/src/main/java/com/summondemons/safely/MainActivity.kt` (placeholder, replaced in Task 11)
- Delete: everything under `app/src/main/java/com/example`, `app/src/test/java/com/example`, `app/src/androidTest`, `app/src/main/res/{layout,menu,navigation,values-land,values-night,values-v23,values-w600dp,values-w1240dp}`, `app/src/main/res/values/dimens.xml`

- [ ] **Step 1: Delete the template sources and resources**

```bash
git rm -r -q app/src/main/java/com/example app/src/test/java/com/example app/src/androidTest \
  app/src/main/res/layout app/src/main/res/menu app/src/main/res/navigation \
  app/src/main/res/values-land app/src/main/res/values-night app/src/main/res/values-v23 \
  app/src/main/res/values-w600dp app/src/main/res/values-w1240dp app/src/main/res/values/dimens.xml
```

- [ ] **Step 2: Rewrite `gradle/libs.versions.toml`**

```toml
[versions]
agp = "9.4.1"
# Must match the Kotlin version built into AGP 9.4.1.
kotlin = "2.2.10"
coreKtx = "1.19.1"
composeBom = "2026.09.00"
activityCompose = "1.13.0"
navigationCompose = "2.10.2"
lifecycle = "2.11.0"
junit = "4.13.2"
coroutinesTest = "1.11.0"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutinesTest" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

- [ ] **Step 3: Rewrite root `build.gradle.kts`**

```kotlin
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
```

- [ ] **Step 4: Rename the Gradle project**

In `settings.gradle.kts`, replace `rootProject.name = "My Demon"` with:

```kotlin
rootProject.name = "Summon Demons Safely"
```

- [ ] **Step 5: Rewrite `app/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

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
        versionName = "1.0"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
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
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
```

- [ ] **Step 6: Rewrite `app/src/main/AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.VIBRATE" />

    <uses-feature android:name="android.hardware.microphone" android:required="false" />
    <uses-feature android:name="android.hardware.sensor.compass" android:required="false" />

    <application
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.SummonDemonsSafely">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:screenOrientation="portrait"
            android:theme="@style/Theme.SummonDemonsSafely">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />

                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

- [ ] **Step 7: Rewrite `app/src/main/res/values/colors.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- DESIGN.md `background`. Used only by the window theme and the adaptive icon;
         Compose reads colors from ui/theme. -->
    <color name="obsidian">#FF0B1326</color>
</resources>
```

- [ ] **Step 8: Rewrite `app/src/main/res/values/strings.xml`** (UI copy is added in Task 11)

```xml
<resources>
    <string name="app_name">Summon Demons Safely</string>
</resources>
```

- [ ] **Step 9: Rewrite `app/src/main/res/values/themes.xml`**

```xml
<resources>
    <style name="Theme.SummonDemonsSafely" parent="android:Theme.Material.NoActionBar">
        <item name="android:windowBackground">@color/obsidian</item>
    </style>
</resources>
```

- [ ] **Step 10: Create a placeholder `app/src/main/java/com/summondemons/safely/MainActivity.kt`**

```kotlin
package com.summondemons.safely

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Text("Summon Demons Safely") }
    }
}
```

- [ ] **Step 11: Build**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`.
If a dependency fails with "Module was compiled with an incompatible version of Kotlin. The binary version of its metadata is X, expected version is Y", step that library down to its newest release that compiles with Kotlin 2.2.10 (check the release notes), rebuild, and write the chosen version and reason as a comment above it in `libs.versions.toml`.

- [ ] **Step 12: Verify no template package remains**

Run: `grep -rn "com.example" app/ || echo clean`
Expected: `clean`

- [ ] **Step 13: Commit**

```bash
git add -A gradle/libs.versions.toml build.gradle.kts settings.gradle.kts app
git commit -q -m "build(app): Switch to Compose and rename package to com.summondemons.safely" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 2: Theme tokens

**Files:**
- Create: `app/src/main/java/com/summondemons/safely/ui/theme/Color.kt`, `DemonColors.kt`, `Spacing.kt`, `Type.kt`, `Shape.kt`, `Theme.kt`

Values are copied 1:1 from the `DESIGN.md` frontmatter. No other file may contain a hex color.

- [ ] **Step 1: Create `ui/theme/Color.kt`**

```kotlin
package com.summondemons.safely.ui.theme

import androidx.compose.ui.graphics.Color

/** DESIGN.md color tokens, one per frontmatter key. Only ui/theme reads these. */
internal object Palette {
    val background = Color(0xFF0B1326)
    val onBackground = Color(0xFFDAE2FD)
    val surface = Color(0xFF0B1326)
    val surfaceDim = Color(0xFF0B1326)
    val surfaceBright = Color(0xFF31394D)
    val surfaceContainerLowest = Color(0xFF060E20)
    val surfaceContainerLow = Color(0xFF131B2E)
    val surfaceContainer = Color(0xFF171F33)
    val surfaceContainerHigh = Color(0xFF222A3D)
    val surfaceContainerHighest = Color(0xFF2D3449)
    val surfaceVariant = Color(0xFF2D3449)
    val onSurface = Color(0xFFDAE2FD)
    val onSurfaceVariant = Color(0xFFD8C3AD)
    val inverseSurface = Color(0xFFDAE2FD)
    val inverseOnSurface = Color(0xFF283044)
    val outline = Color(0xFFA08E7A)
    val outlineVariant = Color(0xFF534434)
    val surfaceTint = Color(0xFFFFB95F)
    val primary = Color(0xFFFFC174)
    val onPrimary = Color(0xFF472A00)
    val primaryContainer = Color(0xFFF59E0B)
    val onPrimaryContainer = Color(0xFF613B00)
    val inversePrimary = Color(0xFF855300)
    val secondary = Color(0xFF7BD0FF)
    val onSecondary = Color(0xFF00354A)
    val secondaryContainer = Color(0xFF00A6E0)
    val onSecondaryContainer = Color(0xFF00374D)
    val tertiary = Color(0xFFFCB4FF)
    val onTertiary = Color(0xFF580065)
    val tertiaryContainer = Color(0xFFF487FF)
    val onTertiaryContainer = Color(0xFF770088)
    val error = Color(0xFFFFB4AB)
    val onError = Color(0xFF690005)
    val errorContainer = Color(0xFF93000A)
    val onErrorContainer = Color(0xFFFFDAD6)
    val demonAsmodeus = Color(0xFFFFD54F)
    val demonAstaroth = Color(0xFFE040FB)
    val ritualSafe = Color(0xFF4ADE80)
}
```

- [ ] **Step 2: Create `ui/theme/DemonColors.kt`**

```kotlin
package com.summondemons.safely.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.summondemons.safely.data.DemonAccent

/** DESIGN.md tokens with no Material slot. */
@Immutable
data class DemonColors(
    val asmodeus: Color,
    val astaroth: Color,
    val ritualSafe: Color,
) {
    /** The only place an accent is resolved to a color; UI never branches on demon id. */
    fun accent(accent: DemonAccent): Color = when (accent) {
        DemonAccent.Asmodeus -> asmodeus
        DemonAccent.Astaroth -> astaroth
    }
}

internal val DefaultDemonColors = DemonColors(
    asmodeus = Palette.demonAsmodeus,
    astaroth = Palette.demonAstaroth,
    ritualSafe = Palette.ritualSafe,
)

val LocalDemonColors = staticCompositionLocalOf { DefaultDemonColors }

val MaterialTheme.demonColors: DemonColors
    @Composable @ReadOnlyComposable get() = LocalDemonColors.current
```

This file references `com.summondemons.safely.data.DemonAccent`, created in Step 7 below so the task compiles on its own.

- [ ] **Step 3: Create `ui/theme/Spacing.kt`**

```kotlin
package com.summondemons.safely.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** DESIGN.md `spacing` tokens. */
@Immutable
data class Spacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }

val MaterialTheme.spacing: Spacing
    @Composable @ReadOnlyComposable get() = LocalSpacing.current
```

- [ ] **Step 4: Create `ui/theme/Type.kt`**

```kotlin
package com.summondemons.safely.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// DESIGN.md: Cinzel and Inter until bundled in res/font/ → Serif / SansSerif fallbacks.
private val Display = FontFamily.Serif
private val Body = FontFamily.SansSerif
private const val TABULAR_FIGURES = "tnum"

val AppTypography = Typography(
    displaySmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
    headlineMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontFamily = Body, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp, fontFeatureSettings = TABULAR_FIGURES),
    bodyMedium = TextStyle(fontFamily = Body, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, fontFeatureSettings = TABULAR_FIGURES),
    bodySmall = TextStyle(fontFamily = Body, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, fontFeatureSettings = TABULAR_FIGURES),
    labelMedium = TextStyle(fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, fontFeatureSettings = TABULAR_FIGURES),
    labelSmall = TextStyle(fontFamily = Body, fontWeight = FontWeight.Bold, fontSize = 10.sp, lineHeight = 12.sp, fontFeatureSettings = TABULAR_FIGURES),
)
```

- [ ] **Step 5: Create `ui/theme/Shape.kt`**

```kotlin
package com.summondemons.safely.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// DESIGN.md shapes: 12 / 16 / 24 dp, buttons are pills.
val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

val PillShape = RoundedCornerShape(percent = 50)
```

- [ ] **Step 6: Create `ui/theme/Theme.kt`**

```kotlin
package com.summondemons.safely.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val DarkColors = darkColorScheme(
    primary = Palette.primary,
    onPrimary = Palette.onPrimary,
    primaryContainer = Palette.primaryContainer,
    onPrimaryContainer = Palette.onPrimaryContainer,
    inversePrimary = Palette.inversePrimary,
    secondary = Palette.secondary,
    onSecondary = Palette.onSecondary,
    secondaryContainer = Palette.secondaryContainer,
    onSecondaryContainer = Palette.onSecondaryContainer,
    tertiary = Palette.tertiary,
    onTertiary = Palette.onTertiary,
    tertiaryContainer = Palette.tertiaryContainer,
    onTertiaryContainer = Palette.onTertiaryContainer,
    background = Palette.background,
    onBackground = Palette.onBackground,
    surface = Palette.surface,
    onSurface = Palette.onSurface,
    surfaceVariant = Palette.surfaceVariant,
    onSurfaceVariant = Palette.onSurfaceVariant,
    surfaceTint = Palette.surfaceTint,
    inverseSurface = Palette.inverseSurface,
    inverseOnSurface = Palette.inverseOnSurface,
    error = Palette.error,
    onError = Palette.onError,
    errorContainer = Palette.errorContainer,
    onErrorContainer = Palette.onErrorContainer,
    outline = Palette.outline,
    outlineVariant = Palette.outlineVariant,
    surfaceBright = Palette.surfaceBright,
    surfaceDim = Palette.surfaceDim,
    surfaceContainer = Palette.surfaceContainer,
    surfaceContainerHigh = Palette.surfaceContainerHigh,
    surfaceContainerHighest = Palette.surfaceContainerHighest,
    surfaceContainerLow = Palette.surfaceContainerLow,
    surfaceContainerLowest = Palette.surfaceContainerLowest,
)

/** Dark only (ADR-0001). */
@Composable
fun SummonDemonsSafelyTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalDemonColors provides DefaultDemonColors,
        LocalSpacing provides Spacing(),
    ) {
        MaterialTheme(
            colorScheme = DarkColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
```

- [ ] **Step 7: Create `app/src/main/java/com/summondemons/safely/data/Demon.kt`** (the full model; `DemonColors` needs `DemonAccent`)

```kotlin
package com.summondemons.safely.data

import androidx.annotation.DrawableRes

/** Accent token; resolved to a color by `DemonColors.accent` (DESIGN.md). */
enum class DemonAccent { Asmodeus, Astaroth }

data class Demon(
    val id: String,
    val name: String,
    val title: String,
    val description: String,
    val quote: String,
    val accent: DemonAccent,
    /** Compass degrees in [0, 360) the player must face (ADR-0006). */
    val ritualHeading: Float,
    /** Exactly 4 phrases (ADR-0005). */
    val chantPhrases: List<String>,
    @param:DrawableRes val cardArt: Int,
    @param:DrawableRes val profileArt: Int,
    /** Null → `SigilView` draws the Canvas fallback. */
    @param:DrawableRes val sigil: Int?,
) {
    init {
        require(chantPhrases.size == 4) { "$id must have exactly 4 chant phrases" }
        require(ritualHeading in 0f..<360f) { "$id ritualHeading must be in [0, 360)" }
    }
}
```

- [ ] **Step 8: Build**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/summondemons/safely/ui/theme app/src/main/java/com/summondemons/safely/data/Demon.kt
git commit -q -m "feat(theme): Add dark theme from DESIGN.md tokens" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 3: Art assets and launcher icon

**Files:**
- Create: `app/src/main/res/drawable-nodpi/{ic_game_logo,demon_asmodeus_full,demon_asmodeus_card,demon_astaroth_card,sigil_astaroth}.webp`
- Create: `app/src/main/res/drawable/ic_lock.xml`
- Rewrite: `app/src/main/res/drawable/ic_launcher_foreground.xml`, `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`, `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`, `app/src/main/res/mipmap-*/ic_launcher{,_round}.webp`
- Delete: `app/src/main/res/drawable/ic_launcher_background.xml`

Requires `cwebp` (`brew install webp`).

- [ ] **Step 1: Convert the art (lossy WebP q85, nodpi)**

```bash
S=reference/stitch; D=app/src/main/res/drawable-nodpi; mkdir -p "$D"
cwebp -quiet -q 85 "$S/summon_demons_safely_game_logo/screen.png" -o "$D/ic_game_logo.webp"
cwebp -quiet -q 85 "$S/full_body_cute_funny_stylized_2d_cartoon_fantasy_rpg_character_artwork_of_demon_1/screen.png" -o "$D/demon_asmodeus_full.webp"
cwebp -quiet -q 85 "$S/full_body_cute_funny_stylized_2d_cartoon_fantasy_rpg_character_artwork_of_demon_2/screen.png" -o "$D/demon_asmodeus_card.webp"
cwebp -quiet -q 85 "$S/full_body_cute_funny_stylized_2d_cartoon_fantasy_rpg_character_artwork_of_demon_3/screen.png" -o "$D/demon_astaroth_card.webp"
cwebp -quiet -q 85 "$S/the_sacred_magical_sigil_of_astaroth_drawn_in_the_identical_style_as_the/screen.png" -o "$D/sigil_astaroth.webp"
ls -la "$D"
```

Expected: five `.webp` files, each well under 500 KB.

- [ ] **Step 2: Generate legacy launcher icons**

```bash
LOGO=reference/stitch/summon_demons_safely_game_logo/screen.png
for pair in mdpi:48 hdpi:72 xhdpi:96 xxhdpi:144 xxxhdpi:192; do
  dens=${pair%%:*}; px=${pair##*:}; dir=app/src/main/res/mipmap-$dens
  cwebp -quiet -q 90 -resize "$px" "$px" "$LOGO" -o "$dir/ic_launcher.webp"
  cp "$dir/ic_launcher.webp" "$dir/ic_launcher_round.webp"
done
```

- [ ] **Step 3: Replace the adaptive icon**

```bash
git rm -q app/src/main/res/drawable/ic_launcher_background.xml
```

Write `app/src/main/res/drawable/ic_launcher_foreground.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- Logo inset so its baked background stays inside the adaptive-icon safe zone. -->
<inset xmlns:android="http://schemas.android.com/apk/res/android"
    android:drawable="@drawable/ic_game_logo"
    android:inset="18%" />
```

Write both `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` and `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml` with:

```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/obsidian" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
```

- [ ] **Step 4: Create `app/src/main/res/drawable/ic_lock.xml`** (Material "lock" icon path, Apache-2.0)

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="M18,8h-1V6c0,-2.76 -2.24,-5 -5,-5S7,3.24 7,6v2H6c-1.1,0 -2,0.9 -2,2v10c0,1.1 0.9,2 2,2h12c1.1,0 2,-0.9 2,-2V10c0,-1.1 -0.9,-2 -2,-2zM12,17c-1.1,0 -2,-0.9 -2,-2s0.9,-2 2,-2 2,0.9 2,2 -0.9,2 -2,2zM15.1,8H8.9V6c0,-1.71 1.39,-3.1 3.1,-3.1 1.71,0 3.1,1.39 3.1,3.1v2z" />
</vector>
```

(`#FFFFFFFF` is a tint mask: `Icon` recolors it from the theme.)

- [ ] **Step 5: Build**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add -A app/src/main/res
git commit -q -m "feat(assets): Add Stitch art as WebP and logo launcher icon" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 4: Demon catalog and DemonViewModel

**Files:**
- Create: `app/src/main/java/com/summondemons/safely/data/DemonCatalog.kt`, `app/src/main/java/com/summondemons/safely/ui/DemonViewModel.kt`
- Test: `app/src/test/java/com/summondemons/safely/data/DemonCatalogTest.kt`, `app/src/test/java/com/summondemons/safely/ui/DemonViewModelTest.kt`

- [ ] **Step 1: Write the failing catalog test**

`app/src/test/java/com/summondemons/safely/data/DemonCatalogTest.kt`:

```kotlin
package com.summondemons.safely.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DemonCatalogTest {

    @Test
    fun containsAsmodeusAndAstarothWithUniqueIds() {
        assertEquals(listOf("asmodeus", "astaroth"), DemonCatalog.all.map { it.id })
    }

    @Test
    fun everyDemonHasFourPhrasesAndAValidHeading() {
        DemonCatalog.all.forEach { demon ->
            assertEquals(4, demon.chantPhrases.size)
            assertTrue(demon.ritualHeading >= 0f && demon.ritualHeading < 360f)
        }
    }

    @Test
    fun asmodeusMatchesProductSpec() {
        val asmodeus = DemonCatalog.byId("asmodeus")!!
        assertEquals("Lord of Arithmetic & Hidden Wealth", asmodeus.title)
        assertEquals(0f, asmodeus.ritualHeading)
        assertEquals(
            listOf(
                "DAEMONIUM RESIDENTE,",
                "OSTENDE MIHI FACIEM TUAM POTENTIAM, VENIT AD ME.",
                "DONA MIHI PER FOEDUS QUOD EST IRE AD PRAESENS SUM VOBIS,",
                "UT VOTA OMNIA OPUS EST.",
            ),
            asmodeus.chantPhrases,
        )
    }

    @Test
    fun astarothMatchesProductSpec() {
        val astaroth = DemonCatalog.byId("astaroth")!!
        assertEquals("Duke of Past, Present & Future", astaroth.title)
        assertEquals(
            "I know your past, your future, and that you didn't read the terms of service.",
            astaroth.quote,
        )
        assertEquals(
            listOf("TUI GRATIA LOVIS", "GRATIA SIT CURA.", "HIC EST ME DIGNUS LEGI", "SINT BENE OBEDIENTES."),
            astaroth.chantPhrases,
        )
    }

    @Test
    fun unknownIdIsNull() {
        assertEquals(null, DemonCatalog.byId("baal"))
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.summondemons.safely.data.DemonCatalogTest"`
Expected: compilation FAILS with `Unresolved reference 'DemonCatalog'`.

- [ ] **Step 3: Create `data/DemonCatalog.kt`** (text verbatim from PRODUCT.md)

```kotlin
package com.summondemons.safely.data

import com.summondemons.safely.R

/** The MVP demons. Text is verbatim from PRODUCT.md. */
object DemonCatalog {

    val all: List<Demon> = listOf(
        Demon(
            id = "asmodeus",
            name = "Asmodeus",
            title = "Lord of Arithmetic & Hidden Wealth",
            description = "Teaches arithmetic, astronomy, geometry, and invincibility. " +
                "Answers all questions—if you can stand the smugness.",
            quote = "Three heads are better than one—especially when two want your soul!",
            accent = DemonAccent.Asmodeus,
            ritualHeading = 0f,
            chantPhrases = listOf(
                "DAEMONIUM RESIDENTE,",
                "OSTENDE MIHI FACIEM TUAM POTENTIAM, VENIT AD ME.",
                "DONA MIHI PER FOEDUS QUOD EST IRE AD PRAESENS SUM VOBIS,",
                "UT VOTA OMNIA OPUS EST.",
            ),
            cardArt = R.drawable.demon_asmodeus_card,
            profileArt = R.drawable.demon_asmodeus_full,
            sigil = null, // DESIGN.md gap 1
        ),
        Demon(
            id = "astaroth",
            name = "Astaroth",
            title = "Duke of Past, Present & Future",
            description = "Detects secret desires and grants protection to the great. " +
                "Will explain his own fall at length if asked.",
            quote = "I know your past, your future, and that you didn't read the terms of service.",
            accent = DemonAccent.Astaroth,
            ritualHeading = 0f,
            chantPhrases = listOf(
                "TUI GRATIA LOVIS", // verbatim, see ADR-0005
                "GRATIA SIT CURA.",
                "HIC EST ME DIGNUS LEGI",
                "SINT BENE OBEDIENTES.",
            ),
            cardArt = R.drawable.demon_astaroth_card,
            profileArt = R.drawable.demon_astaroth_card, // DESIGN.md gap 2
            sigil = R.drawable.sigil_astaroth,
        ),
    )

    fun byId(id: String): Demon? = all.find { it.id == id }
}
```

- [ ] **Step 4: Run the catalog test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.summondemons.safely.data.DemonCatalogTest"`
Expected: PASS (5 tests).

- [ ] **Step 5: Write the failing DemonViewModel test**

`app/src/test/java/com/summondemons/safely/ui/DemonViewModelTest.kt`:

```kotlin
package com.summondemons.safely.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DemonViewModelTest {

    @Test
    fun allDemonsStartLocked() {
        val vm = DemonViewModel()
        assertEquals(listOf(false, false), vm.demons.value.map { it.unlocked })
    }

    @Test
    fun unlockFlipsOnlyThatDemon() {
        val vm = DemonViewModel()
        vm.unlock("astaroth")
        assertEquals(
            mapOf("asmodeus" to false, "astaroth" to true),
            vm.demons.value.associate { it.demon.id to it.unlocked },
        )
    }

    @Test
    fun demonLookup() {
        val vm = DemonViewModel()
        assertNotNull(vm.demon("asmodeus"))
        assertNull(vm.demon("baal"))
    }
}
```

- [ ] **Step 6: Run it to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.summondemons.safely.ui.DemonViewModelTest"`
Expected: compilation FAILS with `Unresolved reference 'DemonViewModel'`.

- [ ] **Step 7: Create `ui/DemonViewModel.kt`**

```kotlin
package com.summondemons.safely.ui

import androidx.lifecycle.ViewModel
import com.summondemons.safely.data.Demon
import com.summondemons.safely.data.DemonCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class DemonEntry(val demon: Demon, val unlocked: Boolean)

/** Activity-scoped. Unlock state is in memory only (PRODUCT.md). */
class DemonViewModel : ViewModel() {

    private val _demons = MutableStateFlow(DemonCatalog.all.map { DemonEntry(it, unlocked = false) })
    val demons: StateFlow<List<DemonEntry>> = _demons.asStateFlow()

    fun demon(id: String): Demon? = DemonCatalog.byId(id)

    fun unlock(id: String) {
        _demons.update { entries ->
            entries.map { if (it.demon.id == id) it.copy(unlocked = true) else it }
        }
    }
}
```

- [ ] **Step 8: Run both tests**

Run: `./gradlew testDebugUnitTest --tests "com.summondemons.safely.data.*" --tests "com.summondemons.safely.ui.*"`
Expected: PASS (8 tests).

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/summondemons/safely/data app/src/main/java/com/summondemons/safely/ui/DemonViewModel.kt app/src/test
git commit -q -m "feat(data): Add demon catalog and DemonViewModel with in-memory unlocks" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 5: Ritual config and heading math

**Files:**
- Create: `app/src/main/java/com/summondemons/safely/ritual/RitualConfig.kt`, `app/src/main/java/com/summondemons/safely/ritual/HeadingMath.kt`
- Test: `app/src/test/java/com/summondemons/safely/ritual/HeadingMathTest.kt`

- [ ] **Step 1: Create `ritual/RitualConfig.kt`** (constants only, no behavior to test)

```kotlin
package com.summondemons.safely.ritual

/** Ritual-wide tuning (ADR-0007). Per-demon parameters live in `Demon` (ADR-0006). */
object RitualConfig {
    const val PHRASE_COUNT = 4
    const val HEADING_TOLERANCE_DEG = 20f
    const val PHRASE_SUSTAIN_MS = 2_000L
    const val TICK_MS = 100L
    const val SILENCE_DECAY_FACTOR = 0.5f
    const val VOLUME_RMS_THRESHOLD = 1_800.0
    const val SUCCESS_HOLD_MS = 800L

    /** Sigil opacity indexed by phrases completed (PRODUCT.md). */
    val OPACITY_STEPS: List<Float> = listOf(0.10f, 0.25f, 0.50f, 0.75f, 1.00f)
}
```

- [ ] **Step 2: Write the failing heading test**

`app/src/test/java/com/summondemons/safely/ritual/HeadingMathTest.kt`:

```kotlin
package com.summondemons.safely.ritual

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeadingMathTest {

    @Test
    fun normalizeWrapsIntoZeroTo360() {
        assertEquals(0f, normalizeDegrees(360f), 0.001f)
        assertEquals(350f, normalizeDegrees(-10f), 0.001f)
        assertEquals(10f, normalizeDegrees(730f), 0.001f)
    }

    @Test
    fun angularDistanceTakesTheShortWayRound() {
        assertEquals(20f, angularDistance(340f, 0f), 0.001f)
        assertEquals(20f, angularDistance(10f, 350f), 0.001f)
        assertEquals(180f, angularDistance(0f, 180f), 0.001f)
        assertEquals(0f, angularDistance(0f, 360f), 0.001f)
    }

    @Test
    fun northToleranceEdges() {
        assertTrue(isAligned(340f, 0f))
        assertTrue(isAligned(20f, 0f))
        assertTrue(isAligned(0f, 0f))
        assertFalse(isAligned(339.9f, 0f))
        assertFalse(isAligned(20.1f, 0f))
    }

    @Test
    fun nonNorthTarget() {
        assertTrue(isAligned(165f, 180f))
        assertTrue(isAligned(200f, 180f))
        assertFalse(isAligned(0f, 180f))
    }

    @Test
    fun headingLabelsRoundToEightPoints() {
        assertEquals("North", headingLabel(0f))
        assertEquals("North", headingLabel(350f))
        assertEquals("North-East", headingLabel(45f))
        assertEquals("East", headingLabel(90f))
        assertEquals("South", headingLabel(180f))
        assertEquals("North-West", headingLabel(315f))
    }

    @Test
    fun smootherPassesFirstValueThrough() {
        assertEquals(123f, AzimuthSmoother().next(123f), 0.01f)
    }

    @Test
    fun smootherCrossesNorthWithoutSwingingSouth() {
        val smoother = AzimuthSmoother(alpha = 0.15f)
        smoother.next(350f)
        val next = smoother.next(10f)
        assertTrue("expected ~353, got $next", next > 350f && next < 360f)
    }
}
```

- [ ] **Step 3: Run it to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.summondemons.safely.ritual.HeadingMathTest"`
Expected: compilation FAILS with `Unresolved reference 'normalizeDegrees'`.

- [ ] **Step 4: Create `ritual/HeadingMath.kt`**

```kotlin
package com.summondemons.safely.ritual

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

fun normalizeDegrees(degrees: Float): Float = ((degrees % 360f) + 360f) % 360f

/** Smallest absolute difference between two headings, in [0, 180]. */
fun angularDistance(a: Float, b: Float): Float {
    val diff = abs(normalizeDegrees(a) - normalizeDegrees(b))
    return if (diff > 180f) 360f - diff else diff
}

fun isAligned(
    azimuth: Float,
    target: Float,
    tolerance: Float = RitualConfig.HEADING_TOLERANCE_DEG,
): Boolean = angularDistance(azimuth, target) <= tolerance

private val COMPASS_POINTS = listOf(
    "North", "North-East", "East", "South-East", "South", "South-West", "West", "North-West",
)

/** Nearest of 8 compass points, e.g. for "face {direction}". */
fun headingLabel(degrees: Float): String =
    COMPASS_POINTS[((normalizeDegrees(degrees) + 22.5f) / 45f).toInt() % COMPASS_POINTS.size]

/** Exponential low-pass on the unit vector so 359° → 0° doesn't swing through 180°. */
class AzimuthSmoother(private val alpha: Float = 0.15f) {
    private var sinAcc = 0f
    private var cosAcc = 0f
    private var initialized = false

    fun next(degrees: Float): Float {
        val radians = Math.toRadians(degrees.toDouble())
        val s = sin(radians).toFloat()
        val c = cos(radians).toFloat()
        if (initialized) {
            sinAcc += alpha * (s - sinAcc)
            cosAcc += alpha * (c - cosAcc)
        } else {
            sinAcc = s
            cosAcc = c
            initialized = true
        }
        return normalizeDegrees(Math.toDegrees(atan2(sinAcc.toDouble(), cosAcc.toDouble())).toFloat())
    }
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.summondemons.safely.ritual.HeadingMathTest"`
Expected: PASS (7 tests).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/summondemons/safely/ritual app/src/test/java/com/summondemons/safely/ritual
git commit -q -m "feat(ritual): Add heading math with per-target tolerance and smoothing" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 6: RitualEngine

**Files:**
- Create: `app/src/main/java/com/summondemons/safely/ritual/RitualEngine.kt`
- Test: `app/src/test/java/com/summondemons/safely/ritual/RitualEngineTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.TICK_MS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RitualEngineTest {

    private fun run(start: RitualProgress, ticks: Int, aligned: Boolean = true, loud: Boolean = true) =
        (1..ticks).fold(start) { p, _ -> RitualEngine.step(p, aligned, loud, TICK_MS) }

    @Test
    fun twentyLoudAlignedTicksAdvanceOnePhrase() {
        assertEquals(RitualProgress(phraseIndex = 0, sustainedMs = 1_900), run(RitualProgress(), 19))
        assertEquals(RitualProgress(phraseIndex = 1, sustainedMs = 0), run(RitualProgress(), 20))
    }

    @Test
    fun leavingTheHeadingResetsEverything() {
        val midway = RitualProgress(phraseIndex = 2, sustainedMs = 1_200)
        assertEquals(RitualProgress(), RitualEngine.step(midway, aligned = false, loud = true, dtMs = TICK_MS))
    }

    @Test
    fun silenceDecaysAtHalfRateAndNeverBelowZero() {
        assertEquals(
            RitualProgress(1, 950),
            RitualEngine.step(RitualProgress(1, 1_000), aligned = true, loud = false, dtMs = TICK_MS),
        )
        assertEquals(
            RitualProgress(1, 0),
            RitualEngine.step(RitualProgress(1, 30), aligned = true, loud = false, dtMs = TICK_MS),
        )
    }

    @Test
    fun eightySecondsWorthOfTicksCompletesTheRitual() {
        val done = run(RitualProgress(), 80)
        assertTrue(done.completed)
        assertEquals(4, done.phraseIndex)
    }

    @Test
    fun stepAfterCompletionIsANoOp() {
        val done = RitualProgress(phraseIndex = 4)
        assertEquals(done, RitualEngine.step(done, aligned = false, loud = false, dtMs = TICK_MS))
    }

    @Test
    fun opacityFollowsProductSteps() {
        assertEquals(listOf(0.10f, 0.25f, 0.50f, 0.75f, 1.00f), (0..4).map { RitualProgress(it).sigilOpacity })
    }

    @Test
    fun sustainFractionIsClamped() {
        assertEquals(0.5f, RitualProgress(0, 1_000).sustainFraction, 0.001f)
        assertFalse(RitualProgress(0, 0).completed)
    }
}
```

Save as `app/src/test/java/com/summondemons/safely/ritual/RitualEngineTest.kt`.

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.summondemons.safely.ritual.RitualEngineTest"`
Expected: compilation FAILS with `Unresolved reference 'RitualProgress'`.

- [ ] **Step 3: Create `ritual/RitualEngine.kt`**

```kotlin
package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.OPACITY_STEPS
import com.summondemons.safely.ritual.RitualConfig.PHRASE_COUNT
import com.summondemons.safely.ritual.RitualConfig.PHRASE_SUSTAIN_MS
import com.summondemons.safely.ritual.RitualConfig.SILENCE_DECAY_FACTOR

/** `phraseIndex` = phrases completed so far (0..4). */
data class RitualProgress(val phraseIndex: Int = 0, val sustainedMs: Long = 0L) {
    val completed: Boolean get() = phraseIndex >= PHRASE_COUNT
    val sigilOpacity: Float get() = OPACITY_STEPS[phraseIndex.coerceAtMost(PHRASE_COUNT)]
    val sustainFraction: Float get() = (sustainedMs.toFloat() / PHRASE_SUSTAIN_MS).coerceIn(0f, 1f)
}

/** Pure ritual rules (PRODUCT.md "Heading gate", "Chant", ADR-0007). */
object RitualEngine {

    fun step(progress: RitualProgress, aligned: Boolean, loud: Boolean, dtMs: Long): RitualProgress {
        if (progress.completed) return progress
        if (!aligned) return RitualProgress()
        if (loud) {
            val sustained = progress.sustainedMs + dtMs
            return if (sustained >= PHRASE_SUSTAIN_MS) {
                RitualProgress(phraseIndex = progress.phraseIndex + 1, sustainedMs = 0L)
            } else {
                progress.copy(sustainedMs = sustained)
            }
        }
        val decayed = progress.sustainedMs - (dtMs * SILENCE_DECAY_FACTOR).toLong()
        return progress.copy(sustainedMs = decayed.coerceAtLeast(0L))
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.summondemons.safely.ritual.RitualEngineTest"`
Expected: PASS (7 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/summondemons/safely/ritual/RitualEngine.kt app/src/test/java/com/summondemons/safely/ritual/RitualEngineTest.kt
git commit -q -m "feat(ritual): Add pure RitualEngine state machine" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 7: Heading and volume sources

**Files:**
- Create: `app/src/main/java/com/summondemons/safely/ritual/Sources.kt`, `app/src/main/java/com/summondemons/safely/platform/AndroidHeadingSource.kt`, `app/src/main/java/com/summondemons/safely/platform/AndroidVolumeSource.kt`
- Test: `app/src/test/java/com/summondemons/safely/ritual/RmsTest.kt`

The Android implementations are not unit-tested (they need hardware); they are exercised by the manual checklist in Task 13.

- [ ] **Step 1: Write the failing RMS test**

`app/src/test/java/com/summondemons/safely/ritual/RmsTest.kt`:

```kotlin
package com.summondemons.safely.ritual

import org.junit.Assert.assertEquals
import org.junit.Test

class RmsTest {

    @Test
    fun rmsOfConstantSignalIsItsMagnitude() {
        assertEquals(1_000.0, rms(shortArrayOf(1_000, -1_000, 1_000, -1_000), 4), 0.001)
    }

    @Test
    fun rmsOnlyUsesTheFirstCountSamples() {
        assertEquals(3.0, rms(shortArrayOf(3, 3, 30_000), 2), 0.001)
    }

    @Test
    fun rmsOfNothingIsZero() {
        assertEquals(0.0, rms(ShortArray(0), 0), 0.0)
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.summondemons.safely.ritual.RmsTest"`
Expected: compilation FAILS with `Unresolved reference 'rms'`.

- [ ] **Step 3: Create `ritual/Sources.kt`**

```kotlin
package com.summondemons.safely.ritual

import kotlinx.coroutines.flow.Flow
import kotlin.math.sqrt

/** Cold flow of device azimuths in degrees [0, 360). Collecting starts the sensors. */
fun interface HeadingSource {
    fun azimuths(): Flow<Float>
}

/** Cold flow of microphone RMS, one value per ~[RitualConfig.TICK_MS] window. Collecting opens the mic. */
fun interface VolumeSource {
    fun rmsLevels(): Flow<Double>
}

fun rms(samples: ShortArray, count: Int): Double {
    if (count <= 0) return 0.0
    var sum = 0.0
    for (i in 0 until count) {
        val s = samples[i].toDouble()
        sum += s * s
    }
    return sqrt(sum / count)
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.summondemons.safely.ritual.RmsTest"`
Expected: PASS (3 tests).

- [ ] **Step 5: Create `platform/AndroidHeadingSource.kt`**

```kotlin
package com.summondemons.safely.platform

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.summondemons.safely.ritual.AzimuthSmoother
import com.summondemons.safely.ritual.HeadingSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Rotation-vector azimuth, falling back to accelerometer + magnetometer.
 * Emits nothing if the device has neither (the UI then shows "--°").
 * Assumes portrait (the activity is locked to it), so no axis remapping.
 */
class AndroidHeadingSource(context: Context) : HeadingSource {

    private val sensorManager: SensorManager? = context.getSystemService(SensorManager::class.java)

    override fun azimuths(): Flow<Float> = callbackFlow {
        val manager = sensorManager
        if (manager == null) {
            awaitClose()
            return@callbackFlow
        }
        val rotationVector = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val smoother = AzimuthSmoother()
        val rotationMatrix = FloatArray(9)
        val orientation = FloatArray(3)
        var gravity: FloatArray? = null
        var geomagnetic: FloatArray? = null

        fun emitFromMatrix() {
            SensorManager.getOrientation(rotationMatrix, orientation)
            val degrees = Math.toDegrees(orientation[0].toDouble()).toFloat()
            trySend(smoother.next(degrees))
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                        emitFromMatrix()
                    }
                    Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_MAGNETIC_FIELD -> {
                        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                            gravity = event.values.clone()
                        } else {
                            geomagnetic = event.values.clone()
                        }
                        val g = gravity
                        val m = geomagnetic
                        if (g != null && m != null && SensorManager.getRotationMatrix(rotationMatrix, null, g, m)) {
                            emitFromMatrix()
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        if (rotationVector != null) {
            manager.registerListener(listener, rotationVector, SensorManager.SENSOR_DELAY_UI)
        } else if (accelerometer != null && magnetometer != null) {
            manager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
            manager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_UI)
        }

        awaitClose { manager.unregisterListener(listener) }
    }
}
```

- [ ] **Step 6: Create `platform/AndroidVolumeSource.kt`**

```kotlin
package com.summondemons.safely.platform

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.summondemons.safely.ritual.RitualConfig
import com.summondemons.safely.ritual.VolumeSource
import com.summondemons.safely.ritual.rms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

/**
 * Mono 16 kHz PCM16 mic, one RMS value per [RitualConfig.TICK_MS] window.
 * Only collected after RECORD_AUDIO is granted (RitualViewModel). If the recorder
 * can't start, the flow just completes and the ritual treats it as silence.
 */
class AndroidVolumeSource : VolumeSource {

    @SuppressLint("MissingPermission") // Collected only once RECORD_AUDIO is granted.
    override fun rmsLevels(): Flow<Double> = flow {
        val minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL, ENCODING)
        if (minBuffer <= 0) return@flow
        val window = (SAMPLE_RATE * RitualConfig.TICK_MS / 1_000).toInt()
        val recorder = try {
            AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL, ENCODING, maxOf(minBuffer, window * 2))
        } catch (e: SecurityException) {
            return@flow
        }
        try {
            if (recorder.state != AudioRecord.STATE_INITIALIZED) return@flow
            recorder.startRecording()
            val buffer = ShortArray(window)
            while (currentCoroutineContext().isActive) {
                val read = recorder.read(buffer, 0, window) // blocks ~TICK_MS
                if (read <= 0) break
                emit(rms(buffer, read))
            }
        } finally {
            recorder.release()
        }
    }.flowOn(Dispatchers.IO)

    private companion object {
        const val SAMPLE_RATE = 16_000
        const val CHANNEL = AudioFormat.CHANNEL_IN_MONO
        const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
    }
}
```

- [ ] **Step 7: Build and run all unit tests**

Run: `./gradlew assembleDebug testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/summondemons/safely/ritual/Sources.kt app/src/main/java/com/summondemons/safely/platform app/src/test/java/com/summondemons/safely/ritual/RmsTest.kt
git commit -q -m "feat(ritual): Add heading and volume sources behind interfaces" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 8: RitualViewModel

**Files:**
- Create: `app/src/main/java/com/summondemons/safely/ritual/RitualViewModel.kt`
- Test: `app/src/test/java/com/summondemons/safely/ritual/RitualViewModelTest.kt`

How the pipeline works (read before coding):
- `state: MutableStateFlow<RitualUiState>` is the single source of truth. Public
  setters (`onMicPermission`, `setSimulateHeading`, `setHoldToChant`) only update it.
- `uiState` is a `channelFlow` that, while subscribed, launches three collectors
  (azimuth, loud ticks, completion hold) which write into `state`, and forwards
  `state` downstream. `stateIn(WhileSubscribed(5_000))` stops them — and so the
  sensors and the mic — 5 s after the screen stops collecting.
- The mic flow is chosen by `flatMapLatest` over `micGranted` **only**, so heading
  changes never restart `AudioRecord` (draft bug).

- [ ] **Step 1: Write the failing test**

`app/src/test/java/com/summondemons/safely/ritual/RitualViewModelTest.kt`:

```kotlin
package com.summondemons.safely.ritual

import com.summondemons.safely.data.DemonCatalog
import com.summondemons.safely.ritual.RitualConfig.SUCCESS_HOLD_MS
import com.summondemons.safely.ritual.RitualConfig.TICK_MS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RitualViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val demon = DemonCatalog.byId("asmodeus")!!

    private class FakeHeading : HeadingSource {
        val readings = MutableSharedFlow<Float>(replay = 1)
        override fun azimuths(): Flow<Float> = readings
    }

    private class FakeVolume(private val level: Double) : VolumeSource {
        var subscriptions = 0
        override fun rmsLevels(): Flow<Double> = flow {
            subscriptions++
            while (true) {
                delay(TICK_MS)
                emit(level)
            }
        }
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun loudAndAlignedForEightSecondsCompletesAfterTheHold() = runTest(dispatcher) {
        val heading = FakeHeading()
        val vm = RitualViewModel(demon, heading, FakeVolume(level = 5_000.0))
        backgroundScope.launch { vm.uiState.collect {} }
        vm.onMicPermission(true)
        heading.readings.emit(0f)

        advanceTimeBy(8_000 + TICK_MS); runCurrent()
        assertEquals(4, vm.uiState.value.progress.phraseIndex)
        assertEquals(1f, vm.uiState.value.progress.sigilOpacity)
        assertFalse(vm.uiState.value.completed)

        advanceTimeBy(SUCCESS_HOLD_MS); runCurrent()
        assertTrue(vm.uiState.value.completed)
    }

    @Test
    fun turningAwayResetsProgress() = runTest(dispatcher) {
        val heading = FakeHeading()
        val vm = RitualViewModel(demon, heading, FakeVolume(level = 5_000.0))
        backgroundScope.launch { vm.uiState.collect {} }
        vm.onMicPermission(true)
        heading.readings.emit(0f)
        advanceTimeBy(2_000 + TICK_MS); runCurrent()
        assertEquals(1, vm.uiState.value.progress.phraseIndex)

        heading.readings.emit(90f)
        advanceTimeBy(2 * TICK_MS); runCurrent()
        assertFalse(vm.uiState.value.aligned)
        assertEquals(RitualProgress(), vm.uiState.value.progress)
    }

    @Test
    fun quietMicDoesNotAdvance() = runTest(dispatcher) {
        val heading = FakeHeading()
        val vm = RitualViewModel(demon, heading, FakeVolume(level = 100.0))
        backgroundScope.launch { vm.uiState.collect {} }
        vm.onMicPermission(true)
        heading.readings.emit(0f)
        advanceTimeBy(5_000); runCurrent()
        assertEquals(RitualProgress(), vm.uiState.value.progress)
    }

    @Test
    fun simulateHeadingAlignsWithoutASensor() = runTest(dispatcher) {
        val vm = RitualViewModel(demon, { emptyFlow() }, FakeVolume(level = 0.0))
        backgroundScope.launch { vm.uiState.collect {} }
        runCurrent()
        assertNull(vm.uiState.value.azimuth)

        vm.setSimulateHeading(true)
        runCurrent()
        assertEquals(demon.ritualHeading, vm.uiState.value.azimuth)
        assertTrue(vm.uiState.value.aligned)

        vm.setSimulateHeading(false)
        runCurrent()
        assertNull(vm.uiState.value.azimuth)
        assertFalse(vm.uiState.value.aligned)
    }

    @Test
    fun holdToChantCountsAsLoudWhenMicDenied() = runTest(dispatcher) {
        val volume = FakeVolume(level = 5_000.0)
        val vm = RitualViewModel(demon, { emptyFlow() }, volume)
        backgroundScope.launch { vm.uiState.collect {} }
        vm.onMicPermission(false)
        vm.setSimulateHeading(true)
        vm.setHoldToChant(true)
        advanceTimeBy(2_000 + TICK_MS); runCurrent()
        assertEquals(1, vm.uiState.value.progress.phraseIndex)
        assertEquals(0, volume.subscriptions)
    }

    @Test
    fun headingChangesDoNotRestartTheMic() = runTest(dispatcher) {
        val heading = FakeHeading()
        val volume = FakeVolume(level = 5_000.0)
        val vm = RitualViewModel(demon, heading, volume)
        backgroundScope.launch { vm.uiState.collect {} }
        vm.onMicPermission(true)
        heading.readings.emit(0f); advanceTimeBy(300); runCurrent()
        heading.readings.emit(90f); advanceTimeBy(300); runCurrent()
        heading.readings.emit(0f); advanceTimeBy(300); runCurrent()
        assertEquals(1, volume.subscriptions)
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.summondemons.safely.ritual.RitualViewModelTest"`
Expected: compilation FAILS with `Unresolved reference 'RitualViewModel'`.

- [ ] **Step 3: Create `ritual/RitualViewModel.kt`**

```kotlin
package com.summondemons.safely.ritual

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.summondemons.safely.data.Demon
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RitualUiState(
    val demon: Demon,
    val azimuth: Float? = null,
    val aligned: Boolean = false,
    val progress: RitualProgress = RitualProgress(),
    /** null = not checked yet. */
    val micGranted: Boolean? = null,
    val simulateHeading: Boolean = false,
    val holdToChant: Boolean = false,
    /** True [RitualConfig.SUCCESS_HOLD_MS] after the 4th phrase; navigate on it. */
    val completed: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class RitualViewModel(
    private val demon: Demon,
    private val headingSource: HeadingSource,
    private val volumeSource: VolumeSource,
) : ViewModel() {

    private val state = MutableStateFlow(RitualUiState(demon))

    private val azimuths: Flow<Float> = state
        .map { it.simulateHeading }
        .distinctUntilChanged()
        .flatMapLatest { simulate -> if (simulate) flowOf(demon.ritualHeading) else headingSource.azimuths() }

    private val micLoudTicks: Flow<Boolean> = state
        .map { it.micGranted }
        .distinctUntilChanged()
        .flatMapLatest { granted -> if (granted == true) micTicks() else silentTicks() }

    val uiState: StateFlow<RitualUiState> = channelFlow {
        launch {
            azimuths.collect { az ->
                state.update { it.copy(azimuth = az, aligned = isAligned(az, demon.ritualHeading)) }
            }
        }
        launch {
            micLoudTicks.collect { micLoud ->
                state.update { s ->
                    if (s.progress.completed) {
                        s
                    } else {
                        s.copy(
                            progress = RitualEngine.step(
                                s.progress, s.aligned, micLoud || s.holdToChant, RitualConfig.TICK_MS,
                            ),
                        )
                    }
                }
            }
        }
        launch {
            state.first { it.progress.completed }
            delay(RitualConfig.SUCCESS_HOLD_MS)
            state.update { it.copy(completed = true) }
        }
        state.collect { send(it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), state.value)

    fun onMicPermission(granted: Boolean) = state.update { it.copy(micGranted = granted) }

    fun setSimulateHeading(on: Boolean) = state.update {
        if (on) it.copy(simulateHeading = true) else it.copy(simulateHeading = false, azimuth = null, aligned = false)
    }

    fun setHoldToChant(on: Boolean) = state.update { it.copy(holdToChant = on) }

    /** Mic RMS → loud flag; if the mic dies (error or end), fall back to silent ticks. */
    private fun micTicks(): Flow<Boolean> = volumeSource.rmsLevels()
        .map { it > RitualConfig.VOLUME_RMS_THRESHOLD }
        .catch { }
        .onCompletion { cause -> if (cause == null) emitAll(silentTicks()) }

    private fun silentTicks(): Flow<Boolean> = flow {
        while (true) {
            delay(RitualConfig.TICK_MS)
            emit(false)
        }
    }

    companion object {
        fun factory(demon: Demon, headingSource: HeadingSource, volumeSource: VolumeSource): ViewModelProvider.Factory =
            viewModelFactory { initializer { RitualViewModel(demon, headingSource, volumeSource) } }
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.summondemons.safely.ritual.RitualViewModelTest"`
Expected: PASS (6 tests). If `loudAndAligned…` is off by one tick, check that ticks are counted from the first `delay(TICK_MS)`. Fix the code, not the timing in the test: 80 ticks × 100 ms = 8 000 ms is the PRODUCT.md rule.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/summondemons/safely/ritual/RitualViewModel.kt app/src/test/java/com/summondemons/safely/ritual/RitualViewModelTest.kt
git commit -q -m "feat(ritual): Add RitualViewModel exposing ritual state as StateFlow" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 9: UI components

**Files:**
- Create in `app/src/main/java/com/summondemons/safely/ui/components/`: `PrimaryActionButton.kt`, `DemonCardBase.kt`, `RibbonBanner.kt`, `RotatingSunburstBackground.kt`, `SigilView.kt`, `CompassIndicator.kt`, `DebugControls.kt`

Rules: colors from `MaterialTheme.colorScheme` / `MaterialTheme.demonColors` only; paddings from `MaterialTheme.spacing`; shapes from `MaterialTheme.shapes` / `PillShape`. Fixed component sizes are private named constants.

- [ ] **Step 1: Create `PrimaryActionButton.kt`**

```kotlin
package com.summondemons.safely.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.summondemons.safely.ui.theme.PillShape
import com.summondemons.safely.ui.theme.spacing

enum class ButtonTone { Primary, Danger }

private val MinButtonHeight = 52.dp
private val GlowElevation = 8.dp

@Composable
fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tone: ButtonTone = ButtonTone.Primary,
) {
    val colors = MaterialTheme.colorScheme
    val container = when (tone) {
        ButtonTone.Primary -> colors.primaryContainer
        ButtonTone.Danger -> colors.surfaceContainerHigh
    }
    val content = when (tone) {
        ButtonTone.Primary -> colors.onPrimaryContainer
        ButtonTone.Danger -> colors.error
    }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "buttonPress")

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = PillShape,
        color = if (enabled) container else colors.surfaceContainerHighest,
        contentColor = if (enabled) content else colors.onSurfaceVariant,
        interactionSource = interactionSource,
        modifier = modifier
            .heightIn(min = MinButtonHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (enabled && tone == ButtonTone.Primary) GlowElevation else 0.dp,
                shape = PillShape,
                ambientColor = container,
                spotColor = container,
            ),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.xl, vertical = MaterialTheme.spacing.md),
        ) {
            Text(text = text, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        }
    }
}
```

- [ ] **Step 2: Create `DemonCardBase.kt`**

```kotlin
package com.summondemons.safely.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.summondemons.safely.ui.theme.spacing

private const val UNLOCKED_TINT = 0.08f
private val CardElevation = 6.dp

/** Locked: grey container, neutral border. Unlocked: accent border and faint accent tint. */
@Composable
fun DemonCardBase(
    locked: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = if (locked) colors.surfaceContainer else lerp(colors.surfaceContainerHigh, accent, UNLOCKED_TINT),
        border = if (locked) BorderStroke(1.dp, colors.outlineVariant) else BorderStroke(2.dp, accent.copy(alpha = 0.7f)),
        shadowElevation = CardElevation,
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
    }
}
```

- [ ] **Step 3: Create `RibbonBanner.kt`**

```kotlin
package com.summondemons.safely.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.summondemons.safely.ui.theme.spacing

@Composable
fun RibbonBanner(text: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = colors.primaryContainer,
        contentColor = colors.onPrimaryContainer,
        border = BorderStroke(2.dp, colors.primary),
        shadowElevation = 8.dp,
    ) {
        Text(
            text = "★  $text  ★",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.xl, vertical = MaterialTheme.spacing.md),
        )
    }
}
```

- [ ] **Step 4: Create `RotatingSunburstBackground.kt`** (ported from the draft)

```kotlin
package com.summondemons.safely.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.max

private const val ROTATION_MS = 28_000

@Composable
fun RotatingSunburstBackground(color: Color, modifier: Modifier = Modifier, beamCount: Int = 16) {
    val transition = rememberInfiniteTransition(label = "sunburst")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(ROTATION_MS, easing = LinearEasing), RepeatMode.Restart),
        label = "sunburstRotation",
    )
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = max(size.width, size.height) * 1.2f
        val bounds = Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius)
        val step = 360f / beamCount
        rotate(rotation, pivot = center) {
            for (i in 0 until beamCount step 2) {
                val beam = Path().apply {
                    moveTo(center.x, center.y)
                    arcTo(bounds, startAngleDegrees = i * step, sweepAngleDegrees = step, forceMoveTo = false)
                    close()
                }
                drawPath(beam, color = color)
            }
        }
    }
}
```

- [ ] **Step 5: Create `SigilView.kt`** (image when available, draft Canvas otherwise)

```kotlin
package com.summondemons.safely.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

private const val GLOW_ALPHA = 0.45f
private const val IMAGE_FRACTION = 0.9f

@Composable
fun SigilView(
    @DrawableRes sigil: Int?,
    opacity: Float,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val alpha by animateFloatAsState(opacity, label = "sigilOpacity")
    val centerGlyph = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerLowest),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = alpha * GLOW_ALPHA), Color.Transparent)))
        }
        if (sigil != null) {
            Image(
                painter = painterResource(sigil),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = alpha,
                modifier = Modifier.fillMaxSize(IMAGE_FRACTION).clip(CircleShape),
            )
        } else {
            Canvas(Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension / 2f * 0.85f
                drawCircle(accent.copy(alpha = alpha), radius, style = Stroke(3.dp.toPx()))
                drawCircle(
                    accent.copy(alpha = alpha * 0.8f),
                    radius * 0.82f,
                    style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f))),
                )
                val points = (0 until 5).map { i ->
                    val angle = Math.toRadians(i * 72.0 - 90.0)
                    Offset(
                        center.x + radius * 0.8f * cos(angle).toFloat(),
                        center.y + radius * 0.8f * sin(angle).toFloat(),
                    )
                }
                for (i in 0 until 5) {
                    drawLine(accent.copy(alpha = alpha), points[i], points[(i + 2) % 5], strokeWidth = 2.dp.toPx())
                }
                drawCircle(centerGlyph.copy(alpha = alpha), radius * 0.22f, style = Stroke(2.dp.toPx()))
            }
        }
    }
}
```

- [ ] **Step 6: Create `CompassIndicator.kt`**

```kotlin
package com.summondemons.safely.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.summondemons.safely.R
import com.summondemons.safely.ritual.headingLabel
import com.summondemons.safely.ritual.normalizeDegrees
import com.summondemons.safely.ui.theme.demonColors
import kotlin.math.roundToInt

private val DialSize = 56.dp

/** Arrow points from the current heading towards [target]; green when aligned. */
@Composable
fun CompassIndicator(azimuth: Float?, target: Float, aligned: Boolean, modifier: Modifier = Modifier) {
    val status by animateColorAsState(
        if (aligned) MaterialTheme.demonColors.ritualSafe else MaterialTheme.colorScheme.error,
        label = "compassStatus",
    )
    val arrowRotation = if (azimuth == null) 0f else normalizeDegrees(target - azimuth)
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(DialSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .border(2.dp, status, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("▲", color = status, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.rotate(arrowRotation))
        }
        Text(
            text = azimuth?.let { "${it.roundToInt()}°" } ?: stringResource(R.string.compass_unknown),
            color = status,
            style = MaterialTheme.typography.labelMedium,
        )
        Text(headingLabel(target), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
    }
}
```

- [ ] **Step 7: Create `DebugControls.kt`**

```kotlin
package com.summondemons.safely.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.summondemons.safely.R
import com.summondemons.safely.ui.theme.PillShape
import com.summondemons.safely.ui.theme.spacing

private val MinTouchHeight = 48.dp

/** Debug builds only (caller checks BuildConfig.DEBUG). */
@Composable
fun DebugControls(
    simulateHeading: Boolean,
    onSimulateHeadingChange: (Boolean) -> Unit,
    onHoldToChantChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val onHold by rememberUpdatedState(onHoldToChantChange)
    var holding by remember { mutableStateOf(false) }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterChip(
            selected = simulateHeading,
            onClick = { onSimulateHeadingChange(!simulateHeading) },
            label = { Text(stringResource(R.string.debug_simulate_heading)) },
            modifier = Modifier.weight(1f),
        )
        Surface(
            shape = PillShape,
            color = if (holding) colors.secondaryContainer else colors.surfaceContainerHigh,
            contentColor = if (holding) colors.onSecondaryContainer else colors.onSurfaceVariant,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = MinTouchHeight)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            holding = true
                            onHold(true)
                            try {
                                awaitRelease()
                            } finally {
                                holding = false
                                onHold(false)
                            }
                        },
                    )
                },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    stringResource(if (holding) R.string.debug_chanting else R.string.debug_hold_to_chant),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}
```

- [ ] **Step 8: Add the strings these components use**

Replace `app/src/main/res/values/strings.xml` with the full UI copy (also used by Task 11):

```xml
<resources>
    <string name="app_name">Summon Demons Safely</string>

    <string name="grimoire_title">My Grimoire</string>
    <string name="grimoire_subtitle">Face the right way. Chant with conviction. Hope for the best.</string>
    <string name="card_sealed">Sealed</string>
    <string name="action_summon">Summon</string>
    <string name="action_view">View</string>

    <string name="ritual_label">SUMMONING RITUAL</string>
    <string name="ritual_instruction">Hold the phone with both hands and face %1$s</string>
    <string name="chant_label">SACRED CHANT</string>
    <string name="chant_resonance">RESONANCE %1$d%%</string>
    <string name="action_abort">Abort Ritual</string>
    <string name="compass_unknown">--°</string>
    <string name="mic_rationale">The ritual listens for how loudly you chant. Nothing is recorded or saved — the microphone only measures volume.</string>
    <string name="action_retry">Allow microphone</string>
    <string name="action_open_settings">Open settings</string>

    <string name="debug_simulate_heading">Simulate heading</string>
    <string name="debug_hold_to_chant">Hold to Chant</string>
    <string name="debug_chanting">Chanting…</string>

    <string name="profile_banner">New Demon Received!</string>
    <string name="action_awesome">Awesome!</string>
    <string name="action_back">Back</string>
</resources>
```

- [ ] **Step 9: Build**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 10: Verify no stray colors**

Run: `grep -rn "Color(0x" app/src/main/java | grep -v "/ui/theme/Color.kt" || echo clean`
Expected: `clean`

- [ ] **Step 11: Commit**

```bash
git add app/src/main/java/com/summondemons/safely/ui/components app/src/main/res/values/strings.xml
git commit -q -m "feat(ui): Port DESIGN.md components onto theme tokens" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 10: Tada sound, haptics, player

**Files:**
- Create: `tools/gen_tada.py`, `app/src/main/res/raw/tada.wav` (generated), `app/src/main/java/com/summondemons/safely/platform/Haptics.kt`, `app/src/main/java/com/summondemons/safely/platform/TadaPlayer.kt`

- [ ] **Step 1: Create `tools/gen_tada.py`**

```python
#!/usr/bin/env python3
"""Generate the placeholder "Tada!" fanfare as a 16-bit mono WAV.

Original synthesized work, dedicated to the public domain (CC0).
Usage: python3 -I tools/gen_tada.py app/src/main/res/raw/tada.wav
"""
import math
import struct
import sys
import wave

RATE = 22050
TOTAL_S = 1.2
C5, E5, G5, C6 = 523.25, 659.25, 783.99, 1046.50


def render():
    n = int(RATE * TOTAL_S)
    buf = [0.0] * n

    def add(freq, start, dur, amp):
        s0 = int(start * RATE)
        for i in range(int(dur * RATE)):
            j = s0 + i
            if j >= n:
                break
            t = i / RATE
            env = min(1.0, t / 0.01) * math.exp(-3.0 * t / dur)
            wave_ = (math.sin(2 * math.pi * freq * t)
                     + 0.35 * math.sin(4 * math.pi * freq * t)
                     + 0.15 * math.sin(6 * math.pi * freq * t))
            buf[j] += amp * env * wave_

    for k, f in enumerate((C5, E5, G5)):  # rising arpeggio
        add(f, 0.09 * k, 0.16, 0.5)
    for f in (C5, E5, G5, C6):  # final chord
        add(f, 0.30, 0.90, 0.35)

    fade = int(0.15 * RATE)
    for i in range(fade):
        buf[n - fade + i] *= (fade - i) / fade
    peak = max(abs(x) for x in buf) or 1.0
    return [int(32767 * 0.8 * x / peak) for x in buf]


def main(path):
    samples = render()
    with wave.open(path, "wb") as out:
        out.setnchannels(1)
        out.setsampwidth(2)
        out.setframerate(RATE)
        out.writeframes(struct.pack("<%dh" % len(samples), *samples))


if __name__ == "__main__":
    main(sys.argv[1])
```

- [ ] **Step 2: Generate the sound**

```bash
mkdir -p app/src/main/res/raw
python3 -I tools/gen_tada.py app/src/main/res/raw/tada.wav
afinfo app/src/main/res/raw/tada.wav | grep -E "duration|format"
```

Expected: about 1.2 s, 1 ch, 16-bit, 22050 Hz; file ≈ 53 KB. Play it once with `afplay app/src/main/res/raw/tada.wav` to sanity-check.

- [ ] **Step 3: Create `platform/Haptics.kt`** (pattern from the draft)

```kotlin
package com.summondemons.safely.platform

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object Haptics {
    private val TIMINGS = longArrayOf(0, 80, 60, 140)
    private val AMPLITUDES = intArrayOf(0, 180, 0, 255)

    /** 80 ms pulse, 60 ms gap, 140 ms burst. */
    fun success(context: Context) {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            context.getSystemService(Vibrator::class.java)
        }
        if (vibrator == null || !vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(TIMINGS, AMPLITUDES, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(TIMINGS, -1)
        }
    }
}
```

- [ ] **Step 4: Create `platform/TadaPlayer.kt`**

```kotlin
package com.summondemons.safely.platform

import android.content.Context
import android.media.MediaPlayer
import com.summondemons.safely.R

object TadaPlayer {
    /** Starts the fanfare; the caller releases the returned player. Null if it can't play. */
    fun play(context: Context): MediaPlayer? = try {
        MediaPlayer.create(context, R.raw.tada)?.apply { start() }
    } catch (e: Exception) {
        null
    }
}
```

- [ ] **Step 5: Build**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add tools/gen_tada.py app/src/main/res/raw/tada.wav app/src/main/java/com/summondemons/safely/platform/Haptics.kt app/src/main/java/com/summondemons/safely/platform/TadaPlayer.kt
git commit -q -m "feat(feedback): Add generated tada fanfare, player and success haptics" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 11: Screens, navigation, MainActivity

**Files:**
- Create: `app/src/main/java/com/summondemons/safely/navigation/Routes.kt`, `app/src/main/java/com/summondemons/safely/navigation/AppNavHost.kt`, `app/src/main/java/com/summondemons/safely/ui/screens/GrimoireScreen.kt`, `app/src/main/java/com/summondemons/safely/ui/screens/RitualScreen.kt`, `app/src/main/java/com/summondemons/safely/ui/screens/ProfileScreen.kt`
- Rewrite: `app/src/main/java/com/summondemons/safely/MainActivity.kt`

- [ ] **Step 1: Create `navigation/Routes.kt`**

```kotlin
package com.summondemons.safely.navigation

import kotlinx.serialization.Serializable

@Serializable
data object Grimoire

@Serializable
data class Ritual(val demonId: String)

@Serializable
data class Profile(val demonId: String, val isNewSummon: Boolean)
```

- [ ] **Step 2: Create `ui/screens/GrimoireScreen.kt`**

```kotlin
package com.summondemons.safely.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.summondemons.safely.R
import com.summondemons.safely.ui.DemonEntry
import com.summondemons.safely.ui.components.DemonCardBase
import com.summondemons.safely.ui.components.PrimaryActionButton
import com.summondemons.safely.ui.theme.demonColors
import com.summondemons.safely.ui.theme.spacing

private const val CARD_ART_ASPECT = 896f / 1200f
private const val SILHOUETTE_ALPHA = 0.92f
private val LogoSize = 36.dp
private val LockSize = 40.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrimoireScreen(
    demons: List<DemonEntry>,
    onSummon: (String) -> Unit,
    onView: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        Image(
                            painter = painterResource(R.drawable.ic_game_logo),
                            contentDescription = null,
                            modifier = Modifier.size(LogoSize).clip(CircleShape),
                        )
                        Text(
                            stringResource(R.string.grimoire_title),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            )
        },
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = spacing.lg)) {
            Text(
                stringResource(R.string.grimoire_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = spacing.md),
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
                contentPadding = PaddingValues(bottom = spacing.xl),
            ) {
                items(demons, key = { it.demon.id }) { entry ->
                    DemonGridItem(
                        entry = entry,
                        onSummon = { onSummon(entry.demon.id) },
                        onView = { onView(entry.demon.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DemonGridItem(entry: DemonEntry, onSummon: () -> Unit, onView: () -> Unit) {
    val demon = entry.demon
    val locked = !entry.unlocked
    val accent = MaterialTheme.demonColors.accent(demon.accent)
    val colors = MaterialTheme.colorScheme
    DemonCardBase(locked = locked, accent = accent) {
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(CARD_ART_ASPECT).clip(MaterialTheme.shapes.medium),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(demon.cardArt),
                contentDescription = if (locked) null else demon.name,
                contentScale = ContentScale.Crop,
                colorFilter = if (locked) {
                    ColorFilter.tint(colors.surfaceContainerLowest.copy(alpha = SILHOUETTE_ALPHA), BlendMode.SrcAtop)
                } else {
                    null
                },
                modifier = Modifier.fillMaxSize(),
            )
            if (locked) {
                Icon(
                    painter = painterResource(R.drawable.ic_lock),
                    contentDescription = stringResource(R.string.card_sealed),
                    tint = colors.outline,
                    modifier = Modifier.size(LockSize),
                )
            }
        }
        Spacer(Modifier.height(MaterialTheme.spacing.sm))
        Text(
            demon.name,
            style = MaterialTheme.typography.titleLarge,
            color = if (locked) colors.onSurfaceVariant else accent,
            maxLines = 1,
        )
        Text(
            if (locked) stringResource(R.string.card_sealed) else demon.title,
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(MaterialTheme.spacing.sm))
        PrimaryActionButton(
            text = stringResource(if (locked) R.string.action_summon else R.string.action_view),
            onClick = if (locked) onSummon else onView,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
```

- [ ] **Step 3: Create `ui/screens/RitualScreen.kt`**

```kotlin
package com.summondemons.safely.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.summondemons.safely.BuildConfig
import com.summondemons.safely.R
import com.summondemons.safely.platform.Haptics
import com.summondemons.safely.ritual.RitualUiState
import com.summondemons.safely.ritual.RitualViewModel
import com.summondemons.safely.ritual.headingLabel
import com.summondemons.safely.ui.components.ButtonTone
import com.summondemons.safely.ui.components.CompassIndicator
import com.summondemons.safely.ui.components.DebugControls
import com.summondemons.safely.ui.components.PrimaryActionButton
import com.summondemons.safely.ui.components.SigilView
import com.summondemons.safely.ui.theme.demonColors
import com.summondemons.safely.ui.theme.spacing
import kotlin.math.roundToInt

private val SigilSize = 220.dp
private const val CURRENT_PHRASE_ALPHA = 0.6f

@Composable
fun RitualScreen(
    viewModel: RitualViewModel,
    onSummoned: () -> Unit,
    onAbort: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current

    // Ask once; re-check on every resume (e.g. after returning from Settings).
    var requested by rememberSaveable { mutableStateOf(false) }
    var permanentlyDenied by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.onMicPermission(granted)
        permanentlyDenied = !granted && activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.RECORD_AUDIO)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        when {
            granted -> viewModel.onMicPermission(true)
            !requested -> {
                requested = true
                launcher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    LaunchedEffect(state.completed) {
        if (state.completed) {
            Haptics.success(context)
            onSummoned()
        }
    }

    RitualContent(
        state = state,
        permanentlyDenied = permanentlyDenied,
        onRetryPermission = { launcher.launch(Manifest.permission.RECORD_AUDIO) },
        onOpenSettings = {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
            )
        },
        onSimulateHeadingChange = viewModel::setSimulateHeading,
        onHoldToChantChange = viewModel::setHoldToChant,
        onAbort = onAbort,
        modifier = modifier,
    )
}

@Composable
private fun RitualContent(
    state: RitualUiState,
    permanentlyDenied: Boolean,
    onRetryPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onSimulateHeadingChange: (Boolean) -> Unit,
    onHoldToChantChange: (Boolean) -> Unit,
    onAbort: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val demon = state.demon
    val accent = MaterialTheme.demonColors.accent(demon.accent)
    val colors = MaterialTheme.colorScheme
    val spacing = MaterialTheme.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = colors.surfaceContainerHigh,
            border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(Modifier.padding(spacing.md), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.ritual_label), style = MaterialTheme.typography.labelMedium, color = accent)
                    Text(demon.name, style = MaterialTheme.typography.headlineLarge, color = colors.onSurface)
                    Text(
                        stringResource(R.string.ritual_instruction, headingLabel(demon.ritualHeading)),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                CompassIndicator(azimuth = state.azimuth, target = demon.ritualHeading, aligned = state.aligned)
            }
        }

        SigilView(sigil = demon.sigil, opacity = state.progress.sigilOpacity, accent = accent, modifier = Modifier.size(SigilSize))

        Surface(shape = MaterialTheme.shapes.medium, color = colors.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.chant_label), style = MaterialTheme.typography.labelMedium, color = accent)
                    Text(
                        stringResource(R.string.chant_resonance, (state.progress.sigilOpacity * 100).roundToInt()),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.primary,
                    )
                }
                demon.chantPhrases.forEachIndexed { index, phrase ->
                    val reached = index < state.progress.phraseIndex
                    val current = index == state.progress.phraseIndex
                    val phraseColor: Color = when {
                        reached -> accent
                        current && state.aligned -> accent.copy(alpha = CURRENT_PHRASE_ALPHA)
                        else -> colors.outline
                    }
                    Text(
                        phrase,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
                        color = phraseColor,
                        textAlign = TextAlign.Center,
                    )
                }
                LinearProgressIndicator(
                    progress = { state.progress.sustainFraction },
                    color = accent,
                    trackColor = colors.surfaceContainerLowest,
                    modifier = Modifier.fillMaxWidth().padding(top = spacing.sm),
                )
            }
        }

        if (state.micGranted == false) {
            Surface(shape = MaterialTheme.shapes.medium, color = colors.errorContainer, contentColor = colors.onErrorContainer) {
                Column(Modifier.padding(spacing.lg), verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                    Text(stringResource(R.string.mic_rationale), style = MaterialTheme.typography.bodyMedium)
                    PrimaryActionButton(
                        text = stringResource(if (permanentlyDenied) R.string.action_open_settings else R.string.action_retry),
                        onClick = if (permanentlyDenied) onOpenSettings else onRetryPermission,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        if (BuildConfig.DEBUG) {
            DebugControls(
                simulateHeading = state.simulateHeading,
                onSimulateHeadingChange = onSimulateHeadingChange,
                onHoldToChantChange = onHoldToChantChange,
            )
        }

        PrimaryActionButton(
            text = stringResource(R.string.action_abort),
            onClick = onAbort,
            tone = ButtonTone.Danger,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
```

- [ ] **Step 4: Create `ui/screens/ProfileScreen.kt`**

```kotlin
package com.summondemons.safely.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.summondemons.safely.R
import com.summondemons.safely.data.Demon
import com.summondemons.safely.platform.TadaPlayer
import com.summondemons.safely.ui.components.PrimaryActionButton
import com.summondemons.safely.ui.components.RibbonBanner
import com.summondemons.safely.ui.components.RotatingSunburstBackground
import com.summondemons.safely.ui.theme.demonColors
import com.summondemons.safely.ui.theme.spacing

private const val ART_ASPECT = 896f / 1200f
private const val ART_WIDTH_FRACTION = 0.8f
private const val SUNBURST_ALPHA = 0.16f

@Composable
fun ProfileScreen(
    demon: Demon,
    isNewSummon: Boolean,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.demonColors.accent(demon.accent)
    val colors = MaterialTheme.colorScheme
    val spacing = MaterialTheme.spacing

    if (isNewSummon) {
        val context = LocalContext.current
        DisposableEffect(Unit) {
            val player = TadaPlayer.play(context)
            onDispose { player?.release() }
        }
    }
    val bounce = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        bounce.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }

    Scaffold(
        modifier = modifier,
        containerColor = colors.background,
        bottomBar = {
            Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(spacing.lg)) {
                PrimaryActionButton(
                    text = stringResource(if (isNewSummon) R.string.action_awesome else R.string.action_back),
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            RotatingSunburstBackground(color = accent.copy(alpha = SUNBURST_ALPHA), modifier = Modifier.fillMaxSize())
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.xl, vertical = spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                if (isNewSummon) RibbonBanner(stringResource(R.string.profile_banner))
                Image(
                    painter = painterResource(demon.profileArt),
                    contentDescription = demon.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth(ART_WIDTH_FRACTION)
                        .aspectRatio(ART_ASPECT)
                        .graphicsLayer {
                            scaleX = bounce.value
                            scaleY = bounce.value
                        }
                        .clip(MaterialTheme.shapes.large)
                        .border(2.dp, accent, MaterialTheme.shapes.large),
                )
                Text(demon.name, style = MaterialTheme.typography.displaySmall, color = accent)
                Text(demon.title, style = MaterialTheme.typography.titleLarge, color = colors.primary, textAlign = TextAlign.Center)
                Text(demon.description, style = MaterialTheme.typography.bodyLarge, color = colors.onSurface, textAlign = TextAlign.Center)
                Surface(shape = MaterialTheme.shapes.medium, color = colors.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "“${demon.quote}”",
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(spacing.lg),
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 5: Create `navigation/AppNavHost.kt`**

```kotlin
package com.summondemons.safely.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.summondemons.safely.platform.AndroidHeadingSource
import com.summondemons.safely.platform.AndroidVolumeSource
import com.summondemons.safely.ritual.RitualViewModel
import com.summondemons.safely.ui.DemonViewModel
import com.summondemons.safely.ui.screens.GrimoireScreen
import com.summondemons.safely.ui.screens.ProfileScreen
import com.summondemons.safely.ui.screens.RitualScreen

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    val demonViewModel: DemonViewModel = viewModel() // activity-scoped
    val appContext = LocalContext.current.applicationContext

    NavHost(navController = navController, startDestination = Grimoire) {
        composable<Grimoire> {
            val demons by demonViewModel.demons.collectAsStateWithLifecycle()
            GrimoireScreen(
                demons = demons,
                onSummon = { id -> navController.navigate(Ritual(id)) },
                onView = { id -> navController.navigate(Profile(id, isNewSummon = false)) },
            )
        }

        composable<Ritual> { entry ->
            val demon = demonViewModel.demon(entry.toRoute<Ritual>().demonId)
            if (demon == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
                return@composable
            }
            val factory = remember(demon) {
                RitualViewModel.factory(demon, AndroidHeadingSource(appContext), AndroidVolumeSource())
            }
            RitualScreen(
                viewModel = viewModel(factory = factory),
                onSummoned = {
                    demonViewModel.unlock(demon.id)
                    navController.navigate(Profile(demon.id, isNewSummon = true)) { popUpTo<Grimoire>() }
                },
                onAbort = dropUnlessResumed { navController.popBackStack() },
            )
        }

        composable<Profile> { entry ->
            val route = entry.toRoute<Profile>()
            val demon = demonViewModel.demon(route.demonId)
            if (demon == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
                return@composable
            }
            ProfileScreen(
                demon = demon,
                isNewSummon = route.isNewSummon,
                onDone = dropUnlessResumed { navController.popBackStack<Grimoire>(inclusive = false) },
            )
        }
    }
}
```

`onSummon` / `onView` are not wrapped in `dropUnlessResumed` because they take an id argument; a double tap there only pushes the same destination twice and Back recovers. If that shows up in manual testing, guard with `if (navController.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED)`.

- [ ] **Step 6: Rewrite `MainActivity.kt`**

```kotlin
package com.summondemons.safely

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.summondemons.safely.navigation.AppNavHost
import com.summondemons.safely.ui.theme.SummonDemonsSafelyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Dark theme only: light-on-dark system bar icons regardless of system setting.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            SummonDemonsSafelyTheme {
                AppNavHost()
            }
        }
    }
}
```

- [ ] **Step 7: Build and test**

Run: `./gradlew assembleDebug testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`, all unit tests pass.

- [ ] **Step 8: Check the canon rules hold in code**

```bash
grep -rn "Color(0x" app/src/main/java | grep -v "/ui/theme/Color.kt" || echo "colors clean"
grep -rniE "\"(north|south|east|west)\"|340f|\"asmodeus\"|\"astaroth\"" app/src/main/java | grep -vE "/data/DemonCatalog.kt|/ritual/HeadingMath.kt" || echo "no hard-coded heading/ids"
```

Expected: `colors clean` and `no hard-coded heading/ids`.

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/summondemons/safely
git commit -q -m "feat(ui): Add Grimoire, Ritual and Profile screens with type-safe navigation" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 12: Docs

**Files:**
- Modify: `PRODUCT.md` (Screens and rules table), `DESIGN.md` (Asset catalog intro, Non-Stitch gap), `AGENTS.md` (Repo map, Current state)
- Create: `docs/decisions/0007-chant-sustain-tolerance.md`

- [ ] **Step 1: Update `PRODUCT.md`**

Replace the **Chant** row with:

```markdown
| Chant | The incantation is shown as **exactly 4 phrases**, grey until reached. Sustained mic volume above threshold for **2 s** while Active advances one phrase and highlights it in the demon's color. Brief silences (between words) drain the 2 s counter at half speed instead of resetting it (ADR-0007). |
```

Replace the **Permissions** row with:

```markdown
| Permissions | `RECORD_AUDIO` requested on entering the Ritual only if not already granted; if denied, explain why and offer a retry (or a shortcut to app settings once the denial is permanent). |
```

Add a row after **Debug aids**:

```markdown
| Orientation | Portrait only (the compass reading assumes it). |
```

- [ ] **Step 2: Update `DESIGN.md`**

Replace the sentence under "## Asset catalog"
`Sources are under \`reference/stitch/\`. Target names are \`res/drawable/\` resource names.` with:

```markdown
Sources are under `reference/stitch/`. Target names are resource names; the app
stores them in `res/drawable-nodpi/` as lossy WebP (`cwebp -q 85`) so large
sources are not density-scaled and the APK stays small. Only drawables a screen
uses are shipped (avatars are not yet).
```

Replace the "### Non-Stitch gap" bullet with:

```markdown
- **`tada` sound**: a placeholder ships as `res/raw/tada.wav` (≈1.2 s rising
  arpeggio + chord, 22.05 kHz mono), generated by `tools/gen_tada.py` — original
  synthesized work, CC0. WAV instead of `.ogg` because no ogg encoder was
  available. To replace it, use a ≤ 2 s CC0 fanfare (e.g. freesound.org filtered
  by CC0) named `tada.*` in `res/raw/` and record its source URL and licence here.
```

- [ ] **Step 3: Create `docs/decisions/0007-chant-sustain-tolerance.md`**

```markdown
# 0007 — Chant sustain tolerance and volume threshold

- Date: 2026-10-15
- Status: Accepted

## Context
PRODUCT.md requires mic volume "sustained above threshold for 2 s" per phrase.
Real chanting dips below any RMS threshold between syllables and words, so a
strict reset-on-silence rule makes phrases nearly impossible to complete. The
draft code decayed the counter instead and used an RMS threshold of ~1800
(16-bit PCM).

## Decision
The ritual ticks every 100 ms. A loud tick adds 100 ms to the sustain counter;
a quiet tick removes 50 ms (half rate, never below 0). At 2 000 ms the phrase
advances and the counter resets. Leaving the demon's heading still resets all
progress (PRODUCT.md). Loud means RMS > 1800 on 16 kHz mono PCM16. These are
global tuning constants in `RitualConfig`, not demon data (ADR-0006 covers
per-demon parameters).

## Consequences
A steady chant completes a phrase in ~2 s; short pauses cost a little time
rather than all of it. The threshold may need tuning per device class; change
`RitualConfig.VOLUME_RMS_THRESHOLD` and note it here.
```

- [ ] **Step 4: Update `AGENTS.md`**

In the Repo map table, add a row after `reference/src/`:

```markdown
| `tools/` | Small generators (e.g. `gen_tada.py` for `res/raw/tada.wav`) |
```

Replace the whole "## Current state" section body with:

```markdown
- `app/` is the Compose port (package `com.summondemons.safely`): Grimoire,
  Ritual and Profile per `PRODUCT.md`. Design:
  `docs/superpowers/specs/2026-10-15-compose-port-design.md`.
- Open items: DESIGN.md art gaps (`sigil_asmodeus`, `demon_astaroth_full`,
  cut-outs, `avatar_astaroth`), a real `tada` sound, bundling Cinzel/Inter, and
  the "LOVIS" question in ADR-0005.
```

- [ ] **Step 5: Commit**

```bash
git add PRODUCT.md DESIGN.md AGENTS.md docs/decisions/0007-chant-sustain-tolerance.md
git commit -q -m "docs: Record Compose port decisions (chant tolerance, assets, tada)" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 13: Final verification

- [ ] **Step 1: Clean build and tests**

Run: `./gradlew clean assembleDebug testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`; tests: DemonCatalogTest 5, DemonViewModelTest 3, HeadingMathTest 7, RitualEngineTest 7, RmsTest 3, RitualViewModelTest 6 — all pass.

- [ ] **Step 2: Lint**

Run: `./gradlew lintDebug`
Expected: no errors. Fix any error-level findings. Warnings about `screenOrientation` (large screens) are accepted, because PRODUCT.md requires portrait.

- [ ] **Step 3: Static canon checks**

```bash
grep -rn "com.example" app/ || echo "package clean"
grep -rn "Color(0x" app/src/main/java | grep -v "/ui/theme/Color.kt" || echo "colors clean"
git status --short reference/ | grep . && echo "REFERENCE TOUCHED" || echo "reference untouched"
```

Expected: `package clean`, `colors clean`, `reference untouched`.

- [ ] **Step 4: Manual checklist (emulator or device; `./gradlew installDebug`)**

Run each check and note the result in the PR / hand-over:

1. Launcher shows the game logo icon and the label "Summon Demons Safely".
2. Grimoire: both cards locked (dark silhouette, lock, "Sealed", **Summon**).
3. Tap **Summon** on Asmodeus → mic permission dialog appears once. Deny → the rationale card with "Allow microphone" appears. Rotate or background-and-return inside the same ritual → **no** new dialog (re-entering the ritual from the Grimoire asks again, per PRODUCT.md). Dismissing the dialog by tapping outside → still "Allow microphone". Deny twice ("Don't allow" again) → the button reads "Open settings" and opens app settings. Grant there, return → the card disappears.
4. Header reads "…face North"; compass arrow and readout move with the device (emulator: Extended controls → Virtual sensors → rotate). Green within ±20° of North, red outside.
5. Asmodeus sigil is the Canvas fallback in gold; Astaroth uses the sigil image in magenta.
6. Debug build: **Simulate heading** turns the compass green; **Hold to Chant** (press and hold) fills the bar; every 2 s one phrase lights up and opacity steps 10 → 25 → 50 → 75 → 100 %. Release mid-phrase → bar drains slowly. Untoggle Simulate (no real North) → progress resets.
7. On a real device: chanting loudly while facing North advances phrases; quiet room does not.
8. After phrase 4: sigil fully lit for ~1 s, vibration, then Profile with banner, sunburst, bounce, tada sound and **Awesome!** → Grimoire with that demon unlocked (accent border, art, **View**).
9. **View** → Profile without banner/sound, button **Back**. System back from a new-summon Profile → Grimoire (not the Ritual).
10. **Abort Ritual** → Grimoire; the demon is still locked.
11. Background the app during a ritual and reopen → no crash; the mic indicator (Android 12+) turns off while backgrounded.
12. `./gradlew assembleRelease` builds, and the release APK shows no debug controls (install it only if signing is configured).

- [ ] **Step 5: Final commit (only if the verification steps changed files)**

```bash
git add -A
git commit -q -m "fix: Address verification findings" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

## Spec coverage

| Spec section | Task |
|---|---|
| Build and packaging, rename, manifest, themes.xml | 1 |
| Theme (Color, Type, Shape, Spacing, DemonColors) | 2 |
| Assets, launcher icon, lock vector | 3 |
| Data (`Demon`, catalog), DemonViewModel | 2 (model), 4 |
| HeadingMath, headingLabel, smoothing, RitualConfig | 5 |
| RitualEngine | 6 |
| HeadingSource / VolumeSource + Android impls | 7 |
| RitualViewModel (pipeline, debug inputs, completion hold) | 8 |
| Components (6 + DebugControls), strings | 9 |
| Tada, Haptics, TadaPlayer | 10 |
| Screens, AppNavHost, permission flow, MainActivity | 11 |
| Docs updates + ADR-0007 | 12 |
| Error handling table, Done criteria, manual checks | 8, 11, 13 |
