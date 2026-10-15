package com.summondemons.safely.ui.fx

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val MaxParallax = 12.dp
private const val SMOOTHING = 0.15f
/** How fast the resting pose follows the current pose, so a new grip re-centres. */
private const val RECENTER = 0.01f

/**
 * Small offset that follows device tilt (TYPE_GAME_ROTATION_VECTOR). Zero when motion is
 * reduced or the sensor is missing. Read `.value` inside `graphicsLayer` to avoid recomposition.
 */
@Composable
fun rememberParallax(maxOffset: Dp = MaxParallax): State<Offset> {
    val offset = remember { mutableStateOf(Offset.Zero) }
    val reduceMotion = LocalReduceMotion.current
    val context = LocalContext.current
    val maxPx = with(LocalDensity.current) { maxOffset.toPx() }
    DisposableEffect(reduceMotion, maxPx) {
        val manager = context.getSystemService(SensorManager::class.java)
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        if (reduceMotion || manager == null || sensor == null) {
            offset.value = Offset.Zero
            return@DisposableEffect onDispose { }
        }
        val matrix = FloatArray(9)
        val angles = FloatArray(3)
        var rest: FloatArray? = null
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(matrix, event.values)
                SensorManager.getOrientation(matrix, angles)
                val base = rest ?: angles.copyOf().also { rest = it }
                val target = parallaxOffset(angles[1] - base[1], angles[2] - base[2], maxPx)
                offset.value = lerp(offset.value, target, SMOOTHING)
                for (i in base.indices) base[i] += (angles[i] - base[i]) * RECENTER
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { manager.unregisterListener(listener) }
    }
    return offset
}
