package com.lkovari.mobile.apps.gtl.engine

import kotlin.math.atan2
import kotlin.math.max

class KalmanTrackFilter {
    private var initialized = false
    private var originLat = 0.0
    private var originLon = 0.0
    private val x = DoubleArray(4)
    private var p = Array(4) { DoubleArray(4) }
    private var lastTimestampMillis = 0L
    private var lastOutput: TrackFix? = null

    fun seedFrom(fix: TrackFix) {
        originLat = fix.latitude
        originLon = fix.longitude
        initializeAt(fix)
        lastOutput = fix
    }

    fun observe(
        fix: TrackFix,
        usage: UsageType,
        strength: SmoothingStrength,
        stationaryLock: Boolean
    ): TrackFix {
        return observe(fix, usage, strength.sliderValue(), stationaryLock)
    }

    fun observe(
        fix: TrackFix,
        usage: UsageType,
        strength: Float,
        stationaryLock: Boolean
    ): TrackFix {
        if (!fix.latitude.isFinite() || !fix.longitude.isFinite()) {
            val previous = lastOutput
            if (previous != null) {
                return previous
            }
            return fix
        }
        if (!initialized) {
            originLat = fix.latitude
            originLon = fix.longitude
            initializeAt(fix)
            return emit(fix, usage, stationaryLock)
        }
        val dtRaw = (fix.timestampMillis - lastTimestampMillis) / 1000.0
        val dt = dtRaw.coerceIn(MinDtSeconds, MaxDtSeconds)
        lastTimestampMillis = fix.timestampMillis
        var q = usage.processNoiseQ() * SmoothingStrength.processNoiseMultiplier(strength)
        val previous = lastOutput
        if (previous != null && isTurning(previous, fix)) {
            q *= usage.turnBoost()
        }
        predict(dt, q)
        if (usage.isPedestrianMode()) {
            val extra = q * dt * dt
            p[0][0] += extra
            p[1][1] += extra
        }
        val measured = GeoProjection.eastNorth(originLat, originLon, fix.latitude, fix.longitude)
        val innovEast = measured.first - x[0]
        val innovNorth = measured.second - x[1]
        val innovation = GeoProjection.hypot(innovEast, innovNorth)
        val jumpThreshold = max(50.0, 8.0 * fix.accuracyMeters)
        if (innovation > jumpThreshold) {
            initializeAt(fix)
            return emit(fix, usage, stationaryLock)
        }
        update(measured.first, measured.second, max(fix.accuracyMeters.toDouble(), MinSigmaMeters))
        applyStationaryLock(fix, usage, stationaryLock)
        return emit(fix, usage, stationaryLock)
    }

    private fun initializeAt(fix: TrackFix) {
        val en = GeoProjection.eastNorth(originLat, originLon, fix.latitude, fix.longitude)
        val velocity = velocityFrom(fix)
        x[0] = en.first
        x[1] = en.second
        x[2] = velocity.first
        x[3] = velocity.second
        val sigma = max(fix.accuracyMeters.toDouble(), MinSigmaMeters)
        val variance = sigma * sigma
        p = Array(4) { DoubleArray(4) }
        p[0][0] = variance
        p[1][1] = variance
        p[2][2] = VelocityVariance
        p[3][3] = VelocityVariance
        lastTimestampMillis = fix.timestampMillis
        initialized = true
    }

    private fun velocityFrom(fix: TrackFix): Pair<Double, Double> {
        if (fix.speedMps < 0.3f || fix.bearing == 0f) {
            return 0.0 to 0.0
        }
        val rad = Math.toRadians(fix.bearing.toDouble())
        val speed = fix.speedMps.toDouble()
        return speed * kotlin.math.sin(rad) to speed * kotlin.math.cos(rad)
    }

