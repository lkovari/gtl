package com.lkovari.mobile.apps.gtl.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
            skySat(GnssConstellation.GPS, 1, true, 32f, GnssClassifier.GPS_L1_HZ),
            skySat(GnssConstellation.GPS, 2, false, 28f, GnssClassifier.GPS_L5_HZ),
            skySat(GnssConstellation.GALILEO, 11, true, 30f, null),
            skySat(GnssConstellation.GLONASS, 21, true, 26f, null),
            skySat(GnssConstellation.BEIDOU, 31, false, 22f, null),
            skySat(GnssConstellation.QZSS, 41, true, 24f, null),
            skySat(GnssConstellation.IRNSS, 51, false, 18f, null)
        )
        val snapshot = GnssClassifier.snapshot(samples)
        assertEquals(1, snapshot.gpsL1.usedInFix)
        assertEquals(1, snapshot.gpsL5.inView)
        assertEquals(1, snapshot.byConstellation.getValue(GnssConstellation.GALILEO).usedInFix)
        assertEquals(1, snapshot.byConstellation.getValue(GnssConstellation.IRNSS).inView)
        assertEquals(4, snapshot.satellitesInFix)
        assertEquals(SignalQuality.GOOD, snapshot.signalQuality)
        assertEquals(7, snapshot.satellites.size)
        assertEquals(samples, snapshot.satellites)
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

class SkyplotProjectionTest {
    @Test
    fun northHorizonIsTopOfCircle() {
        val point = SkyplotProjection.offset(0f, 0f, 100f, 100f, 100f)
        assertEquals(100f, point!!.x, 0.01f)
        assertEquals(0f, point.y, 0.01f)
    }

    @Test
    fun eastHorizonIsRightOfCircle() {
        val point = SkyplotProjection.offset(90f, 0f, 100f, 100f, 100f)
        assertEquals(200f, point!!.x, 0.01f)
        assertEquals(100f, point.y, 0.01f)
    }

    @Test
    fun southHorizonIsBottomOfCircle() {
        val point = SkyplotProjection.offset(180f, 0f, 100f, 100f, 100f)
        assertEquals(100f, point!!.x, 0.01f)
        assertEquals(200f, point.y, 0.01f)
    }

    @Test
    fun westHorizonIsLeftOfCircle() {
        val point = SkyplotProjection.offset(270f, 0f, 100f, 100f, 100f)
        assertEquals(0f, point!!.x, 0.01f)
        assertEquals(100f, point.y, 0.01f)
    }

    @Test
    fun zenithIsCenterForAnyAzimuth() {
        val north = SkyplotProjection.offset(0f, 90f, 100f, 100f, 100f)
        val east = SkyplotProjection.offset(90f, 90f, 100f, 100f, 100f)
        assertEquals(100f, north!!.x, 0.01f)
        assertEquals(100f, north.y, 0.01f)
        assertEquals(100f, east!!.x, 0.01f)
        assertEquals(100f, east.y, 0.01f)
    }

    @Test
    fun sixtyDegreeNorthRingIsOneThirdFromCenter() {
        val point = SkyplotProjection.offset(0f, 60f, 100f, 100f, 100f)
        assertEquals(100f, point!!.x, 0.01f)
        assertEquals(100f - 100f / 3f, point.y, 0.01f)
    }

    @Test
    fun thirtyDegreeNorthRingIsTwoThirdsFromCenter() {
        val point = SkyplotProjection.offset(0f, 30f, 100f, 100f, 100f)
        assertEquals(100f, point!!.x, 0.01f)
        assertEquals(100f - 200f / 3f, point.y, 0.01f)
    }

    @Test
    fun belowHorizonIsOmitted() {
        assertNull(SkyplotProjection.offset(0f, -5f, 100f, 100f, 100f))
    }

    @Test
    fun nonFiniteAnglesAreOmitted() {
        assertNull(SkyplotProjection.offset(Float.NaN, 45f, 100f, 100f, 100f))
        assertNull(SkyplotProjection.offset(10f, Float.POSITIVE_INFINITY, 100f, 100f, 100f))
        assertNull(SkyplotProjection.offset(10f, Float.NaN, 100f, 100f, 100f))
    }

    @Test
    fun elevationAboveZenithClampsToCenter() {
        val point = SkyplotProjection.offset(45f, 95f, 100f, 100f, 100f)
        assertEquals(100f, point!!.x, 0.01f)
        assertEquals(100f, point.y, 0.01f)
    }
}

class SkyplotMarkersTest {
    @Test
    fun emptySamplesYieldNoMarkers() {
        assertTrue(SkyplotMarkers.from(emptyList()).isEmpty())
    }

    @Test
    fun belowHorizonIsDropped() {
        val markers = SkyplotMarkers.from(
            listOf(skySat(GnssConstellation.GPS, 1, true, 30f, elevationDegrees = -1f))
        )
        assertTrue(markers.isEmpty())
    }

    @Test
    fun mergesDualFrequencyIntoOneMarker() {
        val markers = SkyplotMarkers.from(
            listOf(
                skySat(
                    GnssConstellation.GPS,
                    12,
                    true,
                    32f,
                    GnssClassifier.GPS_L1_HZ,
                    azimuthDegrees = 20f,
                    elevationDegrees = 40f
                ),
                skySat(
                    GnssConstellation.GPS,
                    12,
                    false,
                    28f,
                    GnssClassifier.GPS_L5_HZ,
                    azimuthDegrees = 20f,
                    elevationDegrees = 40f
                )
            )
        )
        assertEquals(1, markers.size)
        val marker = markers[0]
        assertEquals(GnssConstellation.GPS, marker.constellation)
        assertEquals(12, marker.svid)
        assertTrue(marker.usedInFix)
        assertTrue(marker.hasL5)
        assertEquals(20f, marker.azimuthDegrees, 0.01f)
        assertEquals(40f, marker.elevationDegrees, 0.01f)
        assertEquals(32f, marker.cn0DbHz, 0.01f)
    }

    @Test
    fun differentSvidsStaySeparate() {
        val markers = SkyplotMarkers.from(
            listOf(
                skySat(GnssConstellation.GPS, 1, true, 30f, azimuthDegrees = 10f, elevationDegrees = 50f),
                skySat(GnssConstellation.GPS, 2, false, 22f, azimuthDegrees = 200f, elevationDegrees = 15f)
            )
        )
        assertEquals(2, markers.size)
        assertEquals(1, markers.count { it.usedInFix })
        assertFalse(markers.any { it.hasL5 })
    }

    @Test
    fun galileoE5aCountsAsL5Ring() {
        val markers = SkyplotMarkers.from(
            listOf(
                skySat(
                    GnssConstellation.GALILEO,
                    7,
                    true,
                    34f,
                    GnssClassifier.GPS_L5_HZ,
                    azimuthDegrees = 90f,
                    elevationDegrees = 25f
                )
            )
        )
        assertEquals(1, markers.size)
        assertTrue(markers[0].hasL5)
        assertTrue(markers[0].usedInFix)
    }

