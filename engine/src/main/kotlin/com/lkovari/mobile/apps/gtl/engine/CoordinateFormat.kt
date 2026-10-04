package com.lkovari.mobile.apps.gtl.engine

import java.util.Locale

object CoordinateFormat {
    fun coordinate(value: Double): String {
        return String.format(Locale.ROOT, "%.7f", finite(value))
    }

    fun altitude(value: Double): String {
        return String.format(Locale.ROOT, "%.1f", finite(value))
    }

    private fun finite(value: Double): Double {
        return if (value.isFinite()) value else 0.0
    }
}
