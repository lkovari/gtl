package com.lkovari.mobile.apps.gtl.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QnhSelectionTest {
    @Test
    fun freshMetarInsideThirtyKilometresBeatsTheModel() {
        val here = point(48.395, 21.656)
        val station = offsetNorth(here, 20_000.0)
        val choice = QnhSelection.select(
            modelHpa = 1010.2,
            metars = listOf(metar("LHNY", 1024.4, station, now - 10 * MINUTE)),
            latitude = here.first,
            longitude = here.second,
            nowMillis = now,
            usage = UsageType.TWO_WHEELERS
        )
        val selected = checkNotNull(choice)
        assertEquals(QnhOrigin.METAR, selected.origin)
        assertEquals("LHNY", selected.icao)
        assertEquals(1024, selected.hpa)
        assertEquals(20, selected.distanceKm)
    }

    @Test
    fun metarAtFortyFiveKilometresLosesOnTheGroundAndWinsInTheAir() {
        val here = point(48.395, 21.656)
        val station = offsetNorth(here, 45_000.0)
        val reports = listOf(metar("LZKZ", 1021.2, station, now - MINUTE))
        val ground = checkNotNull(
            QnhSelection.select(1018.6, reports, here.first, here.second, now, UsageType.TWO_WHEELERS)
        )
        assertEquals(QnhOrigin.MODEL, ground.origin)
        assertEquals(1019, ground.hpa)
        assertNull(ground.icao)
        val air = checkNotNull(
            QnhSelection.select(1018.6, reports, here.first, here.second, now, UsageType.AIRCRAFT)
        )
        assertEquals(QnhOrigin.METAR, air.origin)
        assertEquals("LZKZ", air.icao)
        assertEquals(1021, air.hpa)
    }

    @Test
    fun staleOrMissingAltimIsIgnored() {
        val here = point(47.5, 19.05)
        val close = offsetNorth(here, 5_000.0)
        val reports = listOf(
            metar("OLD1", 1030.0, close, now - QnhSelection.MaxMetarAgeMillis - 1L),
            QnhMetar(null, now, close.first, close.second, "NOALTIM")
        )
        val choice = checkNotNull(
            QnhSelection.select(1008.4, reports, here.first, here.second, now, UsageType.FOUR_WHEELERS)
        )
        assertEquals(QnhOrigin.MODEL, choice.origin)
        assertEquals(1008, choice.hpa)
    }

    @Test
    fun metarExactlyNinetyMinutesOldStillCounts() {
        val here = point(47.5, 19.05)
        val close = offsetNorth(here, 8_000.0)
        val choice = checkNotNull(
            QnhSelection.select(
                modelHpa = 1000.0,
                metars = listOf(metar("EDGE", 1012.0, close, now - QnhSelection.MaxMetarAgeMillis)),
                latitude = here.first,
                longitude = here.second,
                nowMillis = now,
                usage = UsageType.TWO_WHEELERS
            )
        )
        assertEquals(QnhOrigin.METAR, choice.origin)
        assertEquals("EDGE", choice.icao)
    }

    @Test
    fun missingModelKeepsAMetarInsideOneHundredKilometres() {
        val here = point(48.395, 21.656)
        val station = offsetNorth(here, 45_000.0)
        val choice = checkNotNull(
            QnhSelection.select(
                modelHpa = null,
                metars = listOf(metar("LHNY", 1016.0, station, now)),
                latitude = here.first,
                longitude = here.second,
                nowMillis = now,
                usage = UsageType.TWO_WHEELERS
            )
        )
        assertEquals(QnhOrigin.METAR, choice.origin)
        assertEquals("LHNY", choice.icao)
    }

    @Test
    fun nothingToWriteWhenBothSourcesAreMissing() {
        val here = point(48.395, 21.656)
        val far = offsetNorth(here, 120_000.0)
        assertNull(
            QnhSelection.select(
                modelHpa = null,
                metars = listOf(metar("FAR", 1011.0, far, now)),
                latitude = here.first,
                longitude = here.second,
                nowMillis = now,
                usage = UsageType.TWO_WHEELERS
            )
        )
    }

    @Test
    fun manualOrRecordingBlocksAutomaticWrite() {
        assertFalse(QnhSelection.mayApplyAutomatic(manual = true, recording = false))
        assertFalse(QnhSelection.mayApplyAutomatic(manual = false, recording = true))
        assertFalse(QnhSelection.mayApplyAutomatic(manual = true, recording = true))
        assertTrue(QnhSelection.mayApplyAutomatic(manual = false, recording = false))
    }

    @Test
    fun roundingClampsToTheSliderGrid() {
        assertEquals(900, QnhSelection.roundedQnh(899.4f))
        assertEquals(900, QnhSelection.roundedQnh(BaroAltitude.MinQnhHpa))
        assertEquals(1100, QnhSelection.roundedQnh(1100.6f))
        assertEquals(1100, QnhSelection.roundedQnh(BaroAltitude.MaxQnhHpa))
        assertEquals(1013, QnhSelection.roundedQnh(1013.4f))
        assertEquals(1014, QnhSelection.roundedQnh(1013.5f))
    }

    @Test
    fun requestCoordinateUsesThreeDecimals() {
        assertEquals(48.396, QnhSelection.roundCoordinate(48.3956), 0.0)
        assertEquals(21.656, QnhSelection.roundCoordinate(21.6564), 0.0)
        assertEquals("48.395", QnhSelection.formatCoordinate(48.395))
        assertEquals("21.656", QnhSelection.formatCoordinate(21.656))
        val box = QnhSelection.bbox(48.395, 21.656, QnhSelection.BboxDeltaDegrees)
        assertEquals("47.895,21.156,48.895,22.156", box.query())
    }

    private fun metar(
        icao: String,
        altim: Double?,
        at: Pair<Double, Double>,
        observedAtMillis: Long?
    ): QnhMetar {
        return QnhMetar(altim, observedAtMillis, at.first, at.second, icao)
    }

    private fun point(latitude: Double, longitude: Double): Pair<Double, Double> {
        return latitude to longitude
    }

    private fun offsetNorth(origin: Pair<Double, Double>, meters: Double): Pair<Double, Double> {
        val latitude = origin.first + meters / 111_320.0
        return latitude to origin.second
    }

    private companion object {
        const val now = 1_700_000_000_000L
        const val MINUTE = 60_000L
    }
}
