package com.lkovari.mobile.apps.gtl.engine

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DownloadResumeTest {
    @Test
    fun freshFileHasNoRangeHeader() {
        assertNull(DownloadResume.rangeHeader(0L))
    }

    @Test
    fun partialFileAsksForTheRemainder() {
        assertEquals("bytes=1200-", DownloadResume.rangeHeader(1200L))
    }

    @Test
    fun partialContentAppends() {
        assertEquals(DownloadBodyMode.Append, DownloadResume.bodyMode(206, 1200L))
    }

    @Test
    fun ignoredRangeOverwrites() {
        assertEquals(DownloadBodyMode.Overwrite, DownloadResume.bodyMode(200, 1200L))
    }

    @Test
    fun contentRangeTotalReadsTheFullLength() {
        assertEquals(3_499_658_275L, DownloadResume.contentRangeTotal("bytes 100-200/3499658275"))
        assertNull(DownloadResume.contentRangeTotal("bytes 0-1/*"))
        assertNull(DownloadResume.contentRangeTotal(null))
    }

    @Test
    fun partialResponseUsesTheRangeTotal() {
        assertEquals(
            3_499_658_275L,
            DownloadResume.fullSize(
                existingBytes = 100L,
                responseCode = 206,
                contentLength = 50L,
                contentRangeTotal = 3_499_658_275L
            )
        )
    }

    @Test
    fun fullResponseUsesContentLength() {
        assertEquals(
            3_233_325_182L,
            DownloadResume.fullSize(
                existingBytes = 0L,
                responseCode = 200,
                contentLength = 3_233_325_182L,
                contentRangeTotal = null
            )
        )
    }

    @Test
    fun sizeUnderOneGigabyteIsMegabytes() {
        assertEquals("255 MB", formatDownloadSize(266_972_269L, Locale.US))
    }

    @Test
    fun sizeAtOrAboveOneGigabyteKeepsOneDecimal() {
        assertEquals("3.3 GB", formatDownloadSize(3_499_658_275L, Locale.US))
    }
}
