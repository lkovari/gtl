package com.lkovari.mobile.apps.gtl.engine

import java.time.Instant
import kotlin.math.abs

object AppTheme {
    fun isDark(
        mode: ThemeMode,
        latitude: Double?,
        longitude: Double?,
        at: Instant
    ): Boolean {
        return when (mode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.AUTOMATIC -> {
                val lat = latitude
                val lon = longitude
                if (lat == null || lon == null || !placed(lat, lon)) {
                    false
                } else {
                    CivilTwilight.isNight(lat, lon, at)
                }
            }
        }
    }

    private fun placed(latitude: Double, longitude: Double): Boolean {
        if (!latitude.isFinite() || !longitude.isFinite()) {
            return false
        }
        return abs(latitude) <= 90.0 && abs(longitude) <= 180.0
    }
}
