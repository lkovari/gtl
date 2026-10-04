package com.lkovari.mobile.apps.gtl.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.abs

class CivilTwilightTest {
    private val budapestLat = 47.4979
    private val budapestLon = 19.0402
    private val newYorkLat = 40.7128
    private val newYorkLon = -74.0060
    private val svalbardLat = 78.2232
    private val svalbardLon = 15.6469
    private val budapest = ZoneId.of("Europe/Budapest")

    @Test
    fun budapestSolsticeNoonIsLightAndLateEveningIsDark() {
        val noon = ZonedDateTime.of(2026, 6, 21, 12, 0, 0, 0, budapest).toInstant()
        val evening = ZonedDateTime.of(2026, 6, 21, 22, 30, 0, 0, budapest).toInstant()
        assertFalse(CivilTwilight.isNight(budapestLat, budapestLon, noon))
        assertTrue(CivilTwilight.isNight(budapestLat, budapestLon, evening))
    }

    @Test
    fun budapestCivilDuskMatchesThePublishedEvening() {
        val dusk = CivilTwilight.eveningCivilDusk(
            budapestLat,
            budapestLon,
            LocalDate.of(2026, 6, 21),
            budapest
        )
        val published = ZonedDateTime.of(2026, 6, 21, 21, 25, 0, 0, budapest).toInstant()
        val minutes = abs(Duration.between(dusk, published).toMinutes())
        assertTrue(minutes <= 2)
    }

    @Test
    fun sameInstantIsDarkInBudapestAndLightInNewYork() {
        val instant = Instant.parse("2026-12-21T17:30:00Z")
        assertTrue(CivilTwilight.isNight(budapestLat, budapestLon, instant))
        assertFalse(CivilTwilight.isNight(newYorkLat, newYorkLon, instant))
    }

    @Test
    fun polarDayStaysLightAndPolarNightStaysDark() {
        val oslo = ZoneId.of("Europe/Oslo")
        val summerMidnight = ZonedDateTime.of(2026, 6, 21, 0, 0, 0, 0, oslo).toInstant()
        val summerNoon = ZonedDateTime.of(2026, 6, 21, 12, 0, 0, 0, oslo).toInstant()
        val winterMidnight = ZonedDateTime.of(2026, 12, 21, 0, 0, 0, 0, oslo).toInstant()
        val winterNoon = ZonedDateTime.of(2026, 12, 21, 12, 0, 0, 0, oslo).toInstant()
        assertFalse(CivilTwilight.isNight(svalbardLat, svalbardLon, summerMidnight))
        assertFalse(CivilTwilight.isNight(svalbardLat, svalbardLon, summerNoon))
        assertNull(
            CivilTwilight.eveningCivilDusk(
                svalbardLat,
                svalbardLon,
                LocalDate.of(2026, 6, 21),
                oslo
            )
        )
        assertTrue(CivilTwilight.isNight(svalbardLat, svalbardLon, winterMidnight))
        assertTrue(CivilTwilight.isNight(svalbardLat, svalbardLon, winterNoon))
        assertNull(
            CivilTwilight.eveningCivilDusk(
                svalbardLat,
                svalbardLon,
                LocalDate.of(2026, 12, 21),
                oslo
            )
        )
    }

    @Test
    fun manualModesIgnoreTheSunAndAutomaticWithoutAPositionStaysLight() {
        val night = ZonedDateTime.of(2026, 6, 21, 23, 0, 0, 0, budapest).toInstant()
        val day = ZonedDateTime.of(2026, 6, 21, 12, 0, 0, 0, budapest).toInstant()
        assertFalse(AppTheme.isDark(ThemeMode.LIGHT, budapestLat, budapestLon, night))
        assertTrue(AppTheme.isDark(ThemeMode.DARK, budapestLat, budapestLon, day))
        assertFalse(AppTheme.isDark(ThemeMode.AUTOMATIC, null, null, night))
        assertTrue(AppTheme.isDark(ThemeMode.AUTOMATIC, budapestLat, budapestLon, night))
        assertEquals(ThemeMode.AUTOMATIC, ThemeMode.fromStored(null))
        assertEquals(ThemeMode.DARK, ThemeMode.fromStored("DARK"))
    }
}
