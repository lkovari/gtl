package com.lkovari.mobile.apps.gtl.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccelMotionTest {
    private val step = 60_000_000L

    @Test
    fun phoneLyingStillIsNotMoving() {
        var state = AccelMotionState()
        repeat(50) { i ->
            val noise = if (i % 2 == 0) 0.02f else -0.02f
            state = AccelMotion.observe(state, 0.1f, 0.05f + noise, 9.81f, i * step)
        }
        assertEquals(false, AccelMotion.isMoving(state, 50 * step))
    }

    @Test
    fun walkingPhoneIsMoving() {
        // accelZ samples of session 16 while walking.
        val z = floatArrayOf(8.049f, 11.551f, 15.674f, 15.190f, 9.351f, 10.819f, 10.895f, 4.879f, 5.745f, 6.885f, 5.489f, 8.070f)
        var state = AccelMotionState()
        repeat(3) { round ->
            z.forEachIndexed { i, value ->
                state = AccelMotion.observe(state, 1.0f, 2.0f, value, (round * z.size + i) * step)
            }
        }
        assertEquals(true, AccelMotion.isMoving(state, 36 * step))
    }

    @Test
    fun unknownWithoutEnoughOrRecentSamples() {
        assertNull(AccelMotion.isMoving(AccelMotionState(), 0L))
        var state = AccelMotionState()
        repeat(20) { i -> state = AccelMotion.observe(state, 0f, 0f, 9.81f, i * step) }
        assertNull(AccelMotion.isMoving(state, 20 * step + AccelMotion.MaxSampleAgeNanos + 1))
    }
}

class PauseDetectionTest {
    @Test
    fun startRampIsNotAPause() {
        // Session 16 stored two PAUSE placemarks at +1 s and +2 s while the chip speed ramped up.
        var streak = 0
        streak = PauseDetection.slowStreak(streak, 0.006f, UsageType.RUNNER)
        streak = PauseDetection.slowStreak(streak, 0.077f, UsageType.RUNNER)
        streak = PauseDetection.slowStreak(streak, 0.171f, UsageType.RUNNER)
        assertEquals(3, streak)
        assertFalse(PauseDetection.isPause(streak, 2_000L))
    }

    @Test
    fun singleSlowFixIsNotAPause() {
        val streak = PauseDetection.slowStreak(0, 0.1f, UsageType.RUNNER)
        assertFalse(PauseDetection.isPause(streak, 60_000L))
    }

    @Test
    fun sustainedSlowFixesAfterStartArePause() {
        var streak = 0
        repeat(3) { streak = PauseDetection.slowStreak(streak, 0.1f, UsageType.RUNNER) }
        assertTrue(PauseDetection.isPause(streak, 60_000L))
        assertTrue(PauseDetection.isPause(streak, null))
    }

    @Test
    fun movingFixResetsAndMissingSpeedKeepsTheStreak() {
        assertEquals(0, PauseDetection.slowStreak(2, 1.2f, UsageType.RUNNER))
        assertEquals(2, PauseDetection.slowStreak(2, null, UsageType.RUNNER))
    }
}
