package com.lkovari.mobile.apps.gtl.engine

object GeoProjection {
    const val MetersPerDegLat = 111_320.0

    fun metersPerDegLng(latitudeDeg: Double): Double {
        return MetersPerDegLat * kotlin.math.cos(Math.toRadians(latitudeDeg))
    }

    fun eastNorth(
        originLat: Double,
        originLon: Double,
        lat: Double,
        lon: Double
    ): Pair<Double, Double> {
        val east = (lon - originLon) * metersPerDegLng(originLat)
        val north = (lat - originLat) * MetersPerDegLat
        return east to north
    }

    fun latLon(
        originLat: Double,
        originLon: Double,
        east: Double,
        north: Double
    ): Pair<Double, Double> {
        val lat = originLat + north / MetersPerDegLat
        val lon = originLon + east / metersPerDegLng(originLat)
        return lat to lon
    }

    fun hypot(x: Double, y: Double): Double = kotlin.math.sqrt(x * x + y * y)
}
