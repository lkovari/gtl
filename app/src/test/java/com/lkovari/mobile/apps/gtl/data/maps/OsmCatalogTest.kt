package com.lkovari.mobile.apps.gtl.data.maps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OsmCatalogTest {
    @Test
    fun regionsComeFromTheMirror() {
        assertTrue(OsmCatalog.regions.all { it.url.startsWith(OsmCatalog.MIRROR) })
    }

    @Test
    fun labelsDoNotUseTheEuPrefix() {
        val ids = listOf(
            "eu-switzerland",
            "eu-england",
            "eu-scotland",
            "eu-wales",
            "eu-norway",
            "eu-ukraine",
            "eu-serbia"
        )
        ids.forEach { id ->
            val region = OsmCatalog.regions.first { it.id == id }
            assertTrue(region.europe)
            assertFalse(region.label.startsWith("EU"))
            assertEquals("Europe ${region.label}", region.title("Europe"))
        }
        assertTrue(OsmCatalog.regions.none { it.label.startsWith("EU") })
        assertFalse(OsmCatalog.regions.first { it.id == "sa-brazil" }.europe)
        assertFalse(OsmCatalog.regions.first { it.id == "asia-japan" }.europe)
    }

    @Test
    fun fallbackRewritesOnlyTheMirrorHost() {
        val hungary = OsmCatalog.regions.first { it.id == "eu-hungary" }.url
        assertEquals(
            "https://download.mapsforge.org/maps/v5/europe/hungary.map",
            OsmCatalog.fallbackUrl(hungary)
        )
        assertNull(OsmCatalog.fallbackUrl("https://download.mapsforge.org/maps/v5/europe/hungary.map"))
    }
}
