package com.lkovari.mobile.apps.gtl.engine

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

sealed class TargetPointer {
    data object Ring : TargetPointer()

    data class Aim(
        val relativeDegrees: Float,
        val dimmed: Boolean
    ) : TargetPointer()

    companion object {
        fun initialBearingDegrees(
            fromLatitude: Double,
            fromLongitude: Double,
            toLatitude: Double,
            toLongitude: Double
        ): Double? {
            if (!coordsFinite(fromLatitude, fromLongitude, toLatitude, toLongitude)) {
                return null
            }
            val lat1 = Math.toRadians(fromLatitude)
            val lat2 = Math.toRadians(toLatitude)
            val dLon = Math.toRadians(toLongitude - fromLongitude)
            val y = sin(dLon) * cos(lat2)
            val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
            val degrees = Math.toDegrees(atan2(y, x))
            return (degrees + 360.0) % 360.0
        }

        fun resolve(
            fromLatitude: Double,
            fromLongitude: Double,
            toLatitude: Double,
            toLongitude: Double,
            distanceMeters: Double,
            accuracyMeters: Float?,
            courseDegrees: Float?,
            speedMps: Float?,
            magneticHeading: Float?,
            declinationDegrees: Float?,
            compassAccuracy: Int
        ): TargetPointer? {
            if (!distanceMeters.isFinite() || distanceMeters < 0.0) {
                return null
            }
            if (!coordsFinite(fromLatitude, fromLongitude, toLatitude, toLongitude)) {
                return null
            }
            val accuracy = accuracyMeters
                ?.takeIf { it.isFinite() && it >= 0f }
                ?.toDouble()
                ?: 0.0
            if (distanceMeters <= max(accuracy, DeadZoneFloorMeters)) {
                return Ring
            }
            val travel = travelHeading(
                courseDegrees,
                speedMps,
                magneticHeading,
                declinationDegrees,
                compassAccuracy
            ) ?: return null
            val target = initialBearingDegrees(
                fromLatitude,
                fromLongitude,
                toLatitude,
                toLongitude
            ) ?: return null
            return Aim(
                relativeDegrees = CompassHeading.wrapDegrees(target.toFloat() - travel.degrees),
                dimmed = travel.dimmed
            )
        }

        private fun travelHeading(
            courseDegrees: Float?,
            speedMps: Float?,
            magneticHeading: Float?,
            declinationDegrees: Float?,
            compassAccuracy: Int
        ): TravelHeading? {
            val course = courseDegrees?.takeIf { it.isFinite() }
            val speed = speedMps?.takeIf { it.isFinite() }
            if (course != null && speed != null && speed >= CourseSpeedMps) {
                return TravelHeading(CompassHeading.wrapDegrees(course), dimmed = false)
            }
            val magnetic = magneticHeading?.takeIf { it.isFinite() } ?: return null
            val shift = declinationDegrees?.takeIf { it.isFinite() }
            val degrees = if (shift == null) {
                CompassHeading.wrapDegrees(magnetic)
            } else {
                CompassHeading.wrapDegrees(magnetic + shift)
            }
            val dimmed = shift == null || CompassHeading.needsFigureEight(compassAccuracy)
            return TravelHeading(degrees, dimmed)
        }

        private fun coordsFinite(
            fromLatitude: Double,
            fromLongitude: Double,
            toLatitude: Double,
            toLongitude: Double
        ): Boolean {
            return fromLatitude.isFinite() &&
                fromLongitude.isFinite() &&
                toLatitude.isFinite() &&
                toLongitude.isFinite()
        }

        private const val CourseSpeedMps = 1f
        private const val DeadZoneFloorMeters = 20.0
    }
}

private data class TravelHeading(
    val degrees: Float,
    val dimmed: Boolean
)
