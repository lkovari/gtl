package com.lkovari.mobile.apps.gtl.tuhu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TuhuThemeXmlTest {
    @Test
    fun tuhuThemeResolvesEveryOverlayTheSwitchesUse() {
        val resolved = mapsforgeResolvedOverlayIds(tuhuThemeXml().readText())
        assertEquals(overlayIds(), resolved)
    }

    @Test
    fun layerRulesKeepTheirCategoryIds() {
        val xml = tuhuThemeXml().readText()
        overlayIds().forEach { id ->
            assertTrue("missing cat $id", xml.contains("""cat="$id""""))
        }
        assertTrue(xml.contains("""<hillshading cat="hillshading""""))
    }

    private fun overlayIds(): List<String> {
        return listOf(
            TuhuRenderOptions.CAT_BLAZES,
            TuhuRenderOptions.CAT_PATHS,
            TuhuRenderOptions.CAT_CONTOURS,
            TuhuRenderOptions.CAT_CONTOURS_MINOR,
            TuhuRenderOptions.CAT_HIKE_POI,
            TuhuRenderOptions.CAT_PARKS,
            TuhuRenderOptions.CAT_URBAN_POI,
            TuhuRenderOptions.CAT_HILLSHADING
        )
    }

    private fun mapsforgeResolvedOverlayIds(xml: String): List<String> {
        val factory = javax.xml.parsers.DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        val doc = factory.newDocumentBuilder().parse(
            org.xml.sax.InputSource(xml.reader())
        )
        val styleMenus = doc.getElementsByTagNameNS("*", "stylemenu")
        assertEquals(1, styleMenus.length)
        val defined = LinkedHashSet<String>()
        val resolved = ArrayList<String>()
        val children = styleMenus.item(0).childNodes
        for (index in 0 until children.length) {
            val node = children.item(index)
            if (node !is org.w3c.dom.Element || node.localName != "layer") {
                continue
            }
            val id = node.getAttribute("id")
            defined.add(id)
            if (node.getAttribute("visible") != "true") {
                continue
            }
            val kids = node.childNodes
            for (childIndex in 0 until kids.length) {
                val child = kids.item(childIndex)
                if (child !is org.w3c.dom.Element || child.localName != "overlay") {
                    continue
                }
                resolved.add(child.getAttribute("id"))
            }
        }
        resolved.forEach { id ->
            assertTrue(defined.contains(id))
        }
        return resolved
    }

    private fun tuhuThemeXml(): File {
        val candidates = listOf(
            File("src/main/assets/mapsforge/tuhu.xml"),
            File("app/src/main/assets/mapsforge/tuhu.xml")
        )
        return candidates.first { it.isFile }
    }
}