    private fun predict(dt: Double, q: Double) {
        val f = arrayOf(
            doubleArrayOf(1.0, 0.0, dt, 0.0),
            doubleArrayOf(0.0, 1.0, 0.0, dt),
            doubleArrayOf(0.0, 0.0, 1.0, 0.0),
            doubleArrayOf(0.0, 0.0, 0.0, 1.0)
        )
        val dt2 = dt * dt
        val dt3 = dt2 * dt
        val dt4 = dt2 * dt2
        val qMat = arrayOf(
            doubleArrayOf(q * dt4 / 4.0, 0.0, q * dt3 / 2.0, 0.0),
            doubleArrayOf(0.0, q * dt4 / 4.0, 0.0, q * dt3 / 2.0),
            doubleArrayOf(q * dt3 / 2.0, 0.0, q * dt2, 0.0),
            doubleArrayOf(0.0, q * dt3 / 2.0, 0.0, q * dt2)
        )
        val predicted = matVec(f, x)
        for (i in 0..3) {
            x[i] = predicted[i]
        }
        val ft = transpose(f)
        p = add(matMul(matMul(f, p), ft), qMat)
    }

    private fun update(eastMeas: Double, northMeas: Double, sigma: Double) {
        val y0 = eastMeas - x[0]
        val y1 = northMeas - x[1]
        val r = sigma * sigma
        val s00 = p[0][0] + r
        val s01 = p[0][1]
        val s10 = p[1][0]
        val s11 = p[1][1] + r
        val det = s00 * s11 - s01 * s10
        if (!det.isFinite() || kotlin.math.abs(det) < 1e-12) {
            return
        }
        val inv00 = s11 / det
        val inv01 = -s01 / det
        val inv10 = -s10 / det
        val inv11 = s00 / det
        val k = Array(4) { DoubleArray(2) }
        for (i in 0..3) {
            k[i][0] = p[i][0] * inv00 + p[i][1] * inv10
            k[i][1] = p[i][0] * inv01 + p[i][1] * inv11
        }
        for (i in 0..3) {
            x[i] += k[i][0] * y0 + k[i][1] * y1
        }
        val iMinusKh = Array(4) { DoubleArray(4) }
        for (i in 0..3) {
            for (j in 0..3) {
                val kh = when (j) {
                    0 -> k[i][0]
                    1 -> k[i][1]
                    else -> 0.0
                }
                iMinusKh[i][j] = (if (i == j) 1.0 else 0.0) - kh
            }
        }
        val krkt = Array(4) { DoubleArray(4) }
        for (i in 0..3) {
            for (j in 0..3) {
                krkt[i][j] = (k[i][0] * r * k[j][0]) + (k[i][1] * r * k[j][1])
            }
        }
        p = add(matMul(matMul(iMinusKh, p), transpose(iMinusKh)), krkt)
    }

    private fun isTurning(previous: TrackFix, current: TrackFix): Boolean {
        if (SpeedAdaptiveSpacing.isInCurve(previous, current)) {
            return true
        }
        val fromPos = SpeedAdaptiveSpacing.headingDegrees(
            previous.latitude,
            previous.longitude,
            current.latitude,
            current.longitude
        )
        if (fromPos == null) {
            return false
        }
        val stateSpeed = GeoProjection.hypot(x[2], x[3])
        val reference = if (previous.bearing != 0f) {
            previous.bearing
        } else if (stateSpeed >= MinOutputSpeed) {
            val deg = Math.toDegrees(atan2(x[2], x[3]))
            ((deg + 360.0) % 360.0).toFloat()
        } else {
            fromPos
        }
        return SpeedAdaptiveSpacing.headingChangeIsCurve(reference, fromPos)
    }

