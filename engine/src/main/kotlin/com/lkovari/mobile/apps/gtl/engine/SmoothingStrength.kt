package com.lkovari.mobile.apps.gtl.engine

import kotlin.math.pow

enum class SmoothingStrength {
    LOW,
    MEDIUM,
    HIGH;

    fun sliderValue(): Float {
        return when (this) {
            LOW -> 0f
            MEDIUM -> 0.5f
            HIGH -> 1f
        }
    }

    fun processNoiseMultiplier(): Double {
        return SmoothingStrength.processNoiseMultiplier(sliderValue())
    }

    companion object {
        fun processNoiseMultiplier(slider: Float): Double {
            val t = slider.coerceIn(0f, 1f).toDouble()
            return 4.0.pow(1.0 - 2.0 * t)
        }
    }
}
