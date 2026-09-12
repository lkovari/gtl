package com.lkovari.mobile.apps.gtl.engine

import java.util.Locale

data class TrackInspectEvent(
    val id: Long,
    val timestampMillis: Long,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val speedMps: Float,
    val bearing: Float,
    val accuracy: Float,
    val satellitesInFix: Int,
    val ambientTemperature: Float?,
    val accelX: Float?,
    val accelY: Float?,
    val accelZ: Float?,
    val leanAngle: Float?,
    val usageType: String?,
    val isPlacemark: Boolean,
    val eventKind: String,
    val baroAltitude: Double?,
    val pressureHpa: Float?
)

object TrackInspectDump {
    fun format(
        sessionId: Long,
        startedAt: Long,
        stoppedAt: Long?,
        usageType: String,
        measurementSystem: String,
        events: List<TrackInspectEvent>
    ): String {
        val baroNonNull = events.count { it.baroAltitude != null }
        val pressureNonNull = events.count { it.pressureHpa != null }
        val tempNonNull = events.count { it.ambientTemperature != null }
        val lines = mutableListOf(
            "sessionId=$sessionId",
            "startedAt=$startedAt",
            "stoppedAt=${stoppedAt ?: "-"}",
            "usageType=$usageType",
            "measurementSystem=$measurementSystem",
            "points=${events.size}",
            "baroNonNull=$baroNonNull",
            "pressureNonNull=$pressureNonNull",
            "tempNonNull=$tempNonNull",
            "",
            listOf(
                "id",
                "timestamp",
                "eventKind",
                "isPlacemark",
                "lat",
                "lon",
                "alt",
                "speed",
                "bearing",
                "accuracy",
                "sats",
                "temp",
                "baro",
                "hPa",
                "lean",
                "usage",
                "accelX",
                "accelY",
                "accelZ"
            ).joinToString("\t")
        )
        events.forEach { event ->
            lines.add(
                listOf(
                    event.id.toString(),
                    event.timestampMillis.toString(),
                    event.eventKind,
                    event.isPlacemark.toString(),
                    fmt(event.latitude),
                    fmt(event.longitude),
                    fmt(event.altitude),
                    fmt(event.speedMps),
                    fmt(event.bearing),
                    fmt(event.accuracy),
                    event.satellitesInFix.toString(),
                    nullable(event.ambientTemperature),
                    nullable(event.baroAltitude),
                    nullable(event.pressureHpa),
                    nullable(event.leanAngle),
                    event.usageType ?: "-",
                    nullable(event.accelX),
                    nullable(event.accelY),
                    nullable(event.accelZ)
                ).joinToString("\t")
            )
        }
        return lines.joinToString("\n")
    }

    private fun fmt(value: Double): String {
        return String.format(Locale.US, "%.6f", value)
    }

    private fun fmt(value: Float): String {
        return String.format(Locale.US, "%.3f", value)
    }

    private fun nullable(value: Float?): String {
        return if (value == null) "-" else fmt(value)
    }

    private fun nullable(value: Double?): String {
        return if (value == null) "-" else fmt(value)
    }
}
