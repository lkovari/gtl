package com.lkovari.mobile.apps.gtl.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class TrackPreviewPolylineTest {
    @Test
    fun encodeAndDecodeKeepSevenDecimals() {
        val points = listOf(
            GeoPoint(47.5, 19.04),
            GeoPoint(-0.0005, 0.00001)
        )
        val text = TrackPreviewPolyline.encode(points)
        assertEquals("47.5000000,19.0400000;-0.0005000,0.0000100", text)
        assertEquals(points, TrackPreviewPolyline.decode(text))
    }
}
