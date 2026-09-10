package com.lkovari.mobile.apps.gtl.engine

data class FixCloudSample(
    val timeMillis: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val speedMps: Float
)

data class FixCloudStats(
    val sampleCount: Int,
    val centroidLatitude: Double?,
    val centroidLongitude: Double?,
    val rmsMeters: Double?,
    val cep50Meters: Double?,
    val cep95Meters: Double?,
    val reportedAccuracyMedianMeters: Double?,
    val active: Boolean
) {
    companion object {
        val Empty = FixCloudStats(
            sampleCount = 0,
            centroidLatitude = null,
            centroidLongitude = null,
            rmsMeters = null,
            cep50Meters = null,
            cep95Meters = null,
            reportedAccuracyMedianMeters = null,
            active = true
        )
    }
}

data class FixCloudSnapshot(
    val samples: List<FixCloudSample>,
    val stats: FixCloudStats
) {
    companion object {
        val Empty = FixCloudSnapshot(emptyList(), FixCloudStats.Empty)
    }
}

class FixCloudBuffer {
    private val samples = ArrayDeque<FixCloudSample>()
    private var paused = false
    private var consecutiveStationary = 0

    fun observe(sample: FixCloudSample, pauseSpeedMps: Float) {
        if (!sample.latitude.isFinite() || !sample.longitude.isFinite()) {
            return
        }
        if (sample.speedMps >= pauseSpeedMps) {
            paused = true
            consecutiveStationary = 0
            return
        }
        if (paused) {
            consecutiveStationary += 1
            if (consecutiveStationary < StationaryResumeFixes) {
                return
            }
            samples.clear()
            paused = false
            consecutiveStationary = 0
        }
        val last = samples.lastOrNull()
        if (last != null) {
            val distance = FixAcceptance.haversineMeters(
                last.latitude,
                last.longitude,
                sample.latitude,
                sample.longitude
            )
            if (distance < MinSeparationMeters) {
                return
            }
        }
        samples.addLast(sample)
        trim(sample.timeMillis)
    }

    fun clear() {
        samples.clear()
        paused = false
        consecutiveStationary = 0
    }

    fun snapshot(): FixCloudSnapshot {
        return FixCloudSnapshot(
            samples = samples.toList(),
            stats = computeStats(samples.toList(), active = !paused)
        )
    }

    private fun trim(nowMillis: Long) {
        while (samples.size > MaxSamples) {
            samples.removeFirst()
        }
        while (samples.isNotEmpty() && nowMillis - samples.first().timeMillis > MaxAgeMillis) {
            samples.removeFirst()
        }
    }

    companion object {
        const val MaxSamples = 120
        const val MaxAgeMillis = 120_000L
        const val MinSeparationMeters = 0.15
        const val CepMinSamples = 8
        const val StationaryResumeFixes = 3

        fun computeStats(samples: List<FixCloudSample>, active: Boolean): FixCloudStats {
            if (samples.isEmpty()) {
                return FixCloudStats.Empty.copy(active = active)
            }
            val centroidLat = samples.map { it.latitude }.average()
            val centroidLon = samples.map { it.longitude }.average()
            val distances = samples.map { sample ->
                val en = GeoProjection.eastNorth(
                    centroidLat,
                    centroidLon,
                    sample.latitude,
                    sample.longitude
                )
                GeoProjection.hypot(en.first, en.second)
            }
            val meanSq = distances.map { it * it }.average()
            val rms = kotlin.math.sqrt(meanSq)
            val sorted = distances.sorted()
            val cep50 = percentile(sorted, 50)
            val cep95 = if (samples.size >= CepMinSamples) percentile(sorted, 95) else null
            val accuracies = samples.map { it.accuracyMeters }.filter { it > 0f }.sorted()
            val reported = if (accuracies.isEmpty()) {
                null
            } else {
                percentile(accuracies.map { it.toDouble() }, 50)
            }
            return FixCloudStats(
                sampleCount = samples.size,
                centroidLatitude = centroidLat,
                centroidLongitude = centroidLon,
                rmsMeters = rms,
                cep50Meters = cep50,
                cep95Meters = cep95,
                reportedAccuracyMedianMeters = reported,
                active = active
            )
        }

        private fun percentile(sorted: List<Double>, percent: Int): Double {
            if (sorted.isEmpty()) {
                return 0.0
            }
            if (sorted.size == 1) {
                return sorted[0]
            }
            val index = ((sorted.size - 1) * percent) / 100
            return sorted[index]
        }
    }
}
