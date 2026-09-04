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
                minDistanceMeters = 2.5f,
                minTimeMillis = 500L,
                minAccuracyMeters = 45,
                minSatellites = 4
            )
            else -> FixFilter(
                minDistanceMeters = 2.5f,
                minTimeMillis = 500L,
                minAccuracyMeters = 30,
                minSatellites = 4
            )
        }
    }

    fun pauseSpeedMps(): Float {
        return if (this == RUNNER || this == WALKING_HIKE || this == PEDESTRIAN) {
            0.25f
        } else {
            0.4f
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
