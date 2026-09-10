package com.lkovari.mobile.apps.gtl.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DouglasPeuckerTest {
    @Test
    fun keepsEndpointsAndDropsColinearMiddle() {
        val points = listOf(
            GeoPoint(47.0, 19.0),
            GeoPoint(47.00001, 19.00001),
            GeoPoint(47.01, 19.01)
        )
        val simplified = DouglasPeucker.simplify(points, 50.0)
        assertEquals(2, simplified.size)
        assertEquals(points.first(), simplified.first())
        assertEquals(points.last(), simplified.last())
    }

    @Test
    fun keepsSharpCorner() {
        val points = listOf(
            GeoPoint(47.0, 19.0),
            GeoPoint(47.02, 19.0),
            GeoPoint(47.02, 19.03)
        )
        val simplified = DouglasPeucker.simplify(points, 20.0)
        assertEquals(3, simplified.size)
    }

    @Test
    fun clampKeepsDefaultTolerance() {
        assertEquals(20.0, DouglasPeucker.clampTolerance(19.5), 0.0)
    }

    @Test
    fun clampRaisesBelowMinimum() {
        assertEquals(1.0, DouglasPeucker.clampTolerance(0.0), 0.0)
        assertEquals(1.0, DouglasPeucker.clampTolerance(-5.0), 0.0)
    }

    @Test
    fun clampLowersAboveMaximum() {
        assertEquals(20.0, DouglasPeucker.clampTolerance(20.5), 0.0)
        assertEquals(20.0, DouglasPeucker.clampTolerance(100.0), 0.0)
    }

    @Test
    fun clampSnapsToWholeMetres() {
        assertEquals(19.0, DouglasPeucker.clampTolerance(19.4), 0.0)
        assertEquals(19.0, DouglasPeucker.clampTolerance(19.1), 0.0)
        assertEquals(20.0, DouglasPeucker.clampTolerance(19.5), 0.0)
    }

    @Test
    fun highToleranceCollapsesATenMetreFigureEight() {
        val originLat = 47.0
        val originLon = 19.0
        val points = mutableListOf<GeoPoint>()
        val radius = 5.0
        for (i in 0..24) {
            val theta = (i / 24.0) * 2.0 * Math.PI
            val en = GeoProjection.latLon(
                originLat,
                originLon,
                -radius + radius * kotlin.math.cos(theta),
                radius * kotlin.math.sin(theta)
            )
            points.add(GeoPoint(en.first, en.second))
        }
        for (i in 1..24) {
            val theta = Math.PI - (i / 24.0) * 2.0 * Math.PI
            val en = GeoProjection.latLon(
                originLat,
                originLon,
                radius + radius * kotlin.math.cos(theta),
                radius * kotlin.math.sin(theta)
            )
            points.add(GeoPoint(en.first, en.second))
        }
        val simplified = DouglasPeucker.simplify(points, 20.0)
        assertTrue(simplified.size <= 3)
    }
}

class SmoothingStrengthTest {
    @Test
    fun sliderMapsLowMediumHigh() {
        assertEquals(4.0, SmoothingStrength.processNoiseMultiplier(0f), 1e-9)
        assertEquals(1.0, SmoothingStrength.processNoiseMultiplier(0.5f), 1e-9)
        assertEquals(0.25, SmoothingStrength.processNoiseMultiplier(1f), 1e-9)
        assertEquals(4.0, SmoothingStrength.LOW.processNoiseMultiplier(), 1e-9)
        assertEquals(1.0, SmoothingStrength.MEDIUM.processNoiseMultiplier(), 1e-9)
        assertEquals(0.25, SmoothingStrength.HIGH.processNoiseMultiplier(), 1e-9)
    }
}

class SpeedAdaptiveSpacingTest {
    @Test
    fun standingUsesTwoMeters() {
        assertEquals(2f, SpeedAdaptiveSpacing.spacingMeters(0f, false))
    }

    @Test
    fun walkingUsesFourMeters() {
        assertEquals(4f, SpeedAdaptiveSpacing.spacingMeters(1.2f, false))
    }

    @Test
    fun cityDrivingUsesThirtySixMeters() {
        assertEquals(36f, SpeedAdaptiveSpacing.spacingMeters(8.3f, false))
    }

    @Test
    fun highwayUsesSeventyMeters() {
        assertEquals(70f, SpeedAdaptiveSpacing.spacingMeters(27.0f, false))
    }

    @Test
    fun curveHalvesSpacing() {
        assertEquals(18f, SpeedAdaptiveSpacing.spacingMeters(8.3f, true))
    }

    @Test
    fun headingChangeOverFifteenDegreesIsACurve() {
        assertTrue(SpeedAdaptiveSpacing.isInCurve(90f, 110f))
        assertFalse(SpeedAdaptiveSpacing.isInCurve(90f, 100f))
        assertFalse(SpeedAdaptiveSpacing.isInCurve(0f, 40f))
    }

    @Test
    fun displacementHeadingDetectsCurveWhenBearingsAreZero() {
        val originLat = 47.0
        val originLon = 19.0
        val a = GeoProjection.latLon(originLat, originLon, 0.0, 0.0)
        val b = GeoProjection.latLon(originLat, originLon, 10.0, 0.0)
        val c = GeoProjection.latLon(originLat, originLon, 10.0, 5.0)
        val first = TrackFix(10_000L, a.first, a.second, 100.0, 3f, 0f, 5f, 8)
        val second = TrackFix(10_500L, b.first, b.second, 100.0, 3f, 0f, 5f, 8)
        val third = TrackFix(11_000L, c.first, c.second, 100.0, 3f, 0f, 5f, 8)
        assertTrue(SpeedAdaptiveSpacing.isInCurve(first, second, third))
        val straight = GeoProjection.latLon(originLat, originLon, 20.0, 0.0)
        val onward = TrackFix(11_500L, straight.first, straight.second, 100.0, 3f, 0f, 5f, 8)
        assertFalse(SpeedAdaptiveSpacing.isInCurve(first, second, onward))
    }

    @Test
    fun runnerSmartUsesHalfOfWalkingBand() {
        assertEquals(2f, SpeedAdaptiveSpacing.spacingMeters(1.2f, false, UsageType.RUNNER))
        assertEquals(1f, SpeedAdaptiveSpacing.spacingMeters(1.2f, true, UsageType.RUNNER))
        assertEquals(4f, SpeedAdaptiveSpacing.spacingMeters(1.2f, false, UsageType.TWO_WHEELERS))
        assertEquals(18f, SpeedAdaptiveSpacing.spacingMeters(8.3f, true, UsageType.FOUR_WHEELERS))
    }
}

class FixAcceptanceTest {
    private val filter = FixFilter(
        minDistanceMeters = 2f,
        minTimeMillis = 500L,
        minAccuracyMeters = 30,
        minSatellites = 4
    )

    @Test
    fun acceptsFirstFix() {
        val current = sample(10_000L, 47.0, 19.0, 8f, 6)
        assertTrue(FixAcceptance.shouldAccept(null, current, filter))
    }

    @Test
    fun rejectsPoorAccuracy() {
        val current = sample(10_000L, 47.0, 19.0, 80f, 8)
        assertFalse(FixAcceptance.shouldAccept(null, current, filter))
    }

    @Test
    fun rejectsTooFewSatellites() {
        val current = sample(10_000L, 47.0, 19.0, 8f, 2)
        assertFalse(FixAcceptance.shouldAccept(null, current, filter))
    }

    @Test
    fun rejectsUntilSpeedBandDistanceIsTravelled() {
        val previous = sample(10_000L, 47.0, 19.0, 8f, 6, speedMps = 8.3f, bearing = 90f)
        val nearby = sample(10_500L, 47.00005, 19.0, 8f, 6, speedMps = 8.3f, bearing = 90f)
        assertFalse(FixAcceptance.shouldAccept(previous, nearby, filter))
        val farther = sample(12_000L, 47.00040, 19.0, 8f, 6, speedMps = 8.3f, bearing = 90f)
        assertTrue(FixAcceptance.shouldAccept(previous, farther, filter))
    }

