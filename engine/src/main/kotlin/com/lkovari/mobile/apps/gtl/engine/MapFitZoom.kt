package com.lkovari.mobile.apps.gtl.engine

object MapFitZoom {
    const val Min = 3
    const val Max = 20

    fun canFit(widthPx: Int?, heightPx: Int?): Boolean {
        return widthPx != null && heightPx != null && widthPx > 0 && heightPx > 0
    }

    fun clamp(zoom: Int): Int {
        return zoom.coerceIn(Min, Max)
    }
}
