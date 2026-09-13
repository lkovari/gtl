package com.lkovari.mobile.apps.gtl.data.sensor

import android.hardware.SensorManager
import com.lkovari.mobile.apps.gtl.engine.BaroAltitude

object AndroidBaroAltitude {
    fun metersFromPressureHpa(
        pressureHpa: Float,
        qnhHpa: Float = SensorManager.PRESSURE_STANDARD_ATMOSPHERE,
        offsetHpa: Float = 0f
    ): Double? {
        val qnh = BaroAltitude.clampQnh(qnhHpa)
        val corrected = pressureHpa - BaroAltitude.clampOffset(offsetHpa)
        if (corrected <= 0f || qnh <= 0f) {
            return null
        }
        return SensorManager.getAltitude(qnh, corrected).toDouble()
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