    @Test
    fun curveAcceptsSoonerThanStraight() {
        val previous = sample(10_000L, 47.0, 19.0, 8f, 6, speedMps = 8.3f, bearing = 90f)
        val turned = sample(11_000L, 47.00020, 19.0, 8f, 6, speedMps = 8.3f, bearing = 120f)
        assertTrue(FixAcceptance.shouldAccept(previous, turned, filter))
        val straight = sample(11_000L, 47.00020, 19.0, 8f, 6, speedMps = 8.3f, bearing = 90f)
        assertFalse(FixAcceptance.shouldAccept(previous, straight, filter))
    }

    @Test
    fun everyFixAcceptsOnMinTimeAndDropsSubMeterDuplicates() {
        val previous = sample(10_000L, 47.0, 19.0, 8f, 6, speedMps = 3f, bearing = 90f)
        val duplicate = sample(10_600L, 47.0, 19.0, 8f, 6, speedMps = 3f, bearing = 90f)
        assertFalse(
            FixAcceptance.shouldAccept(
                previous,
                duplicate,
                filter,
                RecordingDensity.EVERY_FIX
            )
        )
        val moved = sample(10_600L, 47.00003, 19.0, 8f, 6, speedMps = 3f, bearing = 90f)
        assertTrue(
            FixAcceptance.shouldAccept(
                previous,
                moved,
                filter,
                RecordingDensity.EVERY_FIX
            )
        )
        val tooSoon = sample(10_200L, 47.00003, 19.0, 8f, 6, speedMps = 3f, bearing = 90f)
        assertFalse(
            FixAcceptance.shouldAccept(
                previous,
                tooSoon,
                filter,
                RecordingDensity.EVERY_FIX
            )
        )
    }

    @Test
    fun pedestrianEveryFixAcceptsSixtyCentimetresVehicleDropsThem() {
        val previous = sample(10_000L, 47.0, 19.0, 8f, 6, speedMps = 3f, bearing = 90f)
        val sixtyCm = GeoProjection.latLon(47.0, 19.0, 0.6, 0.0)
        val moved = sample(
            10_600L,
            sixtyCm.first,
            sixtyCm.second,
            8f,
            6,
            speedMps = 3f,
            bearing = 90f
        )
        assertFalse(
            FixAcceptance.shouldAccept(
                previous,
                moved,
                filter,
                RecordingDensity.EVERY_FIX,
                UsageType.TWO_WHEELERS
            )
        )
        assertTrue(
            FixAcceptance.shouldAccept(
                previous,
                moved,
                filter,
                RecordingDensity.EVERY_FIX,
                UsageType.RUNNER
            )
        )
        val stacked = sample(10_600L, 47.0, 19.0, 8f, 6, speedMps = 3f, bearing = 90f)
        assertFalse(
            FixAcceptance.shouldAccept(
                previous,
                stacked,
                filter,
                RecordingDensity.EVERY_FIX,
                UsageType.RUNNER
            )
        )
    }

    @Test
    fun densityMixAcceptsCloserThanSmartWhenHalfway() {
        val previous = sample(10_000L, 47.0, 19.0, 8f, 6, speedMps = 0f, bearing = 90f)
        val closer = sample(10_600L, 47.000012, 19.0, 8f, 6, speedMps = 0f, bearing = 90f)
        assertFalse(FixAcceptance.shouldAccept(previous, closer, filter, 0f))
        assertTrue(FixAcceptance.shouldAccept(previous, closer, filter, 0.5f))
        assertTrue(FixAcceptance.shouldAccept(previous, closer, filter, 1f))
    }

    @Test
    fun runnerSmartAcceptsSoonerThanVehicleBand() {
        val previous = sample(10_000L, 47.0, 19.0, 8f, 6, speedMps = 1.2f, bearing = 90f)
        val mid = sample(10_500L, 47.000025, 19.0, 8f, 6, speedMps = 1.2f, bearing = 90f)
        assertFalse(
            FixAcceptance.shouldAccept(
                previous,
                mid,
                filter,
                RecordingDensity.SMART,
                UsageType.TWO_WHEELERS
            )
        )
        assertTrue(
            FixAcceptance.shouldAccept(
                previous,
                mid,
                filter,
                RecordingDensity.SMART,
                UsageType.RUNNER
            )
        )
    }

    private fun sample(
        time: Long,
        lat: Double,
        lng: Double,
        accuracy: Float,
        sats: Int,
        speedMps: Float = 5f,
        bearing: Float = 90f
    ): TrackFix {
        return TrackFix(
            timestampMillis = time,
            latitude = lat,
            longitude = lng,
            altitude = 100.0,
            speedMps = speedMps,
            bearing = bearing,
            accuracyMeters = accuracy,
            satellitesInFix = sats
        )
    }
}

class GnssClassifierTest {
    @Test
    fun classifiesGpsL1AndL5() {
        val samples = listOf(
            SatelliteSample(GnssConstellation.GPS, 1, true, 32f, GnssClassifier.GPS_L1_HZ),
            SatelliteSample(GnssConstellation.GPS, 2, false, 28f, GnssClassifier.GPS_L5_HZ),
            SatelliteSample(GnssConstellation.GALILEO, 11, true, 30f, null),
            SatelliteSample(GnssConstellation.GLONASS, 21, true, 26f, null),
            SatelliteSample(GnssConstellation.BEIDOU, 31, false, 22f, null),
            SatelliteSample(GnssConstellation.QZSS, 41, true, 24f, null),
            SatelliteSample(GnssConstellation.IRNSS, 51, false, 18f, null)
        )
        val snapshot = GnssClassifier.snapshot(samples)
        assertEquals(1, snapshot.gpsL1.usedInFix)
        assertEquals(1, snapshot.gpsL5.inView)
        assertEquals(1, snapshot.byConstellation.getValue(GnssConstellation.GALILEO).usedInFix)
        assertEquals(1, snapshot.byConstellation.getValue(GnssConstellation.IRNSS).inView)
        assertEquals(4, snapshot.satellitesInFix)
        assertEquals(SignalQuality.GOOD, snapshot.signalQuality)
    }

    @Test
    fun tunnelStrengthIsPoor() {
        assertEquals(SignalQuality.POOR, GnssClassifier.qualityFromCn0(12.0, 3))
        assertEquals(SignalQuality.NONE, GnssClassifier.qualityFromCn0(0.0, 0))
        assertEquals(SignalQuality.EXCELLENT, GnssClassifier.qualityFromCn0(38.0, 8))
    }

    @Test
    fun mapsAndroidConstellationCodes() {
        assertEquals(GnssConstellation.GPS, GnssClassifier.constellationFromAndroid(1))
        assertEquals(GnssConstellation.GALILEO, GnssClassifier.constellationFromAndroid(6))
        assertEquals(GnssConstellation.IRNSS, GnssClassifier.constellationFromAndroid(7))
    }
}

class KmlExporterTest {
    companion object {
        private const val SAMPLE_TIME = 1_725_500_000_000L
    }

    @Test
    fun writesLineAndPlacemarksWithoutRemoteIcons() {
        val kml = sampleKml()
        assertTrue(kml.contains("<gx:Track>"))
        assertTrue(kml.contains("<when>"))
        assertTrue(kml.contains("<gx:coord>"))
        assertTrue(kml.contains("Ride &lt;1&gt;"))
        assertFalse(kml.contains("eklsofttrade"))
        assertFalse(kml.contains("<href>http"))
    }

