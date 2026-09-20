package com.lkovari.mobile.apps.gtl.tuhu

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TuhuPreferences(context: Context) {
    private val dataStore = PreferenceDataStoreFactory.create {
        context.applicationContext.preferencesDataStoreFile("tuhu_settings")
    }

    val options: Flow<TuhuRenderOptions> = dataStore.data.map { prefs ->
        val defaults = TuhuRenderOptions.defaults()
        TuhuRenderOptions(
            blazes = prefs[Keys.blazes] ?: defaults.blazes,
            paths = prefs[Keys.paths] ?: defaults.paths,
            contours = prefs[Keys.contours] ?: defaults.contours,
            contoursMinor = prefs[Keys.contoursMinor] ?: defaults.contoursMinor,
            hikePoi = prefs[Keys.hikePoi] ?: defaults.hikePoi,
            parks = prefs[Keys.parks] ?: defaults.parks,
            urbanPoi = prefs[Keys.urbanPoi] ?: defaults.urbanPoi,
            hillshading = prefs[Keys.hillshading] ?: defaults.hillshading
        )
    }

    suspend fun setBlazes(value: Boolean) {
        dataStore.edit { it[Keys.blazes] = value }
    }

    suspend fun setPaths(value: Boolean) {
        dataStore.edit { it[Keys.paths] = value }
    }

    suspend fun setContours(value: Boolean) {
        dataStore.edit { it[Keys.contours] = value }
    }

    suspend fun setContoursMinor(value: Boolean) {
        dataStore.edit { it[Keys.contoursMinor] = value }
    }

    suspend fun setHikePoi(value: Boolean) {
        dataStore.edit { it[Keys.hikePoi] = value }
    }

    suspend fun setParks(value: Boolean) {
        dataStore.edit { it[Keys.parks] = value }
    }

    suspend fun setUrbanPoi(value: Boolean) {
        dataStore.edit { it[Keys.urbanPoi] = value }
    }

    suspend fun setHillshading(value: Boolean) {
        dataStore.edit { it[Keys.hillshading] = value }
    }

    private object Keys {
        val blazes = booleanPreferencesKey("tuhu_blazes")
        val paths = booleanPreferencesKey("tuhu_paths")
        val contours = booleanPreferencesKey("tuhu_contours")
        val contoursMinor = booleanPreferencesKey("tuhu_contours_minor")
        val hikePoi = booleanPreferencesKey("tuhu_hike_poi")
        val parks = booleanPreferencesKey("tuhu_parks")
        val urbanPoi = booleanPreferencesKey("tuhu_urban_poi")
        val hillshading = booleanPreferencesKey("tuhu_hillshading")
    }
}
