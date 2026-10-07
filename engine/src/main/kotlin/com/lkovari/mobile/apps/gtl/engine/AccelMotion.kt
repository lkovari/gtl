package com.lkovari.mobile.apps.gtl.engine

data class AccelMotionState(
    val meanMagnitude: Double = 0.0,
    val variance: Double = 0.0,
    val samples: Int = 0,
    val lastSampleNanos: Long? = null
)

/**
 * Tells a carried, walking or riding phone apart from one lying still, using the
 * spread of the accelerometer magnitude. GNSS Doppler noise indoors can look like
 * walking speed, but a phone at rest has almost no acceleration spread.
 */
object AccelMotion {
    const val Smoothing = 0.1
    const val MinSamples = 10
    const val MovingStdDevMps2 = 0.6
    const val MaxSampleAgeNanos = 2_000_000_000L

    fun observe(state: AccelMotionState, x: Float, y: Float, z: Float, nanos: Long): AccelMotionState {
        val magnitude = kotlin.math.sqrt((x * x + y * y + z * z).toDouble())
        if (!magnitude.isFinite()) {
            return state
        }
        if (state.samples == 0) {
            return AccelMotionState(magnitude, 0.0, 1, nanos)
        }
        val delta = magnitude - state.meanMagnitude
        val mean = state.meanMagnitude + Smoothing * delta
        val variance = (1.0 - Smoothing) * (state.variance + Smoothing * delta * delta)
        return AccelMotionState(mean, variance, state.samples + 1, nanos)
    }

    fun isMoving(state: AccelMotionState, nowNanos: Long): Boolean? {
        val last = state.lastSampleNanos ?: return null
        if (state.samples < MinSamples || nowNanos - last > MaxSampleAgeNanos) {
            return null
        }
        return kotlin.math.sqrt(state.variance) >= MovingStdDevMps2
    }
}
