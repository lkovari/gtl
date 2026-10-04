package com.lkovari.mobile.apps.gtl.engine

enum class LocationSource {
    None,
    Gps,
    Fused
}

object LocationSourceChoice {
    fun source(fineGranted: Boolean, gnssOnly: Boolean): LocationSource {
        if (!fineGranted) {
            return LocationSource.None
        }
        if (gnssOnly) {
            return LocationSource.Gps
        }
        return LocationSource.Fused
    }
}
