package com.summondemons.safely.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.summondemons.safely.R
import com.summondemons.safely.ritual.headingLabel
import com.summondemons.safely.ui.DemonEntry
import com.summondemons.safely.ui.components.DemonCutout
import com.summondemons.safely.ui.components.PrimaryActionButton
import com.summondemons.safely.ui.fx.HeatShimmer
import com.summondemons.safely.ui.fx.InkedSigil
import com.summondemons.safely.ui.fx.LocalReduceMotion
import com.summondemons.safely.ui.fx.ParticleField
import com.summondemons.safely.ui.fx.ParticleStyle
import com.summondemons.safely.ui.fx.SharedDemonArt
import com.summondemons.safely.ui.fx.rememberFrameTimeMs
import com.summondemons.safely.ui.fx.rememberParallax
import com.summondemons.safely.ui.theme.demonColors
import com.summondemons.safely.ui.theme.spacing
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

private const val SCENE_SCRIM_TOP = 0.55f
private const val SCENE_SCRIM_BOTTOM_LOCKED = 0.92f
private const val SCENE_SCRIM_BOTTOM_UNLOCKED = 0.75f
private const val SCENE_SCALE = 1.1f
private const val SCENE_PARALLAX = -0.5f
private const val SIGIL_ALPHA_LOCKED = 0.15f
private const val SIGIL_ALPHA_UNLOCKED = 0.3f
private const val SIGIL_DEG_PER_SECOND = 4f
private const val FLOAT_PERIOD_S = 4f
private const val EMBER_COUNT = 24
private const val SMOKE_COUNT = 10
private val SceneBlur = 16.dp
private val FloatAmplitude = 6.dp
private val LogoSize = 36.dp
private val AvatarSize = 40.dp
private val SelectedAvatarSize = 52.dp
private val AvatarRing = 2.dp
private val AvatarRowHeight = 72.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrimoireScreen(
    demons: List<DemonEntry>,
    onSummon: (String) -> Unit,
    onView: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    val pager = rememberPagerState { demons.size }
    val scope = rememberCoroutineScope()
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { innerPadding ->
        val top = innerPadding.calculateTopPadding()
        Box(Modifier.fillMaxSize()) {
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize(), key = { demons[it].demon.id }) { page ->
                val entry = demons[page]
                DemonHeroPage(
                    entry = entry,
                    topPadding = top,
                    onSummon = { onSummon(entry.demon.id) },
                    onView = { onView(entry.demon.id) },
                )
            }
            AvatarRow(
                demons = demons,
                selected = pager.currentPage,
                onSelect = { scope.launch { pager.animateScrollToPage(it) } },
                modifier = Modifier.align(Alignment.TopCenter).padding(top = top + spacing.sm),
            )
        }
    }
}

@Composable
private fun DemonHeroPage(entry: DemonEntry, topPadding: Dp, onSummon: () -> Unit, onView: () -> Unit) {
    val demon = entry.demon
    val locked = !entry.unlocked
    val accent = MaterialTheme.demonColors.accent(demon.accent)
    val colors = MaterialTheme.colorScheme
    val spacing = MaterialTheme.spacing
    val reduceMotion = LocalReduceMotion.current
    val parallax by rememberParallax()
    val time by if (reduceMotion) remember { mutableLongStateOf(0L) } else rememberFrameTimeMs()
    val floatPx = with(LocalDensity.current) { FloatAmplitude.toPx() }

    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(demon.sceneArt),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = SCENE_SCALE
                    scaleY = SCENE_SCALE
                    translationX = parallax.x * SCENE_PARALLAX
                    translationY = parallax.y * SCENE_PARALLAX
                }
                .blur(SceneBlur),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            colors.surfaceContainerLowest.copy(alpha = SCENE_SCRIM_TOP),
                            colors.surfaceContainerLowest.copy(alpha = if (locked) SCENE_SCRIM_BOTTOM_LOCKED else SCENE_SCRIM_BOTTOM_UNLOCKED),
                        ),
                    ),
                ),
        )
        InkedSigil(
            sigil = demon.sigil,
            inkProgress = 1f,
            accent = accent,
            rimText = demon.name.uppercase(),
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .aspectRatio(1f)
                .graphicsLayer {
                    rotationZ = if (reduceMotion) 0f else time / 1_000f * SIGIL_DEG_PER_SECOND
                    alpha = if (locked) SIGIL_ALPHA_LOCKED else SIGIL_ALPHA_UNLOCKED
                },
        )
        HeatShimmer(accent, Modifier.fillMaxSize())
        ParticleField(accent, ParticleStyle.Embers, Modifier.fillMaxSize(), count = EMBER_COUNT, seed = demon.id.hashCode())
        if (locked) {
            ParticleField(colors.onSurfaceVariant, ParticleStyle.Smoke, Modifier.fillMaxSize(), count = SMOKE_COUNT, seed = demon.id.hashCode() + 1)
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topPadding + AvatarRowHeight)
                .navigationBarsPadding()
                .padding(horizontal = spacing.xl, vertical = spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            SharedDemonArt(
                demonId = demon.id,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .graphicsLayer {
                        val bob = if (reduceMotion) 0f else sin(time / 1_000f * 2f * PI.toFloat() / FLOAT_PERIOD_S) * floatPx
                        translationX = parallax.x
                        translationY = parallax.y + bob
                    },
            ) {
                DemonCutout(
                    art = demon.cutoutArt,
                    contentDescription = if (locked) null else demon.name,
                    accent = accent,
                    modifier = Modifier.fillMaxSize(),
                    silhouette = if (locked) 1f else 0f,
                )
            }
            Text(demon.name, style = MaterialTheme.typography.displaySmall, color = if (locked) colors.onSurfaceVariant else accent)
            Text(
                if (locked) stringResource(R.string.grimoire_sealed_hint, headingLabel(demon.ritualHeading)) else demon.title,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                minLines = 2,
                maxLines = 2,
            )
            PrimaryActionButton(
                text = stringResource(if (locked) R.string.action_summon else R.string.action_view),
                onClick = if (locked) onSummon else onView,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun AvatarRow(demons: List<DemonEntry>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        demons.forEachIndexed { index, entry ->
            val isSelected = index == selected
            val accent = MaterialTheme.demonColors.accent(entry.demon.accent)
            val size by animateDpAsState(if (isSelected) SelectedAvatarSize else AvatarSize, label = "avatarSize")
            Box(
                Modifier
                    .minimumInteractiveComponentSize()
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(index) }),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(entry.demon.avatarArt),
                    contentDescription = entry.demon.name,
                    contentScale = ContentScale.Crop,
                    colorFilter = if (entry.unlocked) null else ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }),
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape)
                        .border(AvatarRing, if (isSelected) accent else colors.outlineVariant, CircleShape),
                )
            }
        }
    }
}