    @Test
    fun writesTimeLatLonAndSpeedOnEveryTrackPoint() {
        val kml = sampleKml()
        assertTrue(kml.contains("xmlns:gx="))
        assertTrue(kml.contains("<when>2024-09-05T01:33:20Z</when>"))
        assertTrue(kml.contains("<gx:coord>19.05 47.5 120.0</gx:coord>"))
        assertTrue(kml.contains("<gx:SimpleArrayData name=\"speed\">"))
        assertTrue(kml.contains("<gx:value>5.5</gx:value>"))
    }

    @Test
    fun usesLocalPlayPauseStopIconsAndHidesLabels() {
        val kml = sampleKml()
        assertTrue(kml.contains("<href>icons/play.png</href>"))
        assertTrue(kml.contains("<href>icons/pause.png</href>"))
        assertTrue(kml.contains("<href>icons/stop.png</href>"))
        assertTrue(kml.contains("<LabelStyle><scale>0</scale></LabelStyle>"))
        assertFalse(kml.contains("<href>http"))
    }

    @Test
    fun pauseAndStopBalloonsForceZeroSpeed() {
        val pause = KmlDescriptions.balloon(
            kind = EventKind.PAUSE,
            timestampMillis = SAMPLE_TIME,
            latitude = 47.5,
            longitude = 19.05,
            speedMps = 1.1117642f,
            tempCelsius = null
        )
        val stop = KmlDescriptions.balloon(
            kind = EventKind.STOP,
            timestampMillis = SAMPLE_TIME,
            latitude = 47.5,
            longitude = 19.05,
            speedMps = 4.1f,
            tempCelsius = 18.5f,
            maxSpeedMps = 12.0f,
            averageSpeedMps = 6.0f,
            elapsedMillis = 45 * 60 * 1000L
        )
        assertTrue(pause.contains("speed=0.0 km/h"))
        assertFalse(pause.contains("Duration:"))
        assertFalse(pause.contains("Avg. speed:"))
        assertFalse(pause.contains("Max. speed:"))
        assertTrue(stop.contains("speed=0.0 km/h"))
        assertTrue(stop.contains("Duration: 45 min"))
        assertTrue(stop.contains("Avg. speed: 22 km/h"))
        assertTrue(stop.contains("Max. speed: 43 km/h"))
        assertTrue(stop.contains("temp=18.5"))
        assertFalse(stop.contains("lean="))
        assertFalse(stop.contains("maxSpeed="))
        assertFalse(stop.contains("avgSpeed="))
    }

    @Test
    fun stopBalloonShowsSecondsWhenDurationIsAtMostOneMinute() {
        val twenty = KmlDescriptions.balloon(
            kind = EventKind.STOP,
            timestampMillis = SAMPLE_TIME,
            latitude = 47.5,
            longitude = 19.05,
            speedMps = 0f,
            tempCelsius = null,
            maxSpeedMps = 2.8f,
            averageSpeedMps = 1.9f,
            elapsedMillis = 20_000L
        )
        val fiftyEight = KmlDescriptions.balloon(
            kind = EventKind.STOP,
            timestampMillis = SAMPLE_TIME,
            latitude = 47.5,
            longitude = 19.05,
            speedMps = 0f,
            tempCelsius = null,
            maxSpeedMps = 2.8f,
            averageSpeedMps = 1.9f,
            elapsedMillis = 58_000L
        )
        val sixty = KmlDescriptions.balloon(
            kind = EventKind.STOP,
            timestampMillis = SAMPLE_TIME,
            latitude = 47.5,
            longitude = 19.05,
            speedMps = 0f,
            tempCelsius = null,
            maxSpeedMps = 2.8f,
            averageSpeedMps = 1.9f,
            elapsedMillis = 60_000L
        )
        assertTrue(twenty.contains("Duration: 20 s"))
        assertFalse(twenty.contains("Duration: 0 min"))
        assertTrue(fiftyEight.contains("Duration: 58 s"))
        assertFalse(fiftyEight.contains("Duration: 0 min"))
        assertTrue(sixty.contains("Duration: 60 s"))
    }

    @Test
    fun stopBalloonShowsMinutesWhenDurationIsOverSixtySeconds() {
        val stop = KmlDescriptions.balloon(
            kind = EventKind.STOP,
            timestampMillis = SAMPLE_TIME,
            latitude = 47.5,
            longitude = 19.05,
            speedMps = 0f,
            tempCelsius = null,
            maxSpeedMps = 12.0f,
            averageSpeedMps = 6.0f,
            elapsedMillis = 61_000L
        )
        assertTrue(stop.contains("Duration: 1 min"))
        assertFalse(stop.contains("Duration: 61 s"))
    }

    @Test
    fun stopBalloonUsesHmsWhenDurationIsAtLeastOneHour() {
        val stop = KmlDescriptions.balloon(
            kind = EventKind.STOP,
            timestampMillis = SAMPLE_TIME,
            latitude = 47.5,
            longitude = 19.05,
            speedMps = 0f,
            tempCelsius = null,
            maxSpeedMps = 12.0f,
            averageSpeedMps = 6.0f,
            elapsedMillis = 60 * 60 * 1000L
        )
        assertTrue(stop.contains("Duration: 01:00:00"))
        assertFalse(stop.contains("Duration: 60 min"))
    }

    @Test
    fun stopBalloonUsesImperialAndIcaoIntegerSpeeds() {
        val imperial = KmlDescriptions.balloon(
            kind = EventKind.STOP,
            timestampMillis = SAMPLE_TIME,
            latitude = 47.5,
            longitude = 19.05,
            speedMps = 0f,
            tempCelsius = null,
            maxSpeedMps = 12.0f,
            averageSpeedMps = 6.0f,
            elapsedMillis = 10 * 60 * 1000L,
            system = MeasurementSystem.IMPERIAL
        )
        val icao = KmlDescriptions.balloon(
            kind = EventKind.STOP,
            timestampMillis = SAMPLE_TIME,
            latitude = 47.5,
            longitude = 19.05,
            speedMps = 0f,
            tempCelsius = null,
            maxSpeedMps = 12.0f,
            averageSpeedMps = 6.0f,
            elapsedMillis = 10 * 60 * 1000L,
            system = MeasurementSystem.ICAO
        )
        assertTrue(imperial.contains("Avg. speed: 13 mile/h"))
        assertTrue(imperial.contains("Max. speed: 27 mile/h"))
        assertTrue(imperial.contains("speed=0.0 mph"))
        assertTrue(icao.contains("Avg. speed: 12 kt"))
        assertTrue(icao.contains("Max. speed: 23 kt"))
    }

    @Test
    fun balloonsIncludeTimeLatLonAndMovingSpeed() {
        val start = KmlDescriptions.balloon(
            kind = EventKind.START,
            timestampMillis = SAMPLE_TIME,
            latitude = 47.5,
            longitude = 19.05,
            speedMps = 5.5f,
            tempCelsius = null
        )
        assertTrue(start.contains("time=2024-09-05 01:33:20 UTC"))
        assertTrue(start.contains("lat=47.500000"))
        assertTrue(start.contains("lon=19.050000"))
        assertTrue(start.contains("speed=19.8 km/h"))
        val move = KmlDescriptions.balloon(
            kind = EventKind.MOVE,
            timestampMillis = SAMPLE_TIME,
            latitude = 47.51,
            longitude = 19.06,
            speedMps = 8.3f,
            tempCelsius = null
        )
        assertTrue(move.contains("speed=29.9 km/h"))
        assertTrue(move.contains("lat=47.510000"))
        assertTrue(move.contains("lon=19.060000"))
    }

    @Test
    fun balloonsIncludeLeanWhenPresent() {
        val start = KmlDescriptions.balloon(
            kind = EventKind.START,
            timestampMillis = SAMPLE_TIME,
            latitude = 47.5,
            longitude = 19.05,
            speedMps = 5.5f,
            tempCelsius = null,
            leanAngle = 12.4f
        )
        assertTrue(start.contains("lean=12.4"))
    }

