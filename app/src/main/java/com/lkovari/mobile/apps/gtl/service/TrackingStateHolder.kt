package com.lkovari.mobile.apps.gtl.service

import android.location.Location
import android.os.SystemClock
import com.lkovari.mobile.apps.gtl.data.location.toDisplaySpeedFix
import com.lkovari.mobile.apps.gtl.data.sensor.AndroidBaroAltitude
import com.lkovari.mobile.apps.gtl.engine.BaroAltitude
import com.lkovari.mobile.apps.gtl.engine.AccelMotion
import com.lkovari.mobile.apps.gtl.engine.AccelMotionState
import com.lkovari.mobile.apps.gtl.engine.DisplaySpeed
import com.lkovari.mobile.apps.gtl.engine.DisplaySpeedState
import com.lkovari.mobile.apps.gtl.engine.GnssSnapshot
import com.lkovari.mobile.apps.gtl.engine.UsageType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LiveTrackingState(
    val logging: Boolean = false,
    val sessionId: Long? = null,
    val acceptedFixCount: Int = 0,
    val rejectedFixCount: Int = 0,
    val poorGps: Boolean = false,
    val loggingError: Boolean = false,
    val gpsOff: Boolean = false,
    val lastLocation: Location? = null,
    val displaySpeed: DisplaySpeedState = DisplaySpeedState(),
    val displaySpeedMps: Float? = null,
    val gnss: GnssSnapshot? = null,
    val temperatureCelsius: Float? = null,
    val temperatureAvailable: Boolean = false,
    val accel: FloatArray? = null,
    val accelMotion: AccelMotionState = AccelMotionState(),
    val leanAngle: Float? = null,
    val yawRateRadPerSec: Float? = null,
    val azimuthDegrees: Float? = null,
    val compassAccuracy: Int = 2,
    val provider: String? = null,
    val pressureHpa: Float? = null,
    val rawPressureHpa: Float? = null,
    val baroAltitude: Double? = null,
    val pressureAvailable: Boolean = false
)

class TrackingStateHolder {
    private val mutableState = MutableStateFlow(LiveTrackingState())
    val state: StateFlow<LiveTrackingState> = mutableState.asStateFlow()

    fun update(transform: (LiveTrackingState) -> LiveTrackingState) {
        mutableState.value = transform(mutableState.value)
    }

    fun acceptPressure(rawHpa: Float?, qnhHpa: Float, offsetHpa: Float) {
        update { current ->
            val gpsMeters = current.lastLocation?.takeIf { it.hasAltitude() }?.altitude
            val trusted = rawHpa?.takeIf { BaroAltitude.matchesGps(it, gpsMeters, qnhHpa, offsetHpa) }
            current.copy(
                rawPressureHpa = rawHpa,
                pressureHpa = trusted,
                baroAltitude = trusted?.let { AndroidBaroAltitude.metersFromPressureHpa(it, qnhHpa, offsetHpa) },
                pressureAvailable = true
            )
        }
    }

    fun acceptAcceleration(value: FloatArray, nanos: Long) {
        if (value.size < 3) {
            return
        }
        update { current ->
            current.copy(
                accel = value,
                accelMotion = AccelMotion.observe(current.accelMotion, value[0], value[1], value[2], nanos)
            )
        }
    }

    fun acceptFix(
        location: Location,
        usage: UsageType,
        transform: (LiveTrackingState) -> LiveTrackingState
    ) {
        update { current ->
            val moving = AccelMotion.isMoving(current.accelMotion, SystemClock.elapsedRealtimeNanos())
            val decision = DisplaySpeed.apply(
                current.displaySpeed,
                location.toDisplaySpeedFix().copy(deviceMoving = moving),
                DisplaySpeed.floorMps(usage)
            )
            transform(current).copy(
                lastLocation = location,
                displaySpeed = decision.state,
                displaySpeedMps = decision.metersPerSecond
            )
        }
    }
}
