package com.lkovari.mobile.apps.gtl.engine

object ThemePosition {
    const val MinMoveMeters = 5_000.0

    fun shouldStore(
        storedLatitude: Double?,
        storedLongitude: Double?,
        latitude: Double,
        longitude: Double
    ): Boolean {
        if (!latitude.isFinite() || !longitude.isFinite()) {
            return false
        }
        if (storedLatitude == null || storedLongitude == null) {
            return true
        }
        if (!storedLatitude.isFinite() || !storedLongitude.isFinite()) {
            return true
        }
        val (east, north) = GeoProjection.eastNorth(
            storedLatitude,
            storedLongitude,
            latitude,
            longitude
        )
        return GeoProjection.hypot(east, north) >= MinMoveMeters
    }
}
