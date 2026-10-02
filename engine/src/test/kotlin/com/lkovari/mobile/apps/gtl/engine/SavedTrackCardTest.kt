package com.lkovari.mobile.apps.gtl.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedTrackCardTest {
    @Test
    fun emptyTrackHasZeroStatsAndNoPreview() {
        val card = SavedTrackCards.from(emptyList())
        assertEquals(0.0, card.odometerMeters, 0.0)
        assertEquals(0L, card.elapsedMillis)
        assertEquals(0f, card.averageSpeedMps)
        assertEquals(0f, card.maxSpeedMps)
        assertTrue(card.preview.isEmpty())
        assertTrue(TrackPreview.layout(card.preview).isEmpty())
    }

    @Test
    fun statsFollowTheSamples() {
        val card = SavedTrackCards.from(
            listOf(
                sample(0L, 47.0, 19.0, 0f),
                sample(10_000L, 47.001, 19.0, 8f)
            )
        )
        assertTrue(card.odometerMeters > 100.0)
        assertEquals(10_000L, card.elapsedMillis)
        assertEquals(8f, card.maxSpeedMps)
        assertEquals(2, card.preview.size)
    }

    @Test
    fun previewKeepsBothEndsAndCapsTheCount() {
        val samples = (0 until 100).map { index ->
            sample(index * 1_000L, 47.0 + index * 0.001, 19.0, 5f)
        }
        val card = SavedTrackCards.from(samples, previewPoints = 8)
        assertEquals(8, card.preview.size)
        assertEquals(samples.first().latitude, card.preview.first().latitude, 0.0)
        assertEquals(samples.last().latitude, card.preview.last().latitude, 0.0)
    }

    @Test
    fun eastWestTrackSitsOnAHorizontalLine() {
        val laid = TrackPreview.layout(
            listOf(
                GeoPoint(47.0, 19.0),
                GeoPoint(47.0, 19.02)
            )
        )
        assertEquals(0f, laid.first().x, 0.02f)
        assertEquals(1f, laid.last().x, 0.02f)
        assertEquals(0.5f, laid.first().y, 0.02f)
        assertEquals(0.5f, laid.last().y, 0.02f)
    }

    @Test
    fun northSitsAboveSouth() {
        val laid = TrackPreview.layout(
            listOf(
                GeoPoint(47.0, 19.0),
                GeoPoint(47.02, 19.0)
            )
        )
        val south = laid.first()
        val north = laid.last()
        assertTrue(north.y < south.y)
    }

    @Test
    fun singlePointSitsInTheMiddle() {
        val laid = TrackPreview.layout(listOf(GeoPoint(47.0, 19.0)))
        assertEquals(1, laid.size)
        assertEquals(0.5f, laid.first().x, 0.0f)
        assertEquals(0.5f, laid.first().y, 0.0f)
    }

    private fun sample(time: Long, latitude: Double, longitude: Double, speed: Float): TrackSample {
        return TrackSample(time, latitude, longitude, null, speed, 0f, null, EventKind.MOVE)
    }
}
