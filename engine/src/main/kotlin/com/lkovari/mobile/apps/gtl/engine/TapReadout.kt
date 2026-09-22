package com.lkovari.mobile.apps.gtl.engine

import java.util.Locale

data class PostalAddress(
    val zip: String,
    val country: String,
    val city: String,
    val street: String,
    val houseNumber: String
)

object MapAddressLookup {
    fun supported(): Boolean = false

    fun lookup(latitude: Double, longitude: Double): PostalAddress? {
        if (!supported() || !latitude.isFinite() || !longitude.isFinite()) {
            return null
        }
        return null
    }
}

object TapReadout {
    fun formatCoordinate(latitude: Double, longitude: Double): String {
        return String.format(Locale.US, "%.6f, %.6f", latitude, longitude)
    }

    fun formatLongitude(longitude: Double): String {
        return String.format(Locale.US, "%.6f", longitude)
    }

    fun formatLatitude(latitude: Double): String {
        return String.format(Locale.US, "%.6f", latitude)
    }

    fun formatStraightLine(meters: Double, system: MeasurementSystem, prefix: String): String {
        val converted = when (system) {
            MeasurementSystem.METRIC -> meters / 1000.0
            MeasurementSystem.IMPERIAL -> meters / 1609.344
            MeasurementSystem.ICAO -> meters / 1852.0
        }
        val unit = when (system) {
            MeasurementSystem.METRIC -> "km"
            MeasurementSystem.IMPERIAL -> "mi"
            MeasurementSystem.ICAO -> "NM"
        }
        val number = if (converted < 10.0) {
            String.format(Locale.US, "%.1f", converted)
        } else {
            String.format(Locale.US, "%.0f", converted)
        }
        return prefix + number + unit
    }
}
