package com.lkovari.mobile.apps.gtl.engine

data class KmlPlacemark(
    val name: String,
    val kind: EventKind,
    val point: GeoPoint,
    val description: String
)

data class KmlVertex(
    val point: GeoPoint,
    val timestampMillis: Long,
    val speedMps: Float
)

data class KmlTrack(
    val name: String,
    val points: List<KmlVertex>,
    val placemarks: List<KmlPlacemark>
)

data class KmlDocument(
    val name: String,
    val trackColorAabbggrr: String,
    val trackWidth: Int,
    val tracks: List<KmlTrack>
)

object KmlExporter {
    fun export(document: KmlDocument): String {
        val builder = StringBuilder()
        builder.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        builder.appendLine("""<kml xmlns="http://www.opengis.net/kml/2.2" xmlns:gx="http://www.google.com/kml/ext/2.2">""")
        builder.appendLine("<Document>")
        builder.appendLine("<name>${escape(document.name)}</name>")
        appendStyles(builder, document.trackColorAabbggrr, document.trackWidth)
        appendSpeedSchema(builder)
        document.tracks.forEach { track ->
            builder.appendLine("<Folder>")
            builder.appendLine("<name>${escape(track.name)}</name>")
            if (track.points.isNotEmpty()) {
                builder.appendLine("<Placemark>")
                builder.appendLine("<name>${escape(track.name)}</name>")
                builder.appendLine("<styleUrl>#track</styleUrl>")
                builder.appendLine("<gx:Track>")
                builder.appendLine("<altitudeMode>absolute</altitudeMode>")
                track.points.forEach { vertex ->
                    builder.appendLine("<when>${utcWhen(vertex.timestampMillis)}</when>")
                }
                track.points.forEach { vertex ->
                    val p = vertex.point
                    builder.appendLine("<gx:coord>${p.longitude} ${p.latitude} ${p.altitude}</gx:coord>")
                }
                builder.appendLine("<ExtendedData>")
                builder.appendLine("<SchemaData schemaUrl=\"#trackSpeed\">")
                builder.appendLine("<gx:SimpleArrayData name=\"speed\">")
                track.points.forEach { vertex ->
                    builder.appendLine("<gx:value>${vertex.speedMps}</gx:value>")
                }
                builder.appendLine("</gx:SimpleArrayData>")
                builder.appendLine("</SchemaData>")
                builder.appendLine("</ExtendedData>")
                builder.appendLine("</gx:Track>")
                builder.appendLine("</Placemark>")
            }
            track.placemarks.forEach { mark ->
                builder.appendLine("<Placemark>")
                builder.appendLine("<name>${escape(mark.name)}</name>")
                builder.appendLine("<description>${escape(mark.description)}</description>")
                builder.appendLine("<styleUrl>#${styleId(mark.kind)}</styleUrl>")
                builder.appendLine("<Point>")
                builder.appendLine("<coordinates>${mark.point.longitude},${mark.point.latitude},${mark.point.altitude}</coordinates>")
                builder.appendLine("</Point>")
                builder.appendLine("</Placemark>")
            }
            builder.appendLine("</Folder>")
        }
        builder.appendLine("</Document>")
        builder.appendLine("</kml>")
        return builder.toString()
    }

    private fun appendStyles(builder: StringBuilder, color: String, width: Int) {
        builder.appendLine("""<Style id="track"><LineStyle><color>$color</color><width>$width</width></LineStyle></Style>""")
        appendIconStyle(builder, "start", "icons/play.png")
        appendIconStyle(builder, "pause", "icons/pause.png")
        appendIconStyle(builder, "stop", "icons/stop.png")
        builder.appendLine("""<Style id="move"><LabelStyle><scale>0</scale></LabelStyle></Style>""")
    }

    private fun appendSpeedSchema(builder: StringBuilder) {
        builder.appendLine("""<Schema id="trackSpeed"><gx:SimpleArrayField name="speed" type="float"><displayName>Speed (m/s)</displayName></gx:SimpleArrayField></Schema>""")
    }

    private fun utcWhen(timestampMillis: Long): String {
        return java.time.Instant.ofEpochMilli(timestampMillis).toString()
    }

    private fun appendIconStyle(builder: StringBuilder, id: String, href: String) {
        builder.appendLine(
            """<Style id="$id"><IconStyle><scale>0.6</scale><Icon><href>$href</href></Icon><hotSpot x="0.5" y="0.5" xunits="fraction" yunits="fraction"/></IconStyle><LabelStyle><scale>0</scale></LabelStyle></Style>"""
        )
    }

    private fun styleId(kind: EventKind): String {
        return when (kind) {
            EventKind.START -> "start"
            EventKind.PAUSE -> "pause"
            EventKind.STOP -> "stop"
            EventKind.MOVE -> "move"
        }
    }

    private fun escape(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }
}
