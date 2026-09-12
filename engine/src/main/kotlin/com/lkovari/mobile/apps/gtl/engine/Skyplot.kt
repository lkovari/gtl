package com.lkovari.mobile.apps.gtl.engine

import kotlin.math.cos
import kotlin.math.sin

data class SkyplotPoint(
    val x: Float,
    val y: Float
)

data class SkyplotMarker(
    val constellation: GnssConstellation,
    val svid: Int,
    val azimuthDegrees: Float,
    val elevationDegrees: Float,
    val usedInFix: Boolean,
    val hasL5: Boolean,
    val cn0DbHz: Float
)

object SkyplotProjection {
    fun offset(
        azimuthDegrees: Float,
        elevationDegrees: Float,
        centerX: Float,
        centerY: Float,
        radius: Float
    ): SkyplotPoint? {
        if (!azimuthDegrees.isFinite() || !elevationDegrees.isFinite()) {
            return null
        }
        if (elevationDegrees < 0f) {
            return null
        }
        val elevation = elevationDegrees.coerceAtMost(90f)
        val azimuthRadians = Math.toRadians(azimuthDegrees.toDouble())
        val radial = ((90.0 - elevation) / 90.0) * radius
        return SkyplotPoint(
            x = centerX + (radial * sin(azimuthRadians)).toFloat(),
            y = centerY - (radial * cos(azimuthRadians)).toFloat()
        )
    }
}

object SkyplotMarkers {
    fun from(samples: List<SatelliteSample>): List<SkyplotMarker> {
        val grouped = LinkedHashMap<Pair<GnssConstellation, Int>, MutableList<SatelliteSample>>()
        for (sample in samples) {
            if (!sample.azimuthDegrees.isFinite() || !sample.elevationDegrees.isFinite()) {
                continue
            }
            if (sample.elevationDegrees < 0f) {
                continue
            }
            val key = sample.constellation to sample.svid
            grouped.getOrPut(key) { mutableListOf() }.add(sample)
        }
        return grouped.map { (key, group) ->
            val geometry = group.firstOrNull { it.usedInFix } ?: group.first()
            SkyplotMarker(
                constellation = key.first,
                svid = key.second,
                azimuthDegrees = geometry.azimuthDegrees,
                elevationDegrees = geometry.elevationDegrees,
                usedInFix = group.any { it.usedInFix },
                hasL5 = group.any { GnssClassifier.gpsBand(it.carrierFrequencyHz) == GpsBand.L5 },
                cn0DbHz = group.maxOf { it.cn0DbHz }
            )
        }
    }
}
