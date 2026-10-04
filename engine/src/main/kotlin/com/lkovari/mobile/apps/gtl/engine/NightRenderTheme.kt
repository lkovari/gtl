package com.lkovari.mobile.apps.gtl.engine

import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object NightRenderTheme {
    private val hexColor = Regex("""#([0-9A-Fa-f]{8}|[0-9A-Fa-f]{6})(?![0-9A-Fa-f])""")
    private val mapBackground = Regex("""map-background="#[0-9A-Fa-f]+"""")
    private val mapBackgroundOutside = Regex("""map-background-outside="#[0-9A-Fa-f]+"""")

    fun recolor(xml: String): String {
        val painted = hexColor.replace(xml) { match ->
            recolorToken(match.value)
        }
        return painted
            .replace(mapBackground, """map-background="#121A22"""")
            .replace(mapBackgroundOutside, """map-background-outside="#0B1218"""")
    }

    fun writeCache(directory: File, name: String, xml: String): File {
        val file = File(directory, name)
        if (!file.isFile || file.readText() != xml) {
            directory.mkdirs()
            file.writeText(xml)
        }
        return file
    }

    private fun recolorToken(token: String): String {
        val body = token.removePrefix("#")
        val alpha = if (body.length == 8) body.substring(0, 2) else null
        val rgb = if (body.length == 8) body.substring(2) else body
        val red = rgb.substring(0, 2).toInt(16)
        val green = rgb.substring(2, 4).toInt(16)
        val blue = rgb.substring(4, 6).toInt(16)
        val (nextRed, nextGreen, nextBlue) = mapColor(red, green, blue)
        val mapped = "%02X%02X%02X".format(nextRed, nextGreen, nextBlue)
        return if (alpha == null) {
            "#$mapped"
        } else {
            "#${alpha.uppercase()}$mapped"
        }
    }

    private fun mapColor(red: Int, green: Int, blue: Int): Triple<Int, Int, Int> {
        val (hue, saturation, lightness) = hsl(red, green, blue)
        if (saturation < 0.22) {
            return when {
                lightness >= 0.72 -> LAND
                lightness <= 0.45 -> INK
                else -> SLATE
            }
        }
        if (hue in 185.0..235.0 && lightness > 0.72) {
            return WATER
        }
        if (hue in 75.0..160.0 && lightness > 0.40) {
            val target = (lightness * 0.45).coerceIn(0.22, 0.40)
            return fromHsl(hue, saturation.coerceIn(0.25, 0.75), target)
        }
        if (lightness <= 0.62) {
            return Triple(red, green, blue)
        }
        return fromHsl(hue, saturation.coerceAtMost(0.9), 0.62)
    }

    private fun hsl(red: Int, green: Int, blue: Int): Triple<Double, Double, Double> {
        val rf = red / 255.0
        val gf = green / 255.0
        val bf = blue / 255.0
        val max = max(rf, max(gf, bf))
        val min = min(rf, min(gf, bf))
        val lightness = (max + min) / 2.0
        if (max == min) {
            return Triple(0.0, 0.0, lightness)
        }
        val delta = max - min
        val saturation = if (lightness > 0.5) {
            delta / (2.0 - max - min)
        } else {
            delta / (max + min)
        }
        val hue = when (max) {
            rf -> ((gf - bf) / delta + if (gf < bf) 6.0 else 0.0)
            gf -> ((bf - rf) / delta + 2.0)
            else -> ((rf - gf) / delta + 4.0)
        } * 60.0
        return Triple(hue, saturation, lightness)
    }

    private fun fromHsl(hue: Double, saturation: Double, lightness: Double): Triple<Int, Int, Int> {
        val channel = (1.0 - abs(2.0 * lightness - 1.0)) * saturation
        val sector = (hue % 360.0) / 60.0
        val cross = channel * (1.0 - abs(sector % 2.0 - 1.0))
        val (red, green, blue) = when {
            sector < 1.0 -> Triple(channel, cross, 0.0)
            sector < 2.0 -> Triple(cross, channel, 0.0)
            sector < 3.0 -> Triple(0.0, channel, cross)
            sector < 4.0 -> Triple(0.0, cross, channel)
            sector < 5.0 -> Triple(cross, 0.0, channel)
            else -> Triple(channel, 0.0, cross)
        }
        val match = lightness - channel / 2.0
        return Triple(
            ((red + match) * 255.0).roundToInt().coerceIn(0, 255),
            ((green + match) * 255.0).roundToInt().coerceIn(0, 255),
            ((blue + match) * 255.0).roundToInt().coerceIn(0, 255)
        )
    }

    private val LAND = Triple(0x12, 0x1A, 0x22)
    private val INK = Triple(0xE7, 0xF0, 0xEA)
    private val SLATE = Triple(0x8A, 0xA0, 0xA8)
    private val WATER = Triple(0x16, 0x34, 0x4C)
}
