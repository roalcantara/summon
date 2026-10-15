# Prompt 2 — UI design system (given to Stitch, verbatim)

Role & Goal:
Act as an expert Android Developer using Jetpack Compose. I need to build the UI layer for the "Summon Damons Safely" MVP. First, establish a centralized, cute, and colorful Design System. Then, generate the HomeScreen (Grimoire) and the DemonProfileScreen (Success/View) using those exact shared components so the game looks highly cohesive.
Step 1: Establish the Theme (Theme.kt & Components.kt)
Colors: Define a custom MaterialTheme with a bright, "Pokémon Go" style palette. Use a soft off-white/cream for the background, a vibrant primary color (e.g., Amber or Gold), and distinct accent colors for the demons (Yellow for Asmodeus, Purple for Astaroth).
Shapes: Define a Shapes object heavily relying on RoundedCornerShape(16.dp) or 24.dp to give everything a friendly, bubbly, and childish look.
Reusable Components: Create a PrimaryActionButton composable that takes text and an onClick lambda. It should have a vibrant background color, rounded corners, white bold text, and a slight drop shadow. Create a DemonCardBase composable with an elevated surface and rounded corners to be used as the foundation for grid items.
Step 2: Generate the HomeScreen (Grimoire)
Use a Scaffold with a top bar titled "My Grimoire" in bold, friendly typography.
The body should be a LazyVerticalGrid displaying the demons.
Use the DemonCardBase. If the demon is unlocked, show their specific theme color as a subtle border or background tint, display their cute illustration, and use the PrimaryActionButton to say "View".
If locked, tint the card grey, show a silhouette, and use the PrimaryActionButton to say "Summon".
Step 3: Generate the DemonProfileScreen (Unified Success & View)
Use the same Scaffold and background color from the MaterialTheme.
The Banner: If isNewSummon == true, display a custom composable RibbonBanner at the top using the theme's primary color, displaying "New Demon Received!" in white bold text.
The Background: Implement the rotating shiny sunburst effect behind the character using a standard Compose Canvas and infiniteRepeatable rotation animation.
The Character & Info: Center the demon image. Place their name below in the theme's largest header typography, followed by their description in a clean, readable sans-serif.
The Action: Pin the PrimaryActionButton to the bottom of the screen. If isNewSummon, it says "Awesome!". If not, it says "Back".
Output Requirements:
Provide the complete Kotlin code separated into clear blocks for:
Theme.kt (Colors, Typography, Shapes).
Components.kt (The reusable buttons and cards).
HomeScreen.kt.
DemonProfileScreen.kt.
Ensure all composables strictly utilize the custom MaterialTheme rather than hardcoding colors and padding on the fly.
