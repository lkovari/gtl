package com.lkovari.mobile.apps.gtl.engine

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.round

enum class QnhOrigin {
    MODEL,
    METAR,
    MANUAL
}

data class QnhMetar(
    val altimHpa: Double?,
    val observedAtMillis: Long?,
    val latitude: Double,
    val longitude: Double,
    val icao: String
)

data class QnhChoice(
    val hpa: Int,
    val origin: QnhOrigin,
    val icao: String?,
    val distanceKm: Int?
)

data class QnhBbox(
    val minLat: Double,
    val minLon: Double,
    val maxLat: Double,
    val maxLon: Double
) {
    fun query(): String {
        return listOf(minLat, minLon, maxLat, maxLon).joinToString(",") { value ->
            String.format(Locale.ROOT, "%.3f", value)
        }
    }
}

object QnhSelection {
    const val GroundMetarRadiusKm = 30.0
    const val AircraftMetarRadiusKm = 100.0
    const val FallbackMetarRadiusKm = 100.0
    const val MaxMetarAgeMillis = 90L * 60L * 1000L
    const val BboxDeltaDegrees = 0.5
    const val BboxExpandedDeltaDegrees = 1.0

    fun roundCoordinate(value: Double): Double {
        if (!value.isFinite()) {
            return Double.NaN
        }
        return BigDecimal.valueOf(value).setScale(3, RoundingMode.HALF_UP).toDouble()
    }

    fun formatCoordinate(value: Double): String {
        return String.format(Locale.ROOT, "%.3f", roundCoordinate(value))
    }

    fun bbox(latitude: Double, longitude: Double, deltaDegrees: Double): QnhBbox {
        return QnhBbox(
            minLat = latitude - deltaDegrees,
            minLon = longitude - deltaDegrees,
            maxLat = latitude + deltaDegrees,
            maxLon = longitude + deltaDegrees
        )
    }

    fun roundedQnh(hpa: Float): Int {
        val clamped = BaroAltitude.clampQnh(hpa)
        return round(clamped).toInt().coerceIn(BaroAltitude.MinQnhHpa.toInt(), BaroAltitude.MaxQnhHpa.toInt())
    }

    fun mayApplyAutomatic(manual: Boolean, recording: Boolean): Boolean {
        return !manual && !recording
    }

    fun select(
        modelHpa: Double?,
        metars: List<QnhMetar>,
        latitude: Double,
        longitude: Double,
        nowMillis: Long,
        usage: UsageType
    ): QnhChoice? {
        if (!latitude.isFinite() || !longitude.isFinite()) {
            return null
        }
        val eligible = metars.mapNotNull { metar ->
            val altim = metar.altimHpa
            val observed = metar.observedAtMillis
            if (altim == null || !altim.isFinite() || observed == null) {
                return@mapNotNull null
            }
            if (!metar.latitude.isFinite() || !metar.longitude.isFinite()) {
                return@mapNotNull null
            }
            if (nowMillis - observed > MaxMetarAgeMillis) {
                return@mapNotNull null
            }
            val meters = FixAcceptance.haversineMeters(
                latitude,
                longitude,
                metar.latitude,
                metar.longitude
            )
            EligibleMetar(metar, altim, meters)
        }
        val nearest = eligible.minByOrNull { it.meters }
        val preferredKm = if (usage == UsageType.AIRCRAFT) {
            AircraftMetarRadiusKm
        } else {
            GroundMetarRadiusKm
        }
        val preferred = nearest?.takeIf { it.meters <= preferredKm * 1_000.0 }
        if (preferred != null) {
            return metarChoice(preferred)
        }
        val model = modelHpa?.takeIf { it.isFinite() }
        if (model != null) {
            return QnhChoice(
                hpa = roundedQnh(model.toFloat()),
                origin = QnhOrigin.MODEL,
                icao = null,
                distanceKm = null
            )
        }
        val fallback = nearest?.takeIf { it.meters <= FallbackMetarRadiusKm * 1_000.0 }
        if (fallback != null) {
            return metarChoice(fallback)
        }
        return null
    }

    private fun metarChoice(metar: EligibleMetar): QnhChoice {
        val icao = metar.report.icao.trim().takeIf { it.isNotEmpty() }
        return QnhChoice(
            hpa = roundedQnh(metar.altimHpa.toFloat()),
            origin = QnhOrigin.METAR,
            icao = icao,
            distanceKm = round(metar.meters / 1_000.0).toInt()
        )
    }

    private data class EligibleMetar(
        val report: QnhMetar,
        val altimHpa: Double,
        val meters: Double
    )
}
