package com.lkovari.mobile.apps.gtl.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BaroGuardTest {
    // Field case 2026-10-07, session 16: every fix stored 798.860 hPa while the GPS
    // altitude moved between 129 and 145 m, and auto-calibration clamped -207 hPa to -10.
    private val frozenHpa = 798.86f
    private val gpsMeters = 129.9
    private val qnh = 1022f

    @Test
    fun calibrationRefusesPressureThatContradictsGps() {
        assertNull(BaroAltitude.calibrationOffsetHpa(frozenHpa, gpsMeters, qnh))
    }

    @Test
    fun calibrationStillClampsModestQnhError() {
        val expected = BaroAltitude.expectedStationHpa(gpsMeters, qnh)!!
        assertEquals(BaroAltitude.MaxOffsetHpa, BaroAltitude.calibrationOffsetHpa(expected + 20f, gpsMeters, qnh)!!, 0f)
        assertEquals(3f, BaroAltitude.calibrationOffsetHpa(expected + 3f, gpsMeters, qnh)!!, 0.01f)
    }

    @Test
    fun calibrationRefusesImplausiblePressure() {
        assertNull(BaroAltitude.calibrationOffsetHpa(Float.NaN, gpsMeters, qnh))
    }

    @Test
    fun frozenFieldPressureDoesNotMatchGps() {
        assertFalse(BaroAltitude.matchesGps(frozenHpa, gpsMeters, qnh, -10f))
        assertFalse(BaroAltitude.matchesGps(frozenHpa, gpsMeters, qnh, 0f))
    }

    @Test
    fun healthyPressureMatchesGps() {
        assertTrue(BaroAltitude.matchesGps(1006.4f, gpsMeters, qnh, 0f))
        assertTrue(BaroAltitude.matchesGps(998.2f, 140.0, 1013.25f, 0f))
    }

    @Test
    fun pressureIsTrustedWithoutGpsAltitude() {
        assertTrue(BaroAltitude.matchesGps(1006.4f, null, qnh))
        assertFalse(BaroAltitude.matchesGps(200f, null, qnh))
    }

    @Test
    fun autoCalibrateNotEligibleWithPoorVerticalAccuracy() {
        assertFalse(
            BaroAltitude.autoCalibrateEligible(
                pressureHpa = 1006f,
                gpsAltitudeMeters = 130.0,
                alreadyCalibratedThisSession = false,
                enabled = true,
                previousGpsAltitudeMeters = 130.0,
                verticalAccuracyMeters = 19.7f
            )
        )
        assertTrue(
            BaroAltitude.autoCalibrateEligible(
                pressureHpa = 1006f,
                gpsAltitudeMeters = 130.0,
                alreadyCalibratedThisSession = false,
                enabled = true,
                previousGpsAltitudeMeters = 130.0,
                verticalAccuracyMeters = 6f
            )
        )
    }
}

class BaroFreshnessTest {
    private val second = 1_000_000_000L

    @Test
    fun waitsForFirstSampleThenGoesStale() {
        val empty = BaroFreshnessState()
        assertEquals(BaroFeedStatus.Waiting, BaroFreshness.status(empty, 5 * second, 0L))
        assertEquals(BaroFeedStatus.Stale, BaroFreshness.status(empty, 11 * second, 0L))
    }

    @Test
    fun liveWhileSamplesKeepArrivingAndChanging() {
        var state = BaroFreshnessState()
        for (i in 0..600) {
            val hpa = if (i % 2 == 0) 998.19f else 998.20f
            state = BaroFreshness.observe(state, hpa, i * second / 5)
        }
        assertEquals(BaroFeedStatus.Live, BaroFreshness.status(state, 120 * second, 0L))
    }

    @Test
    fun staleWhenSensorStopsDelivering() {
        val state = BaroFreshness.observe(BaroFreshnessState(), 998.19f, 10 * second)
        assertEquals(BaroFeedStatus.Live, BaroFreshness.status(state, 14 * second, 0L))
        assertEquals(BaroFeedStatus.Stale, BaroFreshness.status(state, 16 * second, 0L))
    }

    @Test
    fun frozenWhenSensorRepeatsTheSameValue() {
        var state = BaroFreshnessState()
        for (i in 0..(121 * 5)) {
            state = BaroFreshness.observe(state, 798.86f, i * second / 5)
        }
        assertEquals(BaroFeedStatus.Frozen, BaroFreshness.status(state, 121 * second, 0L))
    }
}
