package com.lkovari.mobile.apps.gtl.engine

object BaroAltitude {
    const val StandardAtmosphereHpa = 1013.25f

    fun metersFromPressureHpa(
        pressureHpa: Float,
        seaLevelHpa: Float = StandardAtmosphereHpa
    ): Double? {
        if (pressureHpa <= 0f || seaLevelHpa <= 0f) {
            return null
        }
        return 44330.0 * (1.0 - Math.pow((pressureHpa / seaLevelHpa).toDouble(), 1.0 / 5.255))
    }
}