    @Test
    fun usedFlagIsTrueIfAnyBandIsUsed() {
        val markers = SkyplotMarkers.from(
            listOf(
                skySat(
                    GnssConstellation.GPS,
                    3,
                    false,
                    20f,
                    GnssClassifier.GPS_L1_HZ,
                    elevationDegrees = 30f
                ),
                skySat(
                    GnssConstellation.GPS,
                    3,
                    true,
                    26f,
                    GnssClassifier.GPS_L5_HZ,
                    elevationDegrees = 30f
                )
            )
        )
        assertEquals(1, markers.size)
        assertTrue(markers[0].usedInFix)
        assertTrue(markers[0].hasL5)
        assertEquals(26f, markers[0].cn0DbHz, 0.01f)
    }

    @Test
    fun prefersUsedSampleGeometryWhenBandsDisagree() {
        val markers = SkyplotMarkers.from(
            listOf(
                skySat(
                    GnssConstellation.GPS,
                    8,
                    false,
                    18f,
                    GnssClassifier.GPS_L1_HZ,
                    azimuthDegrees = 10f,
                    elevationDegrees = 20f
                ),
                skySat(
                    GnssConstellation.GPS,
                    8,
                    true,
                    30f,
                    GnssClassifier.GPS_L5_HZ,
                    azimuthDegrees = 12f,
                    elevationDegrees = 22f
                )
            )
        )
        assertEquals(12f, markers[0].azimuthDegrees, 0.01f)
        assertEquals(22f, markers[0].elevationDegrees, 0.01f)
    }

    @Test
    fun nonFiniteGeometryIsDropped() {
        val markers = SkyplotMarkers.from(
            listOf(
                skySat(GnssConstellation.GPS, 1, true, 30f, azimuthDegrees = Float.NaN, elevationDegrees = 40f),
                skySat(GnssConstellation.GALILEO, 2, true, 30f, azimuthDegrees = 10f, elevationDegrees = Float.NaN)
            )
        )
        assertTrue(markers.isEmpty())
    }
}

