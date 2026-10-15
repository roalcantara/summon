# Android Studio Asset Implementation & Resource Manifest
**Project:** Summon Demons Safely (MVP)  
**Package:** `com.summondemons.safely`  
**Android Target:** API 34+ / Android Compose BOM 2024.02+

---

## 1. Android Project Directory Structure (`res/drawable`)

In Android Studio, place all raster graphics and icons under `app/src/main/res/drawable/` (or density qualifiers such as `drawable-xxhdpi/` / `drawable-xxxhdpi/`).

```
app/src/main/res/
├── drawable/
│   ├── ic_game_logo.png               <-- Game Title Badge (IMAGE_19)
│   ├── avatar_asmodeus.png            <-- Hero Portrait Avatar (IMAGE_17)
│   ├── avatar_astaroth.png            <-- Hero Portrait Avatar (IMAGE_15)
│   ├── demon_asmodeus_card.png        <-- Framed Character Artwork (IMAGE_10)
│   ├── demon_asmodeus_full.png        <-- Full-Body Character Artwork (IMAGE_18)
│   ├── demon_astaroth_card.png        <-- Framed Character Artwork (IMAGE_16)
│   ├── sigil_astaroth.png             <-- Violet Arcane Sigil (IMAGE_9)
│   ├── sigil_asmodeus.png             <-- Golden Runic Sigil (from Ritual)
│   └── ic_star_gold.xml               <-- Vector star icon
└── values/
    ├── strings.xml
    └── colors.xml
```

---

## 2. Asset Mapping Catalog

| Asset Role | Android Resource ID | File Name | Source Asset ID | Suggested Size / Density |
| :--- | :--- | :--- | :--- | :--- |
| **Game Title Logo** | `R.drawable.ic_game_logo` | `ic_game_logo.png` | `{{DATA:IMAGE:IMAGE_19}}` | 1024×1024 (xxxhdpi: 256×256dp) |
| **Asmodeus Avatar** | `R.drawable.avatar_asmodeus` | `avatar_asmodeus.png` | `{{DATA:IMAGE:IMAGE_17}}` | 1024×1024 (xxxhdpi: 120×120dp) |
| **Astaroth Avatar** | `R.drawable.avatar_astaroth` | `avatar_astaroth.png` | `{{DATA:IMAGE:IMAGE_15}}` | 1024×1024 (xxxhdpi: 120×120dp) |
| **Asmodeus Card Art** | `R.drawable.demon_asmodeus_card` | `demon_asmodeus_card.png` | `{{DATA:IMAGE:IMAGE_10}}` | 896×1200 (xxhdpi: 300×400dp) |
| **Asmodeus Full Body** | `R.drawable.demon_asmodeus_full` | `demon_asmodeus_full.png` | `{{DATA:IMAGE:IMAGE_18}}` | 896×1200 (xxhdpi: 300×400dp) |
| **Astaroth Card Art** | `R.drawable.demon_astaroth_card` | `demon_astaroth_card.png` | `{{DATA:IMAGE:IMAGE_16}}` | 896×1200 (xxhdpi: 300×400dp) |
| **Astaroth Arcane Sigil** | `R.drawable.sigil_astaroth` | `sigil_astaroth.png` | `{{DATA:IMAGE:IMAGE_9}}` | 1024×1024 (xxxhdpi: 320×320dp) |

---

## 3. Data Model & Demon Registry (`DemonsData.kt`)

Copy this file into `com.summondemons.safely.data.DemonsData.kt` to bind the resource IDs directly to your Compose UI state:

