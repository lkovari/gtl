package com.lkovari.mobile.apps.gtl.data.maps

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OsmDownloadBudgetTest {
    @Test
    fun rejectsContentLongerThanCapOrDisk() {
        val roomy = OsmDownloadBudget.MaxBytes
        assertFalse(OsmDownloadBudget.canStart(OsmDownloadBudget.MaxBytes + 1L, roomy))
        assertFalse(
            OsmDownloadBudget.canStart(
                OsmDownloadBudget.ReserveBytes + 1L,
                OsmDownloadBudget.ReserveBytes + 1L
            )
        )
        assertFalse(OsmDownloadBudget.canStart(-1L, OsmDownloadBudget.ReserveBytes))
    }

    @Test
    fun allowsUnknownLengthWhenSpaceRemains() {
        val space = OsmDownloadBudget.ReserveBytes + OsmDownloadBudget.SpaceCheckStride
        assertTrue(OsmDownloadBudget.canStart(-1L, space))
        assertTrue(OsmDownloadBudget.canStart(1024L, space))
    }

    @Test
    fun copyLoopStopsAtCapAndRechecksSpaceOnStride() {
        assertTrue(OsmDownloadBudget.copiedAllowed(OsmDownloadBudget.MaxBytes))
        assertFalse(OsmDownloadBudget.copiedAllowed(OsmDownloadBudget.MaxBytes + 1L))
        assertFalse(OsmDownloadBudget.shouldRecheckSpace(OsmDownloadBudget.SpaceCheckStride - 1L, 0L))
        assertTrue(OsmDownloadBudget.shouldRecheckSpace(OsmDownloadBudget.SpaceCheckStride, 0L))
        assertFalse(OsmDownloadBudget.spaceAllowsMore(OsmDownloadBudget.ReserveBytes))
        assertTrue(OsmDownloadBudget.spaceAllowsMore(OsmDownloadBudget.ReserveBytes + 1L))
    }
}