private fun skySat(
    constellation: GnssConstellation,
    svid: Int,
    usedInFix: Boolean,
    cn0DbHz: Float,
    carrierFrequencyHz: Float? = null,
    azimuthDegrees: Float = 0f,
    elevationDegrees: Float = 45f
): SatelliteSample {
    return SatelliteSample(
        constellation = constellation,
        svid = svid,
        usedInFix = usedInFix,
        cn0DbHz = cn0DbHz,
        carrierFrequencyHz = carrierFrequencyHz,
        azimuthDegrees = azimuthDegrees,
        elevationDegrees = elevationDegrees
    )
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
    fun writesTimeLatLonSpeedAndOdometerOnEveryTrackPoint() {
        val kml = sampleKml()
        assertTrue(kml.contains("xmlns:gx="))
        assertTrue(kml.contains("<when>2024-09-05T01:33:20Z</when>"))
        assertTrue(kml.contains("<LineString>"))
        assertTrue(kml.contains("<tessellate>1</tessellate>"))
        assertTrue(kml.contains("19.05,47.5,0"))
        assertTrue(kml.contains("<gx:coord>19.05 47.5 0</gx:coord>"))
        assertTrue(kml.contains("<gx:SimpleArrayData name=\"speed\">"))
        assertTrue(kml.contains("<gx:value>5.5</gx:value>"))
        assertTrue(kml.contains("<gx:SimpleArrayData name=\"odometer\">"))
        assertTrue(kml.contains("<gx:value>0.0</gx:value>"))
        assertTrue(kml.contains("<gx:value>1416.5</gx:value>"))
        assertTrue(kml.contains("Distance (m)"))
        assertTrue(kml.contains("<gx:SimpleArrayData name=\"baro\">"))
        assertTrue(kml.contains("Baro (m)"))
        assertTrue(kml.contains("<gx:SimpleArrayData name=\"alt\">"))
        assertTrue(kml.contains("GPS altitude (m)"))
    }

    @Test
    fun writesBaroExtendedDataWithoutChangingGpsCoord() {
        val kml = KmlExporter.export(
            KmlDocument(
                name = "Ride",
                trackColorAabbggrr = "ff0000ff",
                trackWidth = 6,
                tracks = listOf(
                    KmlTrack(
                        name = "Ride",
                        points = listOf(
                            KmlVertex(GeoPoint(47.5, 19.05, 120.0), SAMPLE_TIME, 5.5f, 0.0, 108.0),
                            KmlVertex(GeoPoint(47.51, 19.06, 125.0), SAMPLE_TIME + 1000, 6.0f, 1416.5, null)
                        ),
                        placemarks = emptyList()
                    )
                )
            )
        )
        assertTrue(kml.contains("<gx:SimpleArrayData name=\"alt\">"))
        assertTrue(kml.contains("GPS altitude (m)"))
        assertTrue(kml.contains("<gx:value>120.0</gx:value>"))
        assertTrue(kml.contains("<gx:value>125.0</gx:value>"))
        assertTrue(kml.contains("<gx:coord>19.05 47.5 0</gx:coord>"))
        assertTrue(kml.contains("<gx:coord>19.06 47.51 0</gx:coord>"))
        assertTrue(kml.contains("19.05,47.5,0"))
        assertTrue(kml.contains("<gx:value>108.0</gx:value>"))
        assertTrue(kml.contains("<gx:value></gx:value>"))
        assertFalse(kml.contains("<gx:coord>19.05 47.5 120.0</gx:coord>"))
        assertFalse(kml.contains("<gx:coord>19.05 47.5 108.0</gx:coord>"))
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
    fun startBalloonMatchesEarthDetailsSpec() {
        val start = KmlDescriptions.balloon(
            kind = EventKind.START,
            timestampMillis = SAMPLE_TIME,
            latitude = 47.5,
            longitude = 19.05,
            altitude = 184.0,
            speedMps = 5.5f,
            tempCelsius = 18.5f
        )
        assertEquals(
            listOf(
                "2024:09:05 01:33:20",
                "temp=18.5 °C",
                "lon=19.050000",
                "lat=47.500000",
                "Altitude: 184 m",
                "Baro: N/A"
            ),
            start.lines()
        )
        assertFalse(start.contains("Speed:"))
        assertFalse(start.contains("Avg. Speed:"))
        assertFalse(start.contains("usage="))
        assertFalse(start.contains("duration="))
        assertFalse(start.contains("distance="))
        assertFalse(start.contains("lean="))
    }

    @Test
    fun pauseBalloonMatchesEarthDetailsSpec() {
        val pause = KmlDescriptions.balloon(
            kind = EventKind.PAUSE,
            timestampMillis = SAMPLE_TIME,
            latitude = 47.5,
            longitude = 19.05,
            altitude = 184.0,
            speedMps = 1.1117642f,
            tempCelsius = null,
            elapsedMillis = 45_000L,
            odometerMeters = 184.0
        )
        assertEquals(
            listOf(
                "2024:09:05 01:33:20",
                "temp=N/A",
                "lon=19.050000",
                "lat=47.500000",
                "Altitude: 184 m",
                "Baro: N/A",
                "Speed: 4.0 km/h",
                "duration=45 s",
                "distance=184 m"
            ),
            pause.lines()
        )
        assertFalse(pause.contains("Avg. Speed:"))
        assertFalse(pause.contains("Max speed:"))
    }

    @Test
    fun stopBalloonMatchesEarthDetailsSpec() {
        val stop = KmlDescriptions.balloon(
            kind = EventKind.STOP,
            timestampMillis = SAMPLE_TIME,
            latitude = 47.5,
            longitude = 19.05,
            altitude = 184.0,
            speedMps = 4.1f,
            tempCelsius = 18.5f,
            maxSpeedMps = 12.0f,
            averageSpeedMps = 6.0f,
            elapsedMillis = 540_000L,
            odometerMeters = 3713.0
        )
        assertEquals(
            listOf(
                "2024:09:05 01:33:20",
                "temp=18.5 °C",
                "lon=19.050000",
                "lat=47.500000",
                "Altitude: 184 m",
                "Baro: N/A",
                "Avg. Speed: 21.6 km/h",
                "Max speed: 43.2 km/h",
                "duration=9 min",
                "distance=3.71 km"
            ),
            stop.lines()
        )
        assertFalse(stop.lines().any { it.startsWith("Speed:") })
        assertFalse(stop.contains("usage="))
    }

    @Test
    fun balloonUsesSelectedUnitsForTempAltitudeAndSpeed() {
        val imperial = KmlDescriptions.balloon(
            kind = EventKind.STOP,
            timestampMillis = SAMPLE_TIME,
            latitude = 47.5,
            longitude = 19.05,
            altitude = 184.0,
            speedMps = 0f,
            tempCelsius = 18.5f,
            maxSpeedMps = 12.0f,
            averageSpeedMps = 6.0f,
            elapsedMillis = 3_661_000L,
            odometerMeters = 2500.0,
            baroAltitude = 184.0,
            system = MeasurementSystem.IMPERIAL
        )
        val icaoPause = KmlDescriptions.balloon(
            kind = EventKind.PAUSE,
            timestampMillis = SAMPLE_TIME,
            latitude = 47.5,
            longitude = 19.05,
            altitude = 184.0,
            speedMps = 6.0f,
            tempCelsius = 18.5f,
            elapsedMillis = 61_000L,
            odometerMeters = 184.0,
            system = MeasurementSystem.ICAO
        )
        assertTrue(imperial.contains("temp=65.3 °F"))
        assertTrue(imperial.contains("Altitude: 604 ft"))
        assertTrue(imperial.contains("Baro: 604 ft"))
        assertTrue(imperial.contains("Avg. Speed: 13.4 mph"))
        assertTrue(imperial.contains("Max speed: 26.8 mph"))
        assertTrue(imperial.contains("duration=01:01:01"))
        assertTrue(imperial.contains("distance=1.55 mi"))
        assertTrue(icaoPause.contains("temp=18.5 °C"))
        assertTrue(icaoPause.contains("Altitude: 604 ft"))
        assertTrue(icaoPause.contains("Speed: 11.7 kt"))
        assertTrue(icaoPause.contains("duration=1 min"))
        assertTrue(icaoPause.contains("distance=0.10 NM"))
    }

    @Test
    fun balloonUsesNaWhenTemperatureIsMissing() {
        val start = KmlDescriptions.balloon(
            kind = EventKind.START,
            timestampMillis = SAMPLE_TIME,
            latitude = 47.5,
            longitude = 19.05,
            altitude = 120.0,
            speedMps = 5.5f,
            tempCelsius = null
        )
        assertTrue(start.contains("temp=N/A"))
        assertFalse(start.contains("temp=-"))
    }

    @Test
    fun balloonKeepsZeroCelsiusAsARealTemperature() {
        val start = KmlDescriptions.balloon(
            kind = EventKind.START,
            timestampMillis = SAMPLE_TIME,
            latitude = 47.5,
            longitude = 19.05,
            altitude = 120.0,
            speedMps = 5.5f,
            tempCelsius = 0f
        )
        assertTrue(start.contains("temp=0.0 °C"))
        assertFalse(start.contains("temp=N/A"))
    }

    @Test
    fun balloonShowsBaroInSelectedUnitsOrNa() {
        val withBaro = KmlDescriptions.balloon(
            kind = EventKind.START,
            timestampMillis = SAMPLE_TIME,
            latitude = 47.5,
            longitude = 19.05,
            altitude = 184.0,
            baroAltitude = 108.0,
            speedMps = 5.5f,
            tempCelsius = 18.5f
        )
        val imperial = KmlDescriptions.balloon(
            kind = EventKind.START,
            timestampMillis = SAMPLE_TIME,
            latitude = 47.5,
            longitude = 19.05,
            altitude = 184.0,
            baroAltitude = 108.0,
            speedMps = 5.5f,
            tempCelsius = 18.5f,
            system = MeasurementSystem.IMPERIAL
        )
        assertTrue(withBaro.contains("Altitude: 184 m"))
        assertTrue(withBaro.contains("Baro: 108 m"))
        assertTrue(imperial.contains("Baro: 354 ft"))
        assertFalse(withBaro.contains("Baro: N/A"))
    }

    @Test
    fun balloonDurationUsesSecondsThenMinutesThenClock() {
        assertEquals("0 s", Units.formatBalloonDuration(0L))
        assertEquals("60 s", Units.formatBalloonDuration(60_000L))
        assertEquals("1 min", Units.formatBalloonDuration(61_000L))
        assertEquals("59 min", Units.formatBalloonDuration(3_599_000L))
        assertEquals("01:00:00", Units.formatBalloonDuration(3_600_000L))
    }

    @Test
    fun drapesTrackAndPointIconsOnTheGround() {
        val kml = sampleKml()
        assertTrue(kml.contains("<gx:Track>"))
        assertTrue(kml.contains("<LineString>"))
        assertTrue(kml.contains("<tessellate>1</tessellate>"))
        assertTrue(kml.contains("<altitudeMode>clampToGround</altitudeMode>"))
        assertFalse(kml.contains("<altitudeMode>absolute</altitudeMode>"))
        assertTrue(kml.contains("<Point>"))
        assertTrue(kml.contains("<coordinates>19.05,47.5,0</coordinates>"))
        assertTrue(kml.contains("<styleUrl>#trackData</styleUrl>"))
        assertTrue(kml.contains("<visibility>0</visibility>"))
    }

    @Test
    fun wrapsBalloonDescriptionAsHtmlCdata() {
        val kml = KmlExporter.export(
            KmlDocument(
                name = "Ride",
                trackColorAabbggrr = "ff0000ff",
                trackWidth = 6,
                tracks = listOf(
                    KmlTrack(
                        name = "Ride",
                        points = listOf(
                            KmlVertex(GeoPoint(47.5, 19.05, 120.0), SAMPLE_TIME, 0f, 0.0)
                        ),
                        placemarks = listOf(
                            KmlPlacemark(
                                name = "Stop",
                                kind = EventKind.STOP,
                                point = GeoPoint(47.5, 19.05, 120.0),
                                description = "2024:09:05 01:33:20\nAvg. Speed: 22.0 km/h\nMax speed: 43.0 km/h",
                                drawOrder = 10
                            )
                        )
                    )
                )
            )
        )
        assertTrue(kml.contains("<description><![CDATA[2024:09:05 01:33:20<br/>Avg. Speed: 22.0 km/h<br/>Max speed: 43.0 km/h]]></description>"))
        assertTrue(kml.contains("<name>Stop</name>"))
        assertFalse(kml.contains("<name>Pause</name>"))
        assertTrue(kml.contains("<gx:drawOrder>10</gx:drawOrder>"))
    }

    @Test
    fun pausePlacemarkDetailsAreHtmlAndNotEmpty() {
        val events = listOf(
            TrackLogEvent(SAMPLE_TIME, 47.50, 19.05, 120.0, 5f, EventKind.START, usageType = "Motorbike"),
            TrackLogEvent(SAMPLE_TIME + 1_000, 47.51, 19.06, 120.0, 8f, EventKind.MOVE),
            TrackLogEvent(SAMPLE_TIME + 2_000, 47.52, 19.07, 120.0, 0f, EventKind.PAUSE, usageType = "Motorbike")
        )
        val track = KmlTrackBuilder.build("GTL ride", events, MeasurementSystem.METRIC)
        val pause = track.placemarks.single { it.kind == EventKind.PAUSE }
        assertEquals("Pause", pause.name)
        assertTrue(pause.description.contains("2024:09:05 01:33:22"))
        assertTrue(pause.description.contains("lon=19.070000"))
        assertTrue(pause.description.contains("lat=47.520000"))
        assertTrue(pause.description.contains("temp=N/A"))
        assertTrue(pause.description.contains("Altitude: 120 m"))
        assertTrue(pause.description.contains("Baro: N/A"))
        assertTrue(pause.description.contains("Speed: 0.0 km/h"))
        assertTrue(pause.description.contains("duration=2 s"))
        assertTrue(pause.description.contains("distance="))
        assertFalse(pause.description.isBlank())
        val kml = KmlExporter.export(
            KmlDocument(
                name = "GTL ride",
                trackColorAabbggrr = "ff0000ff",
                trackWidth = 6,
                tracks = listOf(track)
            )
        )
        assertTrue(kml.contains("<name>Pause</name>"))
        assertTrue(kml.contains("<description><![CDATA["))
        assertTrue(kml.contains("2024:09:05 01:33:22<br/>"))
        assertTrue(kml.contains("lat=47.520000<br/>"))
        assertTrue(kml.contains("temp=N/A<br/>"))
        assertTrue(kml.contains("Altitude: 120 m<br/>"))
        assertTrue(kml.contains("Speed: 0.0 km/h"))
        assertTrue(kml.contains("duration=2 s"))
        assertTrue(kml.contains("distance="))
        assertTrue(kml.contains("<BalloonStyle>"))
        assertTrue(kml.contains("\$[description]"))
        assertFalse(pause.description.contains("Avg. Speed:"))
        assertFalse(pause.description.contains("Max speed:"))
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
                            KmlVertex(GeoPoint(47.5, 19.05, 120.0), SAMPLE_TIME, 5.5f, 0.0),
                            KmlVertex(GeoPoint(47.51, 19.06, 125.0), SAMPLE_TIME + 1000, 6.0f, 1416.5)
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

class TrackLogExportTest {
    @Test
    fun emptySessionHasNoPathOrMarkers() {
        assertTrue(TrackLogExport.path(emptyList()).isEmpty())
        assertTrue(TrackLogExport.markers(emptyList()).isEmpty())
    }

    @Test
    fun startMarkerSitsOnTheFirstPathVertex() {
        val events = listOf(
            event(EventKind.START, 47.50, 19.05, 0),
            event(EventKind.MOVE, 47.51, 19.06, 1_000)
        )
        val start = TrackLogExport.markers(events).first { it.kind == EventKind.START }
        val path = TrackLogExport.path(events)
        assertEquals(path.first().latitude, start.event.latitude, 0.0)
        assertEquals(path.first().longitude, start.event.longitude, 0.0)
        assertEquals("Start", start.kind.kmlPlacemarkName())
    }

    @Test
    fun trailingStopIsNotAPathVertexAndIconSitsOnTrackEnd() {
        val events = listOf(
            event(EventKind.START, 47.50, 19.05, 0),
            event(EventKind.MOVE, 47.51, 19.06, 1_000),
            event(EventKind.STOP, 47.80, 19.40, 2_000)
        )
        val path = TrackLogExport.path(events)
        assertEquals(2, path.size)
        assertEquals(47.51, path.last().latitude, 0.0)
        assertEquals(19.06, path.last().longitude, 0.0)
        val stop = TrackLogExport.markers(events).single { it.kind == EventKind.STOP }
        assertEquals(47.51, stop.event.latitude, 0.0)
        assertEquals(19.06, stop.event.longitude, 0.0)
        assertEquals(2_000L, stop.event.timestampMillis)
        assertEquals("Stop", stop.kind.kmlPlacemarkName())
    }

    @Test
    fun pauseOnTheTrackIsKeptAndPauseAtStopIsDropped() {
        val events = listOf(
            event(EventKind.START, 47.50, 19.05, 0),
            event(EventKind.MOVE, 47.51, 19.06, 1_000),
            event(EventKind.PAUSE, 47.52, 19.07, 2_000),
            event(EventKind.MOVE, 47.53, 19.08, 3_000),
            event(EventKind.PAUSE, 47.53, 19.08, 4_000),
            event(EventKind.STOP, 47.90, 19.50, 5_000)
        )
        val markers = TrackLogExport.markers(events)
        val pauses = markers.filter { it.kind == EventKind.PAUSE }
        assertEquals(1, pauses.size)
        assertEquals(47.52, pauses.single().event.latitude, 0.0)
        assertEquals(19.07, pauses.single().event.longitude, 0.0)
        val path = TrackLogExport.path(events)
        val pauseVertex = path.single { it.kind == EventKind.PAUSE && it.latitude == 47.52 }
        assertEquals(pauseVertex.latitude, pauses.single().event.latitude, 0.0)
        assertEquals(pauseVertex.longitude, pauses.single().event.longitude, 0.0)
        assertEquals("Pause", pauses.single().kind.kmlPlacemarkName())
        assertEquals(1, markers.count { it.kind == EventKind.STOP })
        assertFalse(markers.any { it.kind == EventKind.PAUSE && it.event.latitude == 47.53 })
    }

    @Test
    fun pauseAtStartIsDropped() {
        val events = listOf(
            event(EventKind.START, 47.50, 19.05, 0),
            event(EventKind.PAUSE, 47.50, 19.05, 500),
            event(EventKind.MOVE, 47.51, 19.06, 1_000)
        )
        val markers = TrackLogExport.markers(events)
        assertFalse(markers.any { it.kind == EventKind.PAUSE })
        assertEquals(1, markers.count { it.kind == EventKind.START })
    }

    @Test
    fun consecutivePausesCollapseToTheFirstOfTheStandstill() {
        val events = listOf(
            event(EventKind.START, 47.50, 19.05, 0),
            event(EventKind.MOVE, 47.51, 19.06, 1_000),
            event(EventKind.PAUSE, 47.52, 19.07, 2_000),
            event(EventKind.PAUSE, 47.52001, 19.07001, 3_000),
            event(EventKind.PAUSE, 47.52002, 19.07002, 4_000),
            event(EventKind.MOVE, 47.53, 19.08, 5_000)
        )
        val pauses = TrackLogExport.markers(events).filter { it.kind == EventKind.PAUSE }
        assertEquals(1, pauses.size)
        assertEquals(47.52, pauses.single().event.latitude, 0.0)
        assertEquals(2_000L, pauses.single().event.timestampMillis)
    }

    @Test
    fun singleStopOnlySessionKeepsThatPointAsThePath() {
        val events = listOf(event(EventKind.STOP, 47.50, 19.05, 0))
        val path = TrackLogExport.path(events)
        assertEquals(1, path.size)
        val markers = TrackLogExport.markers(events)
        assertEquals(1, markers.count { it.kind == EventKind.START })
        assertEquals(1, markers.count { it.kind == EventKind.STOP })
        assertEquals(47.50, markers.single { it.kind == EventKind.STOP }.event.latitude, 0.0)
    }

    @Test
    fun builtStopBalloonHasStopNameAvgMaxAndTempNa() {
        val events = listOf(
            event(EventKind.START, 47.50, 19.05, 0, speed = 5f),
            event(EventKind.MOVE, 47.51, 19.06, 60_000, speed = 8f),
            event(EventKind.STOP, 47.90, 19.40, 61_000, speed = 0f)
        )
        val track = KmlTrackBuilder.build("GTL ride", events, MeasurementSystem.METRIC)
        val stop = track.placemarks.single { it.kind == EventKind.STOP }
        assertEquals("Stop", stop.name)
        assertFalse(stop.name.equals("Pause", ignoreCase = true))
        assertEquals(track.points.last().point.latitude, stop.point.latitude, 0.0)
        assertEquals(track.points.last().point.longitude, stop.point.longitude, 0.0)
        assertTrue(stop.description.contains("temp=N/A"))
        assertTrue(stop.description.contains("Avg. Speed:"))
        assertTrue(stop.description.contains("Max speed:"))
        assertTrue(stop.description.contains("Altitude:"))
        assertFalse(stop.description.lines().any { it.startsWith("Speed:") })
        assertTrue(stop.description.contains("duration=1 min"))
        assertTrue(stop.description.contains("distance="))
        assertEquals(10, stop.drawOrder)
        assertTrue(track.placemarks.none { it.name == "Pause" && it.kind == EventKind.STOP })
    }

    private fun event(
        kind: EventKind,
        lat: Double,
        lon: Double,
        time: Long,
        speed: Float = 5f
    ): TrackLogEvent {
        return TrackLogEvent(
            timestampMillis = time,
            latitude = lat,
            longitude = lon,
            altitude = 120.0,
            speedMps = speed,
            kind = kind
        )
    }
}

class GpxExporterTest {
    companion object {
        private const val SAMPLE_TIME = 1_725_500_000_000L
    }

    @Test
    fun writesCoreGpxTrackPoint() {
        val gpx = sampleGpx()
        assertTrue(gpx.contains("""<?xml version="1.0" encoding="UTF-8"?>"""))
        assertTrue(gpx.contains("""<gpx version="1.1" creator="GTL" xmlns="http://www.topografix.com/GPX/1/1">"""))
        assertTrue(gpx.contains("""<trkpt lat="47.5" lon="19.05">"""))
        assertTrue(gpx.contains("<ele>120.0</ele>"))
        assertTrue(gpx.contains("<time>2024-09-05T01:33:20Z</time>"))
        assertFalse(gpx.contains("speed"))
        assertFalse(gpx.contains("gpxtpx"))
    }

    @Test
    fun writesStartAndStopWaypoints() {
        val gpx = sampleGpx()
        assertTrue(gpx.contains("<wpt lat=\"47.5\" lon=\"19.05\">"))
        assertTrue(gpx.contains("<name>START</name>"))
        assertTrue(gpx.contains("<name>STOP</name>"))
        assertTrue(gpx.contains("<trkseg>"))
        assertEquals(1, "<trkseg>".toRegex().findAll(gpx).count())
    }

    @Test
    fun escapesTrackName() {
        val gpx = sampleGpx()
        assertTrue(gpx.contains("<name>Ride &lt;1&gt;</name>"))
    }

    @Test
    fun writesMultipleTracksInOneFile() {
        val gpx = GpxExporter.export(
            GpxDocument(
                tracks = listOf(
                    GpxTrack(
                        name = "Morning",
                        points = listOf(
                            GpxTrackPoint(GeoPoint(47.5, 19.05, 120.0), SAMPLE_TIME)
                        ),
                        waypoints = emptyList()
                    ),
                    GpxTrack(
                        name = "Evening",
                        points = listOf(
                            GpxTrackPoint(GeoPoint(47.6, 19.1, 130.0), SAMPLE_TIME)
                        ),
                        waypoints = emptyList()
                    )
                )
            )
        )
        assertEquals(2, "<trk>".toRegex().findAll(gpx).count())
        assertTrue(gpx.contains("<name>Morning</name>"))
        assertTrue(gpx.contains("<name>Evening</name>"))
    }

    private fun sampleGpx(): String {
        return GpxExporter.export(
            GpxDocument(
                tracks = listOf(
                    GpxTrack(
                        name = "Ride <1>",
                        points = listOf(
                            GpxTrackPoint(GeoPoint(47.5, 19.05, 120.0), SAMPLE_TIME),
                            GpxTrackPoint(GeoPoint(47.51, 19.06, 125.0), SAMPLE_TIME + 1000)
                        ),
                        waypoints = listOf(
                            GpxWaypoint("START", GeoPoint(47.5, 19.05, 120.0), SAMPLE_TIME),
                            GpxWaypoint("STOP", GeoPoint(47.51, 19.06, 125.0), SAMPLE_TIME + 1000)
                        )
                    )
                )
            )
        )
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
        val along = TrackStatsCalculator.cumulativeOdometerMeters(
            listOf(
                GeoPoint(47.0, 19.0, 100.0),
                GeoPoint(47.001, 19.0, 110.0)
            )
        )
        assertEquals(2, along.size)
        assertEquals(0.0, along[0], 0.0)
        assertEquals(stats.odometerMeters, along[1], 0.0)
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

class BaroAltitudeTest {
    @Test
    fun standardPressureIsSeaLevel() {
        val meters = BaroAltitude.metersFromPressureHpa(BaroAltitude.StandardAtmosphereHpa)
        assertEquals(0.0, meters ?: Double.NaN, 0.5)
    }

    @Test
    fun lowerPressureIsHigherAltitude() {
        val meters = BaroAltitude.metersFromPressureHpa(850f)
        assertTrue((meters ?: 0.0) > 1000.0)
    }

    @Test
    fun nonPositivePressureIsNull() {
        assertEquals(null, BaroAltitude.metersFromPressureHpa(0f))
        assertEquals(null, BaroAltitude.metersFromPressureHpa(-10f))
    }

    @Test
    fun higherQnhRaisesIndicatedAltitude() {
        val isa = BaroAltitude.metersFromPressureHpa(990f) ?: 0.0
        val highQnh = BaroAltitude.metersFromPressureHpa(990f, 1025f) ?: 0.0
        assertTrue(highQnh > isa)
    }

    @Test
    fun clampQnhStaysInRange() {
        assertEquals(BaroAltitude.MinQnhHpa, BaroAltitude.clampQnh(800f))
        assertEquals(BaroAltitude.MaxQnhHpa, BaroAltitude.clampQnh(1200f))
        assertEquals(1013.25f, BaroAltitude.clampQnh(1013.25f))
    }

    @Test
    fun displayedMetersPrefersRecomputeFromPressure() {
        val shown = BaroAltitude.displayedMeters(
            pressureHpa = 990f,
            storedBaro = 10.0,
            qnhHpa = 1013.25f
        )
        val recomputed = BaroAltitude.metersFromPressureHpa(990f)
        assertEquals(recomputed, shown)
    }

    @Test
    fun displayedMetersFallsBackToStoredWhenNoPressure() {
        assertEquals(184.0, BaroAltitude.displayedMeters(null, 184.0, 1013.25f))
    }
}

class OsmMapViewRedrawTest {
    @Test
    fun firstLayoutFromZeroNeedsRedraw() {
        assertTrue(OsmMapViewRedraw.shouldRedrawLayers(1080, 1920, 0, 0))
    }

    @Test
    fun zeroSizeDoesNotRedraw() {
        assertFalse(OsmMapViewRedraw.shouldRedrawLayers(0, 1920, 0, 0))
        assertFalse(OsmMapViewRedraw.shouldRedrawLayers(1080, 0, 0, 0))
        assertFalse(OsmMapViewRedraw.shouldRedrawLayers(0, 0, 1080, 1920))
    }

    @Test
    fun sizeChangeNeedsRedraw() {
        assertTrue(OsmMapViewRedraw.shouldRedrawLayers(1080, 1920, 1080, 800))
        assertFalse(OsmMapViewRedraw.shouldRedrawLayers(1080, 1920, 1080, 1920))
    }

    @Test
    fun tilesWhenViewHasSize() {
        assertFalse(OsmMapViewRedraw.shouldRequestTiles(width = 0, height = 1920))
        assertTrue(OsmMapViewRedraw.shouldRequestTiles(width = 1080, height = 1920))
    }

    @Test
    fun animatorThreadMustPostAncestorInvalidate() {
        assertTrue(OsmMapViewRedraw.mustPostAncestorInvalidate(calledOnMainThread = false))
        assertFalse(OsmMapViewRedraw.mustPostAncestorInvalidate(calledOnMainThread = true))
    }
}

class OsmMapCameraTest {
    private val hungary = LatLonBounds(45.74, 16.11, 48.59, 22.90)

    @Test
    fun emulatorMountainViewUsesMapStart() {
        val center = OsmMapCamera.initialCenter(
            mapBounds = hungary,
            mapStartLatitude = 47.16,
            mapStartLongitude = 19.50,
            locationLatitude = 37.421998,
            locationLongitude = -122.084
        )
        assertEquals(47.16, center.latitude, 0.0)
        assertEquals(19.50, center.longitude, 0.0)
    }

    @Test
    fun budapestFixStaysOnGps() {
        val center = OsmMapCamera.initialCenter(
            mapBounds = hungary,
            mapStartLatitude = 47.16,
            mapStartLongitude = 19.50,
            locationLatitude = 47.447202,
            locationLongitude = 19.195482
        )
        assertEquals(47.447202, center.latitude, 0.0)
        assertEquals(19.195482, center.longitude, 0.0)
    }

    @Test
    fun missingFixUsesMapStart() {
        val center = OsmMapCamera.initialCenter(
            mapBounds = hungary,
            mapStartLatitude = 47.16,
            mapStartLongitude = 19.50,
            locationLatitude = null,
            locationLongitude = null
        )
        assertEquals(47.16, center.latitude, 0.0)
        assertEquals(19.50, center.longitude, 0.0)
    }

    @Test
    fun zoomIsCountryWhenGpsOutsideMap() {
        assertEquals(8, OsmMapCamera.initialZoom(gpsInsideMap = false, mapStartZoom = 8))
        assertEquals(14, OsmMapCamera.initialZoom(gpsInsideMap = true, mapStartZoom = 8))
        assertEquals(3, OsmMapCamera.initialZoom(gpsInsideMap = false, mapStartZoom = 1))
    }

    @Test
    fun followIgnoresEmulatorFixOutsideHungary() {
        assertEquals(
            null,
            OsmMapCamera.followCenter(
                mapBounds = hungary,
                preferTrack = false,
                trackLatitude = null,
                trackLongitude = null,
                locationLatitude = 37.421998,
                locationLongitude = -122.084
            )
        )
    }

    @Test
    fun followStaysOnBudapestFix() {
        val center = checkNotNull(
            OsmMapCamera.followCenter(
                mapBounds = hungary,
                preferTrack = false,
                trackLatitude = null,
                trackLongitude = null,
                locationLatitude = 47.447202,
                locationLongitude = 19.195482
            )
        )
        assertEquals(47.447202, center.latitude, 0.0)
        assertEquals(19.195482, center.longitude, 0.0)
    }
}

class GpsAltitudeTest {
    @Test
    fun fusedMinus1787IsRejected() {
        assertEquals(
            null,
            GpsAltitude.pick(
                gnssMsl = null,
                fusedMsl = null,
                gnssEllipsoid = null,
                fusedEllipsoid = -1787.0
            )
        )
    }

    @Test
    fun prefersMslOverJunkEllipsoid() {
        assertEquals(
            124.0,
            GpsAltitude.pick(
                gnssMsl = null,
                fusedMsl = 124.0,
                gnssEllipsoid = null,
                fusedEllipsoid = -1787.0
            )
        )
    }

    @Test
    fun prefersGnssEllipsoidOverFusedJunk() {
        assertEquals(
            163.0,
            GpsAltitude.pick(
                gnssMsl = null,
                fusedMsl = null,
                gnssEllipsoid = 163.0,
                fusedEllipsoid = -1787.0
            )
        )
    }

    @Test
    fun missingAltitudeIsNull() {
        assertEquals(
            null,
            GpsAltitude.pick(
                gnssMsl = null,
                fusedMsl = null,
                gnssEllipsoid = null,
                fusedEllipsoid = null
            )
        )
    }

    @Test
    fun deadSeaEllipsoidIsKept() {
        assertEquals(
            -410.0,
            GpsAltitude.pick(
                gnssMsl = null,
                fusedMsl = null,
                gnssEllipsoid = -410.0,
                fusedEllipsoid = null
            )
        )
    }
}

class OsmMapFileTest {
    @Test
    fun htmlErrorPageIsNotAMap() {
        val file = kotlin.io.path.createTempFile(suffix = ".map").toFile()
        file.writeText("<html>Access denied</html>")
        assertFalse(OsmMapFile.isReadable(file))
        file.delete()
    }

    @Test
    fun truncatedMapsforgeHeaderIsNotReadable() {
        val file = kotlin.io.path.createTempFile(suffix = ".map").toFile()
        file.writeBytes("mapsforge binary OSM".toByteArray(Charsets.US_ASCII))
        assertFalse(OsmMapFile.isReadable(file))
        file.delete()
    }

    @Test
    fun declaredSizeMustMatchFileLength() {
        val file = kotlin.io.path.createTempFile(suffix = ".map").toFile()
        file.writeBytes(fakeMapsforgeBytes(declaredSize = 2_000_000L, actualSize = 4096))
        assertFalse(OsmMapFile.isReadable(file))
        file.delete()
    }

    @Test
    fun completeHeaderWithMatchingSizeIsReadable() {
        val file = kotlin.io.path.createTempFile(suffix = ".map").toFile()
        file.writeBytes(fakeMapsforgeBytes(declaredSize = 4096L, actualSize = 4096))
        assertTrue(OsmMapFile.isReadable(file))
        file.delete()
    }

    private fun fakeMapsforgeBytes(declaredSize: Long, actualSize: Int): ByteArray {
        val bytes = ByteArray(actualSize)
        val magic = "mapsforge binary OSM".toByteArray(Charsets.US_ASCII)
        magic.copyInto(bytes)
        java.nio.ByteBuffer.wrap(bytes, 28, 8).order(java.nio.ByteOrder.BIG_ENDIAN).putLong(declaredSize)
        return bytes
    }
}

class MapFitZoomTest {
    @Test
    fun missingOrZeroDimensionCannotFit() {
        assertFalse(MapFitZoom.canFit(null, 800))
        assertFalse(MapFitZoom.canFit(1080, null))
        assertFalse(MapFitZoom.canFit(0, 800))
        assertFalse(MapFitZoom.canFit(1080, 0))
        assertTrue(MapFitZoom.canFit(1080, 1920))
    }

    @Test
    fun clampKeepsMapsforgeSafeZoom() {
        assertEquals(MapFitZoom.Min, MapFitZoom.clamp(-5))
        assertEquals(MapFitZoom.Max, MapFitZoom.clamp(127))
        assertEquals(16, MapFitZoom.clamp(16))
    }
}

class TrackEndpointsTest {
    @Test
    fun emptyHasNeither() {
        assertEquals(null, TrackEndpoints.start(emptyList()))
        assertEquals(null, TrackEndpoints.end(emptyList(), logging = false))
    }

    @Test
    fun singlePointIsStartOnly() {
        val points = listOf(GeoPoint(47.5, 19.05))
        assertEquals(47.5, TrackEndpoints.start(points)!!.latitude, 0.0)
        assertEquals(null, TrackEndpoints.end(points, logging = false))
    }

    @Test
    fun savedTrackMarksFirstAndLast() {
        val points = listOf(GeoPoint(47.50, 19.05), GeoPoint(47.51, 19.06), GeoPoint(47.52, 19.07))
        assertEquals(47.50, TrackEndpoints.start(points)!!.latitude, 0.0)
        assertEquals(47.52, TrackEndpoints.end(points, logging = false)!!.latitude, 0.0)
    }

    @Test
    fun loggingOmitsEndBecauseUsageIconIsNow() {
        val points = listOf(GeoPoint(47.50, 19.05), GeoPoint(47.51, 19.06))
        assertEquals(47.50, TrackEndpoints.start(points)!!.latitude, 0.0)
        assertEquals(null, TrackEndpoints.end(points, logging = true))
    }
}

class CompassHeadingTest {
    @Test
    fun magIgnoresDeclination() {
        val shown = CompassHeading.display(87f, wantTrue = false, declinationDegrees = 5.4f)
        assertEquals(87f, shown.degrees, 0.01f)
        assertFalse(shown.trueNorth)
        assertFalse(shown.missingFix)
    }

    @Test
    fun trueAddsDeclination() {
        val shown = CompassHeading.display(87f, wantTrue = true, declinationDegrees = 5f)
        assertEquals(92f, shown.degrees, 0.01f)
        assertTrue(shown.trueNorth)
        assertFalse(shown.missingFix)
    }

    @Test
    fun trueWrapsPast360() {
        val shown = CompassHeading.display(358f, wantTrue = true, declinationDegrees = 5f)
        assertEquals(3f, shown.degrees, 0.01f)
        assertTrue(shown.trueNorth)
    }

    @Test
    fun trueWrapsNegativeDeclination() {
        val shown = CompassHeading.display(10f, wantTrue = true, declinationDegrees = -15f)
        assertEquals(355f, shown.degrees, 0.01f)
        assertTrue(shown.trueNorth)
    }

    @Test
    fun trueWithoutFixStaysMag() {
        val shown = CompassHeading.display(87f, wantTrue = true, declinationDegrees = null)
        assertEquals(87f, shown.degrees, 0.01f)
        assertFalse(shown.trueNorth)
        assertTrue(shown.missingFix)
    }

    @Test
    fun figureEightOnLowAndUnreliable() {
        assertTrue(CompassHeading.needsFigureEight(0))
        assertTrue(CompassHeading.needsFigureEight(1))
        assertTrue(CompassHeading.needsFigureEight(-1))
        assertFalse(CompassHeading.needsFigureEight(2))
        assertFalse(CompassHeading.needsFigureEight(3))
    }
}

class MapHudVisibilityTest {
    @Test
    fun fullWhileLogging() {
        assertEquals(MapHudMode.Full, MapHudVisibility.mode(true, null, true))
        assertEquals(MapHudMode.Full, MapHudVisibility.mode(true, 12L, true))
    }

    @Test
    fun hiddenWhenViewingSavedTrackIdle() {
        assertEquals(MapHudMode.Hidden, MapHudVisibility.mode(false, 12L, true))
    }

    @Test
    fun compactWhenIdleWithFix() {
        assertEquals(MapHudMode.Compact, MapHudVisibility.mode(false, null, true))
    }

    @Test
    fun hiddenWhenIdleWithoutFix() {
        assertEquals(MapHudMode.Hidden, MapHudVisibility.mode(false, null, false))
    }
}

class UnitsHudTest {
    @Test
    fun hudSpeedUsesSettingsUnits() {
        assertEquals("36", Units.hudSpeedNumber(10f, MeasurementSystem.METRIC))
        assertEquals("km/h", Units.hudSpeedUnit(MeasurementSystem.METRIC))
        assertEquals("mph", Units.hudSpeedUnit(MeasurementSystem.IMPERIAL))
        assertEquals("kt", Units.hudSpeedUnit(MeasurementSystem.ICAO))
    }
}

class ElevationSeriesTest {
    @Test
    fun accumulatesDistanceAndKeepsAltitudes() {
        val samples = ElevationSeries.fromPoints(
            listOf(
                ElevationPoint(47.0, 19.0, 100.0, 98.0),
                ElevationPoint(47.001, 19.0, 120.0, 119.0)
            )
        )
        assertEquals(2, samples.size)
        assertEquals(0.0, samples[0].distanceMeters, 0.01)
        assertTrue(samples[1].distanceMeters > 100.0)
        assertEquals(120.0, samples[1].gpsAltitude, 0.0)
        assertEquals(119.0, samples[1].baroAltitude ?: 0.0, 0.0)
        assertTrue(ElevationSeries.hasBaroLine(samples))
    }

    @Test
    fun downsampleKeepsFirstAndLast() {
        val points = (0 until 500).map { index ->
            ElevationPoint(47.0 + index * 0.0001, 19.0, 100.0 + index, null)
        }
        val samples = ElevationSeries.downsample(ElevationSeries.fromPoints(points), 200)
        assertEquals(200, samples.size)
        assertEquals(100.0, samples.first().gpsAltitude, 0.0)
        assertEquals(599.0, samples.last().gpsAltitude, 0.0)
        assertFalse(ElevationSeries.hasBaroLine(samples))
    }

    @Test
    fun plotScalePadsAnEightMetreGpsBaroGap() {
        val samples = listOf(
            ElevationSample(0.0, 187.0, 179.0),
            ElevationSample(100.0, 186.0, 179.5)
        )
        val scale = ElevationSeries.plotScale(samples)
        assertEquals(ElevationSeries.MinPlotSpanMeters, scale.span, 0.01)
        val mid = (179.0 + 187.0) / 2.0
        assertEquals(mid, (scale.plotMin + scale.plotMax) / 2.0, 0.01)
        assertTrue(scale.plotMin < 179.0)
        assertTrue(scale.plotMax > 187.0)
    }

    @Test
    fun plotScaleUsesBaroWhenQnhMovesItFarFromGps() {
        val samples = listOf(
            ElevationSample(0.0, 187.0, 870.0),
            ElevationSample(100.0, 179.0, 868.0)
        )
        val scale = ElevationSeries.plotScale(samples)
        assertEquals(179.0, scale.plotMin, 0.01)
        assertEquals(870.0, scale.plotMax, 0.01)
    }

    @Test
    fun plotScaleIgnoresASingleBaroSample() {
        val samples = listOf(
            ElevationSample(0.0, 100.0, 500.0),
            ElevationSample(100.0, 110.0, null)
        )
        val scale = ElevationSeries.plotScale(samples)
        assertEquals(ElevationSeries.MinPlotSpanMeters, scale.span, 0.01)
        assertEquals(105.0, (scale.plotMin + scale.plotMax) / 2.0, 0.01)
    }
}

class TrackInspectDumpTest {
    @Test
    fun countsBaroAndUsesDashForMissingValues() {
        val dump = TrackInspectDump.format(
            sessionId = 12,
            startedAt = 1_000L,
            stoppedAt = 2_000L,
            usageType = "TWO_WHEELERS",
            measurementSystem = "METRIC",
            events = listOf(
                TrackInspectEvent(
                    id = 1,
                    timestampMillis = 1_000L,
                    latitude = 47.5,
                    longitude = 19.05,
                    altitude = 120.0,
                    speedMps = 5.5f,
                    bearing = 90f,
                    accuracy = 4f,
                    satellitesInFix = 12,
                    ambientTemperature = null,
                    accelX = null,
                    accelY = null,
                    accelZ = null,
                    leanAngle = null,
                    usageType = "TWO_WHEELERS",
                    isPlacemark = true,
                    eventKind = "START",
                    baroAltitude = 108.0,
                    pressureHpa = 1001.2f
                ),
                TrackInspectEvent(
                    id = 2,
                    timestampMillis = 2_000L,
                    latitude = 47.51,
                    longitude = 19.06,
                    altitude = 125.0,
                    speedMps = 0f,
                    bearing = 0f,
                    accuracy = 5f,
                    satellitesInFix = 11,
                    ambientTemperature = null,
                    accelX = null,
                    accelY = null,
                    accelZ = null,
                    leanAngle = null,
                    usageType = "TWO_WHEELERS",
                    isPlacemark = true,
                    eventKind = "STOP",
                    baroAltitude = null,
                    pressureHpa = null
                )
            )
        )
        assertTrue(dump.contains("sessionId=12"))
        assertTrue(dump.contains("points=2"))
        assertTrue(dump.contains("baroNonNull=1"))
        assertTrue(dump.contains("pressureNonNull=1"))
        assertTrue(dump.contains("tempNonNull=0"))
        assertTrue(dump.contains("108.000000"))
        assertTrue(dump.contains("1001.200"))
        val stopLine = dump.lineSequence().first { it.startsWith("2\t") }
        assertTrue(stopLine.contains("\t-\t-\t"))
    }
}
