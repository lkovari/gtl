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
    fun shouldAccept(
        previous: TrackFix?,
        current: TrackFix,
        filter: FixFilter,
        density: RecordingDensity = RecordingDensity.SMART,
        usage: UsageType? = null
    ): Boolean {
        return shouldAccept(previous, current, filter, density.sliderValue(), usage)
    }

    fun shouldAccept(
        previous: TrackFix?,
        current: TrackFix,
        filter: FixFilter,
        densityMix: Float,
        usage: UsageType? = null
    ): Boolean {
        if (current.accuracyMeters > filter.minAccuracyMeters) {
            return false
        }
        if (current.satellitesInFix < filter.minSatellites) {
            return false
        }
        if (previous == null) {
            return true
        }
        val distance = haversineMeters(
            previous.latitude,
            previous.longitude,
            current.latitude,
            current.longitude
        )
        val inCurve = SpeedAdaptiveSpacing.isInCurve(previous, current)
        val t = densityMix.coerceIn(0f, 1f)
        val smartNeeded = SpeedAdaptiveSpacing.spacingMeters(current.speedMps, inCurve, usage)
        val everyFixMin = everyFixMinDistanceMeters(usage)
        if (t <= 0.001f) {
            return distance >= smartNeeded
        }
        if (t >= 0.999f) {
            if (distance < everyFixMin) {
                return false
            }
            val elapsed = current.timestampMillis - previous.timestampMillis
            return elapsed >= filter.minTimeMillis || inCurve
        }
        val needed = smartNeeded * (1f - t) + everyFixMin.toFloat() * t
        if (distance >= needed) {
            return true
        }
        if (distance >= everyFixMin) {
            val elapsed = current.timestampMillis - previous.timestampMillis
            return elapsed >= filter.minTimeMillis || inCurve
        }
        return false
    }

    fun everyFixMinDistanceMeters(usage: UsageType?): Double {
        return if (usage != null && usage.isPedestrianMode()) {
            PedestrianEveryFixMinDistanceMeters
        } else {
            EveryFixMinDistanceMeters
        }
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

    private const val EveryFixMinDistanceMeters = 1.0
    private const val PedestrianEveryFixMinDistanceMeters = 0.5
}
