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
        elapsedMillis: Long = 0L,
        system: MeasurementSystem = MeasurementSystem.METRIC,
        leanAngle: Float? = null,
        usageType: String? = null
    ): String {
        val speed = if (kind == EventKind.PAUSE || kind == EventKind.STOP) {
            formatSpeed(0f, system)
        } else {
            formatSpeed(speedMps, system)
        }
        val temp = tempCelsius?.toString() ?: "-"
        val lines = mutableListOf("time=${formatTime(timestampMillis)}")
        if (
            (kind == EventKind.START || kind == EventKind.PAUSE || kind == EventKind.STOP) &&
            !usageType.isNullOrBlank()
        ) {
            lines.add("usage=$usageType")
        }
        lines.add("lat=${formatCoord(latitude)}")
        lines.add("lon=${formatCoord(longitude)}")
        lines.add("speed=$speed")
        lines.add("temp=$temp")
        if (leanAngle != null) {
            lines.add("lean=${String.format(Locale.US, "%.1f", leanAngle)}")
        }
        if (kind == EventKind.STOP) {
            lines.add("Duration: ${formatSessionDuration(elapsedMillis)}")
            lines.add("Avg. speed: ${formatIntegerSpeed(averageSpeedMps ?: 0f, system)}")
            lines.add("Max. speed: ${formatIntegerSpeed(maxSpeedMps ?: 0f, system)}")
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

    private fun formatSessionDuration(elapsedMillis: Long): String {
        val totalSeconds = (elapsedMillis / 1000).coerceAtLeast(0)
        return when {
            totalSeconds <= 60L -> "$totalSeconds s"
            totalSeconds < 3600L -> "${totalSeconds / 60} min"
            else -> Units.formatDuration(elapsedMillis)
        }
    }

    private fun formatIntegerSpeed(metersPerSecond: Float, system: MeasurementSystem): String {
        return when (system) {
            MeasurementSystem.METRIC ->
                "${Math.round(metersPerSecond * 3.6f)} km/h"
            MeasurementSystem.IMPERIAL ->
                "${Math.round(metersPerSecond * 2.2369363f)} mile/h"
            MeasurementSystem.ICAO ->
                "${Math.round(metersPerSecond * 1.9438445f)} kt"
        }
    }
}
