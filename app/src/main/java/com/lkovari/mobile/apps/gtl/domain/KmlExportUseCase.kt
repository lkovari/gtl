package com.lkovari.mobile.apps.gtl.domain

import android.content.Context
import com.lkovari.mobile.apps.gtl.data.db.GpsEventEntity
import com.lkovari.mobile.apps.gtl.data.db.TrackSessionEntity
import com.lkovari.mobile.apps.gtl.engine.EventKind
import com.lkovari.mobile.apps.gtl.engine.GeoPoint
import com.lkovari.mobile.apps.gtl.engine.KmlDescriptions
import com.lkovari.mobile.apps.gtl.engine.KmlDocument
import com.lkovari.mobile.apps.gtl.engine.KmlExporter
import com.lkovari.mobile.apps.gtl.engine.KmlPlacemark
import com.lkovari.mobile.apps.gtl.engine.KmlTrack
import com.lkovari.mobile.apps.gtl.engine.KmlVertex
import com.lkovari.mobile.apps.gtl.engine.KmzExporter
import com.lkovari.mobile.apps.gtl.engine.MeasurementSystem
import com.lkovari.mobile.apps.gtl.engine.TrackSample
import com.lkovari.mobile.apps.gtl.engine.TrackStatsCalculator
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class KmlExportUseCase(private val context: Context) {
    fun write(session: TrackSessionEntity, events: List<GpsEventEntity>): File {
        return write(listOf(session to events))
    }

    fun write(items: List<Pair<TrackSessionEntity, List<GpsEventEntity>>>): File {
        val dir = File(context.filesDir, "gtltracklogs")
        dir.mkdirs()
        val stampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val fileStamp = if (items.size == 1) {
            stampFormat.format(Date(items.first().first.startedAt))
        } else {
            stampFormat.format(Date())
        }
        val file = File(dir, "GTL_$fileStamp.kmz")
        val tracks = items.map { (session, events) ->
            val stamp = stampFormat.format(Date(session.startedAt))
            val system = runCatching { MeasurementSystem.valueOf(session.measurementSystem) }
                .getOrDefault(MeasurementSystem.METRIC)
            val stats = TrackStatsCalculator.compute(events.map { event ->
                TrackSample(
                    timestampMillis = event.timestamp,
                    latitude = event.latitude,
                    longitude = event.longitude,
                    altitude = event.altitude,
                    speedMps = event.speed,
                    bearing = event.bearing,
                    ambientTemperature = event.ambientTemperature,
                    eventKind = runCatching { EventKind.valueOf(event.eventKind) }.getOrDefault(EventKind.MOVE)
                )
            })
            KmlTrack(
                name = "GTL $stamp",
                points = events.map { event ->
                    KmlVertex(
                        point = GeoPoint(event.latitude, event.longitude, event.altitude),
                        timestampMillis = event.timestamp,
                        speedMps = event.speed
                    )
                },
                placemarks = events.filter { it.isPlacemark }.map { event ->
                    val kind = runCatching { EventKind.valueOf(event.eventKind) }.getOrDefault(EventKind.MOVE)
                    KmlPlacemark(
                        name = kind.name,
                        kind = kind,
                        point = GeoPoint(event.latitude, event.longitude, event.altitude),
                        description = KmlDescriptions.balloon(
                            kind = kind,
                            timestampMillis = event.timestamp,
                            latitude = event.latitude,
                            longitude = event.longitude,
                            speedMps = event.speed,
                            tempCelsius = event.ambientTemperature,
                            maxSpeedMps = if (kind == EventKind.STOP) stats.maxSpeedMps else null,
                            averageSpeedMps = if (kind == EventKind.STOP) stats.averageSpeedMps else null,
                            system = system
                        )
                    )
                }
            )
        }
        val documentName = if (tracks.size == 1) tracks.first().name else "GTL export $fileStamp"
        val kml = KmlExporter.export(
            KmlDocument(
                name = documentName,
                trackColorAabbggrr = "ff0000ff",
                trackWidth = 6,
                tracks = tracks
            )
        )
        file.writeBytes(KmzExporter.pack(kml, iconFiles()))
        return file
    }

    private fun iconFiles(): Map<String, ByteArray> {
        return mapOf(
            "icons/play.png" to assetBytes("kml/icons/play.png"),
            "icons/pause.png" to assetBytes("kml/icons/pause.png"),
            "icons/stop.png" to assetBytes("kml/icons/stop.png")
        )
    }

    private fun assetBytes(path: String): ByteArray {
        return context.assets.open(path).use { it.readBytes() }
    }
}
