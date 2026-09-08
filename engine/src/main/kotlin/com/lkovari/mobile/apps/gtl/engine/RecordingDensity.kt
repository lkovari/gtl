package com.lkovari.mobile.apps.gtl.engine

enum class RecordingDensity {
    SMART,
    EVERY_FIX;

    fun sliderValue(): Float {
        return when (this) {
            SMART -> 0f
            EVERY_FIX -> 1f
        }
    }
}
