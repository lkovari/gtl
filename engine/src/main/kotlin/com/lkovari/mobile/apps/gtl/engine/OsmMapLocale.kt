package com.lkovari.mobile.apps.gtl.engine

object OsmMapLocale {
    fun countryMatches(regionCountryCode: String, localeCountry: String): Boolean {
        val region = regionCountryCode.trim().uppercase()
        val locale = localeCountry.trim().uppercase()
        return region.isNotEmpty() && locale.isNotEmpty() && region == locale
    }

    fun switchToGoogleMapsOnDelete(
        regionCountryCode: String,
        localeCountry: String,
        deletingSelectedMap: Boolean
    ): Boolean {
        return deletingSelectedMap || countryMatches(regionCountryCode, localeCountry)
    }
}
