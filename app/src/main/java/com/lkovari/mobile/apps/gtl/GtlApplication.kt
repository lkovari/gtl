package com.lkovari.mobile.apps.gtl

import android.app.Application
import com.lkovari.mobile.apps.gtl.data.db.GtlDatabase
import com.lkovari.mobile.apps.gtl.data.db.TrackRepository
import com.lkovari.mobile.apps.gtl.data.gnss.GnssStatusSource
import com.lkovari.mobile.apps.gtl.data.maps.OsmMapStore
import com.lkovari.mobile.apps.gtl.data.search.MapSearchRepository
import com.lkovari.mobile.apps.gtl.data.prefs.GtlPreferences
import com.lkovari.mobile.apps.gtl.tuhu.TuhuMapStore
import com.lkovari.mobile.apps.gtl.tuhu.TuhuPreferences
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
    lateinit var tuhuPreferences: TuhuPreferences
        private set
    lateinit var tuhuMapStore: TuhuMapStore
        private set
    lateinit var mapSearch: MapSearchRepository
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
        tuhuPreferences = TuhuPreferences(this)
        tuhuMapStore = TuhuMapStore(this, osmMapStore)
        mapSearch = MapSearchRepository(this)
        trackingState.update {
            it.copy(
                temperatureAvailable = ambientTemperatureSource.isAvailable,
                pressureAvailable = pressureSource.isAvailable
            )
        }
    }
}
