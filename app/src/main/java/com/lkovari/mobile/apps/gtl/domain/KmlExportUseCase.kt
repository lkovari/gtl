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
import com.lkovari.mobile.apps.gtl.engine.KmzExporter
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class KmlExportUseCase(private val context: Context) {
    fun write(session: TrackSessionEntity, events: List<GpsEventEntity>): File {
        val dir = File(context.filesDir, "gtltracklogs")
        dir.mkdirs()
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(session.startedAt))
        val file = File(dir, "GTL_$stamp.kmz")
        val line = events.map { GeoPoint(it.latitude, it.longitude, it.altitude) }
        val marks = events.filter { it.isPlacemark }.map { event ->
            val kind = runCatching { EventKind.valueOf(event.eventKind) }.getOrDefault(EventKind.MOVE)
            KmlPlacemark(
                name = kind.name,
                kind = kind,
                point = GeoPoint(event.latitude, event.longitude, event.altitude),
                description = KmlDescriptions.balloon(kind, event.speed, event.ambientTemperature)
            )
        }
        val kml = KmlExporter.export(
            KmlDocument(
                name = "GTL $stamp",
                trackColorAabbggrr = "ff0000ff",
                trackWidth = 6,
                line = line,
                placemarks = marks
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
