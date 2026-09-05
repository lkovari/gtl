package com.lkovari.mobile.apps.gtl.engine

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

object KmlDescriptions {
    private val balloonTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US)

    fun balloon(
        kind: EventKind,
        timestampMillis: Long,
        latitude: Double,
        longitude: Double,
        speedMps: Float,
        tempCelsius: Float?,
        maxSpeedMps: Float? = null,
        averageSpeedMps: Float? = null,
        system: MeasurementSystem = MeasurementSystem.METRIC
    ): String {
        val speed = if (kind == EventKind.PAUSE || kind == EventKind.STOP) {
            formatSpeed(0f, system)
        } else {
            formatSpeed(speedMps, system)
        }
        val temp = tempCelsius?.toString() ?: "-"
        val lines = mutableListOf(
            "time=${formatTime(timestampMillis)}",
            "lat=${formatCoord(latitude)}",
            "lon=${formatCoord(longitude)}",
            "speed=$speed",
            "temp=$temp"
        )
        if (kind == EventKind.STOP) {
            lines.add("maxSpeed=${formatSpeed(maxSpeedMps ?: 0f, system)}")
            lines.add("avgSpeed=${formatSpeed(averageSpeedMps ?: 0f, system)}")
        }
        return lines.joinToString("\n")
    }

    private fun formatTime(timestampMillis: Long): String {
        return Instant.ofEpochMilli(timestampMillis).atOffset(ZoneOffset.UTC).format(balloonTime)
    }

    private fun formatCoord(value: Double): String {
        return String.format(Locale.US, "%.6f", value)
    }

    private fun formatSpeed(metersPerSecond: Float, system: MeasurementSystem): String {
        return when (system) {
            MeasurementSystem.METRIC -> String.format(Locale.US, "%.1f km/h", metersPerSecond * 3.6f)
            MeasurementSystem.IMPERIAL -> String.format(Locale.US, "%.1f mph", metersPerSecond * 2.2369363f)
            MeasurementSystem.ICAO -> String.format(Locale.US, "%.1f kt", metersPerSecond * 1.9438445f)
        }
    }
}
