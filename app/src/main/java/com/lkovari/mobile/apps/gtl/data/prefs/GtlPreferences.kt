package com.lkovari.mobile.apps.gtl.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.lkovari.mobile.apps.gtl.engine.FixFilter
import com.lkovari.mobile.apps.gtl.engine.MeasurementSystem
import com.lkovari.mobile.apps.gtl.engine.UsageType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class GtlSettings(
    val disclaimerAccepted: Boolean,
    val usageType: UsageType,
    val measurementSystem: MeasurementSystem,
    val minDistanceMeters: Float,
    val minTimeMillis: Long,
    val minAccuracyMeters: Int,
    val minSatellites: Int,
    val useOfflineMap: Boolean,
    val selectedMapFile: String,
    val trackColorArgb: Long,
    val trackThickness: Int,
    val optimizationTolerance: Double,
    val optimizationActive: Boolean,
    val showLastTrackOnMap: Boolean,
    val showAccuracyMarker: Boolean
) {
    fun toFilter(): FixFilter {
        return FixFilter(
            minDistanceMeters = minDistanceMeters,
            minTimeMillis = minTimeMillis,
            minAccuracyMeters = minAccuracyMeters,
            minSatellites = minSatellites
        )
    }
}

class GtlPreferences(context: Context) {
    private val dataStore = PreferenceDataStoreFactory.create {
        context.applicationContext.preferencesDataStoreFile("gtl_settings")
    }

    val settings: Flow<GtlSettings> = dataStore.data.map { prefs ->
        GtlSettings(
            disclaimerAccepted = prefs[Keys.disclaimer] ?: false,
            usageType = runCatching {
                UsageType.valueOf(prefs[Keys.usage] ?: UsageType.TWO_WHEELERS.name)
            }.getOrDefault(UsageType.TWO_WHEELERS),
            measurementSystem = runCatching {
                MeasurementSystem.valueOf(prefs[Keys.units] ?: MeasurementSystem.METRIC.name)
            }.getOrDefault(MeasurementSystem.METRIC),
            minDistanceMeters = prefs[Keys.minDistance] ?: 2.5f,
            minTimeMillis = prefs[Keys.minTime] ?: 500L,
            minAccuracyMeters = prefs[Keys.minAccuracy] ?: 30,
            minSatellites = prefs[Keys.minSats] ?: 4,
            useOfflineMap = prefs[Keys.offline] ?: false,
            selectedMapFile = prefs[Keys.mapFile] ?: "",
            trackColorArgb = (prefs[Keys.trackColor] ?: 0xFFE53935.toInt()).toLong() and 0xFFFFFFFFL,
            trackThickness = prefs[Keys.trackWidth] ?: 8,
            optimizationTolerance = (prefs[Keys.tolerance] ?: 19.5f).toDouble(),
            optimizationActive = prefs[Keys.optimize] ?: true,
            showLastTrackOnMap = prefs[Keys.showTrack] ?: true,
            showAccuracyMarker = prefs[Keys.showAccuracy] ?: true
        )
    }

    suspend fun setDisclaimerAccepted(value: Boolean) {
        dataStore.edit { it[Keys.disclaimer] = value }
    }

    suspend fun setUsageType(value: UsageType) {
        val filter = value.defaultFilter()
        dataStore.edit {
            it[Keys.usage] = value.name
            it[Keys.minDistance] = filter.minDistanceMeters
            it[Keys.minTime] = filter.minTimeMillis
            it[Keys.minAccuracy] = filter.minAccuracyMeters
            it[Keys.minSats] = filter.minSatellites
        }
    }

    suspend fun setMeasurementSystem(value: MeasurementSystem) {
        dataStore.edit { it[Keys.units] = value.name }
    }

    suspend fun setMinDistance(value: Float) {
        dataStore.edit { it[Keys.minDistance] = value }
    }

    suspend fun setMinTime(value: Long) {
        dataStore.edit { it[Keys.minTime] = value }
    }

    suspend fun setMinAccuracy(value: Int) {
        dataStore.edit { it[Keys.minAccuracy] = value }
    }

    suspend fun setMinSatellites(value: Int) {
        dataStore.edit { it[Keys.minSats] = value }
    }

    suspend fun setUseOfflineMap(value: Boolean) {
        dataStore.edit { it[Keys.offline] = value }
    }

    suspend fun setSelectedMapFile(value: String) {
        dataStore.edit { it[Keys.mapFile] = value }
    }

    suspend fun setOptimizationActive(value: Boolean) {
        dataStore.edit { it[Keys.optimize] = value }
    }

    suspend fun setShowLastTrackOnMap(value: Boolean) {
        dataStore.edit { it[Keys.showTrack] = value }
    }

    suspend fun setShowAccuracyMarker(value: Boolean) {
        dataStore.edit { it[Keys.showAccuracy] = value }
    }

    private object Keys {
        val disclaimer = booleanPreferencesKey("disclaimer")
        val usage = stringPreferencesKey("usage")
        val units = stringPreferencesKey("units")
        val minDistance = floatPreferencesKey("min_distance")
        val minTime = longPreferencesKey("min_time")
        val minAccuracy = intPreferencesKey("min_accuracy")
        val minSats = intPreferencesKey("min_sats")
        val offline = booleanPreferencesKey("offline")
        val mapFile = stringPreferencesKey("map_file")
        val trackColor = intPreferencesKey("track_color")
        val trackWidth = intPreferencesKey("track_width")
        val tolerance = floatPreferencesKey("tolerance")
        val optimize = booleanPreferencesKey("optimize")
        val showTrack = booleanPreferencesKey("show_last_track")
        val showAccuracy = booleanPreferencesKey("show_accuracy_marker")
    }
}