    @Test
    fun startPauseStopBalloonsIncludeUsageType() {
        val start = KmlDescriptions.balloon(
            kind = EventKind.START,
            timestampMillis = SAMPLE_TIME,
            latitude = 47.5,
            longitude = 19.05,
            speedMps = 5.5f,
            tempCelsius = null,
            usageType = "Motorbike"
        )
        val pause = KmlDescriptions.balloon(
            kind = EventKind.PAUSE,
            timestampMillis = SAMPLE_TIME,
            latitude = 47.5,
            longitude = 19.05,
            speedMps = 0f,
            tempCelsius = null,
            usageType = "Runner"
        )
        val stop = KmlDescriptions.balloon(
            kind = EventKind.STOP,
            timestampMillis = SAMPLE_TIME,
            latitude = 47.5,
            longitude = 19.05,
            speedMps = 0f,
            tempCelsius = null,
            maxSpeedMps = 12.0f,
            averageSpeedMps = 6.0f,
            elapsedMillis = 10 * 60 * 1000L,
            usageType = "Aircraft"
        )
        val move = KmlDescriptions.balloon(
            kind = EventKind.MOVE,
            timestampMillis = SAMPLE_TIME,
            latitude = 47.5,
            longitude = 19.05,
            speedMps = 8.3f,
            tempCelsius = null,
            usageType = "Car"
        )
        assertTrue(start.contains("usage=Motorbike"))
        assertTrue(pause.contains("usage=Runner"))
        assertTrue(stop.contains("usage=Aircraft"))
        assertFalse(move.contains("usage="))
    }

    @Test
    fun usageTypeKmlLabelsMatchSettingsNames() {
        assertEquals("Aircraft", UsageType.AIRCRAFT.kmlLabel())
        assertEquals("Watercraft", UsageType.WATERCRAFT.kmlLabel())
        assertEquals("Car", UsageType.FOUR_WHEELERS.kmlLabel())
        assertEquals("Motorbike", UsageType.TWO_WHEELERS.kmlLabel())
        assertEquals("Bicycle", UsageType.BICYCLE.kmlLabel())
        assertEquals("Runner", UsageType.RUNNER.kmlLabel())
        assertEquals("Motorbike", UsageType.kmlLabelOf("TWO_WHEELERS"))
        assertEquals(null, UsageType.kmlLabelOf(null))
    }

    @Test
    fun kmzBundlesDocKmlAndIcons() {
        val packed = KmzExporter.pack(
            sampleKml(),
            mapOf(
                "icons/play.png" to byteArrayOf(0x89.toByte(), 0x50),
                "icons/pause.png" to byteArrayOf(0x89.toByte(), 0x50),
                "icons/stop.png" to byteArrayOf(0x89.toByte(), 0x50)
            )
        )
        val names = zipNames(packed)
        assertTrue(names.contains("doc.kml"))
        assertTrue(names.contains("icons/play.png"))
        assertTrue(names.contains("icons/pause.png"))
        assertTrue(names.contains("icons/stop.png"))
        assertEquals("doc.kml", names.first())
    }

    @Test
    fun exportsEachSelectedTrackInItsOwnFolder() {
        val kml = KmlExporter.export(
            KmlDocument(
                name = "GTL export",
                trackColorAabbggrr = "ff0000ff",
                trackWidth = 6,
                tracks = listOf(
                    KmlTrack(
                        name = "Morning",
                        points = listOf(
                            KmlVertex(GeoPoint(47.5, 19.05, 120.0), SAMPLE_TIME, 5.5f),
                            KmlVertex(GeoPoint(47.51, 19.06, 125.0), SAMPLE_TIME + 1000, 6.0f)
                        ),
                        placemarks = emptyList()
                    ),
                    KmlTrack(
                        name = "Evening",
                        points = listOf(
                            KmlVertex(GeoPoint(47.6, 19.1, 130.0), SAMPLE_TIME, 4.0f),
                            KmlVertex(GeoPoint(47.61, 19.11, 135.0), SAMPLE_TIME + 1000, 4.2f)
                        ),
                        placemarks = emptyList()
                    )
                )
            )
        )
        assertEquals(2, "<gx:Track>".toRegex().findAll(kml).count())
        assertTrue(kml.contains("<name>Morning</name>"))
        assertTrue(kml.contains("<name>Evening</name>"))
        assertTrue(kml.contains("<Folder>"))
    }

    private fun sampleKml(): String {
        return KmlExporter.export(
            KmlDocument(
                name = "Ride <1>",
                trackColorAabbggrr = "ff0000ff",
                trackWidth = 6,
                tracks = listOf(
                    KmlTrack(
                        name = "Ride <1>",
                        points = listOf(
                            KmlVertex(GeoPoint(47.5, 19.05, 120.0), SAMPLE_TIME, 5.5f),
                            KmlVertex(GeoPoint(47.51, 19.06, 125.0), SAMPLE_TIME + 1000, 6.0f)
                        ),
                        placemarks = listOf(
                            KmlPlacemark("Start", EventKind.START, GeoPoint(47.5, 19.05, 120.0), "begin")
                        )
                    )
                )
            )
        )
    }

    private fun zipNames(bytes: ByteArray): List<String> {
        java.util.zip.ZipInputStream(bytes.inputStream()).use { zip ->
            return generateSequence { zip.nextEntry }.map { it.name }.toList()
        }
    }
}

class UsageTypeTest {
    @Test
    fun runnerUsesMotorbikeSpacingWithTerrainAccuracy() {
        val bike = UsageType.TWO_WHEELERS.defaultFilter()
        val runner = UsageType.RUNNER.defaultFilter()
        assertEquals(bike.minDistanceMeters, runner.minDistanceMeters)
        assertEquals(bike.minTimeMillis, runner.minTimeMillis)
        assertEquals(bike.minSatellites, runner.minSatellites)
        assertTrue(runner.minAccuracyMeters > bike.minAccuracyMeters)
        assertTrue(UsageType.RUNNER.pauseSpeedMps() < UsageType.TWO_WHEELERS.pauseSpeedMps())
    }

    @Test
    fun selectableModesIncludeRunnerAndDefaultMotorbike() {
        assertEquals(UsageType.TWO_WHEELERS, UsageType.selectable[3])
        assertEquals(UsageType.BICYCLE, UsageType.selectable[4])
        assertTrue(UsageType.selectable.contains(UsageType.RUNNER))
        assertEquals(6, UsageType.selectable.size)
    }

    @Test
    fun runnerSmoothingKeepsEveryFixAndDisablesMapSimplify() {
        val defaults = UsageType.RUNNER.defaultSmoothing()
        assertFalse(defaults.trackSmoothingEnabled)
        assertEquals(SmoothingStrength.LOW, defaults.smoothingStrength)
        assertTrue(defaults.stationaryLockEnabled)
        assertEquals(RecordingDensity.EVERY_FIX, defaults.recordingDensity)
        assertFalse(defaults.optimizationActive)
        assertEquals(2.0, defaults.optimizationToleranceMeters, 0.0)
        assertTrue(defaults.gnssOnly)
        assertEquals(8.0, UsageType.RUNNER.processNoiseQ(), 0.0)
        assertEquals(10.0, UsageType.RUNNER.turnBoost(), 0.0)
    }

