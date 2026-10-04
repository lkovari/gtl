package com.lkovari.mobile.apps.gtl.engine

object TrackPreviewPolyline {
    fun encode(points: List<GeoPoint>): String {
        return points.joinToString(";") { point ->
            "${CoordinateFormat.coordinate(point.latitude)},${CoordinateFormat.coordinate(point.longitude)}"
        }
    }

    fun decode(text: String): List<GeoPoint> {
        if (text.isEmpty()) {
            return emptyList()
        }
        return text.split(';').mapNotNull { part ->
            val bits = part.split(',')
            if (bits.size != 2) {
                return@mapNotNull null
            }
            val latitude = bits[0].toDoubleOrNull() ?: return@mapNotNull null
            val longitude = bits[1].toDoubleOrNull() ?: return@mapNotNull null
            GeoPoint(latitude, longitude)
        }
    }
}
