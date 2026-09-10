package com.lkovari.mobile.apps.gtl.engine

enum class UsageType {
    AIRCRAFT,
    WATERCRAFT,
    FOUR_WHEELERS,
    TWO_WHEELERS,
    BICYCLE,
    WALKING_HIKE,
    PEDESTRIAN,
    RUNNER;

    fun defaultFilter(): FixFilter {
        return when (this) {
            RUNNER, BICYCLE, WALKING_HIKE, PEDESTRIAN -> FixFilter(
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
        return if (this == RUNNER || this == BICYCLE || this == WALKING_HIKE || this == PEDESTRIAN) {
            0.25f
        } else {
            0.4f
        }
    }

    fun isPedestrianMode(): Boolean {
        return this == RUNNER || this == BICYCLE || this == WALKING_HIKE || this == PEDESTRIAN
    }

    fun kmlLabel(): String {
        return when (this) {
            AIRCRAFT -> "Aircraft"
            WATERCRAFT -> "Watercraft"
            FOUR_WHEELERS -> "Car"
            TWO_WHEELERS -> "Motorbike"
            BICYCLE -> "Bicycle"
            RUNNER, WALKING_HIKE, PEDESTRIAN -> "Runner"
        }
    }

    fun processNoiseQ(): Double {
        return when (this) {
            RUNNER, WALKING_HIKE, PEDESTRIAN -> 8.0
            BICYCLE -> 6.0
            TWO_WHEELERS -> 2.5
            FOUR_WHEELERS, WATERCRAFT -> 1.5
            AIRCRAFT -> 0.8
        }
    }

    fun turnBoost(): Double {
        return when (this) {
            RUNNER, WALKING_HIKE, PEDESTRIAN -> 10.0
            BICYCLE -> 8.0
            TWO_WHEELERS -> 5.0
            FOUR_WHEELERS, WATERCRAFT -> 3.0
            AIRCRAFT -> 2.0
        }
    }

    fun defaultSmoothing(): UsageSmoothingDefaults {
        return when (this) {
            RUNNER, WALKING_HIKE, PEDESTRIAN -> UsageSmoothingDefaults(
                trackSmoothingEnabled = false,
                smoothingStrength = SmoothingStrength.LOW,
                stationaryLockEnabled = true,
                recordingDensity = RecordingDensity.EVERY_FIX,
                optimizationActive = false,
                optimizationToleranceMeters = 2.0,
                gnssOnly = true
            )
            BICYCLE -> UsageSmoothingDefaults(
                trackSmoothingEnabled = false,
                smoothingStrength = SmoothingStrength.LOW,
                stationaryLockEnabled = true,
                recordingDensity = RecordingDensity.EVERY_FIX,
                optimizationActive = false,
                optimizationToleranceMeters = 3.0,
                gnssOnly = true
            )
            TWO_WHEELERS -> UsageSmoothingDefaults(
                trackSmoothingEnabled = true,
                smoothingStrength = SmoothingStrength.MEDIUM,
                stationaryLockEnabled = true,
                recordingDensity = RecordingDensity.SMART,
                optimizationActive = true,
                optimizationToleranceMeters = 6.0,
                gnssOnly = false
            )
            FOUR_WHEELERS, WATERCRAFT -> UsageSmoothingDefaults(
                trackSmoothingEnabled = true,
                smoothingStrength = SmoothingStrength.MEDIUM,
                stationaryLockEnabled = true,
                recordingDensity = RecordingDensity.SMART,
                optimizationActive = true,
                optimizationToleranceMeters = 8.0,
                gnssOnly = false
            )
            AIRCRAFT -> UsageSmoothingDefaults(
                trackSmoothingEnabled = true,
                smoothingStrength = SmoothingStrength.HIGH,
                stationaryLockEnabled = true,
                recordingDensity = RecordingDensity.SMART,
                optimizationActive = true,
                optimizationToleranceMeters = 15.0,
                gnssOnly = false
            )
        }
    }

    companion object {
        val selectable: List<UsageType> = listOf(
            AIRCRAFT,
            WATERCRAFT,
            FOUR_WHEELERS,
            TWO_WHEELERS,
            BICYCLE,
            RUNNER
        )

        fun kmlLabelOf(stored: String?): String? {
            if (stored.isNullOrBlank()) {
                return null
            }
            return runCatching { valueOf(stored) }.getOrNull()?.kmlLabel() ?: stored
        }
    }
}