    @Test
    fun bicycleSmoothingKeepsEveryFixAndDisablesMapSimplify() {
        val defaults = UsageType.BICYCLE.defaultSmoothing()
        assertFalse(defaults.trackSmoothingEnabled)
        assertEquals(SmoothingStrength.LOW, defaults.smoothingStrength)
        assertTrue(defaults.stationaryLockEnabled)
        assertEquals(RecordingDensity.EVERY_FIX, defaults.recordingDensity)
        assertFalse(defaults.optimizationActive)
        assertEquals(3.0, defaults.optimizationToleranceMeters, 0.0)
        assertTrue(defaults.gnssOnly)
        assertTrue(UsageType.BICYCLE.isPedestrianMode())
        assertEquals(UsageType.RUNNER.pauseSpeedMps(), UsageType.BICYCLE.pauseSpeedMps())
        assertEquals(6.0, UsageType.BICYCLE.processNoiseQ(), 0.0)
        assertEquals(8.0, UsageType.BICYCLE.turnBoost(), 0.0)
        assertEquals(45, UsageType.BICYCLE.defaultFilter().minAccuracyMeters)
        assertEquals(MeasurementSystem.METRIC, UsageType.BICYCLE.defaultMeasurementSystem())
    }

    @Test
    fun motorbikeSmoothingUsesSmartDensityAndSixMetreSimplify() {
        val defaults = UsageType.TWO_WHEELERS.defaultSmoothing()
        assertTrue(defaults.trackSmoothingEnabled)
        assertEquals(SmoothingStrength.MEDIUM, defaults.smoothingStrength)
        assertTrue(defaults.stationaryLockEnabled)
        assertEquals(RecordingDensity.SMART, defaults.recordingDensity)
        assertTrue(defaults.optimizationActive)
        assertEquals(6.0, defaults.optimizationToleranceMeters, 0.0)
        assertFalse(defaults.gnssOnly)
        assertEquals(2.5, UsageType.TWO_WHEELERS.processNoiseQ(), 0.0)
        assertEquals(5.0, UsageType.TWO_WHEELERS.turnBoost(), 0.0)
    }

    @Test
    fun carAndAircraftSmoothingMatchUsageTable() {
        val car = UsageType.FOUR_WHEELERS.defaultSmoothing()
        assertEquals(8.0, car.optimizationToleranceMeters, 0.0)
        assertEquals(RecordingDensity.SMART, car.recordingDensity)
        assertEquals(SmoothingStrength.MEDIUM, car.smoothingStrength)
        val air = UsageType.AIRCRAFT.defaultSmoothing()
        assertEquals(15.0, air.optimizationToleranceMeters, 0.0)
        assertEquals(SmoothingStrength.HIGH, air.smoothingStrength)
        assertEquals(MeasurementSystem.ICAO, UsageType.AIRCRAFT.defaultMeasurementSystem())
        assertEquals(MeasurementSystem.ICAO, UsageType.WATERCRAFT.defaultMeasurementSystem())
        assertEquals(MeasurementSystem.METRIC, UsageType.TWO_WHEELERS.defaultMeasurementSystem())
        assertEquals(MeasurementSystem.METRIC, UsageType.FOUR_WHEELERS.defaultMeasurementSystem())
        assertEquals(MeasurementSystem.METRIC, UsageType.RUNNER.defaultMeasurementSystem())
        assertEquals(
            UsageType.RUNNER.defaultSmoothing(),
            UsageType.WALKING_HIKE.defaultSmoothing()
        )
        assertEquals(
            UsageType.RUNNER.defaultSmoothing(),
            UsageType.PEDESTRIAN.defaultSmoothing()
        )
    }
}

class MapTrackVisibilityTest {
    @Test
    fun liveLoggingAlwaysDraws() {
        assertTrue(MapTrackVisibility.visible(logging = true, showLastTrackOnMap = false, selectedSessionId = null))
    }

    @Test
    fun explicitSelectionDrawsEvenWhenLastTrackHidden() {
        assertTrue(MapTrackVisibility.visible(logging = false, showLastTrackOnMap = false, selectedSessionId = 12L))
    }

    @Test
    fun lastTrackSettingDrawsWithoutSelection() {
        assertTrue(MapTrackVisibility.visible(logging = false, showLastTrackOnMap = true, selectedSessionId = null))
    }

    @Test
    fun idleWithoutSelectionHidesTrack() {
        assertFalse(MapTrackVisibility.visible(logging = false, showLastTrackOnMap = false, selectedSessionId = null))
    }

    @Test
    fun clearedMapHidesTrackEvenIfLastTrackSettingIsOn() {
        assertFalse(
            MapTrackVisibility.visible(
                logging = false,
                showLastTrackOnMap = true,
                selectedSessionId = null,
                mapCleared = true
            )
        )
    }

    @Test
    fun loggingStillDrawsAfterMapWasCleared() {
        assertTrue(
            MapTrackVisibility.visible(
                logging = true,
                showLastTrackOnMap = true,
                selectedSessionId = null,
                mapCleared = true
            )
        )
    }
}

class MapDisplayUsageTest {
    @Test
    fun selectedSavedTrackFollowsSettings() {
        assertTrue(MapDisplayUsage.followsSettings(logging = false, selectedSessionId = 12L))
        assertTrue(MapDisplayUsage.followsSettings(logging = true, selectedSessionId = null))
        assertFalse(MapDisplayUsage.followsSettings(logging = false, selectedSessionId = null))
    }

    @Test
    fun historicalTrackUsesSessionUsageEvenIfSettingsAreMotorbike() {
        val usage = MapDisplayUsage.of(
            logging = false,
            followSettings = false,
            settingsUsage = UsageType.TWO_WHEELERS,
            sessionUsageName = "RUNNER"
        )
        val simplify = MapDisplayUsage.simplify(
            usage = usage,
            logging = false,
            followSettings = false,
            settingsActive = true,
            settingsTolerance = 6.0
        )
        assertEquals(UsageType.RUNNER, usage)
        assertFalse(simplify.first)
        assertEquals(2.0, simplify.second, 0.0)
    }

    @Test
    fun loggingUsesSettingsUsage() {
        val usage = MapDisplayUsage.of(
            logging = true,
            followSettings = false,
            settingsUsage = UsageType.TWO_WHEELERS,
            sessionUsageName = "RUNNER"
        )
        assertEquals(UsageType.TWO_WHEELERS, usage)
    }

    @Test
    fun followSettingsUsesSettingsSimplifyForPreview() {
        val usage = MapDisplayUsage.of(
            logging = false,
            followSettings = true,
            settingsUsage = UsageType.TWO_WHEELERS,
            sessionUsageName = "RUNNER"
        )
        val simplify = MapDisplayUsage.simplify(
            usage = usage,
            logging = false,
            followSettings = true,
            settingsActive = true,
            settingsTolerance = 6.0
        )
        assertEquals(UsageType.TWO_WHEELERS, usage)
        assertTrue(simplify.first)
        assertEquals(6.0, simplify.second, 0.0)
    }

    @Test
    fun unknownSessionUsageFallsBackToSettings() {
        val usage = MapDisplayUsage.of(
            logging = false,
            followSettings = false,
            settingsUsage = UsageType.TWO_WHEELERS,
            sessionUsageName = "NOT_A_USAGE"
        )
        assertEquals(UsageType.TWO_WHEELERS, usage)
    }
}

class TrackCameraBoundsTest {
    @Test
    fun emptyWithoutExtraIsNull() {
        assertEquals(null, TrackCameraBounds.of(emptyList(), extra = null))
    }

    @Test
    fun extraAloneBecomesDegenerateBounds() {
        val extra = GeoPoint(47.5, 19.05)
        val bounds = TrackCameraBounds.of(emptyList(), extra)
        assertEquals(47.5, bounds!!.minLatitude, 0.0)
        assertEquals(19.05, bounds.minLongitude, 0.0)
        assertEquals(47.5, bounds.maxLatitude, 0.0)
        assertEquals(19.05, bounds.maxLongitude, 0.0)
        assertTrue(bounds.isDegenerate)
    }

    @Test
    fun singlePointIsDegenerate() {
        val bounds = TrackCameraBounds.of(listOf(GeoPoint(47.0, 19.0)), extra = null)
        assertTrue(bounds!!.isDegenerate)
        assertEquals(47.0, bounds.minLatitude, 0.0)
        assertEquals(19.0, bounds.minLongitude, 0.0)
    }

