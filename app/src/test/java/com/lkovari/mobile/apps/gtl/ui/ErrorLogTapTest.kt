package com.lkovari.mobile.apps.gtl.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ErrorLogTapTest {
    @Test
    fun seventhTapOpens() {
        var count = 0
        var last = 0L
        var opened = false
        for (step in 1..6) {
            val tap = errorLogTap(count, last, step * 100L)
            count = tap.count
            last = step * 100L
            opened = tap.opened
            assertFalse(opened)
        }
        val seventh = errorLogTap(count, last, 700L)
        assertTrue(seventh.opened)
        assertEquals(0, seventh.count)
    }

    @Test
    fun lateTapResets() {
        val first = errorLogTap(count = 0, lastTapMillis = 0L, nowMillis = 1_000L)
        assertEquals(1, first.count)
        val stillInside = errorLogTap(
            count = first.count,
            lastTapMillis = 1_000L,
            nowMillis = 1_000L + ErrorLogTapWindowMillis
        )
        assertEquals(2, stillInside.count)
        assertFalse(stillInside.opened)
        val reset = errorLogTap(
            count = 6,
            lastTapMillis = 5_000L,
            nowMillis = 5_000L + ErrorLogTapWindowMillis + 1
        )
        assertEquals(1, reset.count)
        assertFalse(reset.opened)
    }
}
