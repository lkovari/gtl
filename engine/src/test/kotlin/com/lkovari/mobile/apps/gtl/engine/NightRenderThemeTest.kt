package com.lkovari.mobile.apps.gtl.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class NightRenderThemeTest {
    @Test
    fun paperBecomesDarkBlackCaptionBecomesLightAndMagentaKeepsItsHue() {
        val xml = """
            <rendertheme map-background="#F8F8F8" map-background-outside="#DDDDDD">
                <stylemenu id="gtl">
                    <layer id="poi"><cat id="poi"/></layer>
                </stylemenu>
                <area fill="#F8F8F8" />
                <caption fill="#000000" />
                <line stroke="#C4007A" />
            </rendertheme>
        """.trimIndent()
        val night = NightRenderTheme.recolor(xml)
        assertTrue(night.contains("""map-background="#121A22""""))
        assertTrue(night.contains("""map-background-outside="#0B1218""""))
        assertTrue(night.contains("""<cat id="poi"/>"""))
        assertFalse(night.contains("#F8F8F8", ignoreCase = true))
        assertFalse(night.contains("#000000"))
        assertTrue(night.contains("#E7F0EA"))
        val stroke = Regex("""stroke="#([0-9A-Fa-f]{6})"""").find(night)!!.groupValues[1]
        assertTrue(hueDistance(hueDegrees(stroke), hueDegrees("C4007A")) <= 15.0)
    }

    @Test
    fun eightDigitLandKeepsItsAlpha() {
        val night = NightRenderTheme.recolor("""<area fill="#BBF8F8F8" />""")
        assertTrue(night.contains("#BB121A22"))
    }

    private fun hueDegrees(hex: String): Double {
        val red = hex.substring(0, 2).toInt(16) / 255.0
        val green = hex.substring(2, 4).toInt(16) / 255.0
        val blue = hex.substring(4, 6).toInt(16) / 255.0
        val max = max(red, max(green, blue))
        val min = min(red, min(green, blue))
        val delta = max - min
        if (delta == 0.0) {
            return 0.0
        }
        val hue = when (max) {
            red -> ((green - blue) / delta + if (green < blue) 6.0 else 0.0)
            green -> ((blue - red) / delta + 2.0)
            else -> ((red - green) / delta + 4.0)
        }
        return hue * 60.0
    }

    private fun hueDistance(left: Double, right: Double): Double {
        val delta = abs(left - right) % 360.0
        return min(delta, 360.0 - delta)
    }
}
