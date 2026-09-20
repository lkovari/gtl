package com.lkovari.mobile.apps.gtl.engine

object OsmOfflineAvailability {
    fun canEnable(hasDownloadedMap: Boolean): Boolean {
        return hasDownloadedMap
    }

    fun effectiveUseOffline(wantOffline: Boolean, hasDownloadedMap: Boolean): Boolean {
        return wantOffline && hasDownloadedMap
    }

    fun forceGoogleAfterDelete(
        deletingSelected: Boolean,
        localeMatch: Boolean,
        mapsRemain: Boolean
    ): Boolean {
        return deletingSelected || localeMatch || !mapsRemain
    }
}
