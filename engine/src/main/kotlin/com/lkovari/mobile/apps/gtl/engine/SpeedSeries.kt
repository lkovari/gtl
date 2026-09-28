package com.lkovari.mobile.apps.gtl.engine

data class SpeedSample(
    val distanceMeters: Double,
    val speedMps: Float?
)

object SpeedSeries {
    const val DefaultMaxPoints = 160

    fun fromVertices(vertices: List<TrackVertex>): List<SpeedSample> {
        if (vertices.isEmpty()) {
            return emptyList()
        }
        var distance = 0.0
        val samples = ArrayList<SpeedSample>(vertices.size)
        val first = vertices.first()
        samples.add(SpeedSample(0.0, finiteSpeed(first.speedMps)))
        for (index in 1 until vertices.size) {
            val previous = vertices[index - 1]
            val current = vertices[index]
            distance += FixAcceptance.haversineMeters(
                previous.latitude,
                previous.longitude,
                current.latitude,
                current.longitude
            )
            samples.add(SpeedSample(distance, finiteSpeed(current.speedMps)))
        }
        return samples
    }

    fun downsample(samples: List<SpeedSample>, maxPoints: Int = DefaultMaxPoints): List<SpeedSample> {
        if (samples.size <= maxPoints || maxPoints < 2) {
            return samples
        }
        val lastIndex = samples.lastIndex
        val stride = lastIndex.toDouble() / (maxPoints - 1).toDouble()
        val out = ArrayList<SpeedSample>(maxPoints)
        for (index in 0 until maxPoints) {
            val source = (index * stride).toInt().coerceAtMost(lastIndex)
            out.add(samples[source])
        }
        out[out.lastIndex] = samples.last()
        return out
    }

    private fun finiteSpeed(speedMps: Float?): Float? {
        if (speedMps == null || !speedMps.isFinite()) {
            return null
        }
        return speedMps
    }
}
