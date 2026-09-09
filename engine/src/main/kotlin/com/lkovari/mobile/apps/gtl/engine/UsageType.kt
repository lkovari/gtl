package com.lkovari.mobile.apps.gtl.engine

enum class UsageType {
    AIRCRAFT,
    WATERCRAFT,
    FOUR_WHEELERS,
    TWO_WHEELERS,
    WALKING_HIKE,
    PEDESTRIAN,
    RUNNER;

    fun defaultFilter(): FixFilter {
        return when (this) {
            RUNNER -> FixFilter(
                minDistanceMeters = 2f,
                minTimeMillis = 500L,
                minAccuracyMeters = 45,
                minSatellites = 4
            )
            else -> FixFilter(
                minDistanceMeters = 2f,
                minTimeMillis = 500L,
                minAccuracyMeters = 30,
                minSatellites = 4
            )
        }
    }

    fun defaultMeasurementSystem(): MeasurementSystem {
        return if (this == AIRCRAFT || this == WATERCRAFT) {
            MeasurementSystem.ICAO
        } else {
            MeasurementSystem.METRIC
        }
    }

    fun pauseSpeedMps(): Float {
        return if (this == RUNNER || this == WALKING_HIKE || this == PEDESTRIAN) {
            0.25f
        } else {
            0.4f
        }
    }

    fun isPedestrianMode(): Boolean {
        return this == RUNNER || this == WALKING_HIKE || this == PEDESTRIAN
    }

    fun processNoiseQ(): Double {
        return when (this) {
            RUNNER, WALKING_HIKE, PEDESTRIAN -> 8.0
            TWO_WHEELERS -> 2.5
            FOUR_WHEELERS, WATERCRAFT -> 1.5
            AIRCRAFT -> 0.8
        }
    }

    fun turnBoost(): Double {
        return when (this) {
            RUNNER, WALKING_HIKE, PEDESTRIAN -> 10.0
            TWO_WHEELERS -> 5.0
            FOUR_WHEELERS, WATERCRAFT -> 3.0
            AIRCRAFT -> 2.0
        }
    }

    fun defaultSmoothing(): UsageSmoothingDefaults {
        return when (this) {
            RUNNER, WALKING_HIKE, PEDESTRIAN -> UsageSmoothingDefaults(
                trackSmoothingEnabled = true,
                smoothingStrength = SmoothingStrength.LOW,
                stationaryLockEnabled = true,
                recordingDensity = RecordingDensity.EVERY_FIX,
                optimizationActive = false,
                optimizationToleranceMeters = 2.0
            )
            TWO_WHEELERS -> UsageSmoothingDefaults(
                trackSmoothingEnabled = true,
                smoothingStrength = SmoothingStrength.MEDIUM,
                stationaryLockEnabled = true,
                recordingDensity = RecordingDensity.SMART,
                optimizationActive = true,
                optimizationToleranceMeters = 6.0
            )
            FOUR_WHEELERS, WATERCRAFT -> UsageSmoothingDefaults(
                trackSmoothingEnabled = true,
                smoothingStrength = SmoothingStrength.MEDIUM,
                stationaryLockEnabled = true,
                recordingDensity = RecordingDensity.SMART,
                optimizationActive = true,
                optimizationToleranceMeters = 8.0
            )
            AIRCRAFT -> UsageSmoothingDefaults(
                trackSmoothingEnabled = true,
                smoothingStrength = SmoothingStrength.HIGH,
                stationaryLockEnabled = true,
                recordingDensity = RecordingDensity.SMART,
                optimizationActive = true,
                optimizationToleranceMeters = 15.0
            )
        }
    }

    companion object {
        val selectable: List<UsageType> = listOf(
            AIRCRAFT,
            WATERCRAFT,
            FOUR_WHEELERS,
            TWO_WHEELERS,
            RUNNER
        )
    }
}
