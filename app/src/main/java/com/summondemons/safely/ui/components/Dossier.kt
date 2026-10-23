package com.summondemons.safely.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.summondemons.safely.R
import com.summondemons.safely.data.Demon
import com.summondemons.safely.progress.DemonRecord
import com.summondemons.safely.ui.theme.demonColors
import com.summondemons.safely.ui.theme.spacing
import com.summondemons.safely.ui.theme.textRoles
import com.summondemons.safely.ui.theme.upper
import kotlinx.coroutines.delay
import com.summondemons.safely.ui.fx.LocalReduceMotion

const val DOSSIER_PAGES = 6
const val SECRET_PAGE = 5

private const val INK_MS = 900
private const val INK_STAGGER_MS = 120L
private const val SMUDGE_FIRST_WIDTH = 0.92f
private const val SMUDGE_WIDTH_STEP = 0.17f
private const val SMUDGE_STRIPE_SPLIT = 7f / 11f
private val InkBlur = 6.dp
private val SmudgeHeight = 12.dp
private val SmudgeStripe = 11.dp
private val PageTextSize = 13.sp

/**
 * The dossier on the back of the demon card (spec §2; replaces the carousel): the six sections
 * (ADR-0011) in one scrolling list, each a header and its text. Sealed sections show a smudge
 * and how to unseal them; [inkCodex] / [inkBattle] ink revealed lines in once. When
 * [scrollToSecret] turns true the list scrolls to its end, where the Secret is.
 */
@Composable
fun DossierList(
    demon: Demon,
    record: DemonRecord,
    inkCodex: Boolean,
    inkBattle: Boolean,
    scrollToSecret: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val spacing = MaterialTheme.spacing
    val scroll = rememberScrollState()
    LaunchedEffect(demon.id) { scroll.scrollTo(0) }
    val reduceMotion = LocalReduceMotion.current
    LaunchedEffect(scrollToSecret) {
        if (scrollToSecret) {
            if (reduceMotion) scroll.scrollTo(scroll.maxValue) else scroll.animateScrollTo(scroll.maxValue)
        }
    }
    Column(modifier.verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        repeat(DOSSIER_PAGES) { page ->
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Text(pageTitle(page, demon, record).upper(), style = MaterialTheme.textRoles.meta)
                PageBody(page, demon, record, inkCodex, inkBattle, colors.onSurface)
            }
        }
    }
}

@Composable
private fun pageTitle(page: Int, demon: Demon, record: DemonRecord): String = when (page) {
    0 -> if (record.revealed) stringResource(R.string.dossier_weakness_named, stringResource(demon.weakness.name)) else stringResource(R.string.dossier_weakness)
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
        0 -> listOf(stringResource(demon.weakness.line))
        2 -> stringArrayResource(demon.offices).map { "•  $it" }
        3 -> listOf(stringResource(demon.appearance))
        4 -> listOf(stringResource(demon.warning))
        else -> emptyList()
    }
    when (page) {
        SECRET_PAGE -> if (record.flawless) {
            Text(stringResource(demon.secret), style = body.copy(fontStyle = FontStyle.Italic)) // no ink-in, as in the prototype
        } else {
            Smudge(lines = 1)
            SealLine(stringResource(R.string.dossier_secret_locked))
        }
        1 -> if (!record.revealed) {
            Smudge(lines = 3)
            SealLine(stringResource(R.string.dossier_sealed))
        } else {
            demon.abilities.forEachIndexed { i, ability ->
                val abilityName = stringResource(ability.name)
                val abilityLine = stringResource(ability.line)
                InkLine(inkBattle, i) {
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = accent, fontFamily = nameStyle.fontFamily, fontWeight = nameStyle.fontWeight)) {
                                append("$abilityName.")
                            }
                            append(" $abilityLine")
                        },
                        style = body,
                    )
                }
            }
        }
        else -> if (!record.revealed) {
            Smudge(lines = if (page == 0 || page == 4) 2 else 3)
            SealLine(stringResource(R.string.dossier_sealed))
        } else {
            val ink = if (page == 0) inkBattle else inkCodex
            lines.forEachIndexed { i, line -> InkLine(ink, i) { Text(line, style = body) } }
        }
    }
}

/** How to unseal a section: a small meta line under its smudge. */
@Composable
private fun SealLine(text: String) {
    Text(text.upper(), style = MaterialTheme.textRoles.meta, modifier = Modifier.padding(top = MaterialTheme.spacing.xs))
}

/** Revealed text soaking in once: blur → sharp, staggered 120 ms per line, 900 ms each (§4); at once with reduced motion. */
@Composable
fun InkLine(ink: Boolean, index: Int, content: @Composable () -> Unit) {
    val animate = ink && !LocalReduceMotion.current
    val progress = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (animate) {
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
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
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
