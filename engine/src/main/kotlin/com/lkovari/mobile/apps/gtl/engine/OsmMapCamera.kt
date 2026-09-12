package com.lkovari.mobile.apps.gtl.engine

data class OsmMapCenter(
    val latitude: Double,
    val longitude: Double
)

object OsmMapCamera {
    const val GpsZoom = 14

    fun contains(bounds: LatLonBounds, latitude: Double, longitude: Double): Boolean {
        return latitude >= bounds.minLatitude &&
            latitude <= bounds.maxLatitude &&
            longitude >= bounds.minLongitude &&
            longitude <= bounds.maxLongitude
    }

    fun initialCenter(
        mapBounds: LatLonBounds,
        mapStartLatitude: Double,
        mapStartLongitude: Double,
        locationLatitude: Double?,
        locationLongitude: Double?
    ): OsmMapCenter {
        if (locationLatitude != null &&
            locationLongitude != null &&
            contains(mapBounds, locationLatitude, locationLongitude)
        ) {
            return OsmMapCenter(locationLatitude, locationLongitude)
        }
        return OsmMapCenter(mapStartLatitude, mapStartLongitude)
    }

    fun initialZoom(gpsInsideMap: Boolean, mapStartZoom: Int): Int {
        return MapFitZoom.clamp(if (gpsInsideMap) GpsZoom else mapStartZoom)
    }

    fun followCenter(
        mapBounds: LatLonBounds,
        preferTrack: Boolean,
        trackLatitude: Double?,
        trackLongitude: Double?,
        locationLatitude: Double?,
        locationLongitude: Double?
    ): OsmMapCenter? {
        val latitude: Double
        val longitude: Double
        if (preferTrack && trackLatitude != null && trackLongitude != null) {
            latitude = trackLatitude
            longitude = trackLongitude
        } else if (locationLatitude != null && locationLongitude != null) {
            latitude = locationLatitude
            longitude = locationLongitude
        } else if (trackLatitude != null && trackLongitude != null) {
            latitude = trackLatitude
            longitude = trackLongitude
        } else {
            return null
        }
        if (!contains(mapBounds, latitude, longitude)) {
            return null
        }
        return OsmMapCenter(latitude, longitude)
    }
}
