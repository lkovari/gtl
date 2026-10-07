package com.lkovari.mobile.apps.gtl.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WaitingTimeTest {
    @Test
    fun ninetySecondsAtARedLightWithNoStoredPointsCountsAsWaiting() {
        // Walk 0-9 s, stand at a light until 100 s (no point stored), then walk again.
        val samples = walk(0L, 0, 10) + walk(100_000L, 10, 10)
        val stats = TrackStatsCalculator.compute(samples)
        assertEquals(91_000L, stats.waitingMillis)
        assertEquals(18_000L, stats.movingMillis)
    }

    @Test
    fun heldHeartbeatPointsDuringTheStopCountAsWaiting() {
        val stop = (1..18).map { i ->
            sample(10_000L + i * 5_000L, metersNorth = 10.0, speed = 0.9f)
        }
        val samples = walk(0L, 0, 10) + stop + walk(100_000L, 10, 10)
        val stats = TrackStatsCalculator.compute(samples)
        assertEquals(91_000L, stats.waitingMillis)
    }

    @Test
    fun aLongGapWhileMovingStaysMoving() {
        val samples = listOf(
            sample(0L, metersNorth = 0.0, speed = 1.4f),
            sample(10_000L, metersNorth = 14.0, speed = 1.4f)
        )
        val stats = TrackStatsCalculator.compute(samples)
        assertEquals(10_000L, stats.movingMillis)
        assertEquals(0L, stats.waitingMillis)
    }

    @Test
    fun heartbeatIsDueAfterFiveSecondsAndHoldsThePreviousPosition() {
        val previous = fix(0L, 47.5)
        assertFalse(StationaryHeartbeat.isDue(previous, fix(4_999L, 47.50001)))
        assertTrue(StationaryHeartbeat.isDue(previous, fix(5_000L, 47.50001)))
        assertFalse(StationaryHeartbeat.isDue(null, fix(5_000L, 47.5)))
        val held = StationaryHeartbeat.hold(previous, fix(5_000L, 47.50001))
        assertEquals(47.5, held.latitude, 0.0)
        assertEquals(5_000L, held.timestampMillis)
    }

    private fun walk(startMillis: Long, startMeters: Int, seconds: Int): List<TrackSample> {
        return (0 until seconds).map { i ->
            sample(startMillis + i * 1_000L, metersNorth = (startMeters + i).toDouble(), speed = 1.0f)
        }
    }

    private fun sample(millis: Long, metersNorth: Double, speed: Float): TrackSample {
        return TrackSample(millis, LAT + metersNorth / 111_195.0, LON, null, speed, 0f, null, EventKind.MOVE)
    }

    private fun fix(millis: Long, latitude: Double): TrackFix {
        return TrackFix(millis, latitude, LON, 100.0, 0.2f, 0f, 10f, 20)
    }

    private companion object {
        const val LAT = 47.5
        const val LON = 19.0
    }
}
