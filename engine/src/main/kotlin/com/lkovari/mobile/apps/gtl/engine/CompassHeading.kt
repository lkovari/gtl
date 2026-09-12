package com.lkovari.mobile.apps.gtl.engine

data class CompassDisplay(
    val degrees: Float,
    val trueNorth: Boolean,
    val missingFix: Boolean
)

object CompassHeading {
    fun wrapDegrees(degrees: Float): Float {
        return ((degrees % 360f) + 360f) % 360f
    }

    fun display(
        magneticDegrees: Float,
        wantTrue: Boolean,
        declinationDegrees: Float?
    ): CompassDisplay {
        val magnetic = wrapDegrees(magneticDegrees)
        if (!wantTrue) {
            return CompassDisplay(magnetic, trueNorth = false, missingFix = false)
        }
        if (declinationDegrees == null || !declinationDegrees.isFinite()) {
            return CompassDisplay(magnetic, trueNorth = false, missingFix = true)
        }
        return CompassDisplay(
            wrapDegrees(magnetic + declinationDegrees),
            trueNorth = true,
            missingFix = false
        )
    }

    fun needsFigureEight(sensorAccuracy: Int): Boolean {
        return sensorAccuracy <= SensorAccuracyLow
    }

    private const val SensorAccuracyLow = 1
}
