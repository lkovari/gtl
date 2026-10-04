package com.lkovari.mobile.apps.gtl.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.lkovari.mobile.apps.gtl.engine.HeadingYawRate
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class AmbientTemperatureSource(context: Context) {
    private val sensorManager =
        context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)

    val isAvailable: Boolean get() = sensor != null

    fun temperatures(): Flow<Float> = callbackFlow {
        val current = sensor
        if (current == null) {
            close()
            return@callbackFlow
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.values.isNotEmpty()) {
                    trySend(event.values[0])
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            }
        }
        sensorManager.registerListener(listener, current, SensorManager.SENSOR_DELAY_NORMAL)
        awaitClose { sensorManager.unregisterListener(listener) }
    }
}

class AccelerometerSource(context: Context) {
    private val sensorManager =
        context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    fun accelerations(): Flow<FloatArray> = callbackFlow {
        val current = sensor
        if (current == null) {
            close()
            return@callbackFlow
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.values.size >= 3) {
                    trySend(floatArrayOf(event.values[0], event.values[1], event.values[2]))
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            }
        }
        sensorManager.registerListener(listener, current, SensorManager.SENSOR_DELAY_UI)
        awaitClose { sensorManager.unregisterListener(listener) }
    }
}

class PressureSource(context: Context) {
    private val sensorManager =
        context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

    val isAvailable: Boolean get() = sensor != null

    fun pressures(): Flow<Float> = callbackFlow {
        val current = sensor
        if (current == null) {
            close()
            return@callbackFlow
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.values.isNotEmpty()) {
                    trySend(event.values[0])
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            }
        }
        sensorManager.registerListener(listener, current, SensorManager.SENSOR_DELAY_NORMAL)
        awaitClose { sensorManager.unregisterListener(listener) }
    }
}

data class CompassSample(
    val azimuthDegrees: Float,
    val accuracy: Int,
    val yawRateRadPerSec: Float? = null
)

class CompassSource(context: Context) {
    private val sensorManager =
        context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotation: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    fun samples(): Flow<CompassSample> = callbackFlow {
        val current = rotation
        if (current == null) {
            close()
            return@callbackFlow
        }
        val rotationMatrix = FloatArray(9)
        val orientation = FloatArray(3)
        val yawRate = HeadingYawRate()
        var lastAzimuth = 0f
        var lastYawRate: Float? = null
        var lastAccuracy = SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientation)
                val azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                lastAzimuth = (azimuth + 360f) % 360f
                lastYawRate = yawRate.sample(lastAzimuth, event.timestamp / 1_000_000L)
                trySend(CompassSample(lastAzimuth, lastAccuracy, lastYawRate))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                lastAccuracy = accuracy
                trySend(CompassSample(lastAzimuth, lastAccuracy, lastYawRate))
            }
        }
        sensorManager.registerListener(listener, current, SensorManager.SENSOR_DELAY_UI)
        awaitClose { sensorManager.unregisterListener(listener) }
    }
}
