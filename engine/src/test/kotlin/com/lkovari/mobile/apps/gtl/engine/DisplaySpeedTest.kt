package com.lkovari.mobile.apps.gtl.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DisplaySpeedTest {
    @Test
    fun missingSpeedShowsDashAndDoesNotStepTheCounter() {
        val origin = fix(speed = null)
        val first = DisplaySpeed.apply(DisplaySpeedState(), origin)
        assertNull(first.metersPerSecond)
        assertEquals(DisplayMotion.Idle, first.state.motion)
        assertEquals(0, first.state.streak)
        assertEquals(LAT, first.state.previousLatitude)
        assertEquals(LON, first.state.previousLongitude)

        val pending = DisplaySpeed.apply(first.state, fix(speed = 3f, speedAccuracy = 0.2f))
        assertEquals(0f, pending.metersPerSecond)
        assertEquals(1, pending.state.streak)

        val gap = DisplaySpeed.apply(pending.state, fix(speed = null, latitude = LAT + 0.01))
        assertNull(gap.metersPerSecond)
        assertEquals(1, gap.state.streak)
        assertEquals(DisplayMotion.Idle, gap.state.motion)

        val moving = DisplaySpeed.apply(gap.state, fix(speed = 3f, speedAccuracy = 0.2f))
        assertEquals(3f, moving.metersPerSecond)
        assertEquals(DisplayMotion.Moving, moving.state.motion)
    }

    @Test
    fun indoorDopplerStaysAtZero() {
        var state = DisplaySpeedState()
        repeat(8) {
            val decision = DisplaySpeed.apply(state, fix(speed = 1.4f, speedAccuracy = 2f))
            assertEquals(0f, decision.metersPerSecond)
            assertEquals(DisplayMotion.Idle, decision.state.motion)
            state = decision.state
        }
    }

    @Test
    fun speedEqualToAccuracyStaysIdle() {
        val decision = DisplaySpeed.apply(DisplaySpeedState(), fix(speed = 2f, speedAccuracy = 2f))
        assertEquals(0f, decision.metersPerSecond)
        assertEquals(0, decision.state.streak)
    }

    @Test
    fun twoSignificantSamplesLeaveZero() {
        val first = DisplaySpeed.apply(DisplaySpeedState(), fix(speed = 1.4f, speedAccuracy = 0.2f))
        assertEquals(0f, first.metersPerSecond)
        assertEquals(1, first.state.streak)

        val second = DisplaySpeed.apply(first.state, fix(speed = 1.6f, speedAccuracy = 0.2f))
        assertEquals(1.6f, second.metersPerSecond)
        assertEquals(DisplayMotion.Moving, second.state.motion)
        assertEquals(0, second.state.streak)
    }

    @Test
    fun oneSpikeDoesNotLeaveZero() {
        val spike = DisplaySpeed.apply(DisplaySpeedState(), fix(speed = 1.4f, speedAccuracy = 0.2f))
        val back = DisplaySpeed.apply(spike.state, fix(speed = 1.4f, speedAccuracy = 2f))
        assertEquals(0f, back.metersPerSecond)
        assertEquals(0, back.state.streak)
        assertEquals(DisplayMotion.Idle, back.state.motion)
    }

    @Test
    fun twoIdleSamplesHoldTheLastSpeedAndTheThirdReturnsToZero() {
        val first = DisplaySpeed.apply(DisplaySpeedState(), fix(speed = 3f, speedAccuracy = 0.2f))
        val moving = DisplaySpeed.apply(first.state, fix(speed = 3.2f, speedAccuracy = 0.2f))
        assertEquals(3.2f, moving.metersPerSecond)

        val held = DisplaySpeed.apply(moving.state, fix(speed = 1.4f, speedAccuracy = 2f))
        assertEquals(3.2f, held.metersPerSecond)
        assertEquals(DisplayMotion.Moving, held.state.motion)

        val stillHeld = DisplaySpeed.apply(held.state, fix(speed = 1.4f, speedAccuracy = 2f))
        assertEquals(3.2f, stillHeld.metersPerSecond)
        assertEquals(DisplayMotion.Moving, stillHeld.state.motion)

        val idle = DisplaySpeed.apply(stillHeld.state, fix(speed = 1.4f, speedAccuracy = 2f))
        assertEquals(0f, idle.metersPerSecond)
        assertEquals(DisplayMotion.Idle, idle.state.motion)
    }

    @Test
    fun indoorDopplerOnAPhoneAtRestStaysAtZero() {
        var state = DisplaySpeedState()
        repeat(8) {
            val decision = DisplaySpeed.apply(
                state,
                fix(speed = 1.4f, speedAccuracy = 2f, deviceMoving = false),
                DisplaySpeed.floorMps(UsageType.TWO_WHEELERS)
            )
            assertEquals(0f, decision.metersPerSecond)
            state = decision.state
        }
    }

    @Test
    fun slowWalkBelowSpeedAccuracyShowsWhenTheAccelerometerMoves() {
        // Session 16 (2026-10-07): the chip ramped 0.006 -> 1.48 m/s while hAcc was 11-16 m.
        val speeds = listOf(0.006f, 0.077f, 0.171f, 0.264f, 0.461f, 0.827f, 0.995f, 1.155f, 1.213f, 0.984f)
        var state = DisplaySpeedState()
        var firstShown: Int? = null
        speeds.forEachIndexed { index, speed ->
            val decision = DisplaySpeed.apply(
                state,
                fix(speed = speed, speedAccuracy = 2f, deviceMoving = true),
                DisplaySpeed.floorMps(UsageType.RUNNER)
            )
            if (firstShown == null && (decision.metersPerSecond ?: 0f) > 0f) {
                firstShown = index
            }
            state = decision.state
        }
        assertEquals(5, firstShown)
        assertEquals(DisplayMotion.Moving, state.motion)
    }

    @Test
    fun standingStillWhileHoldingThePhoneStaysAtZero() {
        var state = DisplaySpeedState()
        listOf(0.006f, 0.077f, 0.171f, 0.12f, 0.2f).forEach { speed ->
            val decision = DisplaySpeed.apply(
                state,
                fix(speed = speed, speedAccuracy = 2f, deviceMoving = true),
                DisplaySpeed.floorMps(UsageType.RUNNER)
            )
            assertEquals(0f, decision.metersPerSecond)
            state = decision.state
        }
    }

    @Test
    fun displacementInsideHorizontalAccuracyStaysZero() {
        val origin = DisplaySpeed.apply(
            DisplaySpeedState(),
            fix(speed = 1.4f, latitude = LAT, horizontal = 15f)
        )
        assertEquals(0f, origin.metersPerSecond)

        val near = LAT + 0.00001
        assertTrue(FixAcceptance.haversineMeters(LAT, LON, near, LON) <= 15.0)
        var state = origin.state
        repeat(4) { step ->
            val latitude = if (step % 2 == 0) near else LAT
            val decision = DisplaySpeed.apply(
                state,
                fix(speed = 1.4f, latitude = latitude, horizontal = 15f)
            )
            assertEquals(0f, decision.metersPerSecond)
            state = decision.state
        }
    }

    @Test
    fun displacementBeyondAccuracyShowsDopplerNotDistanceOverTime() {
        val far = LAT + 0.001
        val moved = FixAcceptance.haversineMeters(LAT, LON, far, LON)
        assertTrue(moved > 5.0)

        val origin = DisplaySpeed.apply(
            DisplaySpeedState(),
            fix(speed = 1.4f, latitude = LAT, horizontal = 5f)
        )
        assertEquals(0f, origin.metersPerSecond)

        val one = DisplaySpeed.apply(
            origin.state,
            fix(speed = 1.4f, latitude = far, horizontal = 5f)
        )
        assertEquals(0f, one.metersPerSecond)

        val two = DisplaySpeed.apply(
            one.state,
            fix(speed = 1.4f, latitude = LAT, horizontal = 5f)
        )
        assertEquals(1.4f, two.metersPerSecond)
        assertTrue(two.metersPerSecond != moved.toFloat())
    }

    @Test
    fun speedAccuracyWinsOverALargeJump() {
        val far = LAT + 0.001
        val origin = DisplaySpeed.apply(
            DisplaySpeedState(),
            fix(speed = 1.4f, speedAccuracy = 2f, latitude = LAT)
        )
        val jumped = DisplaySpeed.apply(
            origin.state,
            fix(speed = 1.4f, speedAccuracy = 2f, latitude = far)
        )
        assertEquals(0f, jumped.metersPerSecond)
        assertEquals(DisplayMotion.Idle, jumped.state.motion)
    }

    @Test
    fun firstFixWithoutSpeedAccuracyStaysZero() {
        val decision = DisplaySpeed.apply(
            DisplaySpeedState(),
            fix(speed = 1.4f, horizontal = null)
        )
        assertEquals(0f, decision.metersPerSecond)
        assertEquals(DisplayMotion.Idle, decision.state.motion)
    }

    private fun fix(
        speed: Float?,
        speedAccuracy: Float? = null,
        latitude: Double = LAT,
        horizontal: Float? = 15f,
        deviceMoving: Boolean? = null
    ): DisplaySpeedFix {
        return DisplaySpeedFix(
            speedMps = speed,
            speedAccuracyMps = speedAccuracy,
            latitude = latitude,
            longitude = LON,
            horizontalAccuracyMeters = horizontal,
            deviceMoving = deviceMoving
        )
    }

    private companion object {
        const val LAT = 47.5
        const val LON = 19.0
    }
}
