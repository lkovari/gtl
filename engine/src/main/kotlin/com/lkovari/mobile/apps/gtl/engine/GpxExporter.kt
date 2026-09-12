package com.lkovari.mobile.apps.gtl.engine

data class GpxWaypoint(
    val name: String,
    val point: GeoPoint,
    val timestampMillis: Long
)

data class GpxTrackPoint(
    val point: GeoPoint,
    val timestampMillis: Long
)

data class GpxTrack(
    val name: String,
    val points: List<GpxTrackPoint>,
    val waypoints: List<GpxWaypoint>
)

data class GpxDocument(
    val tracks: List<GpxTrack>
)

object GpxExporter {
    fun export(document: GpxDocument): String {
        val builder = StringBuilder()
        builder.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        builder.appendLine("""<gpx version="1.1" creator="GTL" xmlns="http://www.topografix.com/GPX/1/1">""")
        document.tracks.forEach { track ->
            track.waypoints.forEach { waypoint ->
                appendWaypoint(builder, waypoint)
            }
            builder.appendLine("<trk>")
            builder.appendLine("<name>${escape(track.name)}</name>")
            builder.appendLine("<trkseg>")
            track.points.forEach { point ->
                appendTrackPoint(builder, point)
            }
            builder.appendLine("</trkseg>")
            builder.appendLine("</trk>")
        }
        builder.appendLine("</gpx>")
        return builder.toString()
    }

    private fun appendWaypoint(builder: StringBuilder, waypoint: GpxWaypoint) {
        val point = waypoint.point
        builder.appendLine("""<wpt lat="${point.latitude}" lon="${point.longitude}">""")
        builder.appendLine("<ele>${point.altitude}</ele>")
        builder.appendLine("<time>${utcWhen(waypoint.timestampMillis)}</time>")
        builder.appendLine("<name>${escape(waypoint.name)}</name>")
        builder.appendLine("</wpt>")
    }

    private fun appendTrackPoint(builder: StringBuilder, vertex: GpxTrackPoint) {
        val point = vertex.point
        builder.appendLine("""<trkpt lat="${point.latitude}" lon="${point.longitude}">""")
        builder.appendLine("<ele>${point.altitude}</ele>")
        builder.appendLine("<time>${utcWhen(vertex.timestampMillis)}</time>")
        builder.appendLine("</trkpt>")
    }

    private fun utcWhen(timestampMillis: Long): String {
        return java.time.Instant.ofEpochMilli(timestampMillis).toString()
    }

    private fun escape(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }
}
