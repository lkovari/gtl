package com.lkovari.mobile.apps.gtl.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackPresentationTest {
    @Test
    fun sameTrackDoesNotRebuild() {
        assertTrue(TrackPresentation.same(sample(), sample()))
    }

    @Test
    fun toleranceChangeRebuildsTheTrack() {
        val changed = sample().copy(toleranceMeters = 8.0)
        assertFalse(TrackPresentation.same(sample(), changed))
    }

    @Test
    fun newPointRebuildsTheTrack() {
        val changed = sample().copy(eventCount = 5, lastEventMillis = 2_000L, lastLatitude = 47.1)
        assertFalse(TrackPresentation.same(sample(), changed))
    }

    private fun sample(): TrackPresentationKey {
        return TrackPresentation.key(
            eventCount = 4,
            lastEventMillis = 1_000L,
            lastLatitude = 47.0,
            lastLongitude = 19.0,
            usageName = "MOTORBIKE",
            toleranceMeters = 6.0,
            optimizationActive = true,
            showLastTrackOnMap = true,
            selectedSessionId = null,
            viewingSessionId = 3L,
            logging = true,
            mapCleared = false,
            measurementSystemName = "METRIC",
            sessionUsageName = "MOTORBIKE"
        )
    }
}
