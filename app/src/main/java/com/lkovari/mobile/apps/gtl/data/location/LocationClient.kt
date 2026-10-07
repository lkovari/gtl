package com.lkovari.mobile.apps.gtl.data.location

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.location.LocationCompat
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.lkovari.mobile.apps.gtl.engine.DisplaySpeedFix
import com.lkovari.mobile.apps.gtl.engine.GpsAltitude
import com.lkovari.mobile.apps.gtl.engine.LocationSource
import com.lkovari.mobile.apps.gtl.engine.LocationSourceChoice
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map

class LocationClient(context: Context) {
    private val appContext = context.applicationContext
    private val client = LocationServices.getFusedLocationProviderClient(appContext)
    private val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val gpsEnabledState = MutableStateFlow(readGpsEnabled())
    val gpsEnabled: StateFlow<Boolean> = gpsEnabledState.asStateFlow()

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
        return when (LocationSourceChoice.source(fineGranted, gnssOnly)) {
            LocationSource.None -> emptyFlow()
            LocationSource.Gps -> gpsProviderLocations(minTimeMillis, minDistanceMeters, recording).map { location ->
                withTrustedAltitude(location, location)
            }
            LocationSource.Fused -> fusedLocations(minTimeMillis, minDistanceMeters, recording)
        }
    }

    fun isGpsProviderEnabled(): Boolean {
        return readGpsEnabled()
    }

    private fun gpsProviderLocations(
        minTimeMillis: Long,
        minDistanceMeters: Float,
        recording: Boolean
    ): Flow<Location> {
        return callbackFlow {
            val listener = locationListener(recording) { location ->
                trySend(location)
            }
            var attached = attachGpsUpdates(listener, minTimeMillis, minDistanceMeters)
            if (attached == GpsAttach.Denied) {
                close()
                return@callbackFlow
            }
            val receiver = providerChanges {
                publishGpsEnabled()
                if (attached != GpsAttach.Listening) {
                    attached = attachGpsUpdates(listener, minTimeMillis, minDistanceMeters)
                    if (attached == GpsAttach.Denied) {
                        close()
                    }
                }
            }
            registerProviderChanges(receiver)
            awaitClose {
                detachGpsUpdates(listener)
                unregisterProviderChanges(receiver)
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
                    gpsEnabledState.value = true
                }

                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                }

                override fun onProviderEnabled(provider: String) {
                    if (provider == LocationManager.GPS_PROVIDER) {
                        gpsEnabledState.value = true
                    }
                }

                override fun onProviderDisabled(provider: String) {
                    if (provider == LocationManager.GPS_PROVIDER) {
                        gpsEnabledState.value = false
                    }
                }
            }
            var gpsAttached = attachGpsUpdates(gpsListener, minTimeMillis, minDistanceMeters)
            val receiver = providerChanges {
                publishGpsEnabled()
                if (gpsAttached != GpsAttach.Listening) {
                    gpsAttached = attachGpsUpdates(gpsListener, minTimeMillis, minDistanceMeters)
                }
            }
            registerProviderChanges(receiver)
            val intervalMillis = minTimeMillis.coerceAtLeast(500L)
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis)
                .setGranularity(Granularity.GRANULARITY_FINE)
                .setMinUpdateIntervalMillis(intervalMillis)
                .setMaxUpdateDelayMillis(0L)
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
                detachGpsUpdates(gpsListener)
                unregisterProviderChanges(receiver)
            }
        }
    }

    private fun locationListener(
        recording: Boolean,
        onFix: (Location) -> Unit
    ): LocationListener {
        return object : LocationListener {
            override fun onLocationChanged(location: Location) {
                gpsEnabledState.value = true
                if (isFreshEnough(location, recording)) {
                    onFix(location)
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            }

            override fun onProviderEnabled(provider: String) {
                if (provider == LocationManager.GPS_PROVIDER) {
                    gpsEnabledState.value = true
                }
            }

            override fun onProviderDisabled(provider: String) {
                if (provider == LocationManager.GPS_PROVIDER) {
                    gpsEnabledState.value = false
                }
            }
        }
    }

    private fun attachGpsUpdates(
        listener: LocationListener,
        minTimeMillis: Long,
        minDistanceMeters: Float
    ): GpsAttach {
        return try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                minTimeMillis.coerceAtLeast(500L),
                minDistanceMeters,
                listener,
                Looper.getMainLooper()
            )
            publishGpsEnabled()
            GpsAttach.Listening
        } catch (_: IllegalArgumentException) {
            publishGpsEnabled()
            GpsAttach.Unavailable
        } catch (_: SecurityException) {
            GpsAttach.Denied
        }
    }

    private fun detachGpsUpdates(listener: LocationListener) {
        try {
            locationManager.removeUpdates(listener)
        } catch (_: SecurityException) {
        }
    }

    private fun providerChanges(onChange: () -> Unit): BroadcastReceiver {
        return object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == LocationManager.PROVIDERS_CHANGED_ACTION) {
                    onChange()
                }
            }
        }
    }

    private fun registerProviderChanges(receiver: BroadcastReceiver) {
        ContextCompat.registerReceiver(
            appContext,
            receiver,
            IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION),
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    private fun unregisterProviderChanges(receiver: BroadcastReceiver) {
        try {
            appContext.unregisterReceiver(receiver)
        } catch (_: IllegalArgumentException) {
        }
    }

    private fun publishGpsEnabled() {
        gpsEnabledState.value = readGpsEnabled()
    }

    private fun readGpsEnabled(): Boolean {
        return runCatching { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false)
    }

    private enum class GpsAttach {
        Listening,
        Unavailable,
        Denied
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
    val freshGnss = gnss?.takeIf { sample ->
        GpsAltitude.gnssAltitudeIsFresh(primary.elapsedRealtimeNanos, sample.elapsedRealtimeNanos)
    }
    val chosen = GpsAltitude.toMsl(
        latitude = primary.latitude,
        longitude = primary.longitude,
        gnssMsl = mslOrNull(freshGnss),
        fusedMsl = mslOrNull(primary),
        gnssEllipsoid = ellipsoidOrNull(freshGnss),
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

fun Location.toDisplaySpeedFix(): DisplaySpeedFix {
    val speedAccuracy = if (LocationCompat.hasSpeedAccuracy(this)) {
        LocationCompat.getSpeedAccuracyMetersPerSecond(this)
    } else {
        null
    }
    return DisplaySpeedFix(
        speedMps = if (hasSpeed()) speed else null,
        speedAccuracyMps = speedAccuracy,
        latitude = latitude,
        longitude = longitude,
        horizontalAccuracyMeters = if (hasAccuracy()) accuracy else null
    )
}

private fun ellipsoidOrNull(location: Location?): Double? {
    if (location == null || !location.hasAltitude()) {
        return null
    }
    return location.altitude
}
