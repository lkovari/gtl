package com.lkovari.mobile.apps.gtl.engine

object BaroAltitude {
    const val StandardAtmosphereHpa = 1013.25f
    const val MinQnhHpa = 900f
    const val MaxQnhHpa = 1100f

    fun clampQnh(hpa: Float): Float {
        return hpa.coerceIn(MinQnhHpa, MaxQnhHpa)
    }

    fun metersFromPressureHpa(
        pressureHpa: Float,
        seaLevelHpa: Float = StandardAtmosphereHpa
    ): Double? {
        val qnh = clampQnh(seaLevelHpa)
        if (pressureHpa <= 0f || qnh <= 0f) {
            return null
        }
        return 44330.0 * (1.0 - Math.pow((pressureHpa / qnh).toDouble(), 1.0 / 5.255))
    }

    fun displayedMeters(
        pressureHpa: Float?,
        storedBaro: Double?,
        qnhHpa: Float
    ): Double? {
        val fromPressure = pressureHpa?.let { metersFromPressureHpa(it, qnhHpa) }
        return fromPressure ?: storedBaro
    }
}
