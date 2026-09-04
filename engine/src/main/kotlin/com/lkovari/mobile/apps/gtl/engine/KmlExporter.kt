package com.lkovari.mobile.apps.gtl.engine

data class KmlPlacemark(
    val name: String,
    val kind: EventKind,
    val point: GeoPoint,
    val description: String
)

data class KmlDocument(
    val name: String,
    val trackColorAabbggrr: String,
    val trackWidth: Int,
    val line: List<GeoPoint>,
    val placemarks: List<KmlPlacemark>
)

object KmlExporter {
    fun export(document: KmlDocument): String {
        val builder = StringBuilder()
        builder.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        builder.appendLine("""<kml xmlns="http://www.opengis.net/kml/2.2">""")
        builder.appendLine("<Document>")
        builder.appendLine("<name>${escape(document.name)}</name>")
        appendStyles(builder, document.trackColorAabbggrr, document.trackWidth)
        if (document.line.isNotEmpty()) {
            builder.appendLine("<Placemark>")
            builder.appendLine("<name>${escape(document.name)}</name>")
            builder.appendLine("<styleUrl>#track</styleUrl>")
            builder.appendLine("<LineString>")
            builder.appendLine("<tessellate>1</tessellate>")
            builder.appendLine("<coordinates>")
            document.line.forEach { point ->
                builder.appendLine("${point.longitude},${point.latitude},${point.altitude}")
            }
            builder.appendLine("</coordinates>")
            builder.appendLine("</LineString>")
            builder.appendLine("</Placemark>")
        }
        document.placemarks.forEach { mark ->
            builder.appendLine("<Placemark>")
            builder.appendLine("<name>${escape(mark.name)}</name>")
            builder.appendLine("<description>${escape(mark.description)}</description>")
            builder.appendLine("<styleUrl>#${styleId(mark.kind)}</styleUrl>")
            builder.appendLine("<Point>")
            builder.appendLine("<coordinates>${mark.point.longitude},${mark.point.latitude},${mark.point.altitude}</coordinates>")
            builder.appendLine("</Point>")
            builder.appendLine("</Placemark>")
        }
        builder.appendLine("</Document>")
        builder.appendLine("</kml>")
        return builder.toString()
    }

    private fun appendStyles(builder: StringBuilder, color: String, width: Int) {
        builder.appendLine("""<Style id="track"><LineStyle><color>$color</color><width>$width</width></LineStyle></Style>""")
        builder.appendLine("""<Style id="start"><LabelStyle><color>ff33cc66</color></LabelStyle></Style>""")
        builder.appendLine("""<Style id="pause"><LabelStyle><color>ff33ccff</color></LabelStyle></Style>""")
        builder.appendLine("""<Style id="stop"><LabelStyle><color>ff3333cc</color></LabelStyle></Style>""")
        builder.appendLine("""<Style id="move"><LabelStyle><color>ffffaa33</color></LabelStyle></Style>""")
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
