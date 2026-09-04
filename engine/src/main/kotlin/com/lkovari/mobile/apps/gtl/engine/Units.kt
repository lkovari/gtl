package com.lkovari.mobile.apps.gtl.engine

object Units {
    fun formatSpeed(metersPerSecond: Float, system: MeasurementSystem): String {
        return when (system) {
            MeasurementSystem.METRIC -> String.format("%.1f km/h", metersPerSecond * 3.6f)
            MeasurementSystem.IMPERIAL -> String.format("%.1f mph", metersPerSecond * 2.2369363f)
            MeasurementSystem.ICAO -> String.format("%.1f kt", metersPerSecond * 1.9438445f)
        }
    }

    fun formatDistance(meters: Double, system: MeasurementSystem): String {
        return when (system) {
            MeasurementSystem.METRIC -> {
                if (meters >= 1000.0) {
                    String.format("%.2f km", meters / 1000.0)
                } else {
                    String.format("%.0f m", meters)
                }
            }
            MeasurementSystem.IMPERIAL -> {
                val miles = meters / 1609.344
                if (miles >= 0.1) {
                    String.format("%.2f mi", miles)
                } else {
                    String.format("%.0f ft", meters * 3.28084)
                }
            }
            MeasurementSystem.ICAO -> String.format("%.2f NM", meters / 1852.0)
        }
    }

    fun formatAltitude(meters: Double, system: MeasurementSystem): String {
        return when (system) {
            MeasurementSystem.METRIC -> String.format("%.0f m", meters)
            MeasurementSystem.IMPERIAL, MeasurementSystem.ICAO ->
                String.format("%.0f ft", meters * 3.28084)
        }
    }

    fun formatDuration(millis: Long): String {
        val totalSeconds = (millis / 1000).coerceAtLeast(0)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun formatTemperature(celsius: Float?): String {
        if (celsius == null) {
            return "—"
        }
        return String.format("%.1f °C", celsius)
    }
}
