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
                painter = painterResource(demon.cutoutArt),
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
