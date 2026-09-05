package com.lkovari.mobile.apps.gtl.viewmodel

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lkovari.mobile.apps.gtl.GtlApplication
import com.lkovari.mobile.apps.gtl.data.db.GpsEventEntity
import com.lkovari.mobile.apps.gtl.data.db.TrackSessionEntity
import com.lkovari.mobile.apps.gtl.data.maps.OsmCatalog
import com.lkovari.mobile.apps.gtl.data.maps.OsmDownloadState
import com.lkovari.mobile.apps.gtl.data.maps.OsmRegion
import com.lkovari.mobile.apps.gtl.data.prefs.GtlSettings
import com.lkovari.mobile.apps.gtl.domain.KmlExportUseCase
import com.lkovari.mobile.apps.gtl.engine.DouglasPeucker
import com.lkovari.mobile.apps.gtl.engine.GeoPoint
import com.lkovari.mobile.apps.gtl.engine.MeasurementSystem
import com.lkovari.mobile.apps.gtl.engine.TrackStats
import com.lkovari.mobile.apps.gtl.engine.TrackStatsCalculator
import com.lkovari.mobile.apps.gtl.engine.UsageType
import com.lkovari.mobile.apps.gtl.service.LiveTrackingState
import com.lkovari.mobile.apps.gtl.service.TrackingForegroundService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class GtlUiState(
    val settings: GtlSettings,
    val live: LiveTrackingState,
    val events: List<GpsEventEntity>,
    val stats: TrackStats,
    val displayPoints: List<GeoPoint>,
    val sessions: List<TrackSessionEntity>,
    val mapsKeyPresent: Boolean,
    val osmFile: File?,
    val requestedTab: Int?
)

