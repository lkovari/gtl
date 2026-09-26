package com.lkovari.mobile.apps.gtl.data.sensor

import android.hardware.GeomagneticField
import android.location.Location

object GeomagneticDeclination {
    fun degrees(location: Location): Float? {
        val latitude = location.latitude
        val longitude = location.longitude
        if (!latitude.isFinite() || !longitude.isFinite()) {
            return null
        }
        val time = if (location.time > 0L) location.time else System.currentTimeMillis()
        val altitude = if (location.hasAltitude()) {
            val meters = location.altitude
            if (meters.isFinite()) meters.toFloat() else 0f
        } else {
            0f
        }
        return GeomagneticField(
            latitude.toFloat(),
            longitude.toFloat(),
            altitude,
            time
        ).declination
    }
}