    @Test
    fun multiplePointsSpanMinMaxAndIncludeExtra() {
        val bounds = TrackCameraBounds.of(
            listOf(
                GeoPoint(47.0, 19.0),
                GeoPoint(47.2, 18.8),
                GeoPoint(46.9, 19.1)
            ),
            extra = GeoPoint(47.3, 18.7)
        )
        assertFalse(bounds!!.isDegenerate)
        assertEquals(46.9, bounds.minLatitude, 0.0)
        assertEquals(18.7, bounds.minLongitude, 0.0)
        assertEquals(47.3, bounds.maxLatitude, 0.0)
        assertEquals(19.1, bounds.maxLongitude, 0.0)
    }
}

class BikeLeanAngleTest {
    @Test
    fun uprightIsZero() {
        assertEquals(0f, BikeLeanAngle.fromGravity(0f, 0f, 9.81f), 0.01f)
    }

    @Test
    fun fortyFiveDegreesRight() {
        val component = (9.81 / kotlin.math.sqrt(2.0)).toFloat()
        assertEquals(45f, BikeLeanAngle.fromGravity(-component, 0f, component), 0.5f)
    }

    @Test
    fun fortyFiveDegreesLeft() {
        val component = (9.81 / kotlin.math.sqrt(2.0)).toFloat()
        assertEquals(-45f, BikeLeanAngle.fromGravity(component, 0f, component), 0.5f)
    }
}

class TrackStatsCalculatorTest {
    @Test
    fun computesDistanceAndTemperatureRange() {
        val samples = listOf(
            TrackSample(0, 47.0, 19.0, 100.0, 0f, 0f, 18.0f, EventKind.START),
            TrackSample(10_000, 47.001, 19.0, 110.0, 8f, 0f, 21.5f, EventKind.MOVE)
        )
        val stats = TrackStatsCalculator.compute(samples)
        assertTrue(stats.odometerMeters > 90.0)
        assertEquals(18.0f, stats.temperatureRange?.minCelsius)
        assertEquals(21.5f, stats.temperatureRange?.maxCelsius)
        assertEquals(10_000L, stats.elapsedMillis)
    }
}

class KalmanTrackFilterTest {
    private val originLat = 47.0
    private val originLon = 19.0

    @Test
    fun highwaySmoothedCrossTrackRmseIsLowerAndLengthStaysNearTruth() {
        val filter = KalmanTrackFilter()
        val raw = mutableListOf<TrackFix>()
        val smoothed = mutableListOf<TrackFix>()
        val count = 20
        val spacing = 20.0
        val speed = 20f
        for (i in 0 until count) {
            val east = i * spacing
            val noise = 8.0 * kotlin.math.sin(i * 1.7)
            val ll = GeoProjection.latLon(originLat, originLon, east, noise)
            val fix = sample(
                time = 10_000L + i * 1_000L,
                lat = ll.first,
                lon = ll.second,
                speedMps = speed,
                bearing = 90f
            )
            raw.add(fix)
            smoothed.add(
                filter.observe(
                    fix,
                    UsageType.FOUR_WHEELERS,
                    SmoothingStrength.MEDIUM,
                    stationaryLock = true
                )
            )
        }
        val rawRmse = crossTrackRmse(raw, spacing)
        val smoothRmse = crossTrackRmse(smoothed, spacing)
        assertTrue(smoothRmse < rawRmse)
        val length = pathLength(smoothed)
        assertTrue(kotlin.math.abs(length - 400.0) <= 20.0)
    }

    @Test
    fun roundaboutStaysWithinSixMetresOfTrueCircle() {
        val filter = KalmanTrackFilter()
        val radius = 25.0
        val speed = 8.0
        val omega = speed / radius
        val steps = 40
        var maxError = 0.0
        for (i in 0 until steps) {
            val theta = i * omega
            val east = radius * kotlin.math.sin(theta)
            val north = radius - radius * kotlin.math.cos(theta)
            val ll = GeoProjection.latLon(originLat, originLon, east, north)
            val bearing = ((Math.toDegrees(kotlin.math.atan2(kotlin.math.cos(theta), kotlin.math.sin(theta))) + 360.0) % 360.0).toFloat()
            val dt = 1000L
            val out = filter.observe(
                sample(
                    time = 10_000L + i * dt,
                    lat = ll.first,
                    lon = ll.second,
                    speedMps = speed.toFloat(),
                    bearing = bearing
                ),
                UsageType.FOUR_WHEELERS,
                SmoothingStrength.MEDIUM,
                stationaryLock = true
            )
            val en = GeoProjection.eastNorth(originLat, originLon, out.latitude, out.longitude)
            val dist = GeoProjection.hypot(en.first, en.second - radius)
            val error = kotlin.math.abs(dist - radius)
            if (error > maxError) {
                maxError = error
            }
        }
        assertTrue(maxError <= 6.0)
    }

    @Test
    fun runnerFigureEightKeepsTwoLobes() {
        val filter = KalmanTrackFilter()
        val radius = 12.0
        val speed = 3.0
        val points = mutableListOf<Pair<Double, Double>>()
        val left = lobePoints(radius, left = true)
        val right = lobePoints(radius, left = false)
        val path = left + right.drop(1)
        val arc = 2.0 * Math.PI * radius
        val dt = 500L
        var time = 10_000L
        var prevE = path[0].first
        var prevN = path[0].second
        for (i in path.indices) {
            val east = path[i].first
            val north = path[i].second
            val ll = GeoProjection.latLon(originLat, originLon, east, north)
            val bearing = if (i == 0) {
                0f
            } else {
                heading(prevE, prevN, east, north)
            }
            if (i > 0) {
                val step = GeoProjection.hypot(east - prevE, north - prevN)
                time += ((step / speed) * 1000.0).toLong().coerceAtLeast(dt)
            }
            val out = filter.observe(
                sample(
                    time = time,
                    lat = ll.first,
                    lon = ll.second,
                    speedMps = speed.toFloat(),
                    bearing = if (bearing == 0f) 1f else bearing
                ),
                UsageType.RUNNER,
                SmoothingStrength.LOW,
                stationaryLock = true
            )
            val en = GeoProjection.eastNorth(originLat, originLon, out.latitude, out.longitude)
            points.add(en)
            prevE = east
            prevN = north
        }
        val mid = pathLengthPoints(points) 
        val startEnd = GeoProjection.hypot(
            points.last().first - points.first().first,
            points.last().second - points.first().second
        )
        var maxPerp = 0.0
        val dx = points.last().first - points.first().first
        val dy = points.last().second - points.first().second
        val chord2 = dx * dx + dy * dy
        for (p in points) {
            val dist = if (chord2 < 1e-6) {
                GeoProjection.hypot(p.first - points.first().first, p.second - points.first().second)
            } else {
                val t = ((p.first - points.first().first) * dx + (p.second - points.first().second) * dy) / chord2
                val px = points.first().first + t * dx
                val py = points.first().second + t * dy
                GeoProjection.hypot(p.first - px, p.second - py)
            }
            if (dist > maxPerp) {
                maxPerp = dist
            }
        }
        assertTrue(maxPerp >= 8.0)
        val half = points.size / 2
        val c1 = centroid(points.subList(0, half))
        val c2 = centroid(points.subList(half, points.size))
        val lobeGap = GeoProjection.hypot(c1.first - c2.first, c1.second - c2.second)
        assertTrue(lobeGap >= 8.0)
        assertTrue(mid > arc)
        assertTrue(startEnd < 8.0 || maxPerp >= 8.0)
    }

