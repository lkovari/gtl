package com.lkovari.mobile.apps.gtl.engine

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max

data class PreviewPoint(
    val x: Float,
    val y: Float
)

data class SavedTrackCard(
    val odometerMeters: Double,
    val elapsedMillis: Long,
    val averageSpeedMps: Float,
    val maxSpeedMps: Float,
    val preview: List<GeoPoint>
)

object SavedTrackCards {
    fun from(samples: List<TrackSample>, previewPoints: Int = 64): SavedTrackCard {
        val stats = TrackStatsCalculator.compute(samples)
        return SavedTrackCard(
            odometerMeters = stats.odometerMeters,
            elapsedMillis = stats.elapsedMillis,
            averageSpeedMps = stats.averageSpeedMps,
            maxSpeedMps = stats.maxSpeedMps,
            preview = TrackPreview.points(samples, previewPoints)
        )
    }
}

object TrackPreview {
    fun points(samples: List<TrackSample>, maxPoints: Int): List<GeoPoint> {
        if (samples.isEmpty() || maxPoints <= 0) {
            return emptyList()
        }
        if (samples.size <= maxPoints) {
            return samples.map { GeoPoint(it.latitude, it.longitude) }
        }
        if (maxPoints == 1) {
            val first = samples.first()
            return listOf(GeoPoint(first.latitude, first.longitude))
        }
        val last = samples.lastIndex
        return (0 until maxPoints).map { step ->
            val index = (step.toLong() * last / (maxPoints - 1)).toInt()
            val sample = samples[index]
            GeoPoint(sample.latitude, sample.longitude)
        }
    }

    fun layout(points: List<GeoPoint>): List<PreviewPoint> {
        if (points.isEmpty()) {
            return emptyList()
        }
        if (points.size == 1) {
            return listOf(PreviewPoint(0.5f, 0.5f))
        }
        val midLat = points.sumOf { it.latitude } / points.size
        val cosLat = cos(midLat * PI / 180.0).coerceAtLeast(0.2)
        val xs = points.map { it.longitude * cosLat }
        val ys = points.map { it.latitude }
        val minX = xs.min()
        val maxX = xs.max()
        val minY = ys.min()
        val maxY = ys.max()
        if (maxX - minX < 1e-9 && maxY - minY < 1e-9) {
            return List(points.size) { PreviewPoint(0.5f, 0.5f) }
        }
        val spanX = (maxX - minX).coerceAtLeast(1e-12)
        val spanY = (maxY - minY).coerceAtLeast(1e-12)
        val scale = 1.0 / max(spanX, spanY)
        val offsetX = (1.0 - spanX * scale) / 2.0
        val offsetY = (1.0 - spanY * scale) / 2.0
        return points.indices.map { index ->
            val x = offsetX + (xs[index] - minX) * scale
            val y = 1.0 - (offsetY + (ys[index] - minY) * scale)
            PreviewPoint(x.toFloat(), y.toFloat())
        }
    }
}
