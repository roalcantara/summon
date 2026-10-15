# Prompt 1 — Game MVP (given to Stitch, verbatim)

Role & Goal:
Act as an expert Android Developer. Build a one-day MVP for a native Android game called "Summon Damons Safely" using Kotlin and Jetpack Compose. The aesthetic is colorful, cute, and slightly childish (similar to Pokémon Go), contrasting the occult theme. Ensure the code follows Modern Android Development (MAD) guidelines.
Game Flow & Mechanics:
The Grimoire (Home Screen): A Jetpack Compose LazyColumn showing 2 playable Demons (Asmodeus and Astaroth). Each card shows a cute placeholder illustration, attributes, and their sigil.
The Ritual Setup: Tapping a demon navigates to the Ritual Screen. Display a large, opaque version of their sigil. Text instructs the user to "Hold the phone with both hands and face North".
Sensor Integration: Use Android's SensorManager (Magnetometer/Accelerometer) to calculate the device's azimuth. The game state only becomes "Active" when the phone is facing North (azimuth within 340° to 20°).
The Chanting Phase (Audio Detection):
Display the demon's incantation text on the screen, initially colored grey.
Use AudioRecord to monitor microphone input volume (request RECORD_AUDIO permissions).
Instead of complex speech-to-text, use a volume threshold approach: if the user produces sustained audio above a specific amplitude threshold for 2 seconds while facing North, advance the highlight to the next phrase (using the demon's theme color) and increase the alpha/opacity of the sigil by 25%.
If the device orientation drops away from North, reset the progress state to 0.
Success: When the chant is complete, the full sigil glows (opacity 1.0f). Trigger a simple animateDpAsState or updateTransition bounce animation of a cute demon appearing, accompanied by a placeholder "Tada!" sound effect using MediaPlayer.
Demon Data:
Asmodeus:
Theme Color: Color.Yellow
Chant: "DAEMONIUM RESIDENTE, OSTENDE MIHI FACIEM TUAM POTENTIAM, VENIT AD ME. DONA MIHI PER FOEDUS QUOD EST IRE AD PRAESENS SUM VOBIS, UT VOTA OMNIA OPUS EST".
Astaroth:
Theme Color: Color.Magenta (Purple)
Chant: "TUI GRATIA LOVIS GRATIA SIT CURA. HIC EST ME DIGNUS LEGI SINT BENE OBEDIENTES".
Implementation Steps:
Please provide the complete code for:
The MainActivity.kt setting up the Compose Navigation.
The SensorManager and AudioRecord logic abstracted into a clean ViewModel (or separate helper classes) using Kotlin Coroutines/Flow to emit state updates.
The Compose UI functions for the Grimoire List and the Ritual Screen.
Instructions on what permissions (e.g., RECORD_AUDIO) to add to the AndroidManifest.xml.
