package com.lkovari.mobile.apps.gtl.engine

import kotlin.math.abs

object GpsAltitude {
    const val MinPlausibleMeters = -430.0
    const val MaxPlausibleMeters = 20000.0
    const val MaxGnssSkewNanos = 2_000_000_000L

    fun isPlausible(meters: Double): Boolean {
        return meters.isFinite() && meters >= MinPlausibleMeters && meters <= MaxPlausibleMeters
    }

    fun gnssAltitudeIsFresh(primaryElapsedRealtimeNanos: Long, gnssElapsedRealtimeNanos: Long): Boolean {
        return abs(primaryElapsedRealtimeNanos - gnssElapsedRealtimeNanos) <= MaxGnssSkewNanos
    }

    fun toMsl(
        latitude: Double,
        longitude: Double,
        gnssMsl: Double?,
        fusedMsl: Double?,
        gnssEllipsoid: Double?,
        fusedEllipsoid: Double?,
        undulationMeters: Double = Egm2008Geoid.undulationMeters(latitude, longitude)
    ): Double? {
        val msl = listOf(gnssMsl, fusedMsl).firstOrNull { value ->
            value != null && isPlausible(value)
        }
        if (msl != null) {
            return msl
        }
        return listOf(gnssEllipsoid, fusedEllipsoid).firstNotNullOfOrNull { value ->
            if (value == null || !value.isFinite() || !undulationMeters.isFinite()) {
                null
            } else {
                (value - undulationMeters).takeIf { converted -> isPlausible(converted) }
            }
        }
    }
}
