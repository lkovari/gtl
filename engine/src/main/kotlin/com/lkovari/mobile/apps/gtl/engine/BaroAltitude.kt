package com.lkovari.mobile.apps.gtl.engine

object BaroAltitude {
    const val StandardAtmosphereHpa = 1013.25f
    const val MinQnhHpa = 900f
    const val MaxQnhHpa = 1100f
    const val MinOffsetHpa = -10f
    const val MaxOffsetHpa = 10f
    const val MaxGpsDeltaMeters = 1500.0
    const val MaxAltitudeJitterMeters = 15.0
    const val MinPlausiblePressureHpa = 300f
    const val MaxPlausiblePressureHpa = 1100f
    private const val IsaScale = 44330.0
    private const val IsaExponent = 5.255

    fun clampQnh(hpa: Float): Float {
        return hpa.coerceIn(MinQnhHpa, MaxQnhHpa)
    }

    fun clampOffset(hpa: Float): Float {
        if (!hpa.isFinite()) {
            return 0f
        }
        return hpa.coerceIn(MinOffsetHpa, MaxOffsetHpa)
    }

    fun isPlausiblePressureHpa(hpa: Float): Boolean {
        return hpa.isFinite() && hpa >= MinPlausiblePressureHpa && hpa <= MaxPlausiblePressureHpa
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
        if (!isPlausiblePressureHpa(pressureHpa)) {
            return 0f
        }
        val expected = expectedStationHpa(gpsMeters, qnhHpa) ?: return 0f
        return clampOffset(pressureHpa - expected)
    }

    fun autoCalibrateEligible(
        pressureHpa: Float?,
        gpsAltitudeMeters: Double?,
        alreadyCalibratedThisSession: Boolean,
        enabled: Boolean,
        previousGpsAltitudeMeters: Double?
    ): Boolean {
        if (!enabled || alreadyCalibratedThisSession) {
            return false
        }
        if (pressureHpa == null || !isPlausiblePressureHpa(pressureHpa) ||
            gpsAltitudeMeters == null || previousGpsAltitudeMeters == null
        ) {
            return false
        }
        if (!GpsAltitude.isPlausible(gpsAltitudeMeters)) {
            return false
        }
        return kotlin.math.abs(gpsAltitudeMeters - previousGpsAltitudeMeters) <= MaxAltitudeJitterMeters
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
        offsetHpa: Float = 0f,
        gpsMeters: Double? = null
    ): Double? {
        val fromPressure = pressureHpa?.let { metersFromPressureHpa(it, qnhHpa, offsetHpa) }
        return pickDisplayed(fromPressure, storedBaro, gpsMeters)
    }

    fun pickDisplayed(
        fromPressure: Double?,
        storedBaro: Double?,
        gpsMeters: Double?
    ): Double? {
        val candidate = storedBaro ?: fromPressure
        if (candidate == null) {
            return null
        }
        if (gpsMeters == null || !gpsMeters.isFinite()) {
            return candidate
        }
        if (kotlin.math.abs(candidate - gpsMeters) <= MaxGpsDeltaMeters) {
            return candidate
        }
        if (storedBaro != null && kotlin.math.abs(storedBaro - gpsMeters) <= MaxGpsDeltaMeters) {
            return storedBaro
        }
        if (fromPressure != null && kotlin.math.abs(fromPressure - gpsMeters) <= MaxGpsDeltaMeters) {
            return fromPressure
        }
        return null
    }
}
