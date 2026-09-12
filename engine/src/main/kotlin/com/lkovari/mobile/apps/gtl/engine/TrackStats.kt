package com.lkovari.mobile.apps.gtl.engine

data class TrackSample(
    val timestampMillis: Long,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val speedMps: Float,
    val bearing: Float,
    val ambientTemperature: Float?,
    val eventKind: EventKind
)

data class TemperatureRange(
    val minCelsius: Float,
    val maxCelsius: Float
)

data class TrackStats(
    val pointCount: Int,
    val odometerMeters: Double,
    val elapsedMillis: Long,
    val movingMillis: Long,
    val waitingMillis: Long,
    val maxSpeedMps: Float,
    val averageSpeedMps: Float,
    val maxAltitude: Double,
    val minAltitude: Double,
    val temperatureRange: TemperatureRange?
)

object TrackStatsCalculator {
    private const val MOVING_SPEED_MPS = 0.5f

    fun compute(samples: List<TrackSample>): TrackStats {
        if (samples.isEmpty()) {
            return TrackStats(
                pointCount = 0,
                odometerMeters = 0.0,
                elapsedMillis = 0L,
                movingMillis = 0L,
                waitingMillis = 0L,
                maxSpeedMps = 0f,
                averageSpeedMps = 0f,
                maxAltitude = 0.0,
                minAltitude = 0.0,
                temperatureRange = null
            )
        }
        var odometer = 0.0
        var moving = 0L
        var waiting = 0L
        for (index in 1 until samples.size) {
            val previous = samples[index - 1]
            val current = samples[index]
            odometer += FixAcceptance.haversineMeters(
                previous.latitude,
                previous.longitude,
                current.latitude,
                current.longitude
            )
            val dt = (current.timestampMillis - previous.timestampMillis).coerceAtLeast(0L)
            if (current.speedMps >= MOVING_SPEED_MPS) {
                moving += dt
            } else {
                waiting += dt
            }
        }
        val elapsed = samples.last().timestampMillis - samples.first().timestampMillis
        val temps = samples.mapNotNull { it.ambientTemperature }
        val temperatureRange = if (temps.isEmpty()) {
            null
        } else {
            TemperatureRange(temps.min(), temps.max())
        }
        val altitudes = samples.map { it.altitude }
        val speeds = samples.map { it.speedMps }
        val movingElapsedSeconds = moving / 1000.0
        val average = if (movingElapsedSeconds > 0) {
            (odometer / movingElapsedSeconds).toFloat()
        } else {
            0f
        }
        return TrackStats(
            pointCount = samples.size,
            odometerMeters = odometer,
            elapsedMillis = elapsed.coerceAtLeast(0L),
            movingMillis = moving,
            waitingMillis = waiting,
            maxSpeedMps = speeds.max(),
            averageSpeedMps = average,
            maxAltitude = altitudes.max(),
            minAltitude = altitudes.min(),
            temperatureRange = temperatureRange
        )
    }

    fun cumulativeOdometerMeters(points: List<GeoPoint>): List<Double> {
        if (points.isEmpty()) {
            return emptyList()
        }
        val distances = ArrayList<Double>(points.size)
        var odometer = 0.0
        distances.add(0.0)
        for (index in 1 until points.size) {
            val previous = points[index - 1]
            val current = points[index]
            odometer += FixAcceptance.haversineMeters(
                previous.latitude,
                previous.longitude,
                current.latitude,
                current.longitude
            )
            distances.add(odometer)
        }
        return distances
    }
}
