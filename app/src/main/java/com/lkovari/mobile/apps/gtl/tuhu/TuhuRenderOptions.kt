package com.lkovari.mobile.apps.gtl.tuhu

data class TuhuRenderOptions(
    val blazes: Boolean,
    val paths: Boolean,
    val contours: Boolean,
    val contoursMinor: Boolean,
    val hikePoi: Boolean,
    val parks: Boolean,
    val urbanPoi: Boolean,
    val hillshading: Boolean
) {
    fun categoryIds(): Set<String> {
        return buildSet {
            if (blazes) add(CAT_BLAZES)
            if (paths) add(CAT_PATHS)
            if (contours) add(CAT_CONTOURS)
            if (contoursMinor) add(CAT_CONTOURS_MINOR)
            if (hikePoi) add(CAT_HIKE_POI)
            if (parks) add(CAT_PARKS)
            if (urbanPoi) add(CAT_URBAN_POI)
            if (hillshading) add(CAT_HILLSHADING)
        }
    }

    fun forMap(hillshadingAvailable: Boolean): TuhuRenderOptions {
        return if (hillshadingAvailable) {
            this
        } else {
            copy(hillshading = false)
        }
    }

    companion object {
        const val CAT_BLAZES = "blazes"
        const val CAT_PATHS = "paths"
        const val CAT_CONTOURS = "contours"
        const val CAT_CONTOURS_MINOR = "contours_minor"
        const val CAT_HIKE_POI = "hike_poi"
        const val CAT_PARKS = "parks"
        const val CAT_URBAN_POI = "urban_poi"
        const val CAT_HILLSHADING = "hillshading"

        fun defaults(): TuhuRenderOptions {
            return TuhuRenderOptions(
                blazes = true,
                paths = true,
                contours = true,
                contoursMinor = false,
                hikePoi = true,
                parks = false,
                urbanPoi = false,
                hillshading = false
            )
        }
    }
}
