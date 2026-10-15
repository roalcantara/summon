package com.example.summondemonssafely.ui.screens

import android.Manifest
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.summondemonssafely.ui.components.PrimaryActionButton
import com.example.summondemonssafely.ui.components.SigilAltarCanvas
import com.example.summondemonssafely.ui.theme.*
import com.example.summondemonssafely.viewmodel.Demon
import com.example.summondemonssafely.viewmodel.DemonViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

// Haptic feedback helper mimicking Android HapticFeedbackConstants.CONFIRM
fun triggerHapticSuccess(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        val vibrator = vibratorManager?.defaultVibrator
        // Success Pattern: 80ms pulse, 60ms gap, 140ms resonant burst
        val timings = longArrayOf(0, 80, 60, 140)
        val amplitudes = intArrayOf(0, 180, 0, 255)
        vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
    } else {
        @Suppress("DEPRECATION")
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 80, 60, 140), -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 80, 60, 140), -1)
        }
    }
}

@Composable
fun RitualScreen(
    demonId: String,
    viewModel: DemonViewModel,
    onSummonSuccess: (String) -> Unit,
    onAbort: () -> Unit,
    modifier: Modifier = Modifier
) {
    val demon = remember(demonId) { viewModel.getDemonById(demonId) } ?: return
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // ==========================================
    // 1. Compass / Sensor Orientation State
    // ==========================================
    var azimuthDegrees by remember { mutableStateOf(0f) }
    var isNorthOverride by remember { mutableStateOf(false) }

    val isFacingNorth = remember(azimuthDegrees, isNorthOverride) {
        if (isNorthOverride) true
        else (azimuthDegrees in 340f..360f) || (azimuthDegrees in 0f..20f)
    }

    // ==========================================
    // 2. Chanting & Mic Threshold State
    // ==========================================
    var currentPhraseIndex by remember { mutableStateOf(0) }
    var sustainedChantMs by remember { mutableStateOf(0L) }
    var sigilOpacity by remember { mutableStateOf(0.15f) }
    var hasAudioPermission by remember { mutableStateOf(false) }
    var isChantingSimulated by remember { mutableStateOf(false) }

    // Request RECORD_AUDIO Permission
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    // SensorManager Orientation Listener (Accelerometer + Magnetometer)
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        var gravity: FloatArray? = null
        var geomagnetic: FloatArray? = null

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    gravity = event.values.clone()
                } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    geomagnetic = event.values.clone()
                }

                if (gravity != null && geomagnetic != null) {
                    val rMatrix = FloatArray(9)
                    val iMatrix = FloatArray(9)
                    if (SensorManager.getRotationMatrix(rMatrix, iMatrix, gravity, geomagnetic)) {
                        val orientation = FloatArray(3)
                        SensorManager.getOrientation(rMatrix, orientation)
                        val azimuthRad = orientation[0]
                        val azDeg = ((Math.toDegrees(azimuthRad.toDouble()) + 360.0) % 360.0).toFloat()
                        azimuthDegrees = azDeg
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_UI)

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    // Real-Time AudioRecord Coroutine (Volume Threshold: >2000 sustained for 2 sec)
    LaunchedEffect(hasAudioPermission, isFacingNorth, isChantingSimulated) {
        if (!hasAudioPermission && !isChantingSimulated) return@LaunchedEffect

        withContext(Dispatchers.Default) {
            val sampleRate = 44100
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val audioBuffer = ShortArray(bufferSize)

            var recorder: AudioRecord? = null
            try {
                if (hasAudioPermission) {
                    recorder = AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        sampleRate,
                        channelConfig,
                        audioFormat,
                        bufferSize
                    )
                    recorder.startRecording()
                }

                while (isActive) {
                    var isVoiceAboveThreshold = isChantingSimulated
                    if (recorder != null && recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                        val readSize = recorder.read(audioBuffer, 0, bufferSize)
                        if (readSize > 0) {
                            var sum = 0.0
                            for (i in 0 until readSize) {
                                sum += audioBuffer[i] * audioBuffer[i]
                            }
                            val rms = sqrt(sum / readSize)
                            if (rms > 1800.0) { // Vocal threshold
                                isVoiceAboveThreshold = true
                            }
                        }
                    }

                    // Mechanic Rule 4: If device drops away from North, reset progress to 0!
                    if (!isFacingNorth) {
                        sustainedChantMs = 0L
                        sigilOpacity = 0.15f
                        currentPhraseIndex = 0
                    } else if (isVoiceAboveThreshold) {
                        sustainedChantMs += 100L
                        if (sustainedChantMs >= 2000L) {
                            // Advance phrase and increase sigil alpha by 25%
                            sustainedChantMs = 0L
                            val nextIndex = currentPhraseIndex + 1
                            sigilOpacity = (sigilOpacity + 0.25f).coerceAtMost(1.0f)
                            if (nextIndex >= demon.chantPhrases.size || sigilOpacity >= 0.99f) {
                                // Chant Complete! Trigger Android Haptic Feedback Success pattern (HapticFeedbackConstants.CONFIRM)
                                triggerHapticSuccess(context)
                                viewModel.unlockDemon(demon.id)
                                withContext(Dispatchers.Main) {
                                    onSummonSuccess(demon.id)
                                }
                                break
                            } else {
                                currentPhraseIndex = nextIndex
                            }
                        }
                    } else {
                        // Decay sustained ms if silence
                        sustainedChantMs = (sustainedChantMs - 50L).coerceAtLeast(0L)
                    }

                    kotlinx.coroutines.delay(100L)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                recorder?.release()
            }
        }
    }

    // Animated colors and values
    val animatedSigilAlpha by animateFloatAsState(targetValue = sigilOpacity, label = "sigilAlpha")
    val northStatusColor by animateColorAsState(
        targetValue = if (isFacingNorth) RitualSafeGreen else MaterialTheme.colorScheme.error,
        label = "northColor"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterVertically,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // TOP HUD: Target Demon Info & Compass Lock
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, demon.themeColor.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "SUMMONING RITUAL",
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                        color = demon.themeColor
                    )
                    Text(
                        text = demon.name,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Hold phone with both hands and face North",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Compass Rose Widget
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerLowest),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "▲",
                            color = northStatusColor,
                            fontSize = 20.sp,
                            modifier = Modifier.rotate(azimuthDegrees)
                        )
                    }
                    Text(
                        text = "${azimuthDegrees.toInt()}° N",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = northStatusColor
                    )
                }
            }
        }

        // CENTER STAGE: The Glowing Sigil
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest),
            contentAlignment = Alignment.Center
        ) {
            SigilAltarCanvas(
                opacity = animatedSigilAlpha,
                themeColor = demon.themeColor,
                modifier = Modifier.fillMaxSize()
            )
        }

        // THE CHANTING PHASE: Latin Incantation & Karaoke Highlighting
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceContainerHigh),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SACRED CHANT",
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                        color = demon.themeColor
                    )
                    Text(
                        text = "RESONANCE: ${(sigilOpacity * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                        color = AmberGoldPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Latin chant phrases
                demon.chantPhrases.forEachIndexed { index, phrase ->
                    val isPast = index < currentPhraseIndex
                    val isCurrent = index == currentPhraseIndex
                    val phraseColor = when {
                        isPast -> demon.themeColor
                        isCurrent && isFacingNorth -> demon.themeColor
                        else -> Color.Gray.copy(alpha = 0.5f)
                    }

                    Text(
                        text = phrase,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 13.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = phraseColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 3.dp)
                    )
                }

                // Vocal sustained progress bar
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { (sustainedChantMs / 2000f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = demon.themeColor,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerLowest
                )
            }
        }

        // BOTTOM CONTROLS & TEST TOGGLES
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Emulation test controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { isNorthOverride = !isNorthOverride },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isNorthOverride) RitualSafeGreen else OnSurfaceVariant
                    )
                ) {
                    Text(if (isNorthOverride) "Lock: North ✓" else "Simulate North")
                }

                OutlinedButton(
                    onClick = { isChantingSimulated = !isChantingSimulated },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isChantingSimulated) AmberGoldPrimary else OnSurfaceVariant
                    )
                ) {
                    Text(if (isChantingSimulated) "Chanting..." else "Hold to Chant")
                }
            }

            PrimaryActionButton(
                text = "Abort Ritual",
                onClick = onAbort,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}