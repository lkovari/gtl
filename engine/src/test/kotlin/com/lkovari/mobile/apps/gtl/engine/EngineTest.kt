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
}

class FixAcceptanceTest {
    private val filter = FixFilter(
        minDistanceMeters = 2.5f,
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
    @Test
    fun writesLineAndPlacemarksWithoutRemoteIcons() {
        val kml = sampleKml()
        assertTrue(kml.contains("<LineString>"))
        assertTrue(kml.contains("Ride &lt;1&gt;"))
        assertFalse(kml.contains("eklsofttrade"))
        assertFalse(kml.contains("<href>http"))
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
        assertEquals("speed=0 temp=-", KmlDescriptions.balloon(EventKind.PAUSE, 1.1117642f, null))
        assertEquals("speed=0 temp=18.5", KmlDescriptions.balloon(EventKind.STOP, 4.1f, 18.5f))
        assertEquals("speed=5.5 temp=-", KmlDescriptions.balloon(EventKind.START, 5.5f, null))
        assertEquals("speed=8.3 temp=-", KmlDescriptions.balloon(EventKind.MOVE, 8.3f, null))
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

    private fun sampleKml(): String {
        return KmlExporter.export(
            KmlDocument(
                name = "Ride <1>",
                trackColorAabbggrr = "ff0000ff",
                trackWidth = 6,
                line = listOf(GeoPoint(47.5, 19.05, 120.0), GeoPoint(47.51, 19.06, 125.0)),
                placemarks = listOf(
                    KmlPlacemark("Start", EventKind.START, GeoPoint(47.5, 19.05, 120.0), "begin")
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
        assertTrue(UsageType.selectable.contains(UsageType.RUNNER))
        assertEquals(5, UsageType.selectable.size)
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
