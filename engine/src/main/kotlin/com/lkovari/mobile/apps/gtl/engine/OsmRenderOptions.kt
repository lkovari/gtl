package com.lkovari.mobile.apps.gtl.engine

data class OsmRenderOptions(
    val buildings: Boolean,
    val poi: Boolean,
    val transit: Boolean,
    val cycleways: Boolean,
    val parks: Boolean,
    val hillshading: Boolean
) {
    fun categoryIds(): Set<String> {
        return buildSet {
            if (buildings) add(CAT_BUILDINGS)
            if (poi) add(CAT_POI)
            if (transit) add(CAT_TRANSIT)
            if (cycleways) add(CAT_CYCLEWAYS)
            if (parks) add(CAT_PARKS)
            if (hillshading) add(CAT_HILLSHADING)
        }
    }

    fun forMap(hillshadingAvailable: Boolean): OsmRenderOptions {
        return if (hillshadingAvailable) {
            this
        } else {
            copy(hillshading = false)
        }
    }

    companion object {
        const val CAT_BUILDINGS = "buildings"
        const val CAT_POI = "poi"
        const val CAT_TRANSIT = "transit"
        const val CAT_CYCLEWAYS = "cycleways"
        const val CAT_PARKS = "parks"
        const val CAT_HILLSHADING = "hillshading"

        fun cyclewaysForUsage(usage: UsageType): Boolean {
            return usage == UsageType.BICYCLE
        }

        fun defaults(usage: UsageType): OsmRenderOptions {
            return OsmRenderOptions(
                buildings = true,
                poi = false,
                transit = false,
                cycleways = cyclewaysForUsage(usage),
                parks = true,
                hillshading = false
            )
        }
    }
}