    @Test
    fun runnerRoadLoopsStayOffTheStreetWhenKalmanIsOff() {
        val path = tripleRoadLoops()
        val filter = UsageType.RUNNER.defaultFilter()
        var previous: TrackFix? = null
        val stored = mutableListOf<Pair<Double, Double>>()
        for (fix in path) {
            if (FixAcceptance.shouldAccept(
                    previous,
                    fix,
                    filter,
                    RecordingDensity.EVERY_FIX,
                    UsageType.RUNNER
                )
            ) {
                val en = GeoProjection.eastNorth(originLat, originLon, fix.latitude, fix.longitude)
                stored.add(en)
                previous = fix
            }
        }
        assertTrue(stored.size > 20)
        val maxNorth = stored.maxOf { kotlin.math.abs(it.second) }
        assertTrue(maxNorth >= 3.0)
    }

    @Test
    fun runnerRoadLoopsSurviveKalmanLow() {
        val filter = KalmanTrackFilter()
        val path = tripleRoadLoops()
        var maxNorth = 0.0
        for (fix in path) {
            val out = filter.observe(
                fix,
                UsageType.RUNNER,
                SmoothingStrength.LOW,
                stationaryLock = true
            )
            val en = GeoProjection.eastNorth(originLat, originLon, out.latitude, out.longitude)
            if (kotlin.math.abs(en.second) > maxNorth) {
                maxNorth = kotlin.math.abs(en.second)
            }
        }
        assertTrue(maxNorth >= 3.0)
    }

    @Test
    fun runnerZigzagKeepsAmplitude() {
        val filter = KalmanTrackFilter()
        val amplitude = 6.0
        val wavelength = 4.0
        val legs = 8
        val speed = 3.0
        var minN = Double.POSITIVE_INFINITY
        var maxN = Double.NEGATIVE_INFINITY
        var prevE = 0.0
        var prevN = 0.0
        var time = 10_000L
        for (i in 0..legs) {
            val east = i * (wavelength / 2.0)
            val north = if (i % 2 == 0) amplitude else -amplitude
            val ll = GeoProjection.latLon(originLat, originLon, east, north)
            val bearing = if (i == 0) {
                45f
            } else {
                heading(prevE, prevN, east, north)
            }
            if (i > 0) {
                val step = GeoProjection.hypot(east - prevE, north - prevN)
                time += ((step / speed) * 1000.0).toLong().coerceAtLeast(400L)
            }
            val out = filter.observe(
                sample(
                    time = time,
                    lat = ll.first,
                    lon = ll.second,
                    speedMps = speed.toFloat(),
                    bearing = bearing
                ),
                UsageType.RUNNER,
                SmoothingStrength.LOW,
                stationaryLock = true
            )
            val en = GeoProjection.eastNorth(originLat, originLon, out.latitude, out.longitude)
            if (en.second < minN) {
                minN = en.second
            }
            if (en.second > maxN) {
                maxN = en.second
            }
            prevE = east
            prevN = north
        }
        assertTrue(maxN - minN >= 4.0)
    }

    @Test
    fun stationaryLockCollapsesWander() {
        val filter = KalmanTrackFilter()
        val first = sample(10_000L, originLat, originLon, speedMps = 0f, bearing = 0f)
        val outputs = mutableListOf<TrackFix>()
        outputs.add(
            filter.observe(first, UsageType.FOUR_WHEELERS, SmoothingStrength.MEDIUM, true)
        )
        for (i in 1 until 30) {
            val east = 6.0 * kotlin.math.sin(i * 2.1)
            val north = 6.0 * kotlin.math.cos(i * 1.3)
            val ll = GeoProjection.latLon(originLat, originLon, east, north)
            outputs.add(
                filter.observe(
                    sample(
                        time = 10_000L + i * 1_000L,
                        lat = ll.first,
                        lon = ll.second,
                        speedMps = 0f,
                        bearing = 0f
                    ),
                    UsageType.FOUR_WHEELERS,
                    SmoothingStrength.MEDIUM,
                    stationaryLock = true
                )
            )
        }
        val anchor = outputs.first()
        for (out in outputs) {
            val d = FixAcceptance.haversineMeters(
                anchor.latitude,
                anchor.longitude,
                out.latitude,
                out.longitude
            )
            assertTrue(d <= 2.0)
        }
    }

    @Test
    fun jumpReinitializesWithoutInterpolating() {
        val filter = KalmanTrackFilter()
        val start = sample(10_000L, originLat, originLon, speedMps = 10f, bearing = 90f)
        filter.observe(start, UsageType.FOUR_WHEELERS, SmoothingStrength.MEDIUM, true)
        val jumpedLl = GeoProjection.latLon(originLat, originLon, 100.0, 0.0)
        val jumped = sample(
            time = 11_000L,
            lat = jumpedLl.first,
            lon = jumpedLl.second,
            speedMps = 10f,
            bearing = 90f,
            accuracy = 5f
        )
        val out = filter.observe(jumped, UsageType.FOUR_WHEELERS, SmoothingStrength.MEDIUM, true)
        val dNew = FixAcceptance.haversineMeters(
            jumped.latitude,
            jumped.longitude,
            out.latitude,
            out.longitude
        )
        val dMid = FixAcceptance.haversineMeters(
            originLat,
            originLon,
            out.latitude,
            out.longitude
        )
        assertTrue(dNew < 15.0)
        assertTrue(dMid > 70.0)
    }

    private fun crossTrackRmse(points: List<TrackFix>, spacing: Double): Double {
        var sum = 0.0
        for (i in points.indices) {
            val en = GeoProjection.eastNorth(originLat, originLon, points[i].latitude, points[i].longitude)
            val expectedEast = i * spacing
            val err = GeoProjection.hypot(en.first - expectedEast, en.second)
            sum += err * err
        }
        return kotlin.math.sqrt(sum / points.size)
    }

    private fun pathLength(points: List<TrackFix>): Double {
        var sum = 0.0
        for (i in 1 until points.size) {
            sum += FixAcceptance.haversineMeters(
                points[i - 1].latitude,
                points[i - 1].longitude,
                points[i].latitude,
                points[i].longitude
            )
        }
        return sum
    }

    private fun pathLengthPoints(points: List<Pair<Double, Double>>): Double {
        var sum = 0.0
        for (i in 1 until points.size) {
            sum += GeoProjection.hypot(
                points[i].first - points[i - 1].first,
                points[i].second - points[i - 1].second
            )
        }
        return sum
    }

    private fun centroid(points: List<Pair<Double, Double>>): Pair<Double, Double> {
        var e = 0.0
        var n = 0.0
        for (p in points) {
            e += p.first
            n += p.second
        }
        val count = points.size.toDouble()
        return e / count to n / count
    }

    private fun lobePoints(radius: Double, left: Boolean): List<Pair<Double, Double>> {
        val steps = 24
        val out = mutableListOf<Pair<Double, Double>>()
        val cx = if (left) -radius else radius
        for (i in 0..steps) {
            val theta = if (left) {
                (i / steps.toDouble()) * 2.0 * Math.PI
            } else {
                Math.PI - (i / steps.toDouble()) * 2.0 * Math.PI
            }
            out.add(cx + radius * kotlin.math.cos(theta) to radius * kotlin.math.sin(theta))
        }
        return out
    }

