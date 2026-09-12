package com.lkovari.mobile.apps.gtl.service

import android.location.Location
import com.lkovari.mobile.apps.gtl.engine.GnssSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LiveTrackingState(
    val logging: Boolean = false,
    val sessionId: Long? = null,
    val lastLocation: Location? = null,
    val gnss: GnssSnapshot? = null,
    val temperatureCelsius: Float? = null,
    val temperatureAvailable: Boolean = false,
    val accel: FloatArray? = null,
    val leanAngle: Float? = null,
    val azimuthDegrees: Float? = null,
    val compassAccuracy: Int = 2,
    val provider: String? = null,
    val pressureHpa: Float? = null,
    val baroAltitude: Double? = null,
    val pressureAvailable: Boolean = false
)

class TrackingStateHolder {
    private val mutableState = MutableStateFlow(LiveTrackingState())
    val state: StateFlow<LiveTrackingState> = mutableState.asStateFlow()

    fun update(transform: (LiveTrackingState) -> LiveTrackingState) {
        mutableState.value = transform(mutableState.value)
    }
}
