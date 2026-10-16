package com.summondemons.safely.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.summondemons.safely.R
import com.summondemons.safely.data.Demon
import com.summondemons.safely.progress.DemonRecord
import com.summondemons.safely.ui.fx.LocalReduceMotion
import com.summondemons.safely.ui.theme.demonColors
import com.summondemons.safely.ui.theme.spacing
import com.summondemons.safely.ui.theme.textRoles
import com.summondemons.safely.ui.theme.upper
import kotlinx.coroutines.delay
import kotlin.math.abs

const val DOSSIER_PAGES = 6
const val SECRET_PAGE = 5

private const val PAGE_ANIM_MS = 180
private const val PAGE_SLIDE_DIVISOR = 12
private const val INK_MS = 900
private const val INK_STAGGER_MS = 120L
private const val SMUDGE_FIRST_WIDTH = 0.92f
private const val SMUDGE_WIDTH_STEP = 0.17f
private const val SMUDGE_STRIPE_SPLIT = 7f / 11f
private val Swipe = 40.dp
private val InkBlur = 6.dp
private val SmudgeHeight = 12.dp
private val SmudgeStripe = 11.dp
private val PageTextSize = 13.sp

/**
 * The six dossier pages (ADR-0011): ‹ › and horizontal swipe, wrapping. The page body scrolls
 * inside and never resizes the card. Unrevealed demons show smudges; [inkCodex] / [inkBattle]
 * ink the revealed lines in once after a summon.
 */
@Composable
fun DossierCarousel(
    demon: Demon,
    record: DemonRecord,
    inkCodex: Boolean,
    inkBattle: Boolean,
    page: Int,
    onPage: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val roles = MaterialTheme.textRoles
    val colors = MaterialTheme.colorScheme
    val reduceMotion = LocalReduceMotion.current
    val swipePx = with(LocalDensity.current) { Swipe.toPx() }
    val step: (Int) -> Unit = { delta -> onPage((page + delta + DOSSIER_PAGES) % DOSSIER_PAGES) }
    val currentStep by rememberUpdatedState(step)
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        PageButton("‹", stringResource(R.string.dossier_previous)) { step(-1) }
        Column(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .pointerInput(Unit) {
                    var total = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { total = 0f },
                        onDragEnd = { if (abs(total) > swipePx) currentStep(if (total < 0f) 1 else -1) },
                    ) { _, amount -> total += amount }
                },
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    pageTitle(page, demon, record).upper(),
                    style = roles.meta,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Text(stringResource(R.string.dossier_page, page + 1, DOSSIER_PAGES), style = roles.meta)
            }
            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    if (reduceMotion) {
                        fadeIn(tween(PAGE_ANIM_MS)) togetherWith fadeOut(tween(PAGE_ANIM_MS))
                    } else {
                        (fadeIn(tween(PAGE_ANIM_MS)) + slideInHorizontally(tween(PAGE_ANIM_MS)) { it / PAGE_SLIDE_DIVISOR }) togetherWith
                            (fadeOut(tween(PAGE_ANIM_MS)) + slideOutHorizontally(tween(PAGE_ANIM_MS)) { -it / PAGE_SLIDE_DIVISOR })
                    }
                },
                label = "dossierPage",
                modifier = Modifier.weight(1f).fillMaxWidth().padding(top = MaterialTheme.spacing.xs + 2.dp),
            ) { p ->
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
                ) {
                    PageBody(p, demon, record, inkCodex, inkBattle, colors.onSurface)
                }
            }
        }
        PageButton("›", stringResource(R.string.dossier_next)) { step(1) }
    }
}

@Composable
private fun pageTitle(page: Int, demon: Demon, record: DemonRecord): String = when (page) {
    0 -> if (record.revealed) stringResource(R.string.dossier_weakness_named, demon.weakness.name) else stringResource(R.string.dossier_weakness)
    1 -> stringResource(R.string.dossier_abilities)
    2 -> stringResource(R.string.dossier_offices)
    3 -> stringResource(R.string.dossier_appearance)
    4 -> stringResource(R.string.dossier_warning)
    else -> stringResource(R.string.dossier_secret)
}

@Composable
private fun PageBody(page: Int, demon: Demon, record: DemonRecord, inkCodex: Boolean, inkBattle: Boolean, textColor: Color) {
    val body = MaterialTheme.textRoles.body.copy(fontSize = PageTextSize, color = textColor)
    val accent = MaterialTheme.demonColors.accent(demon.accent)
    val nameStyle = MaterialTheme.textRoles.name
    val lines: List<String> = when (page) {
        0 -> listOf(demon.weakness.line)
        2 -> demon.offices.map { "•  $it" }
        3 -> listOf(demon.appearance)
        4 -> listOf(demon.warning)
        else -> emptyList()
    }
    when (page) {
        SECRET_PAGE -> if (record.flawless) {
            Text(demon.secret, style = body.copy(fontStyle = FontStyle.Italic)) // no ink-in, as in the prototype
        } else {
            Smudge(lines = 1)
            Text(stringResource(R.string.dossier_secret_locked).upper(), style = MaterialTheme.textRoles.meta)
        }
        1 -> if (!record.revealed) {
            Smudge(lines = 3)
        } else {
            demon.abilities.forEachIndexed { i, ability ->
                InkLine(inkBattle, i) {
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = accent, fontFamily = nameStyle.fontFamily, fontWeight = nameStyle.fontWeight)) {
                                append("${ability.name}.")
                            }
                            append(" ${ability.line}")
                        },
                        style = body,
                    )
                }
            }
        }
        else -> if (!record.revealed) {
            Smudge(lines = if (page == 0 || page == 4) 2 else 3)
        } else {
            val ink = if (page == 0) inkBattle else inkCodex
            lines.forEachIndexed { i, line -> InkLine(ink, i) { Text(line, style = body) } }
        }
    }
}

@Composable
private fun PageButton(glyph: String, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.semantics { contentDescription = label }) {
        Text(glyph, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.outline, modifier = Modifier.clearAndSetSemantics { })
    }
}

/** Revealed text soaking in once: blur → sharp, staggered 120 ms per line, 900 ms each (§4). */
@Composable
fun InkLine(ink: Boolean, index: Int, content: @Composable () -> Unit) {
    val progress = remember { Animatable(if (ink) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (ink) {
            delay(index * INK_STAGGER_MS)
            progress.animateTo(1f, tween(INK_MS, easing = FastOutSlowInEasing))
        }
    }
    Box(Modifier.graphicsLayer { alpha = progress.value }.blur(InkBlur * (1f - progress.value))) { content() }
}

/** Sealed lore: rounded bars with a diagonal stripe pattern (DESIGN.md). */
@Composable
fun Smudge(lines: Int, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val stripe = with(LocalDensity.current) { SmudgeStripe.toPx() }
    val a = colors.surfaceContainerHighest
    val b = colors.surfaceContainerHigh
    val brush = remember(a, b, stripe) {
        Brush.linearGradient(
            0f to a, SMUDGE_STRIPE_SPLIT to a, SMUDGE_STRIPE_SPLIT to b, 1f to b,
            start = Offset.Zero, end = Offset(stripe, stripe * 0.2f), tileMode = TileMode.Repeated,
        )
    }
    val sealed = stringResource(R.string.dossier_sealed)
    Column(
        modifier.semantics { contentDescription = sealed },
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs + 2.dp),
    ) {
        repeat(lines) { i ->
            Box(
                Modifier
                    .fillMaxWidth(SMUDGE_FIRST_WIDTH - i * SMUDGE_WIDTH_STEP)
                    .height(SmudgeHeight)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(brush),
            )
        }
    }
}
