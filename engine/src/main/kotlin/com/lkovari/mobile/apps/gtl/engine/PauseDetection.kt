package com.lkovari.mobile.apps.gtl.engine

/**
 * A GNSS chip ramps its speed up over several seconds after Start, and a single slow
 * fix mid-walk is just noise, so a PAUSE needs a run of slow fixes outside the start window.
 */
object PauseDetection {
    const val MinSlowFixes = 3
    const val StartGraceMillis = 5_000L

    fun slowStreak(previous: Int, speedMps: Float?, usage: UsageType): Int {
        if (speedMps == null || !speedMps.isFinite()) {
            return previous
        }
        return if (speedMps < usage.pauseSpeedMps()) previous + 1 else 0
    }

    fun isPause(slowStreak: Int, millisSinceStart: Long?): Boolean {
        if (millisSinceStart != null && millisSinceStart < StartGraceMillis) {
            return false
        }
        return slowStreak >= MinSlowFixes
    }
}
