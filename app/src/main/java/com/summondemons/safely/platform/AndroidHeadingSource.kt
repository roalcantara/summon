package com.summondemons.safely.platform

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.summondemons.safely.ritual.AzimuthSmoother
import com.summondemons.safely.ritual.HeadingSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Rotation-vector azimuth, falling back to accelerometer + magnetometer.
 * Emits nothing if the device has neither (the UI then shows "--°").
 * Assumes portrait (the activity is locked to it), so no axis remapping.
 */
class AndroidHeadingSource(context: Context) : HeadingSource {

    private val sensorManager: SensorManager? = context.getSystemService(SensorManager::class.java)

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
        val rotationMatrix = FloatArray(9)
        val orientation = FloatArray(3)
        var gravity: FloatArray? = null
        var geomagnetic: FloatArray? = null

        fun emitFromMatrix() {
            SensorManager.getOrientation(rotationMatrix, orientation)
            val degrees = Math.toDegrees(orientation[0].toDouble()).toFloat()
            trySend(smoother.next(degrees))
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
