package com.lkovari.mobile.apps.gtl.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.location.LocationCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.lkovari.mobile.apps.gtl.engine.GpsAltitude
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map

class LocationClient(context: Context) {
    private val appContext = context.applicationContext
    private val client = LocationServices.getFusedLocationProviderClient(appContext)
    private val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    fun locations(
        minTimeMillis: Long,
        minDistanceMeters: Float,
        gnssOnly: Boolean = false,
        recording: Boolean = false
    ): Flow<Location> {
        val fineGranted = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!fineGranted) {
            return emptyFlow()
        }
        if (gnssOnly) {
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                return emptyFlow()
            }
            return gpsProviderLocations(minTimeMillis, minDistanceMeters, recording).map { location ->
                withTrustedAltitude(location, location)
            }
        }
        return fusedLocations(minTimeMillis, minDistanceMeters, recording)
    }

    fun isGpsProviderEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun gpsProviderLocations(
        minTimeMillis: Long,
        minDistanceMeters: Float,
        recording: Boolean
    ): Flow<Location> {
        return callbackFlow {
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    if (isFreshEnough(location, recording)) {
                        trySend(location)
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                }

                override fun onProviderEnabled(provider: String) {
                }

                override fun onProviderDisabled(provider: String) {
                }
            }
            try {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    minTimeMillis.coerceAtLeast(500L),
                    minDistanceMeters,
                    listener,
                    Looper.getMainLooper()
                )
            } catch (_: SecurityException) {
                close()
            }
            awaitClose {
                try {
                    locationManager.removeUpdates(listener)
                } catch (_: SecurityException) {
                }
            }
        }
    }

    private fun fusedLocations(
        minTimeMillis: Long,
        minDistanceMeters: Float,
        recording: Boolean
    ): Flow<Location> {
        return callbackFlow {
            var lastGnss: Location? = null
            val gpsListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    lastGnss = location
                }

                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                }

                override fun onProviderEnabled(provider: String) {
                }

                override fun onProviderDisabled(provider: String) {
                }
            }
            try {
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        minTimeMillis.coerceAtLeast(500L),
                        minDistanceMeters,
                        gpsListener,
                        Looper.getMainLooper()
                    )
                }
            } catch (_: SecurityException) {
            }
            val request = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                minTimeMillis.coerceAtLeast(500L)
            )
                .setMinUpdateDistanceMeters(minDistanceMeters)
                .setWaitForAccurateLocation(recording)
                .build()
            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.locations.forEach { location ->
                        if (isFreshEnough(location, recording)) {
                            trySend(withTrustedAltitude(location, lastGnss))
                        }
                    }
                }
            }
            try {
                client.requestLocationUpdates(request, callback, Looper.getMainLooper())
            } catch (_: SecurityException) {
                close()
            }
            awaitClose {
                try {
                    client.removeLocationUpdates(callback)
                } catch (_: SecurityException) {
                }
                try {
                    locationManager.removeUpdates(gpsListener)
                } catch (_: SecurityException) {
                }
            }
        }
    }

    companion object {
        private const val MaxRecordingAgeNanos = 10_000_000_000L

        internal fun isFreshEnough(location: Location, recording: Boolean): Boolean {
            if (!recording) {
                return true
            }
            val elapsed = location.elapsedRealtimeNanos
            if (elapsed <= 0L) {
                return false
            }
            val age = SystemClock.elapsedRealtimeNanos() - elapsed
            return age in 0L..MaxRecordingAgeNanos
        }
    }
}

internal fun withTrustedAltitude(primary: Location, gnss: Location?): Location {
    val chosen = GpsAltitude.pick(
        gnssMsl = mslOrNull(gnss),
        fusedMsl = mslOrNull(primary),
        gnssEllipsoid = ellipsoidOrNull(gnss),
        fusedEllipsoid = ellipsoidOrNull(primary)
    )
    if (chosen == null) {
        if (!primary.hasAltitude()) {
            return primary
        }
        val copy = Location(primary)
        copy.removeAltitude()
        return copy
    }
    if (primary.hasAltitude() && primary.altitude == chosen) {
        return primary
    }
    val copy = Location(primary)
    copy.altitude = chosen
    return copy
}

private fun mslOrNull(location: Location?): Double? {
    if (location == null || !LocationCompat.hasMslAltitude(location)) {
        return null
    }
    return LocationCompat.getMslAltitudeMeters(location)
}

private fun ellipsoidOrNull(location: Location?): Double? {
    if (location == null || !location.hasAltitude()) {
        return null
    }
    return location.altitude
}
