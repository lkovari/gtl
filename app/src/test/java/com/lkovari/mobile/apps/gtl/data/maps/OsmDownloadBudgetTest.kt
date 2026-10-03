package com.lkovari.mobile.apps.gtl.data.maps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OsmDownloadBudgetTest {
    private val GermanyBytes = 3_233_325_182L
    private val FranceBytes = 3_499_658_275L
    private val BrazilBytes = 2_141_449_906L

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
    fun liveCountryFilesFitWithRoomAboveFrance() {
        val roomy = OsmDownloadBudget.MaxBytes + OsmDownloadBudget.ReserveBytes
        assertTrue(OsmDownloadBudget.canStart(GermanyBytes, roomy))
        assertTrue(OsmDownloadBudget.canStart(FranceBytes, roomy))
        assertTrue(OsmDownloadBudget.canStart(BrazilBytes, roomy))
        assertTrue(OsmDownloadBudget.MaxBytes - FranceBytes >= 512L * 1024L * 1024L)
        assertEquals(OsmDownloadBudget.ReasonTooLarge, OsmDownloadBudget.rejection(OsmDownloadBudget.MaxBytes + 1L, roomy))
    }

    @Test
    fun blocksBeforeStartWhenTheFileDoesNotFit() {
        val france = FranceBytes
        val reserve = OsmDownloadBudget.ReserveBytes
        assertEquals(
            OsmDownloadBudget.ReasonNoSpace,
            OsmDownloadBudget.blockBeforeStart(france, france)
        )
        assertEquals(
            OsmDownloadBudget.ReasonNoSpace,
            OsmDownloadBudget.blockBeforeStart(null, reserve)
        )
        assertEquals(null, OsmDownloadBudget.blockBeforeStart(null, reserve + 1L))
        assertEquals(
            OsmDownloadBudget.ReasonTooLarge,
            OsmDownloadBudget.blockBeforeStart(OsmDownloadBudget.MaxBytes + 1L, OsmDownloadBudget.MaxBytes)
        )
        val partial = 1024L * 1024L * 1024L
        val usable = france - partial + reserve
        assertEquals(null, OsmDownloadBudget.blockBeforeStart(france, usable, partial))
        assertEquals(
            OsmDownloadBudget.ReasonNoSpace,
            OsmDownloadBudget.blockBeforeStart(france, usable, partial - 1L)
        )
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
