package com.example.summondemonssafely.ui.screens

import android.media.MediaPlayer
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.summondemonssafely.ui.components.PrimaryActionButton
import com.example.summondemonssafely.ui.components.RibbonBanner
import com.example.summondemonssafely.ui.components.RotatingSunburstBackground
import com.example.summondemonssafely.ui.theme.*
import com.example.summondemonssafely.viewmodel.DemonViewModel

@Composable
fun DemonProfileScreen(
    demonId: String,
    isNewSummon: Boolean,
    viewModel: DemonViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val demon = remember(demonId) { viewModel.getDemonById(demonId) } ?: return
    val context = LocalContext.current

    // Trigger placeholder "Tada!" sound effect on summon success
    LaunchedEffect(isNewSummon) {
        if (isNewSummon) {
            try {
                // In production: MediaPlayer.create(context, R.raw.tada_fanfare)?.start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Bouncing demon character entrance animation
    val bounceAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        bounceAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Rotating shiny sunburst effect behind character
            RotatingSunburstBackground(
                modifier = Modifier.fillMaxSize(),
                beamCount = 18,
                color = demon.themeColor.copy(alpha = 0.16f)
            )

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // The Banner: Displayed if isNewSummon is true
                if (isNewSummon) {
                    RibbonBanner(
                        text = "New Demon Received!",
                        bannerColor = Color(0xFFDC2626)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Character Art Showcase with Bouncing Scale
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .graphicsLayer {
                            scaleX = bounceAnim.value
                            scaleY = bounceAnim.value
                        }
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (demon.id == "asmodeus") "👑" else "🐉",
                        fontSize = 84.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Character Name & Title in Largest Header Typography
                Text(
                    text = demon.name,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = demon.themeColor
                    ),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = demon.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        color = AmberGoldPrimary
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Lore Quote Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceContainerLowest)
                ) {
                    Text(
                        text = "“${demon.quote}”",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Description in clean, readable sans-serif
                Text(
                    text = demon.description,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // RPG Tactical Stats Grid
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceContainerHigh),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "COMBAT POWER",
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "${demon.power}",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = 15.sp,
                                    color = AmberGoldPrimary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        StatBar("Damage", demon.stats.damage, 10000, AmberGoldPrimary)
                        StatBar("Defense", demon.stats.defense, 10000, ArcaneCyanSecondary)
                        StatBar("Arcane Mastery", demon.stats.arcaneMastery, 12000, demon.themeColor)
                        StatBar("Healing", demon.stats.healing, 5000, RitualSafeGreen)
                    }
                }

                Spacer(modifier = Modifier.height(80.dp)) // Space for pinned bottom button
            }

            // Pinned Bottom PrimaryActionButton
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                PrimaryActionButton(
                    text = if (isNewSummon) "Awesome!" else "Back",
                    onClick = onNavigateBack,
                    containerColor = if (isNewSummon) AmberGoldContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = if (isNewSummon) OnAmberGold else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun StatBar(
    label: String,
    value: Int,
    maxVal: Int,
    color: Color
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp))
            Text(
                value.toString(),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp, color = color)
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        LinearProgressIndicator(
            progress = { (value.toFloat() / maxVal).coerceIn(0f, 1f) },
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
        )
    }
}