    private fun applyStationaryLock(fix: TrackFix, usage: UsageType, enabled: Boolean) {
        if (!enabled) {
            return
        }
        val pause = usage.pauseSpeedMps()
        val predictedSpeed = GeoProjection.hypot(x[2], x[3])
        val previous = lastOutput
        val displacement = if (previous == null) {
            0.0
        } else {
            val held = GeoProjection.latLon(originLat, originLon, x[0], x[1])
            FixAcceptance.haversineMeters(
                previous.latitude,
                previous.longitude,
                held.first,
                held.second
            )
        }
        val gpsStopped = fix.speedMps < pause
        val predictedStopped = predictedSpeed < pause && displacement < StationaryDisplaceMeters
        if (!gpsStopped && !predictedStopped) {
            return
        }
        if (previous != null) {
            val en = GeoProjection.eastNorth(
                originLat,
                originLon,
                previous.latitude,
                previous.longitude
            )
            x[0] = en.first
            x[1] = en.second
        }
        x[2] = 0.0
        x[3] = 0.0
        p[0][0] *= PositionShrink
        p[1][1] *= PositionShrink
        p[0][1] = 0.0
        p[1][0] = 0.0
        p[0][2] = 0.0
        p[0][3] = 0.0
        p[1][2] = 0.0
        p[1][3] = 0.0
        p[2][0] = 0.0
        p[3][0] = 0.0
        p[2][1] = 0.0
        p[3][1] = 0.0
        p[2][2] = VelocityVariance * PositionShrink
        p[3][3] = VelocityVariance * PositionShrink
    }

    private fun emit(fix: TrackFix, usage: UsageType, stationaryLock: Boolean): TrackFix {
        val ll = GeoProjection.latLon(originLat, originLon, x[0], x[1])
        val stateSpeed = GeoProjection.hypot(x[2], x[3])
        val previous = lastOutput
        val speedOut: Float
        val bearingOut: Float
        if (stateSpeed >= MinOutputSpeed) {
            speedOut = stateSpeed.toFloat()
            val deg = Math.toDegrees(atan2(x[2], x[3]))
            bearingOut = ((deg + 360.0) % 360.0).toFloat()
        } else {
            speedOut = if (stationaryLock && fix.speedMps < usage.pauseSpeedMps()) {
                0f
            } else {
                fix.speedMps
            }
            bearingOut = if (fix.bearing != 0f) {
                fix.bearing
            } else if (previous != null) {
                previous.bearing
            } else {
                0f
            }
        }
        val out = TrackFix(
            timestampMillis = fix.timestampMillis,
            latitude = ll.first,
            longitude = ll.second,
            altitude = fix.altitude,
            speedMps = speedOut,
            bearing = bearingOut,
            accuracyMeters = fix.accuracyMeters,
            satellitesInFix = fix.satellitesInFix
        )
        lastOutput = out
        return out
    }

    private fun matVec(a: Array<DoubleArray>, v: DoubleArray): DoubleArray {
        val out = DoubleArray(4)
        for (i in 0..3) {
            var sum = 0.0
            for (j in 0..3) {
                sum += a[i][j] * v[j]
            }
            out[i] = sum
        }
        return out
    }

    private fun matMul(a: Array<DoubleArray>, b: Array<DoubleArray>): Array<DoubleArray> {
        val out = Array(4) { DoubleArray(4) }
        for (i in 0..3) {
            for (j in 0..3) {
                var sum = 0.0
                for (k in 0..3) {
                    sum += a[i][k] * b[k][j]
                }
                out[i][j] = sum
            }
        }
        return out
    }

    private fun transpose(a: Array<DoubleArray>): Array<DoubleArray> {
        val out = Array(4) { DoubleArray(4) }
        for (i in 0..3) {
            for (j in 0..3) {
                out[j][i] = a[i][j]
            }
        }
        return out
    }

    private fun add(a: Array<DoubleArray>, b: Array<DoubleArray>): Array<DoubleArray> {
        val out = Array(4) { DoubleArray(4) }
        for (i in 0..3) {
            for (j in 0..3) {
                out[i][j] = a[i][j] + b[i][j]
            }
        }
        return out
    }

    companion object {
        private const val MinDtSeconds = 0.05
        private const val MaxDtSeconds = 5.0
        private const val MinSigmaMeters = 2.0
        private const val VelocityVariance = 25.0
        private const val MinOutputSpeed = 0.3
        private const val StationaryDisplaceMeters = 1.5
        private const val PositionShrink = 0.05
    }
}
