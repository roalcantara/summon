package com.summondemons.safely.platform

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.os.SystemClock
import android.view.Display
import com.summondemons.safely.ritual.AzimuthSmoother
import com.summondemons.safely.ritual.HeadingSource
import com.summondemons.safely.ritual.RotationGrace
import com.summondemons.safely.ritual.azimuthFromRotationMatrix
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Rotation-vector azimuth, falling back to accelerometer + magnetometer.
 * Emits nothing if the device has neither (the UI then shows "--°").
 * The heading is where the phone's back points when held upright, or its top when
 * held flat, whichever way the screen is rotated (see [azimuthFromRotationMatrix]; the chant
 * screen is landscape, ADR-0019).
 */
class AndroidHeadingSource(context: Context) : HeadingSource {

    private val sensorManager: SensorManager? = context.getSystemService(SensorManager::class.java)
    private val displayManager: DisplayManager? = context.getSystemService(DisplayManager::class.java)

    /** Current display rotation in quarter turns (`Surface.ROTATION_*`), read per reading. */
    private fun screenRotation(): Int = displayManager?.getDisplay(Display.DEFAULT_DISPLAY)?.rotation ?: 0

    override val available: Boolean = sensorManager?.let { manager ->
        manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null ||
            (manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null &&
                manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null)
    } ?: false

    override fun azimuths(): Flow<Float> = callbackFlow {
        val manager = sensorManager
        if (manager == null) {
            awaitClose()
            return@callbackFlow
        }
        val rotationVector = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val smoother = AzimuthSmoother()
        val grace = RotationGrace()
        val rotationMatrix = FloatArray(9)
        var gravity: FloatArray? = null
        var geomagnetic: FloatArray? = null

        fun emitFromMatrix() {
            val rotation = screenRotation()
            val azimuth = smoother.next(azimuthFromRotationMatrix(rotationMatrix, rotation))
            trySend(grace.next(SystemClock.elapsedRealtime(), rotation, azimuth))
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                        emitFromMatrix()
                    }
                    Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_MAGNETIC_FIELD -> {
                        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                            gravity = event.values.clone()
                        } else {
                            geomagnetic = event.values.clone()
                        }
                        val g = gravity
                        val m = geomagnetic
                        if (g != null && m != null && SensorManager.getRotationMatrix(rotationMatrix, null, g, m)) {
                            emitFromMatrix()
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        if (rotationVector != null) {
            manager.registerListener(listener, rotationVector, SensorManager.SENSOR_DELAY_UI)
        } else if (accelerometer != null && magnetometer != null) {
            manager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
            manager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_UI)
        }

        awaitClose { manager.unregisterListener(listener) }
    }
}