    private fun tripleRoadLoops(): List<TrackFix> {
        val radius = 5.0
        val speed = 3.0
        val centers = listOf(5.0, 15.0, 25.0)
        val coords = mutableListOf<Pair<Double, Double>>()
        coords.add(0.0 to 0.0)
        var x = 0.0
        for (center in centers) {
            while (x < center - 0.01) {
                x = (x + 1.2).coerceAtMost(center)
                coords.add(x to 0.0)
            }
            val steps = 16
            for (i in 1..steps) {
                val theta = i * 2.0 * Math.PI / steps
                coords.add(
                    center + radius * kotlin.math.sin(theta) to
                        radius - radius * kotlin.math.cos(theta)
                )
            }
            x = center
        }
        while (x < 40.0) {
            x += 1.2
            coords.add(x.coerceAtMost(40.0) to 0.0)
        }
        val out = mutableListOf<TrackFix>()
        var time = 10_000L
        var prevE = coords[0].first
        var prevN = coords[0].second
        for (i in coords.indices) {
            val east = coords[i].first
            val north = coords[i].second
            val ll = GeoProjection.latLon(originLat, originLon, east, north)
            val bearing = if (i == 0) {
                90f
            } else {
                heading(prevE, prevN, east, north)
            }
            if (i > 0) {
                val step = GeoProjection.hypot(east - prevE, north - prevN)
                time += ((step / speed) * 1000.0).toLong().coerceAtLeast(400L)
            }
            out.add(
                sample(
                    time = time,
                    lat = ll.first,
                    lon = ll.second,
                    speedMps = speed.toFloat(),
                    bearing = if (bearing == 0f) 1f else bearing
                )
            )
            prevE = east
            prevN = north
        }
        return out
    }

    private fun heading(fromE: Double, fromN: Double, toE: Double, toN: Double): Float {
        val deg = Math.toDegrees(kotlin.math.atan2(toE - fromE, toN - fromN))
        return ((deg + 360.0) % 360.0).toFloat()
    }

    private fun sample(
        time: Long,
        lat: Double,
        lon: Double,
        speedMps: Float,
        bearing: Float,
        accuracy: Float = 5f
    ): TrackFix {
        return TrackFix(
            timestampMillis = time,
            latitude = lat,
            longitude = lon,
            altitude = 100.0,
            speedMps = speedMps,
            bearing = bearing,
            accuracyMeters = accuracy,
            satellitesInFix = 8
        )
    }
}

class FixCloudBufferTest {
    @Test
    fun gaussianCloudHasExpectedRmsAndCep95() {
        val buffer = FixCloudBuffer()
        val originLat = 47.0
        val originLon = 19.0
        val random = java.util.Random(42)
        val sigma = 1.0 / kotlin.math.sqrt(2.0)
        repeat(20) { i ->
            val east = random.nextGaussian() * sigma
            val north = random.nextGaussian() * sigma
            val ll = GeoProjection.latLon(originLat, originLon, east, north)
            buffer.observe(
                sample(1_000L * (i + 1), ll.first, ll.second, 4f, 0f),
                pauseSpeedMps = 0.4f
            )
        }
        val stats = buffer.snapshot().stats
        assertEquals(20, stats.sampleCount)
        val rms = stats.rmsMeters ?: 0.0
        val cep95 = stats.cep95Meters ?: 0.0
        assertTrue(rms in 0.7..1.3)
        assertTrue(cep95 in 1.2..2.5)
    }

    @Test
    fun tenCentimetreDuplicateDoesNotIncreaseCount() {
        val buffer = FixCloudBuffer()
        val origin = GeoProjection.latLon(47.0, 19.0, 0.0, 0.0)
        val nearby = GeoProjection.latLon(47.0, 19.0, 0.10, 0.0)
        buffer.observe(sample(1_000L, origin.first, origin.second, 4f, 0f), 0.4f)
        buffer.observe(sample(2_000L, nearby.first, nearby.second, 4f, 0f), 0.4f)
        assertEquals(1, buffer.snapshot().stats.sampleCount)
    }

    @Test
    fun movingSamplesAreIgnoredThenThreeStationaryFixesClearBuffer() {
        val buffer = FixCloudBuffer()
        repeat(5) { i ->
            val ll = GeoProjection.latLon(47.0, 19.0, i * 0.5, 0.0)
            buffer.observe(sample(1_000L * (i + 1), ll.first, ll.second, 4f, 0f), 0.4f)
        }
        assertEquals(5, buffer.snapshot().stats.sampleCount)
        repeat(3) { i ->
            val ll = GeoProjection.latLon(47.0, 19.0, 10.0 + i, 0.0)
            buffer.observe(sample(10_000L + i, ll.first, ll.second, 4f, 5f), 0.4f)
        }
        val paused = buffer.snapshot()
        assertEquals(5, paused.stats.sampleCount)
        assertFalse(paused.stats.active)
        repeat(2) { i ->
            val ll = GeoProjection.latLon(47.0, 19.0, 20.0 + i * 0.5, 0.0)
            buffer.observe(sample(20_000L + i, ll.first, ll.second, 4f, 0f), 0.4f)
        }
        assertEquals(5, buffer.snapshot().stats.sampleCount)
        val resume = GeoProjection.latLon(47.0, 19.0, 21.0, 0.0)
        buffer.observe(sample(20_002L, resume.first, resume.second, 4f, 0f), 0.4f)
        val afterClear = buffer.snapshot()
        assertEquals(1, afterClear.stats.sampleCount)
        assertTrue(afterClear.stats.active)
        val next = GeoProjection.latLon(47.0, 19.0, 21.5, 0.0)
        buffer.observe(sample(20_003L, next.first, next.second, 4f, 0f), 0.4f)
        assertEquals(2, buffer.snapshot().stats.sampleCount)
    }

    @Test
    fun sevenPointsHaveRmsButNoCep95() {
        val buffer = FixCloudBuffer()
        repeat(7) { i ->
            val ll = GeoProjection.latLon(47.0, 19.0, i * 0.5, 0.0)
            buffer.observe(sample(1_000L * (i + 1), ll.first, ll.second, 4f, 0f), 0.4f)
        }
        val stats = buffer.snapshot().stats
        assertEquals(7, stats.sampleCount)
        assertTrue(stats.rmsMeters != null)
        assertTrue(stats.cep95Meters == null)
    }

    @Test
    fun centroidIsNotTheLastFixAndCepIsFromCentroid() {
        val buffer = FixCloudBuffer()
        val cluster = listOf(
            0.0 to 0.0,
            0.4 to 0.2,
            -0.3 to 0.1,
            0.2 to -0.4,
            -0.1 to -0.2,
            0.3 to 0.3,
            -0.2 to 0.4,
            0.1 to -0.3
        )
        cluster.forEachIndexed { i, (east, north) ->
            val ll = GeoProjection.latLon(47.0, 19.0, 40.0 + east, north)
            buffer.observe(sample(1_000L * (i + 1), ll.first, ll.second, 4f, 0f), 0.4f)
        }
        val last = GeoProjection.latLon(47.0, 19.0, 0.0, 0.0)
        buffer.observe(sample(20_000L, last.first, last.second, 4f, 0f), 0.4f)
        val snap = buffer.snapshot()
        val stats = snap.stats
        val centroidLat = stats.centroidLatitude ?: 0.0
        val centroidLon = stats.centroidLongitude ?: 0.0
        val lastSample = snap.samples.last()
        val centroidToLast = FixAcceptance.haversineMeters(
            centroidLat,
            centroidLon,
            lastSample.latitude,
            lastSample.longitude
        )
        assertTrue(centroidToLast > 20.0)
        val cep95 = stats.cep95Meters ?: Double.MAX_VALUE
        assertTrue(cep95 < 15.0)
        val fromLast = snap.samples.map { sample ->
            FixAcceptance.haversineMeters(
                lastSample.latitude,
                lastSample.longitude,
                sample.latitude,
                sample.longitude
            )
        }.sorted()
        val cepFromLast = fromLast[((fromLast.size - 1) * 95) / 100]
        assertTrue(cep95 < cepFromLast)
    }

    private fun sample(
        time: Long,
        lat: Double,
        lon: Double,
        accuracy: Float,
        speedMps: Float
    ): FixCloudSample {
        return FixCloudSample(
            timeMillis = time,
            latitude = lat,
            longitude = lon,
            accuracyMeters = accuracy,
            speedMps = speedMps
        )
    }
}
