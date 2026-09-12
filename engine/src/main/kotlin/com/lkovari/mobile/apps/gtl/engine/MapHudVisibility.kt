package com.lkovari.mobile.apps.gtl.engine

enum class MapHudMode {
    Hidden,
    Compact,
    Full
}

object MapHudVisibility {
    fun mode(logging: Boolean, selectedSessionId: Long?, hasFix: Boolean): MapHudMode {
        if (selectedSessionId != null && !logging) {
            return MapHudMode.Hidden
        }
        if (logging) {
            return MapHudMode.Full
        }
        if (hasFix) {
            return MapHudMode.Compact
        }
        return MapHudMode.Hidden
    }
}
