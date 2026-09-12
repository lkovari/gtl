package com.lkovari.mobile.apps.gtl.engine

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

object KmlDescriptions {
    private val balloonTime = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss", Locale.US)

    fun balloon(
        kind: EventKind,
        timestampMillis: Long,
        latitude: Double,
        longitude: Double,
        altitude: Double,
        baroAltitude: Double? = null,
        speedMps: Float,
        tempCelsius: Float?,
        maxSpeedMps: Float? = null,
        averageSpeedMps: Float? = null,
        elapsedMillis: Long = 0L,
        odometerMeters: Double = 0.0,
        system: MeasurementSystem = MeasurementSystem.METRIC
    ): String {
        val temp = if (tempCelsius == null) {
            "N/A"
        } else {
            Units.formatTemperature(tempCelsius, system)
        }
        val baro = if (baroAltitude == null) {
            "N/A"
        } else {
            Units.formatAltitude(baroAltitude, system)
        }
        val lines = mutableListOf(
            formatTime(timestampMillis),
            "temp=$temp",
            "lon=${formatCoord(longitude)}",
            "lat=${formatCoord(latitude)}",
            "Altitude: ${Units.formatAltitude(altitude, system)}",
            "Baro: $baro"
        )
        when (kind) {
            EventKind.PAUSE -> {
                lines.add("Speed: ${Units.formatSpeed(speedMps, system)}")
                lines.add("duration=${Units.formatBalloonDuration(elapsedMillis)}")
                lines.add("distance=${Units.formatDistance(odometerMeters, system)}")
            }
            EventKind.STOP -> {
                lines.add("Avg. Speed: ${Units.formatSpeed(averageSpeedMps ?: 0f, system)}")
                lines.add("Max speed: ${Units.formatSpeed(maxSpeedMps ?: 0f, system)}")
                lines.add("duration=${Units.formatBalloonDuration(elapsedMillis)}")
                lines.add("distance=${Units.formatDistance(odometerMeters, system)}")
            }
            EventKind.START, EventKind.MOVE -> Unit
        }
        return lines.joinToString("\n")
    }

    private fun formatTime(timestampMillis: Long): String {
        return Instant.ofEpochMilli(timestampMillis).atOffset(ZoneOffset.UTC).format(balloonTime)
    }

    private fun formatCoord(value: Double): String {
        return String.format(Locale.US, "%.6f", value)
    }
}
