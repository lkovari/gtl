package com.lkovari.mobile.apps.gtl.engine

import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler
import java.io.InputStream
import javax.xml.XMLConstants
import javax.xml.parsers.SAXParserFactory

sealed class GpxImport {
    data class Track(val lines: List<List<GeoPoint>>) : GpxImport()
    data object NotGpx : GpxImport()
    data object NoTrack : GpxImport()
    data object Unreadable : GpxImport()
}

object ImportedGpxStyle {
    val Argb: Int = 0xFFE20074.toInt()
}

object GpxImporter {
    const val SimplifyPointCount = 8_000
    const val SimplifyToleranceMeters = 5.0

    fun parse(source: InputStream): GpxImport {
        return try {
            val factory = SAXParserFactory.newInstance()
            factory.isNamespaceAware = true
            setFeature(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true)
            setFeature(factory, "http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature(factory, "http://xml.org/sax/features/external-general-entities", false)
            setFeature(factory, "http://xml.org/sax/features/external-parameter-entities", false)
            val handler = GpxHandler()
            factory.newSAXParser().parse(InputSource(source), handler)
            handler.result()
        } catch (_: Exception) {
            GpxImport.Unreadable
        }
    }

    private fun setFeature(factory: SAXParserFactory, name: String, value: Boolean) {
        try {
            factory.setFeature(name, value)
        } catch (_: Exception) {
        }
    }
}

private class GpxHandler : DefaultHandler() {
    private val lines = mutableListOf<MutableList<GeoPoint>>()
    private var loose: MutableList<GeoPoint>? = null
    private var segment: MutableList<GeoPoint>? = null
    private var route: MutableList<GeoPoint>? = null
    private var inTrk = false
    private var inTrkseg = false
    private var inRte = false
    private var extensionDepth = 0
    private var rootSeen = false
    private var rootIsGpx = false

    fun result(): GpxImport {
        if (!rootIsGpx) {
            return if (rootSeen) GpxImport.NotGpx else GpxImport.Unreadable
        }
        val drawable = lines.map { thin(it) }.filter { it.size >= 2 }
        if (drawable.isEmpty()) {
            return GpxImport.NoTrack
        }
        return GpxImport.Track(drawable)
    }

    override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
        val name = bare(localName, qName)
        if (!rootSeen) {
            rootSeen = true
            rootIsGpx = name == "gpx"
        }
        if (!rootIsGpx) {
            return
        }
        if (name == "extensions") {
            extensionDepth += 1
            return
        }
        if (extensionDepth > 0) {
            return
        }
        when (name) {
            "trk" -> {
                inTrk = true
                loose = ArrayList()
            }
            "trkseg" -> {
                if (inTrk) {
                    commit(loose)
                    loose = ArrayList()
                    inTrkseg = true
                    segment = ArrayList()
                }
            }
            "rte" -> {
                inRte = true
                route = ArrayList()
            }
            "trkpt" -> {
                if (!inTrk) {
                    return
                }
                val point = point(attributes) ?: return
                if (inTrkseg) {
                    segment?.add(point)
                } else {
                    loose?.add(point)
                }
            }
            "rtept" -> {
                if (inRte) {
                    val point = point(attributes) ?: return
                    route?.add(point)
                }
            }
        }
    }

    override fun endElement(uri: String, localName: String, qName: String) {
        if (!rootIsGpx) {
            return
        }
        val name = bare(localName, qName)
        if (name == "extensions") {
            if (extensionDepth > 0) {
                extensionDepth -= 1
            }
            return
        }
        if (extensionDepth > 0) {
            return
        }
        when (name) {
            "trkseg" -> {
                if (inTrkseg) {
                    commit(segment)
                    segment = null
                    inTrkseg = false
                }
            }
            "trk" -> {
                commit(loose)
                loose = null
                inTrk = false
                inTrkseg = false
            }
            "rte" -> {
                commit(route)
                route = null
                inRte = false
            }
        }
    }

    private fun commit(line: MutableList<GeoPoint>?) {
        if (line != null && line.size >= 2) {
            lines.add(line)
        }
    }

    private fun point(attributes: Attributes): GeoPoint? {
        val latitude = coordinate(attr(attributes, "lat")) ?: return null
        val longitude = coordinate(attr(attributes, "lon")) ?: return null
        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) {
            return null
        }
        return GeoPoint(latitude, longitude)
    }

    private fun attr(attributes: Attributes, name: String): String? {
        for (index in 0 until attributes.length) {
            val local = attributes.getLocalName(index).ifEmpty { attributes.getQName(index) }
            if (local.equals(name, ignoreCase = true)) {
                return attributes.getValue(index)
            }
        }
        return null
    }

    private fun coordinate(raw: String?): Double? {
        if (raw == null) {
            return null
        }
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) {
            return null
        }
        val normalized = if (',' in trimmed && '.' !in trimmed) {
            trimmed.replace(',', '.')
        } else {
            trimmed
        }
        val value = normalized.toDoubleOrNull() ?: return null
        if (!value.isFinite()) {
            return null
        }
        return value
    }

    private fun bare(localName: String, qName: String): String {
        val raw = localName.ifEmpty { qName }
        val colon = raw.lastIndexOf(':')
        val name = if (colon >= 0) raw.substring(colon + 1) else raw
        return name.lowercase()
    }

    private fun thin(line: List<GeoPoint>): List<GeoPoint> {
        if (line.size <= GpxImporter.SimplifyPointCount) {
            return line
        }
        val keep = DouglasPeucker.keepIndices(line, GpxImporter.SimplifyToleranceMeters)
        return line.filterIndexed { index, _ -> keep[index] }
    }
}