```kotlin
package com.summondemons.safely.data

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.summondemons.safely.R
import com.summondemons.safely.ui.theme.*

data class DemonModel(
    val id: String,
    val name: String,
    val title: String,
    val catchphrase: String,
    val description: String,
    val latinChant: String,
    val chantTranslation: String,
    val safetyProtocol: String,
    val candleColorName: String,
    val incenseType: String,
    val isUnlocked: Boolean,
    val stars: Int,
    val powerLevel: Int,
    val damage: Float,
    val defense: Float,
    val arcaneMastery: Float,
    @DrawableRes val avatarResId: Int,
    @DrawableRes val cardImageResId: Int,
    @DrawableRes val sigilResId: Int,
    val accentColor: Color,
    val containerColor: Color
)

object DemonRegistry {
    val Asmodeus = DemonModel(
        id = "demon_asmodeus",
        name = "Asmodeus",
        title = "Lord of Arithmetic & Hidden Wealth",
        catchphrase = "Three heads are better than one—especially when two want your soul!",
        description = "Teaches arithmetic, astronomy, geometry, and invincibility. Answers all questions—if you can stand the smugness.",
        latinChant = "DAEMONIUM RESIDENTE, OSTENDE MIHI FACIEM TUAM POTENTIAM, VENIT AD ME. DONA MIHI PER FOEDUS QUOD EST IRE AD PRAESENS SUM VOBIS, UT VOTA OMNIA OPUS EST.",
        chantTranslation = "Residing demon, show me thy face and power, come unto me.",
        safetyProtocol = "When Asmodeus senses no hostility, contact manifests as sudden warmth or mental geometric insight. Maintain north focus and keep the parchment stable.",
        candleColorName = "Yellow Candle",
        incenseType = "Cedar / Myrrh",
        isUnlocked = true,
        stars = 5,
        powerLevel = 18450,
        damage = 0.84f,
        defense = 0.52f,
        arcaneMastery = 0.99f,
        avatarResId = R.drawable.avatar_asmodeus,
        cardImageResId = R.drawable.demon_asmodeus_card,
        sigilResId = R.drawable.demon_asmodeus_card, // or dedicated sigil
        accentColor = AsmodeusYellow,
        containerColor = AsmodeusYellowContainer
    )

    val Astaroth = DemonModel(
        id = "demon_astaroth",
        name = "Astaroth",
        title = "Duke of Past, Present & Future • 40 Legions",
        catchphrase = "I know your past, your future, and that you didn't read the terms of service.",
        description = "Detects secret desires and grants protection to the great. Declares how the spirits fell and the reason for his own fall.",
        latinChant = "TUI GRATIA LOVIS GRATIA SIT CURA. HIC EST ME DIGNUS LEGI SINT BENE OBEDIENTES.",
        chantTranslation = "Thy grace, Jupiter's grace be thy care. Here is one worthy of my legion, well obedient.",
        safetyProtocol = "When Astaroth feels safe, contact manifests through mental imagery, whisperings of past secrets, or faint shadow movement. Maintain reverent focus.",
        candleColorName = "Yellow Flame",
        incenseType = "Cedar Incense",
        isUnlocked = true,
        stars = 4,
        powerLevel = 15920,
        damage = 0.72f,
        defense = 0.68f,
        arcaneMastery = 0.94f,
        avatarResId = R.drawable.avatar_astaroth,
        cardImageResId = R.drawable.demon_astaroth_card,
        sigilResId = R.drawable.sigil_astaroth,
        accentColor = AstarothPurple,
        containerColor = AstarothPurpleContainer
    )

    val AllDemons: List<DemonModel> = listOf(Asmodeus, Astaroth)
}
```

---

## 4. Compose Image Implementation Snippets

### A. Displaying Demon Profile Header or Avatar
```kotlin
Image(
    painter = painterResource(id = demon.avatarResId),
    contentDescription = "${demon.name} Avatar",
    modifier = Modifier
        .size(64.dp)
        .clip(CircleShape)
        .border(2.dp, demon.accentColor, CircleShape)
)
```

### B. Displaying Summoning Sigil with Arcane Glow
```kotlin
Box(
    modifier = Modifier
        .size(280.dp)
        .padding(16.dp),
    contentAlignment = Alignment.Center
) {
    Image(
        painter = painterResource(id = demon.sigilResId),
        contentDescription = "Sigil of ${demon.name}",
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize()
    )
}
```

### C. Displaying Hero Character Stage Card (with 24.dp Rounded Framing)
```kotlin
Card(
    shape = RoundedCornerShape(20.dp),
    border = BorderStroke(2.dp, demon.accentColor.copy(alpha = 0.75f)),
    modifier = Modifier
        .fillMaxWidth(0.92f)
        .height(380.dp)
) {
    Image(
        painter = painterResource(id = demon.cardImageResId),
        contentDescription = demon.name,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
    )
}
```
