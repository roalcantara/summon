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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
        // Saved so activity recreation (font scale, locale) doesn't replay the fanfare.
        var tadaPlayed by rememberSaveable { mutableStateOf(false) }
        DisposableEffect(Unit) {
            val player = if (tadaPlayed) null else TadaPlayer.play(context).also { tadaPlayed = true }
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
                    painter = painterResource(demon.sceneArt),
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
