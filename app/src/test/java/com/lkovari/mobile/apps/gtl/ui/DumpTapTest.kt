package com.lkovari.mobile.apps.gtl.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DumpTapTest {
    @Test
    fun thirdTapInsideTheWindowOpensTheDump() {
        val first = dumpTap(count = 0, lastTapMillis = 0L, nowMillis = 1_000L)
        assertEquals(1, first.count)
        assertFalse(first.opened)
        val second = dumpTap(count = first.count, lastTapMillis = 1_000L, nowMillis = 1_200L)
        assertEquals(2, second.count)
        assertFalse(second.opened)
        val third = dumpTap(count = second.count, lastTapMillis = 1_200L, nowMillis = 1_400L)
        assertTrue(third.opened)
        assertEquals(0, third.count)
    }

    @Test
    fun lateTapStartsOver() {
        val reset = dumpTap(count = 2, lastTapMillis = 1_000L, nowMillis = 1_000L + DumpTapWindowMillis + 1)
        assertEquals(1, reset.count)
        assertFalse(reset.opened)
    }
}
