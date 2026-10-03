package com.lkovari.mobile.apps.gtl.data.device

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceIdentityTest {
    @Test
    fun joinsBrandModelAndRelease() {
        assertEquals(
            "Google Pixel 8 · Android 16",
            DeviceIdentity.format("Google", "Pixel 8", "16")
        )
    }

    @Test
    fun skipsBrandWhenTheModelAlreadyContainsIt() {
        assertEquals(
            "samsung SM-S911B · Android 15",
            DeviceIdentity.format("samsung", "samsung SM-S911B", "15")
        )
    }

    @Test
    fun fallsBackWhenNamesAreBlank() {
        assertEquals("Android · Android 14", DeviceIdentity.format("  ", "", "14"))
        assertEquals("Pixel 8", DeviceIdentity.format("", "Pixel 8", ""))
    }
}
