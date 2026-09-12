package com.lkovari.mobile.apps.gtl

import android.app.Application
import com.lkovari.mobile.apps.gtl.data.db.GtlDatabase
import com.lkovari.mobile.apps.gtl.data.db.TrackRepository
import com.lkovari.mobile.apps.gtl.data.gnss.GnssStatusSource
import com.lkovari.mobile.apps.gtl.data.maps.OsmMapStore
import com.lkovari.mobile.apps.gtl.data.prefs.GtlPreferences
import com.lkovari.mobile.apps.gtl.data.sensor.AccelerometerSource
import com.lkovari.mobile.apps.gtl.data.sensor.AmbientTemperatureSource
import com.lkovari.mobile.apps.gtl.data.sensor.CompassSource
import com.lkovari.mobile.apps.gtl.data.sensor.GravitySource
import com.lkovari.mobile.apps.gtl.data.sensor.PressureSource
import com.lkovari.mobile.apps.gtl.data.sync.NoOpRemoteTrackSync
import com.lkovari.mobile.apps.gtl.service.TrackingStateHolder
import org.mapsforge.map.android.graphics.AndroidGraphicFactory

class GtlApplication : Application() {
    lateinit var database: GtlDatabase
        private set
    lateinit var trackRepository: TrackRepository
        private set
    lateinit var preferences: GtlPreferences
        private set
    lateinit var trackingState: TrackingStateHolder
        private set
    lateinit var gnssStatusSource: GnssStatusSource
        private set
    lateinit var ambientTemperatureSource: AmbientTemperatureSource
        private set
    lateinit var accelerometerSource: AccelerometerSource
        private set
    lateinit var gravitySource: GravitySource
        private set
    lateinit var compassSource: CompassSource
        private set
    lateinit var pressureSource: PressureSource
        private set
    lateinit var osmMapStore: OsmMapStore
        private set

    override fun onCreate() {
        super.onCreate()
        AndroidGraphicFactory.createInstance(this)
        database = GtlDatabase.create(this)
        trackRepository = TrackRepository(database, NoOpRemoteTrackSync())
        preferences = GtlPreferences(this)
        trackingState = TrackingStateHolder()
        gnssStatusSource = GnssStatusSource(this)
        ambientTemperatureSource = AmbientTemperatureSource(this)
        accelerometerSource = AccelerometerSource(this)
        gravitySource = GravitySource(this)
        compassSource = CompassSource(this)
        pressureSource = PressureSource(this)
        osmMapStore = OsmMapStore(this)
        trackingState.update {
            it.copy(
                temperatureAvailable = ambientTemperatureSource.isAvailable,
                pressureAvailable = pressureSource.isAvailable
            )
        }
    }
}
