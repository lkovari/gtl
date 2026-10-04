package com.lkovari.mobile.apps.gtl.engine

import kotlin.math.floor

object Egm2008Geoid {
    private const val Width = 360
    private const val Height = 181
    private val centimeters: ShortArray by lazy { loadGrid() }

    fun undulationMeters(latitude: Double, longitude: Double): Double {
        val lat = latitude.coerceIn(-90.0, 90.0)
        val lon = wrapLongitude(longitude)
        val lat0 = floor(lat).toInt()
        val lonFloor = floor(lon)
        val lon0 = lonFloor.toInt() % Width
        val lat1 = (lat0 + 1).coerceAtMost(90)
        val lon1 = (lon0 + 1) % Width
        val t = if (lat0 == 90) 0.0 else lat - lat0
        val u = lon - lonFloor
        val south = meters(lat0, lon0) * (1.0 - u) + meters(lat0, lon1) * u
        val north = meters(lat1, lon0) * (1.0 - u) + meters(lat1, lon1) * u
        return south * (1.0 - t) + north * t
    }

    private fun meters(latitude: Int, longitude: Int): Double {
        val row = 90 - latitude
        val index = row * Width + (longitude % Width)
        return centimeters[index].toInt() / 100.0
    }

    private fun wrapLongitude(longitude: Double): Double {
        val wrapped = longitude % 360.0
        return if (wrapped < 0.0) wrapped + 360.0 else wrapped
    }

    private fun loadGrid(): ShortArray {
        val bytes = Egm2008Geoid::class.java.getResourceAsStream("/geoid/egm2008-1deg.bin")
            ?.use { stream -> stream.readBytes() }
            ?: error("Missing EGM2008 grid")
        val payload = Width * Height * 2
        check(bytes.size >= 8 + payload)
        check(bytes[0] == 'E'.code.toByte() && bytes[1] == 'G'.code.toByte() && bytes[2] == 'M'.code.toByte() && bytes[3] == '8'.code.toByte())
        val values = ShortArray(Width * Height)
        var offset = 8
        for (index in values.indices) {
            val low = bytes[offset].toInt() and 0xff
            val high = bytes[offset + 1].toInt()
            values[index] = ((high shl 8) or low).toShort()
            offset += 2
        }
        return values
    }
}
