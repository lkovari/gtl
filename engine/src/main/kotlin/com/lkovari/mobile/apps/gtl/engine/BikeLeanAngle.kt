package com.lkovari.mobile.apps.gtl.engine

import kotlin.math.atan

object BikeLeanAngle {
    const val StandardGravity = 9.80665
    const val MinSpeedMps = 3f
    const val MinBearingIntervalMillis = 200L
    const val MaxBearingIntervalMillis = 5_000L

    fun fromYawRate(speedMps: Float?, yawRateRadPerSec: Float?): Float? {
        if (speedMps == null || yawRateRadPerSec == null) {
            return null
        }
        if (!speedMps.isFinite() || !yawRateRadPerSec.isFinite() || speedMps < MinSpeedMps) {
            return null
        }
        val radians = atan((speedMps.toDouble() * yawRateRadPerSec.toDouble()) / StandardGravity)
        return Math.toDegrees(radians).toFloat()
    }

    fun fromBearingChange(
        speedMps: Float,
        previousBearingDegrees: Float,
        bearingDegrees: Float,
        deltaMillis: Long
    ): Float? {
        if (deltaMillis < MinBearingIntervalMillis || deltaMillis > MaxBearingIntervalMillis) {
            return null
        }
        val yaw = yawRateRadPerSec(previousBearingDegrees, bearingDegrees, deltaMillis) ?: return null
        return fromYawRate(speedMps, yaw)
    }

    fun liveDegrees(speedMps: Float?, yawRateRadPerSec: Float?, bearingLeanDegrees: Float?): Float? {
        if (yawRateRadPerSec != null) {
            return fromYawRate(speedMps, yawRateRadPerSec)
        }
        if (speedMps == null || !speedMps.isFinite() || speedMps < MinSpeedMps) {
            return null
        }
        return bearingLeanDegrees
    }

    fun yawRateRadPerSec(
        previousHeadingDegrees: Float,
        headingDegrees: Float,
        deltaMillis: Long
    ): Float? {
        if (deltaMillis <= 0L) {
            return null
        }
        if (!previousHeadingDegrees.isFinite() || !headingDegrees.isFinite()) {
            return null
        }
        val deltaDegrees = signedHeadingDeltaDegrees(previousHeadingDegrees, headingDegrees)
        val deltaRadians = Math.toRadians(deltaDegrees.toDouble())
        return (deltaRadians / (deltaMillis / 1000.0)).toFloat()
    }
}

class HeadingYawRate {
    private var anchorDegrees: Float? = null
    private var anchorMillis: Long = 0L
    private var published: Float? = null

    fun sample(headingDegrees: Float, timeMillis: Long): Float? {
        if (!headingDegrees.isFinite()) {
            return null
        }
        val anchor = anchorDegrees
        if (anchor == null) {
            anchorDegrees = headingDegrees
            anchorMillis = timeMillis
            published = null
            return null
        }
        val dt = timeMillis - anchorMillis
        if (dt < 0L || dt > MaxWindowMillis) {
            anchorDegrees = headingDegrees
            anchorMillis = timeMillis
            published = null
            return null
        }
        if (dt < MinWindowMillis) {
            return published
        }
        published = BikeLeanAngle.yawRateRadPerSec(anchor, headingDegrees, dt)
        anchorDegrees = headingDegrees
        anchorMillis = timeMillis
        return published
    }

    private companion object {
        const val MinWindowMillis = 250L
        const val MaxWindowMillis = 2_000L
    }
}

private fun signedHeadingDeltaDegrees(fromDegrees: Float, toDegrees: Float): Float {
    var delta = (toDegrees - fromDegrees) % 360f
    if (delta > 180f) {
        delta -= 360f
    }
    if (delta < -180f) {
        delta += 360f
    }
    return delta
}
