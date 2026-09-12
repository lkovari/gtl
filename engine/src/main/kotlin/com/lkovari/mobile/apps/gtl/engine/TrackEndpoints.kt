package com.lkovari.mobile.apps.gtl.engine

object TrackEndpoints {
    fun start(points: List<GeoPoint>): GeoPoint? {
        return points.firstOrNull()
    }

    fun end(points: List<GeoPoint>, logging: Boolean): GeoPoint? {
        if (logging || points.size < 2) {
            return null
        }
        return points.last()
    }
}
