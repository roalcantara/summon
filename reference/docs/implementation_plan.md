# Summon Demons Safely — Native Android MVP & Interactive Simulator

A native Android game MVP built with Kotlin and Jetpack Compose featuring safe Solomonic demon summoning with magnetometer orientation gating (True North) and microphone chant detection, paired with a live interactive web emulation preview in this workspace.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions were confirmed based on your requirements and design system specifications:

- **Deliverables**: Production-grade Kotlin Jetpack Compose code organized into clean MAD (Modern Android Development) modules (`../Theme.kt`, `../Components.kt`, `Demon.kt & DemonViewModel.kt`, `../HomeScreen.kt`, `../RitualScreen.kt`, `../DemonProfileScreen.kt`, and `../NavGraph.kt`), coupled with an interactive web simulator preview in the applet so you can interactively test the ritual compass, microphone chanting, sigil opacity progression, and demon unlock states immediately.
- **Visual Aesthetic & Theme**: Dark fantasy tactical RPG style matching the uploaded design system: abyssal obsidian/midnight navy surfaces (`#0B1326`), imperial amber gold (`#FFC174` / `#F59E0B`), arcane mana cyan (`#7BD0FF`), infernal nether magenta (`#FCB4FF`), Cinzel headers, tabular Inter statistics, and high-contrast glowing occult sigils.
- **Sensor & Audio Architecture**: Full Android `SensorManager` (Magnetometer + Accelerometer rotation matrix & orientation sensor) and `AudioRecord` RMS amplitude tracking coroutine flow, plus hardware test toggles (manual North lock and hold-to-chant simulator buttons) for seamless emulation on emulators or desktops.

---

### 1. Overview & Core Concept

- **What It Does**: "Summon Demons Safely" lets summoners safely invoke legendary Goetic spirits (Asmodeus & Astaroth) by fulfilling safe ritual protocols: aligning the device strictly toward True North ($340^\circ$–$20^\circ$) and reciting sacred Latin incantations into the microphone with sustained vocal resonance. Once summoned, demons unlock in the Grimoire and celebrate with a classic rotating sunburst victory screen, full stat dossiers, and demonic synergies.
- **Target Audience**: Mobile tactical RPG and monster collection players who love the juxtaposition of occult demonology with cute, collectible chibi character designs and physical device interactions.
- **Key Value**: Novel physical interaction loop combining real-world compass orientation and voice modulation with deep RPG progression and safe Solomonic lore.

---

### 2. User Experience & Visual Design

#### Key User Flows
1. **The Grimoire (Home Screen)**:
   - Header with summoner rank, currencies, and Grimoire title.
   - 2-column tactical roster displaying **Asmodeus** (unlocked by default or summonable) and **Astaroth** (locked).
   - Unlocked demons feature vibrant rarity borders, star tiers, power rating, and a "View" action button.
   - Locked demons are silhouetted in myst with an active "Summon" CTA.
2. **The Ritual Setup & Gating**:
   - Tapping "Summon" transitions to the ritual altar.
   - The user is instructed: *"Hold phone with both hands and face North"*.
   - A real-time occult compass needle tracks azimuth. The ritual state activates **only** when pointing North ($340^\circ$–$20^\circ$).
3. **The Chanting Phase**:
   - The demon's authentic Latin chant appears with karaoke-style highlighting.
   - Sustained vocal audio above amplitude threshold for 2 seconds advances phrase progress by 25% and intensifies the glowing sigil opacity ($0.25 \to 0.50 \to 0.75 \to 1.00$).
   - If the player tilts away from North before completion, the ritual destabilizes and progress resets to 0.
4. **Demon Profile & Summoning Triumph**:
   - When the chant reaches 100%, the sigil flashes, triumphant fanfare plays, and the screen reveals a golden rotating sunburst backdrop with an animated bouncing chibi demon.
   - A crimson "New Demon Received!" banner crowns the top.
   - Complete stat dossier displayed (Damage, Defense, Arcane Mastery, Intel, Legions, Demonic Synergies).
   - Bottom CTA toggles between "Awesome!" (new summon) and "Back" (roster view).

#### Visual Identity & Theme Tokens
- **Background & Canvas**: Obsidian navy (`#0B1326`, surface container `#171F33`, high `#222A3D`).
- **Primary / Gold**: Imperial Amber Gold (`#FFC174` / `#F59E0B`) for buttons, stars, and Asmodeus theme.
- **Secondary / Arcane**: Mystic Mana Cyan (`#7BD0FF`) for compass accuracy, defense, and stability.
- **Tertiary / Violet**: Infernal Nether Magenta (`#FCB4FF` / `#F487FF`) for Astaroth sigil and arcane mastery.
- **Typography**: Classical serif for banners/titles (`Cinzel` or Android `FontFamily.Serif`), clean sans-serif for stats (`Inter` / `FontFamily.SansSerif` with tabular figures).
- **Shapes & Elevation**: Chamfered stone slabs with metallic bronze border lines (`1px`), pill-free unboxed metadata, and glowing outer rune dropshadows.

