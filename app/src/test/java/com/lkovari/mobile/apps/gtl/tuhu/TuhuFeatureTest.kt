package com.lkovari.mobile.apps.gtl.tuhu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TuhuUiTest {
    @Test
    fun flagOffHidesEverySurface() {
        assertFalse(TuhuUi.showDownloadRow(enabled = false))
        assertFalse(TuhuUi.showSettings(enabled = false, inUse = true))
        assertFalse(TuhuUi.showAbout(enabled = false))
        assertFalse(TuhuUi.showHelp(enabled = false, mapDownloaded = true))
        assertFalse(TuhuUi.showMapControls(enabled = false, selectedPath = "/maps/tuhu.map"))
        assertFalse(TuhuUi.isActive(enabled = false, selectedPath = "/maps/tuhu.map"))
    }

    @Test
    fun flagOnWithoutMapShowsDownloadAndAboutOnly() {
        assertTrue(TuhuUi.showDownloadRow(enabled = true))
        assertFalse(TuhuUi.showSettings(enabled = true, inUse = false))
        assertTrue(TuhuUi.showAbout(enabled = true))
        assertFalse(TuhuUi.showHelp(enabled = true, mapDownloaded = false))
        assertFalse(TuhuUi.showMapControls(enabled = true, selectedPath = "/maps/eu-hungary.map"))
        assertFalse(TuhuUi.isActive(enabled = true, selectedPath = "/maps/eu-hungary.map"))
    }

    @Test
    fun flagOnWithTuhuMapShowsHelpAndMapControls() {
        val path = "/data/files/maps/tuhu.map"
        assertTrue(TuhuUi.showHelp(enabled = true, mapDownloaded = true))
        assertTrue(TuhuUi.showSettings(enabled = true, inUse = true))
        assertFalse(TuhuUi.showSettings(enabled = true, inUse = false))
        assertTrue(TuhuUi.showMapControls(enabled = true, selectedPath = path))
        assertTrue(TuhuUi.isActive(enabled = true, selectedPath = path))
        assertTrue(TuhuUi.isTuhuMap(path))
    }

    @Test
    fun blankOrOsmPathIsNotTuhu() {
        assertFalse(TuhuUi.isTuhuMap(""))
        assertFalse(TuhuUi.isTuhuMap("/maps/hungary.map"))
        assertFalse(TuhuUi.isTuhuMap("/maps/tuhu.map.part"))
    }
}

class TuhuDeletePolicyTest {
    @Test
    fun deletingSelectedTuhuForcesGoogle() {
        assertTrue(TuhuDeletePolicy.forceGoogle(deletingSelectedTuhu = true))
    }

    @Test
    fun deletingTuhuWhileAnotherMapIsSelectedKeepsOffline() {
        assertFalse(TuhuDeletePolicy.forceGoogle(deletingSelectedTuhu = false))
    }
}

class TuhuRenderOptionsTest {
    @Test
    fun defaultsMatchProductSwitches() {
        val options = TuhuRenderOptions.defaults()
        assertTrue(options.blazes)
        assertTrue(options.paths)
        assertTrue(options.contours)
        assertFalse(options.contoursMinor)
        assertTrue(options.hikePoi)
        assertFalse(options.parks)
        assertFalse(options.urbanPoi)
        assertFalse(options.hillshading)
    }

    @Test
    fun categoryIdsIncludeOnlyEnabledLayers() {
        val none = TuhuRenderOptions(
            blazes = false,
            paths = false,
            contours = false,
            contoursMinor = false,
            hikePoi = false,
            parks = false,
            urbanPoi = false,
            hillshading = false
        )
        assertTrue(none.categoryIds().isEmpty())
        val some = none.copy(blazes = true, contours = true, hikePoi = true)
        assertEquals(
            setOf(
                TuhuRenderOptions.CAT_BLAZES,
                TuhuRenderOptions.CAT_CONTOURS,
                TuhuRenderOptions.CAT_HIKE_POI
            ),
            some.categoryIds()
        )
    }

    @Test
    fun hillshadingCategoryDropsWhenMapHasNoElevation() {
        val all = TuhuRenderOptions.defaults().copy(hillshading = true)
        assertFalse(all.forMap(hillshadingAvailable = false).hillshading)
        assertFalse(all.forMap(false).categoryIds().contains(TuhuRenderOptions.CAT_HILLSHADING))
        assertTrue(all.forMap(true).hillshading)
        assertTrue(all.forMap(true).categoryIds().contains(TuhuRenderOptions.CAT_HILLSHADING))
    }

    @Test
    fun forMapWithoutElevationKeepsOtherLayerSwitches() {
        val all = TuhuRenderOptions.defaults().copy(parks = true, urbanPoi = true, hillshading = true)
        val forMap = all.forMap(hillshadingAvailable = false)
        assertTrue(forMap.blazes)
        assertTrue(forMap.paths)
        assertTrue(forMap.contours)
        assertTrue(forMap.hikePoi)
        assertTrue(forMap.parks)
        assertTrue(forMap.urbanPoi)
        assertFalse(forMap.hillshading)
    }
}
