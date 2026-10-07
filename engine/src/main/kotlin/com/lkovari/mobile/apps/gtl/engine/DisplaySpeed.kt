package com.lkovari.mobile.apps.gtl.engine

data class DisplaySpeedFix(
    val speedMps: Float?,
    val speedAccuracyMps: Float?,
    val latitude: Double,
    val longitude: Double,
    val horizontalAccuracyMeters: Float?,
    val deviceMoving: Boolean? = null
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
    const val EXIT_SAMPLES = 3
    const val DefaultFloorMps = 0.8f

    fun floorMps(usage: UsageType): Float {
        return when (usage) {
            UsageType.RUNNER, UsageType.WALKING_HIKE, UsageType.PEDESTRIAN -> 0.3f
            UsageType.BICYCLE -> 0.5f
            UsageType.TWO_WHEELERS, UsageType.FOUR_WHEELERS, UsageType.WATERCRAFT -> 0.8f
            UsageType.AIRCRAFT -> 1.5f
        }
    }

    fun apply(
        state: DisplaySpeedState,
        fix: DisplaySpeedFix,
        floorMps: Float = DefaultFloorMps
    ): DisplaySpeedDecision {
        val anchored = state.copy(
            previousLatitude = fix.latitude,
            previousLongitude = fix.longitude
        )
        val speed = fix.speedMps
        if (speed == null || !speed.isFinite()) {
            return DisplaySpeedDecision(anchored, null)
        }
        val significant = isSignificant(state, fix, speed, floorMps)
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
        return if (streak >= EXIT_SAMPLES) {
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
        speed: Float,
        floorMps: Float
    ): Boolean {
        val accuracy = fix.speedAccuracyMps
        val hasAccuracy = accuracy != null && accuracy.isFinite() && accuracy >= 0f
        if (hasAccuracy && speed > accuracy!!) {
            return true
        }
        // A slow walk often reports speed below its own speed accuracy; the accelerometer
        // tells it apart from indoor Doppler noise on a phone lying still.
        if (speed >= floorMps && fix.deviceMoving == true) {
            return true
        }
        if (hasAccuracy) {
            return false
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
