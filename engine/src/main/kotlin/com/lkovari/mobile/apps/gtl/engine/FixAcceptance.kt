package com.lkovari.mobile.apps.gtl.engine

data class TrackFix(
    val timestampMillis: Long,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val speedMps: Float,
    val bearing: Float,
    val accuracyMeters: Float,
    val satellitesInFix: Int
)

data class FixFilter(
    val minDistanceMeters: Float,
    val minTimeMillis: Long,
    val minAccuracyMeters: Int,
    val minSatellites: Int
)

object FixAcceptance {
    fun shouldAccept(previous: TrackFix?, current: TrackFix, filter: FixFilter): Boolean {
        if (current.accuracyMeters > filter.minAccuracyMeters) {
            return false
        }
        if (current.satellitesInFix < filter.minSatellites) {
            return false
        }
        if (previous == null) {
            return true
        }
        val inCurve = SpeedAdaptiveSpacing.isInCurve(previous.bearing, current.bearing)
        val needed = SpeedAdaptiveSpacing.spacingMeters(current.speedMps, inCurve)
        val distance = haversineMeters(
            previous.latitude,
            previous.longitude,
            current.latitude,
            current.longitude
        )
        return distance >= needed
    }

    fun haversineMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val earthRadius = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
            kotlin.math.cos(Math.toRadians(lat1)) *
            kotlin.math.cos(Math.toRadians(lat2)) *
            kotlin.math.sin(dLng / 2) * kotlin.math.sin(dLng / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return earthRadius * c
    }
}
