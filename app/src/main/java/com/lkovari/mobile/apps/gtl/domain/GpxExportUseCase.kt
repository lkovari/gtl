package com.lkovari.mobile.apps.gtl.domain

import android.content.Context
import com.lkovari.mobile.apps.gtl.data.db.GpsEventEntity
import com.lkovari.mobile.apps.gtl.data.db.TrackSessionEntity
import com.lkovari.mobile.apps.gtl.engine.EventKind
import com.lkovari.mobile.apps.gtl.engine.GpxDocument
import com.lkovari.mobile.apps.gtl.engine.GpxExporter
import com.lkovari.mobile.apps.gtl.engine.GpxTrack
import com.lkovari.mobile.apps.gtl.engine.GpxTrackPoint
import com.lkovari.mobile.apps.gtl.engine.GpxWaypoint
import com.lkovari.mobile.apps.gtl.engine.TrackLogEvent
import com.lkovari.mobile.apps.gtl.engine.TrackLogExport
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class TrackShareFormat {
    KMZ,
    GPX
}

class GpxExportUseCase(private val context: Context) {
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
        val file = File(dir, "GTL_$fileStamp.gpx")
        val tracks = items.map { (session, events) ->
            val stamp = stampFormat.format(Date(session.startedAt))
            val logEvents = events.map { event ->
                TrackLogEvent(
                    timestampMillis = event.timestamp,
                    latitude = event.latitude,
                    longitude = event.longitude,
                    altitude = event.altitude,
                    speedMps = event.speed,
                    kind = runCatching { EventKind.valueOf(event.eventKind) }.getOrDefault(EventKind.MOVE)
                )
            }
            val path = TrackLogExport.path(logEvents)
            val markers = TrackLogExport.markers(logEvents)
            GpxTrack(
                name = "GTL $stamp",
                points = path.map { event ->
                    GpxTrackPoint(
                        point = event.point(),
                        timestampMillis = event.timestampMillis
                    )
                },
                waypoints = markers.map { marker ->
                    GpxWaypoint(
                        name = marker.kind.kmlPlacemarkName(),
                        point = marker.event.point(),
                        timestampMillis = marker.event.timestampMillis
                    )
                }
            )
        }
        file.writeText(GpxExporter.export(GpxDocument(tracks = tracks)), Charsets.UTF_8)
        return file
    }
}
