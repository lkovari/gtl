package com.lkovari.mobile.apps.gtl.engine

object BaroAltitude {
    const val StandardAtmosphereHpa = 1013.25f
    const val MinQnhHpa = 900f
    const val MaxQnhHpa = 1100f
    const val MinOffsetHpa = -10f
    const val MaxOffsetHpa = 10f
    private const val IsaScale = 44330.0
    private const val IsaExponent = 5.255

    fun clampQnh(hpa: Float): Float {
        return hpa.coerceIn(MinQnhHpa, MaxQnhHpa)
    }

    fun clampOffset(hpa: Float): Float {
        return hpa.coerceIn(MinOffsetHpa, MaxOffsetHpa)
    }

    fun expectedStationHpa(gpsMeters: Double, qnhHpa: Float): Float? {
        val qnh = clampQnh(qnhHpa)
        if (!gpsMeters.isFinite() || qnh <= 0f) {
            return null
        }
        val ratio = 1.0 - gpsMeters / IsaScale
        if (ratio <= 0.0) {
            return null
        }
        return (qnh * Math.pow(ratio, IsaExponent)).toFloat()
    }

    fun offsetHpa(pressureHpa: Float, gpsMeters: Double, qnhHpa: Float): Float {
        val expected = expectedStationHpa(gpsMeters, qnhHpa) ?: return 0f
        return clampOffset(pressureHpa - expected)
    }

    fun metersFromPressureHpa(
        pressureHpa: Float,
        seaLevelHpa: Float = StandardAtmosphereHpa,
        offsetHpa: Float = 0f
    ): Double? {
        val qnh = clampQnh(seaLevelHpa)
        val corrected = pressureHpa - clampOffset(offsetHpa)
        if (corrected <= 0f || qnh <= 0f) {
            return null
        }
        return IsaScale * (1.0 - Math.pow((corrected / qnh).toDouble(), 1.0 / IsaExponent))
    }

    fun displayedMeters(
        pressureHpa: Float?,
        storedBaro: Double?,
        qnhHpa: Float,
        offsetHpa: Float = 0f
    ): Double? {
        val fromPressure = pressureHpa?.let { metersFromPressureHpa(it, qnhHpa, offsetHpa) }
        return fromPressure ?: storedBaro
    }
}
