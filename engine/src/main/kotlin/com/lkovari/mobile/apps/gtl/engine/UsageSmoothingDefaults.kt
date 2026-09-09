package com.lkovari.mobile.apps.gtl.engine

data class UsageSmoothingDefaults(
    val trackSmoothingEnabled: Boolean,
    val smoothingStrength: SmoothingStrength,
    val stationaryLockEnabled: Boolean,
    val recordingDensity: RecordingDensity,
    val optimizationActive: Boolean,
    val optimizationToleranceMeters: Double,
    val gnssOnly: Boolean
)
