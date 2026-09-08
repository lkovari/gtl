package com.lkovari.mobile.apps.gtl.engine

object DouglasPeucker {
    const val MinToleranceMeters = 1.0
    const val MaxToleranceMeters = 20.0
    const val DefaultToleranceMeters = 19.5

    fun clampTolerance(value: Double): Double {
        val clamped = value.coerceIn(MinToleranceMeters, MaxToleranceMeters)
        return kotlin.math.round(clamped)
    }

    fun simplify(points: List<GeoPoint>, toleranceMeters: Double): List<GeoPoint> {
        if (points.size < 3) {
            return points
        }
        val keep = BooleanArray(points.size)
        keep[0] = true
        keep[points.lastIndex] = true
        simplifyRange(points, 0, points.lastIndex, toleranceMeters, keep)
        return points.filterIndexed { index, _ -> keep[index] }
    }

    private fun simplifyRange(
        points: List<GeoPoint>,
        first: Int,
        last: Int,
        toleranceMeters: Double,
        keep: BooleanArray
    ) {
        var maxDistance = 0.0
        var farthest = first
        val start = points[first]
        val end = points[last]
        for (index in first + 1 until last) {
            val distance = perpendicularDistanceMeters(start, end, points[index])
            if (distance > maxDistance) {
                maxDistance = distance
                farthest = index
            }
        }
        if (maxDistance > toleranceMeters && farthest != first) {
            keep[farthest] = true
            simplifyRange(points, first, farthest, toleranceMeters, keep)
            simplifyRange(points, farthest, last, toleranceMeters, keep)
        }
    }

    private fun perpendicularDistanceMeters(start: GeoPoint, end: GeoPoint, point: GeoPoint): Double {
        val startX = 0.0
        val startY = 0.0
        val endXY = projectMeters(start, end)
        val pointXY = projectMeters(start, point)
        val dx = endXY.first - startX
        val dy = endXY.second - startY
        val lengthSquared = dx * dx + dy * dy
        if (lengthSquared == 0.0) {
            return GeoProjection.hypot(pointXY.first, pointXY.second)
        }
        val t = ((pointXY.first - startX) * dx + (pointXY.second - startY) * dy) / lengthSquared
        val projX = startX + t * dx
        val projY = startY + t * dy
        return GeoProjection.hypot(pointXY.first - projX, pointXY.second - projY)
    }

    private fun projectMeters(origin: GeoPoint, point: GeoPoint): Pair<Double, Double> {
        return GeoProjection.eastNorth(
            origin.latitude,
            origin.longitude,
            point.latitude,
            point.longitude
        )
    }
}
