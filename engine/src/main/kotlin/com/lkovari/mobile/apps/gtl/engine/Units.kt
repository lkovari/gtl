package com.lkovari.mobile.apps.gtl.engine

import java.util.Locale

object Units {
    fun formatSpeed(metersPerSecond: Float, system: MeasurementSystem): String {
        return when (system) {
            MeasurementSystem.METRIC -> String.format(Locale.US, "%.1f km/h", metersPerSecond * 3.6f)
            MeasurementSystem.IMPERIAL -> String.format(Locale.US, "%.1f mph", metersPerSecond * 2.2369363f)
            MeasurementSystem.ICAO -> String.format(Locale.US, "%.1f kt", metersPerSecond * 1.9438445f)
        }
    }

    fun formatDistance(meters: Double, system: MeasurementSystem): String {
        return when (system) {
            MeasurementSystem.METRIC -> {
                if (meters >= 1000.0) {
                    String.format(Locale.US, "%.2f km", meters / 1000.0)
                } else {
                    String.format(Locale.US, "%.0f m", meters)
                }
            }
            MeasurementSystem.IMPERIAL -> {
                val miles = meters / 1609.344
                if (miles >= 0.1) {
                    String.format(Locale.US, "%.2f mi", miles)
                } else {
                    String.format(Locale.US, "%.0f ft", meters * 3.28084)
                }
            }
            MeasurementSystem.ICAO -> String.format(Locale.US, "%.2f NM", meters / 1852.0)
        }
    }

    fun formatAltitude(meters: Double, system: MeasurementSystem): String {
        return when (system) {
            MeasurementSystem.METRIC -> String.format(Locale.US, "%.0f m", meters)
            MeasurementSystem.IMPERIAL, MeasurementSystem.ICAO ->
                String.format(Locale.US, "%.0f ft", meters * 3.28084)
        }
    }

    fun formatDuration(millis: Long): String {
        val totalSeconds = (millis / 1000).coerceAtLeast(0)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun formatBalloonDuration(millis: Long): String {
        val totalSeconds = (millis / 1000).coerceAtLeast(0)
        return when {
            totalSeconds <= 60L -> "$totalSeconds s"
            totalSeconds < 3600L -> "${totalSeconds / 60} min"
            else -> {
                val hours = totalSeconds / 3600
                val minutes = (totalSeconds % 3600) / 60
                val seconds = totalSeconds % 60
                String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
            }
        }
    }

    fun formatTemperature(celsius: Float?): String {
        if (celsius == null) {
            return "—"
        }
        return String.format(Locale.US, "%.1f °C", celsius)
    }

    fun formatTemperature(celsius: Float, system: MeasurementSystem): String {
        return when (system) {
            MeasurementSystem.METRIC, MeasurementSystem.ICAO ->
                String.format(Locale.US, "%.1f °C", celsius)
            MeasurementSystem.IMPERIAL ->
                String.format(Locale.US, "%.1f °F", celsius * 1.8f + 32f)
        }
    }

    fun hudSpeedNumber(metersPerSecond: Float, system: MeasurementSystem): String {
        val value = when (system) {
            MeasurementSystem.METRIC -> metersPerSecond * 3.6f
            MeasurementSystem.IMPERIAL -> metersPerSecond * 2.2369363f
            MeasurementSystem.ICAO -> metersPerSecond * 1.9438445f
        }
        return String.format("%.0f", value)
    }

    fun hudSpeedUnit(system: MeasurementSystem): String {
        return when (system) {
            MeasurementSystem.METRIC -> "km/h"
            MeasurementSystem.IMPERIAL -> "mph"
            MeasurementSystem.ICAO -> "kt"
        }
    }
}
