package com.lkovari.mobile.apps.gtl.data.maps

import com.lkovari.mobile.apps.gtl.engine.NightRenderTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class NightRenderThemeAssetTest {
    @Test
    fun gtlAndTuhuNightThemesKeepCategoriesAndDarkenThePaper() {
        val gtl = NightRenderTheme.recolor(themeFile("gtl.xml").readText())
        val tuhu = NightRenderTheme.recolor(themeFile("tuhu.xml").readText())
        assertTrue(gtl.contains("""map-background="#121A22""""))
        assertTrue(tuhu.contains("""map-background="#121A22""""))
        assertFalse(gtl.contains("#F8F8F8", ignoreCase = true))
        assertTrue(gtl.contains("""cat id="buildings""""))
        assertTrue(gtl.contains("""cat id="hillshading""""))
        assertTrue(tuhu.contains("""cat="paths""""))
        assertTrue(tuhu.contains("#C4007A"))
        assertTrue(tuhu.contains("""<hillshading cat="hillshading""""))
    }

    private fun themeFile(name: String): File {
        val candidates = listOf(
            File("src/main/assets/mapsforge/$name"),
            File("app/src/main/assets/mapsforge/$name")
        )
        return candidates.first { it.isFile }
    }
}
