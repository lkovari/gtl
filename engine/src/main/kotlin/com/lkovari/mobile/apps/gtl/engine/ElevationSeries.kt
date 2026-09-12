package com.lkovari.mobile.apps.gtl.engine

data class ElevationPoint(
    val latitude: Double,
    val longitude: Double,
    val gpsAltitude: Double,
    val baroAltitude: Double?
)

data class ElevationSample(
    val distanceMeters: Double,
    val gpsAltitude: Double,
    val baroAltitude: Double?
)

object ElevationSeries {
    const val DefaultMaxPoints = 200

    fun fromPoints(points: List<ElevationPoint>): List<ElevationSample> {
        if (points.isEmpty()) {
            return emptyList()
        }
        var distance = 0.0
        val samples = ArrayList<ElevationSample>(points.size)
        val first = points.first()
        samples.add(ElevationSample(0.0, first.gpsAltitude, first.baroAltitude))
        for (index in 1 until points.size) {
            val previous = points[index - 1]
            val current = points[index]
            distance += FixAcceptance.haversineMeters(
                previous.latitude,
                previous.longitude,
                current.latitude,
                current.longitude
            )
            samples.add(ElevationSample(distance, current.gpsAltitude, current.baroAltitude))
        }
        return samples
    }

    fun downsample(samples: List<ElevationSample>, maxPoints: Int = DefaultMaxPoints): List<ElevationSample> {
        if (samples.size <= maxPoints || maxPoints < 2) {
            return samples
        }
        val lastIndex = samples.lastIndex
        val stride = lastIndex.toDouble() / (maxPoints - 1).toDouble()
        val out = ArrayList<ElevationSample>(maxPoints)
        for (index in 0 until maxPoints) {
            val source = (index * stride).toInt().coerceAtMost(lastIndex)
            out.add(samples[source])
        }
        out[out.lastIndex] = samples.last()
        return out
    }

    fun hasBaroLine(samples: List<ElevationSample>): Boolean {
        return samples.count { it.baroAltitude != null } >= 2
    }
}
