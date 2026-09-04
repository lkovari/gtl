package com.lkovari.mobile.apps.gtl.domain

import android.content.Context
import com.lkovari.mobile.apps.gtl.data.db.GpsEventEntity
import com.lkovari.mobile.apps.gtl.data.db.TrackSessionEntity
import com.lkovari.mobile.apps.gtl.engine.EventKind
import com.lkovari.mobile.apps.gtl.engine.GeoPoint
import com.lkovari.mobile.apps.gtl.engine.KmlDocument
import com.lkovari.mobile.apps.gtl.engine.KmlExporter
import com.lkovari.mobile.apps.gtl.engine.KmlPlacemark
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class KmlExportUseCase(private val context: Context) {
    fun write(session: TrackSessionEntity, events: List<GpsEventEntity>): File {
        val dir = File(context.filesDir, "gtltracklogs")
        dir.mkdirs()
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(session.startedAt))
        val file = File(dir, "GTL_$stamp.kml")
        val line = events.map { GeoPoint(it.latitude, it.longitude, it.altitude) }
        val marks = events.filter { it.isPlacemark }.map { event ->
            val kind = runCatching { EventKind.valueOf(event.eventKind) }.getOrDefault(EventKind.MOVE)
            KmlPlacemark(
                name = kind.name,
                kind = kind,
                point = GeoPoint(event.latitude, event.longitude, event.altitude),
                description = "speed=${event.speed} temp=${event.ambientTemperature ?: "-"}"
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
        file.writeText(kml)
        return file
    }
}
