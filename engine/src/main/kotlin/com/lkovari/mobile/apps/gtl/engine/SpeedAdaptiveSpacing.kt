package com.lkovari.mobile.apps.gtl.engine

import kotlin.math.abs
import kotlin.math.min

object SpeedAdaptiveSpacing {
    const val CurveDegrees = 15f

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

    fun isInCurve(previousBearing: Float, currentBearing: Float): Boolean {
        if (previousBearing == 0f || currentBearing == 0f) {
            return false
        }
        val raw = abs(currentBearing - previousBearing)
        val diff = min(raw, 360f - raw)
        return diff > CurveDegrees
    }
}
