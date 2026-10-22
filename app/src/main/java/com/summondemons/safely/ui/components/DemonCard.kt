package com.summondemons.safely.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.summondemons.safely.R
import com.summondemons.safely.data.Demon
import com.summondemons.safely.progress.DemonRecord
import com.summondemons.safely.ui.fx.attentionGlow
import com.summondemons.safely.ui.fx.rememberAttention
import com.summondemons.safely.ui.theme.demonColors
import com.summondemons.safely.ui.theme.spacing
import com.summondemons.safely.ui.theme.textRoles

private const val CARD_ALPHA = 0.95f
private const val METAL_RING_ALPHA = 0.45f
private val CarouselTopPadding = 12.dp
private val MedallionSize = 48.dp
private val NameSize = 26.sp
private val NumberSize = 18.sp
private val TitleSize = 12.sp

/**
 * The demon card (ADR-0016, spec §2), a tarot card with two faces. Front: medallion, name,
 * title, the info band and the badges; [onFixedHeight] reports its height, which the Grimoire
 * keeps for both faces. Back: the dossier list. The ⟲ button in the header flips it and glows
 * while the dossier has unread text ([glowFlip]).
 */
@Composable
internal fun DemonCard(
    demons: List<Demon>,
    demon: Demon,
    record: DemonRecord,
    side: CardSide,
    onFlip: () -> Unit,
    glowFlip: Boolean,
    badge: BadgeKey?,
    onBadge: (BadgeKey?) -> Unit,
    inkCodex: Boolean,
    inkBattle: Boolean,
    scrollToSecret: Boolean,
    onFixedHeight: (Int) -> Unit,
    onBadgeRowBounds: (Rect) -> Unit,
    modifier: Modifier,
) {
    val spacing = MaterialTheme.spacing
    FlipCard(
        side = side,
        modifier = modifier,
        front = {
            CardFace {
                Column(Modifier.onSizeChanged { onFixedHeight(it.height) }) {
                    CardHeader(demon, record.bound, side, onFlip, glowFlip, Modifier.padding(horizontal = spacing.lg))
                    InfoBand(demons, demon, badge, Modifier.padding(horizontal = spacing.lg).padding(top = spacing.lg))
                    BadgeRow(
                        demon = demon,
                        selected = badge,
                        onSelect = onBadge,
                        modifier = Modifier
                            .padding(horizontal = spacing.lg)
                            .padding(top = spacing.lg)
                            .onGloballyPositioned { onBadgeRowBounds(it.boundsInRoot()) },
                    )
                }
            }
        },
        back = {
            CardFace {
                CardHeader(demon, record.bound, side, onFlip, glowFlip, Modifier.padding(horizontal = spacing.lg))
                DossierList(
                    demon = demon,
                    record = record,
                    inkCodex = inkCodex,
                    inkBattle = inkBattle,
                    scrollToSecret = scrollToSecret,
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(top = CarouselTopPadding).padding(horizontal = spacing.lg),
                )
            }
        },
    )
}

/** One face: the card's surface, border and insets; [content] is laid out in a column. */
@Composable
private fun CardFace(content: @Composable ColumnScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    val spacing = MaterialTheme.spacing
    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = MaterialTheme.shapes.large.copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp)),
        color = colors.surfaceContainerLow.copy(alpha = CARD_ALPHA),
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Column(Modifier.navigationBarsPadding().padding(top = spacing.lg, bottom = spacing.md), content = content)
    }
}

/** Tarot-style header: number medallion in the House metal, name, title, and the flip button. */
@Composable
private fun CardHeader(demon: Demon, bound: Boolean, side: CardSide, onFlip: () -> Unit, glowFlip: Boolean, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val metal = MaterialTheme.demonColors.metal(demon.house)
    val accent = MaterialTheme.demonColors.accent(demon.accent)
    val roles = MaterialTheme.textRoles
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(MedallionSize)
                .background(colors.surfaceContainerLow, CircleShape)
                .clip(CircleShape)
                .background(metal.copy(alpha = METAL_RING_ALPHA), CircleShape)
                .padding(1.dp)
                .background(colors.surfaceContainerLow, CircleShape)
                .padding(2.dp)
                .background(metal, CircleShape)
                .padding(1.5.dp)
                .background(colors.surfaceContainerLow, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(demon.number.toString(), style = roles.name.copy(fontSize = NumberSize), color = metal)
        }
        Column(Modifier.weight(1f)) {
            Text(
                demon.name,
                style = roles.name.copy(fontSize = NameSize),
                color = if (bound) accent else colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(demon.title),
                style = roles.body.copy(fontSize = TitleSize),
                color = colors.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        val flipLabel = stringResource(if (side == CardSide.Front) R.string.grimoire_show_dossier else R.string.grimoire_show_profile)
        val attention = rememberAttention(glowFlip)
        IconButton(onClick = onFlip, modifier = Modifier.attentionGlow(attention, accent)) {
            Icon(painterResource(R.drawable.ic_flip), contentDescription = flipLabel, tint = if (glowFlip) accent else colors.onSurfaceVariant)
        }
    }
}
