package com.lkovari.mobile.apps.gtl.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow

class LocationClient(context: Context) {
    private val appContext = context.applicationContext
    private val client = LocationServices.getFusedLocationProviderClient(appContext)
    private val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    fun locations(
        minTimeMillis: Long,
        minDistanceMeters: Float,
        gnssOnly: Boolean = false
    ): Flow<Location> {
        val granted = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            return emptyFlow()
        }
        if (gnssOnly && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            return gpsProviderLocations(minTimeMillis, minDistanceMeters)
        }
        return fusedLocations(minTimeMillis, minDistanceMeters)
    }

    private fun gpsProviderLocations(
        minTimeMillis: Long,
        minDistanceMeters: Float
    ): Flow<Location> {
        return callbackFlow {
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    trySend(location)
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
        minDistanceMeters: Float
    ): Flow<Location> {
        return callbackFlow {
            val request = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                minTimeMillis.coerceAtLeast(500L)
            )
                .setMinUpdateDistanceMeters(minDistanceMeters)
                .setWaitForAccurateLocation(false)
                .build()
            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.locations.forEach { location ->
                        trySend(location)
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
            }
        }
    }
}
