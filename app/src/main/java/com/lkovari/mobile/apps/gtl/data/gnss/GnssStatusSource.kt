package com.lkovari.mobile.apps.gtl.data.gnss

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.GnssStatus
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.lkovari.mobile.apps.gtl.engine.GnssClassifier
import com.lkovari.mobile.apps.gtl.engine.GnssSnapshot
import com.lkovari.mobile.apps.gtl.engine.SatelliteSample
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow

class GnssStatusSource(context: Context) {
    private val appContext = context.applicationContext
    private val locationManager =
        appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    fun snapshots(): Flow<GnssSnapshot> {
        val granted = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            return emptyFlow()
        }
        return callbackFlow {
            val callback = object : GnssStatus.Callback() {
                override fun onSatelliteStatusChanged(status: GnssStatus) {
                    val samples = buildList {
                        for (index in 0 until status.satelliteCount) {
                            val frequency = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                status.getCarrierFrequencyHz(index)
                            } else {
                                null
                            }
                            add(
                                SatelliteSample(
                                    constellation = GnssClassifier.constellationFromAndroid(status.getConstellationType(index)),
                                    svid = status.getSvid(index),
                                    usedInFix = status.usedInFix(index),
                                    cn0DbHz = status.getCn0DbHz(index),
                                    carrierFrequencyHz = frequency
                                )
                            )
                        }
                    }
                    trySend(GnssClassifier.snapshot(samples))
                }
            }
            val registered = try {
                locationManager.registerGnssStatusCallback(callback, Handler(Looper.getMainLooper()))
            } catch (_: SecurityException) {
                false
            }
            if (!registered) {
                close()
            }
            awaitClose {
                try {
                    locationManager.unregisterGnssStatusCallback(callback)
                } catch (_: SecurityException) {
                }
            }
        }
    }
}