---

### 3. Key Product Decisions & Trade-Offs

- **Kotlin Jetpack Compose MAD Code Structure**:
  - *Chosen Approach*: Deliver pristine Kotlin files using Compose 1.7+ APIs (`MaterialTheme`, `rememberCoroutineScope`, `produceState`, `Canvas`, `Animatable`, `NavHost`).
  - *Why*: Directly copy-pasteable into Android Studio Iguana/Jellyfish/Koala with zero build errors.
- **Audio & Sensor Hardware Handling**:
  - *Chosen Approach*: Encapsulate `SensorEventListener` with `SensorManager.getRotationMatrix` and `AudioRecord` amplitude polling inside an `AndroidRitualController` / StateFlow pattern, paired with quick UI simulation toggles.
  - *Why*: Allows the app to work seamlessly both on physical Android devices (with permission checks) and in Android Studio emulators or workspace browser previews where sensors may be virtual.
- **Interactive Dual Delivery**:
  - *Chosen Approach*: Deliver both the modular Kotlin code blocks (with copyable code tabs and export options) and a fully functional interactive web preview emulating the Android app experience with real compass & microphone detection.

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                      ANDROID JETPACK COMPOSE APP                       │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│   ┌─────────────────────┐                 ┌────────────────────────┐   │
│   │   DemonViewModel    │ ◄────────────── │ AndroidRitualEngine    │   │
│   │ - demons: StateFlow │                 │ - SensorManager (Azim) │   │
│   │ - unlockDemon(id)   │                 │ - AudioRecord (RMS)    │   │
│   └──────────┬──────────┘                 └────────────────────────┘   │
│              │                                                         │
│              ▼                                                         │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │                          NavHost                               │   │
│   │                                                                │   │
│   │  [Screen.Home]              [Screen.Ritual]                    │   │
│   │  ┌──────────────────────┐   ┌──────────────────────────────┐   │   │
│   │  │ Grimoire Grid        │   │ Occult Compass Needle        │   │   │
│   │  │ DemonCardBase        │──►│ 4-Stage Sigil Alpha (Canvas) │   │   │
│   │  │ PrimaryActionButton  │   │ Karaoke Latin Incantation    │   │   │
│   │  └──────────────────────┘   └──────────────┬───────────────┘   │   │
│   │                                            │                   │   │
│   │                                            ▼                   │   │
│   │                             [Screen.Profile]                   │   │
│   │                             ┌──────────────────────────────┐   │   │
│   │                             │ RibbonBanner ("New Demon!")  │   │   │
│   │                             │ Rotating Sunburst Canvas     │   │   │
│   │                             │ Bouncing Chibi Avatar        │   │   │
│   │                             │ Solomonic Stat Dossier       │   │   │
│   │                             └──────────────────────────────┘   │   │
│   └────────────────────────────────────────────────────────────────┘   │
│                                                                        │
│   Theme: Dark Fantasy Occult RPG Palette • Shapes 16-24dp • MAD Enforced│
└────────────────────────────────────────────────────────────────────────┘
```

#### Modules to Deliver:
1. **`../Theme.kt`**: MaterialTheme color palette (`Primary = Gold`, `Tertiary = Magenta`, `Surface = Obsidian`), Typography (`Cinzel` serif display, `Inter` body), and friendly rounded shapes (`16.dp` and `24.dp`).
2. **`../Components.kt`**: `PrimaryActionButton`, `DemonCardBase`, `RibbonBanner`, `RotatingSunburstBackground`, `CompassRose`, `SigilCanvas`.
3. **`../Demon.kt` & `DemonViewModel.kt`**: Data model with stats, chants, and reactive unlock state for Asmodeus and Astaroth.
4. **`../HomeScreen.kt`**: Grimoire grid with unlocked/locked cards and navigation callbacks.
5. **`../RitualScreen.kt`**: SensorManager orientation listener, AudioRecord vocal amplitude loop, 4-stage sigil opacity reveal, and fallback test controls.
6. **`../DemonProfileScreen.kt`**: Sunburst canvas animation, bounce transition, stat matrices, and synergistic pact info.
7. **`../NavGraph.kt` & `MainActivity.kt`**: Navigation setup connecting all three destinations.
8. **Interactive Web Application (`App.tsx` & components)**: High-fidelity interactive simulator letting the user test the game live in the browser.
