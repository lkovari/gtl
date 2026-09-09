package com.lkovari.mobile.apps.gtl.engine

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.min

object SpeedAdaptiveSpacing {
    const val CurveDegrees = 15f
    private const val MinHeadingDisplacementMeters = 0.05

    fun spacingMeters(speedMps: Float, inCurve: Boolean): Float {
        val kmh = speedMps * 3.6
        val base = when {
            kmh <= 0.0 -> 2f
            kmh <= 5.0 -> 4f
            kmh <= 10.0 -> 10f
            kmh <= 25.0 -> 20f
            kmh <= 50.0 -> 36f
            kmh <= 75.0 -> 48f
            kmh <= 100.0 -> 70f
            kmh <= 150.0 -> 98f
            kmh <= 200.0 -> 124f
            kmh <= 250.0 -> 152f
            kmh <= 300.0 -> 194f
            kmh <= 400.0 -> 250f
            kmh <= 500.0 -> 348f
            kmh <= 750.0 -> 243f
            kmh <= 1000.0 -> 556f
            kmh <= 1500.0 -> 695f
            else -> 834f
        }
        return if (inCurve) (base / 2f).coerceAtLeast(1f) else base
    }

    fun spacingMeters(speedMps: Float, inCurve: Boolean, usage: UsageType?): Float {
        val spaced = spacingMeters(speedMps, inCurve)
        if (usage == null || !usage.isPedestrianMode()) {
            return spaced
        }
        return (spaced / 2f).coerceAtLeast(1f)
    }

    fun headingDegrees(
        fromLat: Double,
        fromLon: Double,
        toLat: Double,
        toLon: Double
    ): Float? {
        val en = GeoProjection.eastNorth(fromLat, fromLon, toLat, toLon)
        if (GeoProjection.hypot(en.first, en.second) < MinHeadingDisplacementMeters) {
            return null
        }
        val deg = Math.toDegrees(atan2(en.first, en.second))
        return ((deg + 360.0) % 360.0).toFloat()
    }

    fun headingChangeIsCurve(previousHeading: Float, currentHeading: Float): Boolean {
        val raw = abs(currentHeading - previousHeading)
        val diff = min(raw, 360f - raw)
        return diff > CurveDegrees
    }

    fun isInCurve(previousBearing: Float, currentBearing: Float): Boolean {
        if (previousBearing == 0f || currentBearing == 0f) {
            return false
        }
        return headingChangeIsCurve(previousBearing, currentBearing)
    }

    fun isInCurve(previous: TrackFix, current: TrackFix): Boolean {
        if (isInCurve(previous.bearing, current.bearing)) {
            return true
        }
        if (previous.bearing != 0f && current.bearing != 0f) {
            return false
        }
        val displacement = headingDegrees(
            previous.latitude,
            previous.longitude,
            current.latitude,
            current.longitude
        )
        if (displacement == null) {
            return false
        }
        if (previous.bearing != 0f) {
            return headingChangeIsCurve(previous.bearing, displacement)
        }
        if (current.bearing != 0f) {
            return headingChangeIsCurve(displacement, current.bearing)
        }
        return false
    }

    fun isInCurve(first: TrackFix, second: TrackFix, third: TrackFix): Boolean {
        if (isInCurve(second, third)) {
            return true
        }
        val firstLeg = headingDegrees(
            first.latitude,
            first.longitude,
            second.latitude,
            second.longitude
        )
        val secondLeg = headingDegrees(
            second.latitude,
            second.longitude,
            third.latitude,
            third.longitude
        )
        if (firstLeg == null || secondLeg == null) {
            return false
        }
        return headingChangeIsCurve(firstLeg, secondLeg)
    }
}