@OptIn(ExperimentalCoroutinesApi::class)
class GtlViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as GtlApplication
    private val exporter = KmlExportUseCase(application)
    private val selectedSessionId = MutableStateFlow<Long?>(null)
    private val requestedTab = MutableStateFlow<Int?>(null)
    private var gnssJob: Job? = null
    private var locationJob: Job? = null
    private var compassJob: Job? = null
    private var temperatureJob: Job? = null

    val settings: StateFlow<GtlSettings> = app.preferences.settings.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        GtlSettings(
            disclaimerAccepted = false,
            usageType = UsageType.TWO_WHEELERS,
            measurementSystem = MeasurementSystem.METRIC,
            minDistanceMeters = 2.5f,
            minTimeMillis = 500L,
            minAccuracyMeters = 30,
            minSatellites = 4,
            useOfflineMap = false,
            selectedMapFile = "",
            trackColorArgb = 0xFFE53935L,
            trackThickness = 8,
            optimizationTolerance = 19.5,
            optimizationActive = true,
            showLastTrackOnMap = true,
            showAccuracyMarker = true
        )
    )

    val live: StateFlow<LiveTrackingState> = app.trackingState.state

    init {
        startPreview()
    }

    fun startPreview() {
        listenGnss()
        listenLocation()
        listenCompass()
        listenTemperature()
    }

    private fun listenGnss() {
        gnssJob?.cancel()
        gnssJob = viewModelScope.launch {
            app.gnssStatusSource.snapshots().collectLatest { snapshot ->
                app.trackingState.update { it.copy(gnss = snapshot) }
            }
        }
    }

    private fun listenLocation() {
        locationJob?.cancel()
        locationJob = viewModelScope.launch {
            val client = com.lkovari.mobile.apps.gtl.data.location.LocationClient(app)
            client.locations(1000L, 0f).collectLatest { location ->
                app.trackingState.update {
                    it.copy(lastLocation = location, provider = location.provider)
                }
            }
        }
    }

    private fun listenCompass() {
        compassJob?.cancel()
        compassJob = viewModelScope.launch {
            app.compassSource.azimuthDegrees().collectLatest { value ->
                app.trackingState.update { it.copy(azimuthDegrees = value) }
            }
        }
    }

    private fun listenTemperature() {
        temperatureJob?.cancel()
        temperatureJob = viewModelScope.launch {
            app.trackingState.update {
                it.copy(temperatureAvailable = app.ambientTemperatureSource.isAvailable)
            }
            app.ambientTemperatureSource.temperatures().collectLatest { value ->
                app.trackingState.update { it.copy(temperatureCelsius = value, temperatureAvailable = true) }
            }
        }
    }

    val sessions: StateFlow<List<TrackSessionEntity>> = app.trackRepository.observeSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val activeEvents = combine(live, selectedSessionId, settings, sessions) { liveState, selected, prefs, sessionList ->
        liveState.sessionId
            ?: selected
            ?: if (prefs.showLastTrackOnMap) sessionList.firstOrNull()?.id else null
    }.flatMapLatest { sessionId ->
        if (sessionId == null) {
            flowOf(emptyList())
        } else {
            app.trackRepository.observeEvents(sessionId)
        }
    }

    val uiState: StateFlow<GtlUiState> = combine(
        settings,
        live,
        activeEvents,
        sessions,
        requestedTab
    ) { prefs, liveState, events, sessionList, tab ->
        val samples = app.trackRepository.toSamples(events)
        val routeStats = if (liveState.logging) {
            TrackStatsCalculator.compute(samples)
        } else {
            TrackStatsCalculator.compute(emptyList())
        }
        val points = events.map { GeoPoint(it.latitude, it.longitude, it.altitude) }
        val display = if (prefs.optimizationActive && points.size > 4) {
            DouglasPeucker.simplify(points, prefs.optimizationTolerance)
        } else {
            points
        }
        val mapPoints = if (liveState.logging || prefs.showLastTrackOnMap) display else emptyList()
        val osm = if (prefs.selectedMapFile.isNotBlank()) {
            File(prefs.selectedMapFile).takeIf { it.exists() }
        } else {
            null
        }
        GtlUiState(
            settings = prefs,
            live = liveState,
            events = events,
            stats = routeStats,
            displayPoints = mapPoints,
            sessions = sessionList,
            mapsKeyPresent = com.lkovari.mobile.apps.gtl.BuildConfig.MAPS_API_KEY.isNotBlank(),
            osmFile = osm,
            requestedTab = tab
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        GtlUiState(
            settings = settings.value,
            live = live.value,
            events = emptyList(),
            stats = TrackStatsCalculator.compute(emptyList()),
            displayPoints = emptyList(),
            sessions = emptyList(),
            mapsKeyPresent = com.lkovari.mobile.apps.gtl.BuildConfig.MAPS_API_KEY.isNotBlank(),
            osmFile = null,
            requestedTab = null
        )
    )

    fun acceptDisclaimer() {
        viewModelScope.launch { app.preferences.setDisclaimerAccepted(true) }
    }

    fun startLogging() {
        startPreview()
        val intent = Intent(app, TrackingForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            app.startForegroundService(intent)
        } else {
            app.startService(intent)
        }
    }

    fun stopLogging() {
        val intent = Intent(app, TrackingForegroundService::class.java).setAction(TrackingForegroundService.ACTION_STOP)
        app.startService(intent)
    }

    fun selectSession(id: Long) {
        selectedSessionId.value = id
    }

    fun showSessionOnMap(id: Long) {
        selectedSessionId.value = id
        requestedTab.value = 2
    }

    fun consumeRequestedTab() {
        requestedTab.value = null
    }

    fun deleteSession(id: Long) {
        viewModelScope.launch { app.trackRepository.deleteSession(id) }
    }

    fun setUsage(value: UsageType) {
        viewModelScope.launch { app.preferences.setUsageType(value) }
    }

    fun setUnits(value: MeasurementSystem) {
        viewModelScope.launch { app.preferences.setMeasurementSystem(value) }
    }

    fun setMinDistance(value: Float) {
        viewModelScope.launch { app.preferences.setMinDistance(value) }
    }

    fun setMinTime(value: Long) {
        viewModelScope.launch { app.preferences.setMinTime(value) }
    }

    fun setMinAccuracy(value: Int) {
        viewModelScope.launch { app.preferences.setMinAccuracy(value) }
    }

    fun setMinSatellites(value: Int) {
        viewModelScope.launch { app.preferences.setMinSatellites(value) }
    }

    fun setUseOfflineMap(value: Boolean) {
        viewModelScope.launch { app.preferences.setUseOfflineMap(value) }
    }

    fun setOptimization(value: Boolean) {
        viewModelScope.launch { app.preferences.setOptimizationActive(value) }
    }

    fun setShowLastTrackOnMap(value: Boolean) {
        viewModelScope.launch { app.preferences.setShowLastTrackOnMap(value) }
    }

    fun setShowAccuracyMarker(value: Boolean) {
        viewModelScope.launch { app.preferences.setShowAccuracyMarker(value) }
    }

    fun downloadRegion(region: OsmRegion) {
        app.osmMapStore.enqueue(region)
    }

    fun observeDownload(regionId: String): kotlinx.coroutines.flow.Flow<OsmDownloadState> {
        return app.osmMapStore.observe(regionId)
    }

    fun selectDownloadedMap(region: OsmRegion) {
        val file = app.osmMapStore.downloadedFile(region.id) ?: return
        viewModelScope.launch {
            app.preferences.setSelectedMapFile(file.absolutePath)
            app.preferences.setUseOfflineMap(true)
        }
    }

    fun regions(): List<OsmRegion> = OsmCatalog.regions

    fun isDownloaded(region: OsmRegion): Boolean = app.osmMapStore.downloadedFile(region.id) != null

    fun shareLatestKml(): Intent? {
        val session = uiState.value.live.sessionId?.let { id ->
            uiState.value.sessions.firstOrNull { it.id == id }
        } ?: uiState.value.sessions.firstOrNull() ?: return null
        val events = uiState.value.events
        if (events.isEmpty()) {
            return null
        }
        val file = exporter.write(session, events)
        val uri = FileProvider.getUriForFile(app, "${app.packageName}.files", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.google-earth.kml+xml"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
