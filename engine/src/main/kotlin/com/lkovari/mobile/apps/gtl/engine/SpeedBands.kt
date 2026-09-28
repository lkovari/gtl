package com.lkovari.mobile.apps.gtl.engine

enum class SpeedBand {
    Slow,
    Brisk,
    Mid,
    Fast,
    Rapid,
    High,
    Max,
    Unknown
}

object SpeedBands {
    val CasingArgb: Int = 0xFFF4F1EA.toInt()
    const val CasingWidthPx: Float = 14f
    const val CoreWidthPx: Float = 8f

    private const val MpsPerKmh = 3.6f
    private const val MpsPerMph = 2.2369363f
    private const val MpsPerKt = 1.9438445f

    fun edges(usage: UsageType, system: MeasurementSystem): List<Int> {
        return when (system) {
            MeasurementSystem.METRIC -> metricEdges(usage)
            MeasurementSystem.IMPERIAL -> imperialEdges(usage)
            MeasurementSystem.ICAO -> icaoEdges(usage)
        }
    }

    fun of(speedMps: Float?, usage: UsageType, system: MeasurementSystem): SpeedBand {
        if (speedMps == null || !speedMps.isFinite()) {
            return SpeedBand.Unknown
        }
        val limits = edges(usage, system)
        for (index in limits.indices) {
            if (speedMps <= edgeMps(limits[index], system)) {
                return bands[index]
            }
        }
        return SpeedBand.Max
    }

    fun argb(band: SpeedBand): Int {
        return when (band) {
            SpeedBand.Slow -> 0xFF0B6B66.toInt()
            SpeedBand.Brisk -> 0xFF5B2D86.toInt()
            SpeedBand.Mid -> 0xFF7A5E00.toInt()
            SpeedBand.Fast -> 0xFFC13B2E.toInt()
            SpeedBand.Rapid -> 0xFFA84300.toInt()
            SpeedBand.High -> 0xFF1B5E20.toInt()
            SpeedBand.Max -> 0xFF2A2118.toInt()
            SpeedBand.Unknown -> 0xFF5C6B73.toInt()
        }
    }

    private val bands = listOf(
        SpeedBand.Slow,
        SpeedBand.Brisk,
        SpeedBand.Mid,
        SpeedBand.Fast,
        SpeedBand.Rapid,
        SpeedBand.High
    )

    private fun metricEdges(usage: UsageType): List<Int> {
        return when (usage) {
            UsageType.RUNNER, UsageType.WALKING_HIKE, UsageType.PEDESTRIAN ->
                listOf(4, 7, 10, 13, 16, 20)
            UsageType.BICYCLE -> listOf(8, 15, 22, 28, 35, 45)
            UsageType.FOUR_WHEELERS -> listOf(6, 15, 40, 80, 110, 130)
            UsageType.TWO_WHEELERS -> listOf(20, 50, 90, 130, 160, 200)
            UsageType.WATERCRAFT -> listOf(6, 12, 20, 30, 45, 60)
            UsageType.AIRCRAFT -> listOf(70, 120, 160, 200, 250, 300)
        }
    }

    private fun imperialEdges(usage: UsageType): List<Int> {
        return when (usage) {
            UsageType.RUNNER, UsageType.WALKING_HIKE, UsageType.PEDESTRIAN ->
                listOf(2, 4, 6, 8, 10, 12)
            UsageType.BICYCLE -> listOf(5, 9, 14, 17, 22, 28)
            UsageType.FOUR_WHEELERS -> listOf(4, 9, 25, 50, 70, 80)
            UsageType.TWO_WHEELERS -> listOf(12, 30, 55, 80, 100, 125)
            UsageType.WATERCRAFT -> listOf(4, 8, 12, 19, 28, 37)
            UsageType.AIRCRAFT -> listOf(45, 75, 100, 125, 155, 185)
        }
    }

    private fun icaoEdges(usage: UsageType): List<Int> {
        return when (usage) {
            UsageType.RUNNER, UsageType.WALKING_HIKE, UsageType.PEDESTRIAN ->
                listOf(2, 4, 5, 7, 9, 11)
            UsageType.BICYCLE -> listOf(4, 8, 12, 15, 19, 24)
            UsageType.FOUR_WHEELERS -> listOf(3, 8, 22, 43, 60, 70)
            UsageType.TWO_WHEELERS -> listOf(11, 27, 49, 70, 86, 108)
            UsageType.WATERCRAFT -> listOf(3, 6, 11, 16, 24, 32)
            UsageType.AIRCRAFT -> listOf(40, 65, 85, 110, 135, 160)
        }
    }

    private fun edgeMps(edge: Int, system: MeasurementSystem): Float {
        val value = edge.toFloat()
        return when (system) {
            MeasurementSystem.METRIC -> value / MpsPerKmh
            MeasurementSystem.IMPERIAL -> value / MpsPerMph
            MeasurementSystem.ICAO -> value / MpsPerKt
        }
    }
}
