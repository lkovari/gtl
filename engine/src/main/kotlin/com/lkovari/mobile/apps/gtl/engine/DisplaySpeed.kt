package com.lkovari.mobile.apps.gtl.engine

data class DisplaySpeedFix(
    val speedMps: Float?,
    val speedAccuracyMps: Float?,
    val latitude: Double,
    val longitude: Double,
    val horizontalAccuracyMeters: Float?
)

enum class DisplayMotion {
    Idle,
    Moving
}

data class DisplaySpeedState(
    val motion: DisplayMotion = DisplayMotion.Idle,
    val streak: Int = 0,
    val heldMps: Float? = null,
    val previousLatitude: Double? = null,
    val previousLongitude: Double? = null
)

data class DisplaySpeedDecision(
    val state: DisplaySpeedState,
    val metersPerSecond: Float?
)

object DisplaySpeed {
    const val HYSTERESIS_SAMPLES = 2

    fun apply(state: DisplaySpeedState, fix: DisplaySpeedFix): DisplaySpeedDecision {
        val anchored = state.copy(
            previousLatitude = fix.latitude,
            previousLongitude = fix.longitude
        )
        val speed = fix.speedMps
        if (speed == null || !speed.isFinite()) {
            return DisplaySpeedDecision(anchored, null)
        }
        val significant = isSignificant(state, fix, speed)
        return when (state.motion) {
            DisplayMotion.Idle -> idleStep(anchored, speed, significant)
            DisplayMotion.Moving -> movingStep(state, anchored, speed, significant)
        }
    }

    private fun idleStep(
        anchored: DisplaySpeedState,
        speed: Float,
        significant: Boolean
    ): DisplaySpeedDecision {
        if (!significant) {
            return DisplaySpeedDecision(anchored.copy(streak = 0, heldMps = null), 0f)
        }
        val streak = anchored.streak + 1
        return if (streak >= HYSTERESIS_SAMPLES) {
            DisplaySpeedDecision(
                anchored.copy(motion = DisplayMotion.Moving, streak = 0, heldMps = speed),
                speed
            )
        } else {
            DisplaySpeedDecision(anchored.copy(streak = streak, heldMps = null), 0f)
        }
    }

    private fun movingStep(
        previous: DisplaySpeedState,
        anchored: DisplaySpeedState,
        speed: Float,
        significant: Boolean
    ): DisplaySpeedDecision {
        if (significant) {
            return DisplaySpeedDecision(anchored.copy(streak = 0, heldMps = speed), speed)
        }
        val streak = previous.streak + 1
        return if (streak >= HYSTERESIS_SAMPLES) {
            DisplaySpeedDecision(
                anchored.copy(motion = DisplayMotion.Idle, streak = 0, heldMps = null),
                0f
            )
        } else {
            DisplaySpeedDecision(anchored.copy(streak = streak), previous.heldMps ?: speed)
        }
    }

    private fun isSignificant(
        state: DisplaySpeedState,
        fix: DisplaySpeedFix,
        speed: Float
    ): Boolean {
        val accuracy = fix.speedAccuracyMps
        if (accuracy != null && accuracy.isFinite() && accuracy >= 0f) {
            return speed > accuracy
        }
        val previousLatitude = state.previousLatitude
        val previousLongitude = state.previousLongitude
        val horizontal = fix.horizontalAccuracyMeters
        if (
            previousLatitude == null ||
            previousLongitude == null ||
            horizontal == null ||
            !horizontal.isFinite() ||
            horizontal < 0f
        ) {
            return false
        }
        val moved = FixAcceptance.haversineMeters(
            previousLatitude,
            previousLongitude,
            fix.latitude,
            fix.longitude
        )
        return moved > horizontal.toDouble()
    }
}
