package com.lkovari.mobile.apps.gtl.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.lkovari.mobile.apps.gtl.engine.DouglasPeucker
import com.lkovari.mobile.apps.gtl.engine.FixFilter
import com.lkovari.mobile.apps.gtl.engine.MeasurementSystem
import com.lkovari.mobile.apps.gtl.engine.RecordingDensity
import com.lkovari.mobile.apps.gtl.engine.SmoothingStrength
import com.lkovari.mobile.apps.gtl.engine.UsageSmoothingDefaults
import com.lkovari.mobile.apps.gtl.engine.UsageType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
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
    val keepWholeTrackOnScreen: Boolean,
    val showAccuracyMarker: Boolean,
    val showFixCloud: Boolean,
    val keepScreenOnWhileLogging: Boolean,
    val trackSmoothingEnabled: Boolean,
    val smoothingStrengthValue: Float,
    val stationaryLockEnabled: Boolean,
    val recordingDensityValue: Float,
    val gnssOnly: Boolean,
    val compassTrueNorth: Boolean
) {
    fun toFilter(): FixFilter {
        return FixFilter(
            minDistanceMeters = minDistanceMeters,
            minTimeMillis = minTimeMillis,
            minAccuracyMeters = minAccuracyMeters,
            minSatellites = minSatellites
        )
    }

    companion object {
        fun placeholder(): GtlSettings {
            val smoothing = UsageType.TWO_WHEELERS.defaultSmoothing()
            return GtlSettings(
                disclaimerAccepted = false,
                usageType = UsageType.TWO_WHEELERS,
                measurementSystem = MeasurementSystem.METRIC,
                minDistanceMeters = 2f,
                minTimeMillis = 500L,
                minAccuracyMeters = 30,
                minSatellites = 4,
                useOfflineMap = false,
                selectedMapFile = "",
                trackColorArgb = 0xFFE53935L,
                trackThickness = 8,
                optimizationTolerance = smoothing.optimizationToleranceMeters,
                optimizationActive = smoothing.optimizationActive,
                showLastTrackOnMap = true,
                keepWholeTrackOnScreen = false,
                showAccuracyMarker = true,
                showFixCloud = false,
                keepScreenOnWhileLogging = false,
                trackSmoothingEnabled = smoothing.trackSmoothingEnabled,
                smoothingStrengthValue = smoothing.smoothingStrength.sliderValue(),
                stationaryLockEnabled = smoothing.stationaryLockEnabled,
                recordingDensityValue = smoothing.recordingDensity.sliderValue(),
                gnssOnly = smoothing.gnssOnly,
                compassTrueNorth = false
            )
        }
    }
}

class GtlPreferences(context: Context) {
    private val dataStore = PreferenceDataStoreFactory.create {
        context.applicationContext.preferencesDataStoreFile("gtl_settings")
    }

    val settings: Flow<GtlSettings> = flow {
        migrateSmoothingIfNeeded()
        emitAll(dataStore.data.map { prefs -> mapSettings(prefs) })
    }

    suspend fun setDisclaimerAccepted(value: Boolean) {
        dataStore.edit { it[Keys.disclaimer] = value }
    }

