package com.lkovari.mobile.apps.gtl.engine

data class LatLonBounds(
    val minLatitude: Double,
    val minLongitude: Double,
    val maxLatitude: Double,
    val maxLongitude: Double
) {
    val isDegenerate: Boolean
        get() = minLatitude == maxLatitude && minLongitude == maxLongitude
}

object TrackCameraBounds {
    fun of(points: List<GeoPoint>, extra: GeoPoint?): LatLonBounds? {
        if (points.isEmpty() && extra == null) {
            return null
        }
        var minLat = Double.POSITIVE_INFINITY
        var minLon = Double.POSITIVE_INFINITY
        var maxLat = Double.NEGATIVE_INFINITY
        var maxLon = Double.NEGATIVE_INFINITY
        points.forEach { point ->
            minLat = minOf(minLat, point.latitude)
            minLon = minOf(minLon, point.longitude)
            maxLat = maxOf(maxLat, point.latitude)
            maxLon = maxOf(maxLon, point.longitude)
        }
        if (extra != null) {
            minLat = minOf(minLat, extra.latitude)
            minLon = minOf(minLon, extra.longitude)
            maxLat = maxOf(maxLat, extra.latitude)
            maxLon = maxOf(maxLon, extra.longitude)
        }
        return LatLonBounds(minLat, minLon, maxLat, maxLon)
    }
}
