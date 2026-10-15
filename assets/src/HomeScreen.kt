package com.example.summondemonssafely.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.summondemonssafely.ui.components.DemonCardBase
import com.example.summondemonssafely.ui.components.PrimaryActionButton
import com.example.summondemonssafely.ui.theme.*
import com.example.summondemonssafely.viewmodel.Demon
import com.example.summondemonssafely.viewmodel.DemonViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: DemonViewModel,
    onViewDemon: (String) -> Unit,
    onSummonDemon: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val demons by viewModel.demons.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("📖", fontSize = 18.sp)
                            }
                        }
                        Text(
                            text = "My Grimoire",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                ),
                actions = {
                    // Currency / Summoner Level Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("🛡️", fontSize = 14.sp)
                            Text(
                                "LV. 65",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Subtitle info
            Text(
                text = "Summon ancient Goetic spirits safely with compass alignment & Latin chanting.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // The 2 playable demons Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(demons, key = { it.id }) { demon ->
                    DemonGridItem(
                        demon = demon,
                        onView = { onViewDemon(demon.id) },
                        onSummon = { onSummonDemon(demon.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun DemonGridItem(
    demon: Demon,
    onView: () -> Unit,
    onSummon: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBackground = if (demon.isUnlocked) {
        MaterialTheme.colorScheme.surfaceContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.8f)
    }

    val cardBorder = if (demon.isUnlocked) {
        BorderStroke(1.5.dp, demon.themeColor.copy(alpha = 0.6f))
    } else {
        BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f))
    }

    DemonCardBase(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = cardBackground,
        border = cardBorder
    ) {
        // Star Rating & Legion Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (demon.isUnlocked) "★".repeat(demon.stars) else "???",
                color = AmberGoldPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Text(
                    text = "${demon.legions}L",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Cute Demon Illustration (or locked silhouette)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (demon.isUnlocked)
                        Brush.radialGradient(listOf(demon.themeColor.copy(alpha = 0.25f), Color.Transparent))
                    else
                        Brush.radialGradient(listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent))
                ),
            contentAlignment = Alignment.Center
        ) {
            if (demon.isUnlocked) {
                // Placeholder chibi demon emoji / vector graphic
                Column(horizontalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (demon.id == "asmodeus") "👑" else "🐉",
                        fontSize = 54.sp
                    )
                    Text(
                        text = if (demon.id == "asmodeus") "🔥 Asmodeus" else "🔮 Astaroth",
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                        color = demon.themeColor
                    )
                }
            } else {
                // Locked Silhouette
                Column(horizontalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🔒",
                        fontSize = 44.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "Awaiting Summon",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 10.sp),
                        color = Color.Gray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Name & Title
        Text(
            text = demon.name,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = if (demon.isUnlocked) demon.themeColor else Color.Gray
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = if (demon.isUnlocked) demon.role else "Unknown Entity",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Action Button: "View" if unlocked, "Summon" if locked
        if (demon.isUnlocked) {
            PrimaryActionButton(
                text = "View",
                onClick = onView,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth().height(44.dp)
            )
        } else {
            PrimaryActionButton(
                text = "Summon",
                onClick = onSummon,
                containerColor = AmberGoldContainer,
                contentColor = OnAmberGold,
                modifier = Modifier.fillMaxWidth().height(44.dp)
            )
        }
    }
}