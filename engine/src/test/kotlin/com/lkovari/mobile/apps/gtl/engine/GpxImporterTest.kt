package com.lkovari.mobile.apps.gtl.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class GpxImporterTest {
    @Test
    fun magentaIsTMobile() {
        assertEquals(0xFFE20074.toInt(), ImportedGpxStyle.Argb)
    }

    @Test
    fun roundTripsGtlExport() {
        val gpx = GpxExporter.export(
            GpxDocument(
                tracks = listOf(
                    GpxTrack(
                        name = "Ride",
                        points = listOf(
                            GpxTrackPoint(GeoPoint(47.5, 19.05, 120.0), 1_725_500_000_000L),
                            GpxTrackPoint(GeoPoint(47.51, 19.06, 125.0), 1_725_500_001_000L)
                        ),
                        waypoints = listOf(
                            GpxWaypoint("START", GeoPoint(47.5, 19.05, 120.0), 1_725_500_000_000L),
                            GpxWaypoint("STOP", GeoPoint(47.51, 19.06, 125.0), 1_725_500_001_000L)
                        )
                    )
                )
            )
        )
        val track = parse(gpx) as GpxImport.Track
        assertEquals(1, track.lines.size)
        assertEquals(2, track.lines[0].size)
        assertEquals(47.5, track.lines[0][0].latitude, 0.0000001)
        assertEquals(19.05, track.lines[0][0].longitude, 0.0000001)
        assertEquals(47.51, track.lines[0][1].latitude, 0.0000001)
        assertEquals(19.06, track.lines[0][1].longitude, 0.0000001)
    }

    @Test
    fun readsGpx10() {
        val track = parse(
            """
            <gpx version="1.0" creator="Legacy" xmlns="http://www.topografix.com/GPX/1/0">
              <trk><trkseg>
                <trkpt lat="47.5" lon="19.05"><ele>10</ele><time>2020-01-01T00:00:00Z</time></trkpt>
                <trkpt lat="47.51" lon="19.06"></trkpt>
              </trkseg></trk>
            </gpx>
            """.trimIndent()
        ) as GpxImport.Track
        assertEquals(1, track.lines.size)
        assertEquals(47.51, track.lines[0][1].latitude, 0.0000001)
    }

    @Test
    fun readsFileWithoutNamespace() {
        val track = parse(
            """
            <gpx version="1.1">
              <trk><trkseg>
                <trkpt lat="47.5" lon="19.05"/>
                <trkpt lat="47.6" lon="19.1"/>
              </trkseg></trk>
            </gpx>
            """.trimIndent()
        ) as GpxImport.Track
        assertEquals(19.1, track.lines[0][1].longitude, 0.0000001)
    }

    @Test
    fun readsPrefixedTags() {
        val track = parse(
            """
            <gpx:gpx xmlns:gpx="http://www.topografix.com/GPX/1/1" version="1.1">
              <gpx:trk><gpx:trkseg>
                <gpx:trkpt lat="47.5" lon="19.05"/>
                <gpx:trkpt lat="47.52" lon="19.07"/>
              </gpx:trkseg></gpx:trk>
            </gpx:gpx>
            """.trimIndent()
        ) as GpxImport.Track
        assertEquals(47.52, track.lines[0][1].latitude, 0.0000001)
    }

    @Test
    fun keepsSegmentsSeparate() {
        val track = parse(
            """
            <gpx version="1.1">
              <trk>
                <trkseg>
                  <trkpt lat="47.5" lon="19.05"/>
                  <trkpt lat="47.51" lon="19.06"/>
                </trkseg>
                <trkseg>
                  <trkpt lat="48.0" lon="19.5"/>
                  <trkpt lat="48.01" lon="19.51"/>
                </trkseg>
              </trk>
            </gpx>
            """.trimIndent()
        ) as GpxImport.Track
        assertEquals(2, track.lines.size)
        assertEquals(2, track.lines[0].size)
        assertEquals(48.0, track.lines[1][0].latitude, 0.0000001)
        assertEquals(19.05, track.lines[0][0].longitude, 0.0000001)
    }

    @Test
    fun readsRouteAsItsOwnLine() {
        val track = parse(
            """
            <gpx version="1.1">
              <trk><trkseg>
                <trkpt lat="47.5" lon="19.05"/>
                <trkpt lat="47.51" lon="19.06"/>
              </trkseg></trk>
              <rte>
                <rtept lat="46.0" lon="18.0"/>
                <rtept lat="46.1" lon="18.1"/>
              </rte>
            </gpx>
            """.trimIndent()
        ) as GpxImport.Track
        assertEquals(2, track.lines.size)
        assertEquals(46.0, track.lines[1][0].latitude, 0.0000001)
        assertEquals(18.1, track.lines[1][1].longitude, 0.0000001)
    }

    @Test
    fun ignoresGarminExtensionPoints() {
        val track = parse(
            """
            <gpx xmlns="http://www.topografix.com/GPX/1/1" version="1.1"
                 xmlns:gpxtpx="http://www.garmin.com/xmlschemas/TrackPointExtension/v1">
              <trk><trkseg>
                <trkpt lat="47.5" lon="19.05">
                  <extensions>
                    <gpxtpx:TrackPointExtension>
                      <gpxtpx:hr>120</gpxtpx:hr>
                      <gpxtpx:trkpt lat="0" lon="0"/>
                    </gpxtpx:TrackPointExtension>
                  </extensions>
                </trkpt>
                <trkpt lat="47.51" lon="19.06"/>
              </trkseg></trk>
            </gpx>
            """.trimIndent()
        ) as GpxImport.Track
        assertEquals(1, track.lines.size)
        assertEquals(2, track.lines[0].size)
        assertEquals(47.5, track.lines[0][0].latitude, 0.0000001)
    }

    @Test
    fun acceptsCommaDecimalsAndScientificNotation() {
        val track = parse(
            """
            <gpx version="1.1">
              <trk><trkseg>
                <trkpt lat="47,5" lon="19,05"/>
                <trkpt lat="4.751E1" lon="1.906E1"/>
              </trkseg></trk>
            </gpx>
            """.trimIndent()
        ) as GpxImport.Track
        assertEquals(47.5, track.lines[0][0].latitude, 0.0000001)
        assertEquals(19.05, track.lines[0][0].longitude, 0.0000001)
        assertEquals(47.51, track.lines[0][1].latitude, 0.00001)
        assertEquals(19.06, track.lines[0][1].longitude, 0.00001)
    }

    @Test
    fun drawsPointsThatHaveNoElevationOrTime() {
        val track = parse(
            """
            <gpx>
              <trk><trkseg>
                <trkpt lat="47.5" lon="19.05"/>
                <trkpt lat="47.51" lon="19.06"/>
              </trkseg></trk>
            </gpx>
            """.trimIndent()
        ) as GpxImport.Track
        assertEquals(2, track.lines[0].size)
    }

    @Test
    fun skipsABadPointAndKeepsNeighbours() {
        val track = parse(
            """
            <gpx version="1.1">
              <trk><trkseg>
                <trkpt lat="47.5" lon="19.05"/>
                <trkpt lat="99" lon="19.05"/>
                <trkpt lat="47.51" lon="19.06"/>
              </trkseg></trk>
            </gpx>
            """.trimIndent()
        ) as GpxImport.Track
        assertEquals(1, track.lines.size)
        assertEquals(2, track.lines[0].size)
        assertEquals(47.51, track.lines[0][1].latitude, 0.0000001)
    }

    @Test
    fun readsTrackPointsWrittenDirectlyUnderTheTrack() {
        val track = parse(
            """
            <gpx version="1.0">
              <trk>
                <trkpt lat="47.5" lon="19.05"/>
                <trkpt lat="47.52" lon="19.08"/>
              </trk>
            </gpx>
            """.trimIndent()
        ) as GpxImport.Track
        assertEquals(1, track.lines.size)
        assertEquals(47.52, track.lines[0][1].latitude, 0.0000001)
    }

    @Test
    fun waypointOnlyFileIsNoTrack() {
        val result = parse(
            """
            <gpx version="1.1">
              <wpt lat="47.5" lon="19.05"><name>A</name></wpt>
              <wpt lat="47.6" lon="19.1"><name>B</name></wpt>
            </gpx>
            """.trimIndent()
        )
        assertEquals(GpxImport.NoTrack, result)
    }

    @Test
    fun htmlFileIsNotGpx() {
        val result = parse("<html><body>not a track</body></html>")
        assertEquals(GpxImport.NotGpx, result)
    }

    @Test
    fun truncatedXmlIsUnreadable() {
        val result = parse("""<gpx version="1.1"><trk><trkpt lat="47.5" lon="19.05">""")
        assertEquals(GpxImport.Unreadable, result)
    }

    @Test
    fun thinsALineLongerThanEightThousandPoints() {
        val xml = buildString {
            append("""<gpx version="1.1"><trk><trkseg>""")
            for (index in 0 until 8_001) {
                append("""<trkpt lat="47.5" lon="${19.0 + index * 0.00001}"/>""")
            }
            append("""</trkseg></trk></gpx>""")
        }
        val track = parse(xml) as GpxImport.Track
        assertEquals(1, track.lines.size)
        assertTrue(track.lines[0].size < 8_001)
        assertEquals(19.0, track.lines[0].first().longitude, 0.0000001)
        assertEquals(19.0 + 8_000 * 0.00001, track.lines[0].last().longitude, 0.0000001)
    }

    private fun parse(xml: String): GpxImport {
        return GpxImporter.parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))
    }
}
