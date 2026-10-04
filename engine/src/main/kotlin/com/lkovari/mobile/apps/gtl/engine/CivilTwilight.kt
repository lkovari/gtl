package com.lkovari.mobile.apps.gtl.engine

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object CivilTwilight {
    const val NightAltitudeDegrees = -6.0

    fun sunAltitudeDegrees(latitude: Double, longitude: Double, at: Instant): Double {
        val lw = RAD * -longitude
        val phi = RAD * latitude
        val days = toDays(at)
        val coords = sunCoords(days)
        val hourAngle = siderealTime(days, lw) - coords.rightAscension
        val sine = sin(phi) * sin(coords.declination) +
            cos(phi) * cos(coords.declination) * cos(hourAngle)
        return Math.toDegrees(asin(sine.coerceIn(-1.0, 1.0)))
    }

    fun isNight(latitude: Double, longitude: Double, at: Instant): Boolean {
        return sunAltitudeDegrees(latitude, longitude, at) < NightAltitudeDegrees
    }

    fun eveningCivilDusk(
        latitude: Double,
        longitude: Double,
        day: LocalDate,
        zone: ZoneId
    ): Instant? {
        val start = day.atTime(12, 0).atZone(zone).toInstant()
        val end = day.plusDays(1).atStartOfDay(zone).toInstant()
        if (isNight(latitude, longitude, start) == isNight(latitude, longitude, end)) {
            return null
        }
        var low = start.toEpochMilli()
        var high = end.toEpochMilli()
        repeat(48) {
            val mid = (low + high) / 2
            if (isNight(latitude, longitude, Instant.ofEpochMilli(mid))) {
                high = mid
            } else {
                low = mid
            }
        }
        return Instant.ofEpochMilli(high)
    }

    private fun toDays(at: Instant): Double {
        val julian = at.toEpochMilli() / DAY_MS - 0.5 + J1970
        return julian - J2000
    }

    private fun sunCoords(days: Double): SunCoords {
        val anomaly = RAD * (357.5291 + 0.98560028 * days)
        val center = RAD * (1.9148 * sin(anomaly) + 0.02 * sin(2 * anomaly) + 0.0003 * sin(3 * anomaly))
        val longitude = anomaly + center + RAD * 102.9372 + PI
        return SunCoords(
            declination = declination(longitude),
            rightAscension = rightAscension(longitude)
        )
    }

    private fun rightAscension(eclipticLongitude: Double): Double {
        return atan2(sin(eclipticLongitude) * cos(OBLIQUITY), cos(eclipticLongitude))
    }

    private fun declination(eclipticLongitude: Double): Double {
        return asin(sin(eclipticLongitude) * sin(OBLIQUITY))
    }

    private fun siderealTime(days: Double, longitudeWest: Double): Double {
        return RAD * (280.16 + 360.9856235 * days) - longitudeWest
    }

    private data class SunCoords(
        val declination: Double,
        val rightAscension: Double
    )

    private const val RAD = PI / 180.0
    private const val DAY_MS = 86_400_000.0
    private const val J1970 = 2_440_588.0
    private const val J2000 = 2_451_545.0
    private const val OBLIQUITY = RAD * 23.4397
}
