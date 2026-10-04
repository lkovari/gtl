package com.lkovari.mobile.apps.gtl.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemePositionTest {
    @Test
    fun storesTheFirstFixAndSkipsAShortMove() {
        assertTrue(ThemePosition.shouldStore(null, null, 47.5, 19.0))
        assertFalse(ThemePosition.shouldStore(47.5, 19.0, 47.51, 19.01))
        assertTrue(ThemePosition.shouldStore(47.5, 19.0, 47.55, 19.0))
    }
}