    suspend fun setUsageType(value: UsageType) {
        val filter = value.defaultFilter()
        val smoothing = value.defaultSmoothing()
        dataStore.edit {
            it[Keys.usage] = value.name
            it[Keys.minDistance] = filter.minDistanceMeters
            it[Keys.minTime] = filter.minTimeMillis
            it[Keys.minAccuracy] = filter.minAccuracyMeters
            it[Keys.minSats] = filter.minSatellites
            it[Keys.units] = value.defaultMeasurementSystem().name
            writeSmoothing(it, smoothing, includeMapSimplify = true)
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

    suspend fun setOptimizationTolerance(value: Double) {
        val clamped = DouglasPeucker.clampTolerance(value)
        dataStore.edit { it[Keys.tolerance] = clamped.toFloat() }
    }

    suspend fun setShowLastTrackOnMap(value: Boolean) {
        dataStore.edit { it[Keys.showTrack] = value }
    }

    suspend fun setKeepWholeTrackOnScreen(value: Boolean) {
        dataStore.edit { it[Keys.keepWholeTrack] = value }
    }

    suspend fun setShowAccuracyMarker(value: Boolean) {
        dataStore.edit { it[Keys.showAccuracy] = value }
    }

    suspend fun setShowFixCloud(value: Boolean) {
        dataStore.edit {
            it[Keys.showFixCloud] = value
            if (value) {
                it[Keys.showAccuracy] = true
            }
        }
    }

    suspend fun setKeepScreenOnWhileLogging(value: Boolean) {
        dataStore.edit { it[Keys.keepScreenOn] = value }
    }

    suspend fun setTrackSmoothingEnabled(value: Boolean) {
        dataStore.edit { it[Keys.trackSmoothing] = value }
    }

    suspend fun setSmoothingStrength(value: Float) {
        dataStore.edit { it[Keys.smoothingStrengthValue] = value.coerceIn(0f, 1f) }
    }

    suspend fun setStationaryLockEnabled(value: Boolean) {
        dataStore.edit { it[Keys.stationaryLock] = value }
    }

    suspend fun setRecordingDensity(value: Float) {
        dataStore.edit { it[Keys.recordingDensityValue] = value.coerceIn(0f, 1f) }
    }

    suspend fun setGnssOnly(value: Boolean) {
        dataStore.edit { it[Keys.gnssOnly] = value }
    }

    suspend fun setCompassTrueNorth(value: Boolean) {
        dataStore.edit { it[Keys.compassTrueNorth] = value }
    }

    private suspend fun migrateSmoothingIfNeeded() {
        dataStore.edit { prefs ->
            if (prefs.contains(Keys.trackSmoothing)) {
                return@edit
            }
            val usage = parseUsage(prefs[Keys.usage])
            val smoothing = usage.defaultSmoothing()
            writeSmoothing(prefs, smoothing, includeMapSimplify = false)
            val rawTol = prefs[Keys.tolerance]
            if (rawTol == null || rawTol == LegacyToleranceSentinel) {
                prefs[Keys.tolerance] = smoothing.optimizationToleranceMeters.toFloat()
                prefs[Keys.optimize] = smoothing.optimizationActive
            }
        }
    }

    private fun writeSmoothing(
        prefs: MutablePreferences,
        smoothing: UsageSmoothingDefaults,
        includeMapSimplify: Boolean
    ) {
        prefs[Keys.trackSmoothing] = smoothing.trackSmoothingEnabled
        prefs[Keys.smoothingStrengthValue] = smoothing.smoothingStrength.sliderValue()
        prefs[Keys.stationaryLock] = smoothing.stationaryLockEnabled
        prefs[Keys.recordingDensityValue] = smoothing.recordingDensity.sliderValue()
        prefs[Keys.gnssOnly] = smoothing.gnssOnly
        if (includeMapSimplify) {
            prefs[Keys.tolerance] = smoothing.optimizationToleranceMeters.toFloat()
            prefs[Keys.optimize] = smoothing.optimizationActive
        }
    }

    private fun mapSettings(prefs: Preferences): GtlSettings {
        val usage = parseUsage(prefs[Keys.usage])
        val smoothing = usage.defaultSmoothing()
        val hasSmoothing = prefs.contains(Keys.trackSmoothing)
        val toleranceSource = prefs[Keys.tolerance]
        val tolerance = if (!hasSmoothing && (toleranceSource == null || toleranceSource == LegacyToleranceSentinel)) {
            smoothing.optimizationToleranceMeters
        } else {
            DouglasPeucker.clampTolerance(
                (toleranceSource ?: smoothing.optimizationToleranceMeters.toFloat()).toDouble()
            )
        }
        val optimizeDefault = if (hasSmoothing) true else smoothing.optimizationActive
        return GtlSettings(
            disclaimerAccepted = prefs[Keys.disclaimer] ?: false,
            usageType = usage,
            measurementSystem = runCatching {
                MeasurementSystem.valueOf(prefs[Keys.units] ?: MeasurementSystem.METRIC.name)
            }.getOrDefault(MeasurementSystem.METRIC),
            minDistanceMeters = prefs[Keys.minDistance] ?: 2f,
            minTimeMillis = prefs[Keys.minTime] ?: 500L,
            minAccuracyMeters = prefs[Keys.minAccuracy] ?: 30,
            minSatellites = prefs[Keys.minSats] ?: 4,
            useOfflineMap = prefs[Keys.offline] ?: false,
            selectedMapFile = prefs[Keys.mapFile] ?: "",
            trackColorArgb = (prefs[Keys.trackColor] ?: 0xFFE53935.toInt()).toLong() and 0xFFFFFFFFL,
            trackThickness = prefs[Keys.trackWidth] ?: 8,
            optimizationTolerance = DouglasPeucker.clampTolerance(tolerance),
            optimizationActive = prefs[Keys.optimize] ?: optimizeDefault,
            showLastTrackOnMap = prefs[Keys.showTrack] ?: true,
            keepWholeTrackOnScreen = prefs[Keys.keepWholeTrack] ?: false,
            showAccuracyMarker = prefs[Keys.showAccuracy] ?: true,
            showFixCloud = prefs[Keys.showFixCloud] ?: false,
            keepScreenOnWhileLogging = prefs[Keys.keepScreenOn] ?: false,
            trackSmoothingEnabled = prefs[Keys.trackSmoothing] ?: smoothing.trackSmoothingEnabled,
            smoothingStrengthValue = readSmoothingStrength(prefs, smoothing),
            stationaryLockEnabled = prefs[Keys.stationaryLock] ?: smoothing.stationaryLockEnabled,
            recordingDensityValue = readRecordingDensity(prefs, smoothing),
            gnssOnly = prefs[Keys.gnssOnly] ?: smoothing.gnssOnly,
            compassTrueNorth = prefs[Keys.compassTrueNorth] ?: false
        )
    }

    private fun readSmoothingStrength(prefs: Preferences, smoothing: UsageSmoothingDefaults): Float {
        val stored = prefs[Keys.smoothingStrengthValue]
        if (stored != null) {
            return stored.coerceIn(0f, 1f)
        }
        val named = prefs[Keys.smoothingStrength]
        if (named != null) {
            return runCatching {
                SmoothingStrength.valueOf(named).sliderValue()
            }.getOrDefault(smoothing.smoothingStrength.sliderValue())
        }
        return smoothing.smoothingStrength.sliderValue()
    }

    private fun readRecordingDensity(prefs: Preferences, smoothing: UsageSmoothingDefaults): Float {
        val stored = prefs[Keys.recordingDensityValue]
        if (stored != null) {
            return stored.coerceIn(0f, 1f)
        }
        val named = prefs[Keys.recordingDensity]
        if (named != null) {
            return runCatching {
                RecordingDensity.valueOf(named).sliderValue()
            }.getOrDefault(smoothing.recordingDensity.sliderValue())
        }
        return smoothing.recordingDensity.sliderValue()
    }

    private fun parseUsage(raw: String?): UsageType {
        return runCatching {
            UsageType.valueOf(raw ?: UsageType.TWO_WHEELERS.name)
        }.getOrDefault(UsageType.TWO_WHEELERS)
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
        val keepWholeTrack = booleanPreferencesKey("keep_whole_track")
        val showAccuracy = booleanPreferencesKey("show_accuracy_marker")
        val showFixCloud = booleanPreferencesKey("show_fix_cloud")
        val keepScreenOn = booleanPreferencesKey("keep_screen_on_logging")
        val trackSmoothing = booleanPreferencesKey("track_smoothing")
        val smoothingStrength = stringPreferencesKey("smoothing_strength")
        val smoothingStrengthValue = floatPreferencesKey("smoothing_strength_value")
        val stationaryLock = booleanPreferencesKey("stationary_lock")
        val recordingDensity = stringPreferencesKey("recording_density")
        val recordingDensityValue = floatPreferencesKey("recording_density_value")
        val gnssOnly = booleanPreferencesKey("gnss_only")
        val compassTrueNorth = booleanPreferencesKey("compass_true_north")
    }

    companion object {
        private const val LegacyToleranceSentinel = 19.5f
    }
}
