package com.lkovari.mobile.apps.gtl.engine

object GpsAltitude {
    const val MinPlausibleMeters = -430.0
    const val MaxPlausibleMeters = 9000.0

    fun isPlausible(meters: Double): Boolean {
        return meters.isFinite() && meters >= MinPlausibleMeters && meters <= MaxPlausibleMeters
    }

    fun pick(
        gnssMsl: Double?,
        fusedMsl: Double?,
        gnssEllipsoid: Double?,
        fusedEllipsoid: Double?
    ): Double? {
        return listOf(gnssMsl, fusedMsl, gnssEllipsoid, fusedEllipsoid).firstOrNull { value ->
            value != null && isPlausible(value)
        }
    }
}
