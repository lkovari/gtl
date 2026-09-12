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
import com.lkovari.mobile.apps.gtl.data.prefs.GoogleMapLayer
import com.lkovari.mobile.apps.gtl.data.prefs.GtlSettings
import com.lkovari.mobile.apps.gtl.domain.GpxExportUseCase
import com.lkovari.mobile.apps.gtl.domain.KmlExportUseCase
import com.lkovari.mobile.apps.gtl.domain.TrackShareFormat
import com.lkovari.mobile.apps.gtl.engine.BaroAltitude
import com.lkovari.mobile.apps.gtl.engine.BikeLeanAngle
import com.lkovari.mobile.apps.gtl.engine.DouglasPeucker
import com.lkovari.mobile.apps.gtl.engine.ElevationPoint
import com.lkovari.mobile.apps.gtl.engine.ElevationSample
import com.lkovari.mobile.apps.gtl.engine.ElevationSeries
import com.lkovari.mobile.apps.gtl.engine.FixCloudBuffer
import com.lkovari.mobile.apps.gtl.engine.FixCloudSample
import com.lkovari.mobile.apps.gtl.engine.FixCloudSnapshot
import com.lkovari.mobile.apps.gtl.engine.GeoPoint
import com.lkovari.mobile.apps.gtl.engine.MapDisplayUsage
import com.lkovari.mobile.apps.gtl.engine.MapTrackVisibility
import com.lkovari.mobile.apps.gtl.engine.MeasurementSystem
import com.lkovari.mobile.apps.gtl.engine.OsmMapFile
import com.lkovari.mobile.apps.gtl.engine.TrackInspectDump
import com.lkovari.mobile.apps.gtl.engine.TrackInspectEvent
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

private data class MapUiBits(
    val requestedTab: Int?,
    val selectedSessionId: Long?,
    val fixCloud: FixCloudSnapshot,
    val mainTab: Int,
    val mapCleared: Boolean
)

data class GtlUiState(
    val settings: GtlSettings,
    val live: LiveTrackingState,
    val events: List<GpsEventEntity>,
    val stats: TrackStats,
    val displayPoints: List<GeoPoint>,
    val sessions: List<TrackSessionEntity>,
    val mapsKeyPresent: Boolean,
    val osmFile: File?,
    val requestedTab: Int?,
    val selectedSessionId: Long?,
    val fixCloud: FixCloudSnapshot,
    val mapUsageType: UsageType,
    val mainTab: Int
)

@OptIn(ExperimentalCoroutinesApi::class)
class GtlViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as GtlApplication
    private val exporter = KmlExportUseCase(application)
    private val gpxExporter = GpxExportUseCase(application)
    private val selectedSessionId = MutableStateFlow<Long?>(null)
    private val requestedTab = MutableStateFlow<Int?>(null)
    private val mainTab = MutableStateFlow(0)
    private val mapCleared = MutableStateFlow(false)
    private var gnssJob: Job? = null
    private var locationJob: Job? = null
    private var compassJob: Job? = null
    private var temperatureJob: Job? = null
    private var gravityJob: Job? = null
    private var pressureJob: Job? = null
    private val fixCloudBuffer = FixCloudBuffer()
    private val fixCloudView = MutableStateFlow(FixCloudSnapshot.Empty)
    private val savedElevationSessionId = MutableStateFlow<Long?>(null)
    private val savedElevationSamples = MutableStateFlow<List<ElevationSample>>(emptyList())
    private var lastObservedNanos = Long.MIN_VALUE
    private var lastGnssOnly: Boolean? = null

    val settings: StateFlow<GtlSettings> = app.preferences.settings.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        GtlSettings.placeholder()
    )

    val live: StateFlow<LiveTrackingState> = app.trackingState.state

    val savedElevationId: StateFlow<Long?> = savedElevationSessionId
    val savedElevation: StateFlow<List<ElevationSample>> = savedElevationSamples
    private val inspectDumpText = MutableStateFlow<String?>(null)
    val inspectDump: StateFlow<String?> = inspectDumpText

    val sessions: StateFlow<List<TrackSessionEntity>> = app.trackRepository.observeSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        startPreview()
        observeFixCloud()
        observeSavedElevationQnh()
    }

    fun startPreview() {
        listenGnss()
        listenLocation()
        listenCompass()
        listenTemperature()
        listenGravity()
        listenPressure()
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
            settings.map { it.gnssOnly }.distinctUntilChanged().collectLatest { gnssOnly ->
                val client = com.lkovari.mobile.apps.gtl.data.location.LocationClient(app)
                client.locations(1000L, 0f, gnssOnly).collect { location ->
                    app.trackingState.update {
                        it.copy(lastLocation = location, provider = location.provider)
                    }
                }
            }
        }
    }

    private fun listenCompass() {
        compassJob?.cancel()
        compassJob = viewModelScope.launch {
            app.compassSource.samples().collectLatest { sample ->
                app.trackingState.update {
                    it.copy(
                        azimuthDegrees = sample.azimuthDegrees,
                        compassAccuracy = sample.accuracy
                    )
                }
            }
        }
    }

    private fun listenGravity() {
        gravityJob?.cancel()
        gravityJob = viewModelScope.launch {
            app.gravitySource.gravity().collectLatest { value ->
                app.trackingState.update {
                    it.copy(leanAngle = BikeLeanAngle.fromGravity(value[0], value[1], value[2]))
                }
            }
        }
    }

    private fun listenPressure() {
        pressureJob?.cancel()
        pressureJob = viewModelScope.launch {
            settings.map { it.qnhHpa }.distinctUntilChanged().collectLatest { qnh ->
                app.trackingState.update {
                    it.copy(pressureAvailable = app.pressureSource.isAvailable)
                }
                app.pressureSource.pressures().collect { value ->
                    app.trackingState.update {
                        it.copy(
                            pressureHpa = value,
                            baroAltitude = BaroAltitude.metersFromPressureHpa(value, qnh),
                            pressureAvailable = true
                        )
                    }
                }
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

    private fun observeFixCloud() {
        viewModelScope.launch {
            combine(
                settings.map { Triple(it.showFixCloud, it.gnssOnly, it.usageType.pauseSpeedMps()) }
                    .distinctUntilChanged(),
                live
            ) { cloudPrefs, liveState ->
                cloudPrefs to liveState.lastLocation
            }.collect { (cloudPrefs, location) ->
                val enabled = cloudPrefs.first
                val gnssOnly = cloudPrefs.second
                val pauseSpeed = cloudPrefs.third
                if (lastGnssOnly != null && lastGnssOnly != gnssOnly) {
                    fixCloudBuffer.clear()
                    lastObservedNanos = location?.elapsedRealtimeNanos ?: Long.MIN_VALUE
                    lastGnssOnly = gnssOnly
                    fixCloudView.value = if (enabled) {
                        fixCloudBuffer.snapshot()
                    } else {
                        FixCloudSnapshot.Empty
                    }
                    return@collect
                }
                lastGnssOnly = gnssOnly
                if (!enabled) {
                    fixCloudBuffer.clear()
                    lastObservedNanos = Long.MIN_VALUE
                    if (fixCloudView.value != FixCloudSnapshot.Empty) {
                        fixCloudView.value = FixCloudSnapshot.Empty
                    }
                    return@collect
                }
                if (location == null) {
                    return@collect
                }
                val nanos = location.elapsedRealtimeNanos
                if (nanos != 0L && nanos == lastObservedNanos) {
                    return@collect
                }
                lastObservedNanos = nanos
                val speed = if (location.hasSpeed()) location.speed else 0f
                val accuracy = if (location.hasAccuracy()) location.accuracy else 0f
                fixCloudBuffer.observe(
                    FixCloudSample(
                        timeMillis = location.time,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracyMeters = accuracy,
                        speedMps = speed
                    ),
                    pauseSpeed
                )
                fixCloudView.value = fixCloudBuffer.snapshot()
            }
        }
    }

    private fun observeSavedElevationQnh() {
        viewModelScope.launch {
            settings.map { it.qnhHpa }.distinctUntilChanged().collectLatest { qnh ->
                val id = savedElevationSessionId.value ?: return@collectLatest
                val events = app.trackRepository.eventsFor(id)
                savedElevationSamples.value = elevationSamplesOf(events, qnh)
            }
        }
    }

    private val activeEvents = combine(live, selectedSessionId, settings, sessions, mapCleared) { liveState, selected, prefs, sessionList, cleared ->
        liveState.sessionId
            ?: selected
            ?: if (!cleared && prefs.showLastTrackOnMap) sessionList.firstOrNull()?.id else null
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
        combine(requestedTab, selectedSessionId, fixCloudView, mainTab, mapCleared) { tab, selected, cloud, persistedTab, cleared ->
            MapUiBits(tab, selected, cloud, persistedTab, cleared)
        }
    ) { prefs, liveState, events, sessionList, mapBits ->
        val tab = mapBits.requestedTab
        val selected = mapBits.selectedSessionId
        val cloud = mapBits.fixCloud
        val persistedTab = mapBits.mainTab
        val cleared = mapBits.mapCleared
        val followSettings = MapDisplayUsage.followsSettings(liveState.logging, selected)
        val samples = app.trackRepository.toSamples(events)
        val routeStats = TrackStatsCalculator.compute(samples)
        val viewingId = liveState.sessionId
            ?: selected
            ?: if (!cleared && prefs.showLastTrackOnMap) sessionList.firstOrNull()?.id else null
        val viewed = sessionList.find { it.id == viewingId }
        val mapUsage = MapDisplayUsage.of(
            logging = liveState.logging,
            followSettings = followSettings,
            settingsUsage = prefs.usageType,
            sessionUsageName = viewed?.usageType
        )
        val simplify = MapDisplayUsage.simplify(
            usage = mapUsage,
            logging = liveState.logging,
            followSettings = followSettings,
            settingsActive = prefs.optimizationActive,
            settingsTolerance = prefs.optimizationTolerance
        )
        val points = events.map { GeoPoint(it.latitude, it.longitude, it.altitude) }
        val display = if (simplify.first && points.size > 4) {
            DouglasPeucker.simplify(points, DouglasPeucker.clampTolerance(simplify.second))
        } else {
            points
        }
        val mapPoints = if (MapTrackVisibility.visible(liveState.logging, prefs.showLastTrackOnMap, selected, cleared)) {
            display
        } else {
            emptyList()
        }
        val osm = if (prefs.selectedMapFile.isNotBlank()) {
            File(prefs.selectedMapFile).takeIf { OsmMapFile.isReadable(it) }
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
            requestedTab = tab,
            selectedSessionId = selected,
            fixCloud = cloud,
            mapUsageType = mapUsage,
            mainTab = persistedTab
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
            requestedTab = null,
            selectedSessionId = null,
            fixCloud = FixCloudSnapshot.Empty,
            mapUsageType = settings.value.usageType,
            mainTab = 0
        )
    )

    fun acceptDisclaimer() {
        viewModelScope.launch { app.preferences.setDisclaimerAccepted(true) }
    }

    fun startLogging() {
        selectedSessionId.value = null
        mapCleared.value = false
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
        viewModelScope.launch {
            val session = app.trackRepository.sessionById(id)
            val usage = session?.usageType?.let { raw ->
                runCatching { UsageType.valueOf(raw) }.getOrNull()
            }
            if (usage != null) {
                app.preferences.setUsageType(usage)
            }
            mapCleared.value = false
            selectedSessionId.value = id
            mainTab.value = 2
            requestedTab.value = 2
        }
    }

    fun consumeRequestedTab() {
        requestedTab.value = null
    }

    fun clearShownTrack() {
        selectedSessionId.value = null
        mapCleared.value = true
    }

    fun deleteSession(id: Long) {
        viewModelScope.launch {
            app.trackRepository.deleteSession(id)
            if (savedElevationSessionId.value == id) {
                savedElevationSessionId.value = null
                savedElevationSamples.value = emptyList()
            }
            if (selectedSessionId.value == id) {
                selectedSessionId.value = null
                mapCleared.value = true
            }
            inspectDumpText.value = null
        }
    }

    fun inspectSession(id: Long) {
        viewModelScope.launch {
            val session = app.trackRepository.sessionById(id) ?: return@launch
            val events = app.trackRepository.eventsFor(id)
            inspectDumpText.value = TrackInspectDump.format(
                sessionId = session.id,
                startedAt = session.startedAt,
                stoppedAt = session.stoppedAt,
                usageType = session.usageType,
                measurementSystem = session.measurementSystem,
                events = events.map { event ->
                    TrackInspectEvent(
                        id = event.id,
                        timestampMillis = event.timestamp,
                        latitude = event.latitude,
                        longitude = event.longitude,
                        altitude = event.altitude,
                        speedMps = event.speed,
                        bearing = event.bearing,
                        accuracy = event.accuracy,
                        satellitesInFix = event.satellitesInFix,
                        ambientTemperature = event.ambientTemperature,
                        accelX = event.accelX,
                        accelY = event.accelY,
                        accelZ = event.accelZ,
                        leanAngle = event.leanAngle,
                        usageType = event.usageType,
                        isPlacemark = event.isPlacemark,
                        eventKind = event.eventKind,
                        baroAltitude = event.baroAltitude,
                        pressureHpa = event.pressureHpa
                    )
                }
            )
        }
    }

    fun closeInspect() {
        inspectDumpText.value = null
    }

    fun toggleSavedElevation(sessionId: Long) {
        viewModelScope.launch {
            if (savedElevationSessionId.value == sessionId) {
                savedElevationSessionId.value = null
                savedElevationSamples.value = emptyList()
                return@launch
            }
            val events = app.trackRepository.eventsFor(sessionId)
            savedElevationSessionId.value = sessionId
            savedElevationSamples.value = elevationSamplesOf(events, settings.value.qnhHpa)
        }
    }

    fun setMainTab(index: Int) {
        mainTab.value = index
    }

    fun setUsage(value: UsageType) {
        viewModelScope.launch {
            app.preferences.setUsageType(value)
        }
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

    fun setOptimizationTolerance(value: Double) {
        viewModelScope.launch { app.preferences.setOptimizationTolerance(value) }
    }

    fun setShowLastTrackOnMap(value: Boolean) {
        viewModelScope.launch { app.preferences.setShowLastTrackOnMap(value) }
    }

    fun setKeepWholeTrackOnScreen(value: Boolean) {
        viewModelScope.launch { app.preferences.setKeepWholeTrackOnScreen(value) }
    }

    fun setKeepScreenOnWhileLogging(value: Boolean) {
        viewModelScope.launch { app.preferences.setKeepScreenOnWhileLogging(value) }
    }

    fun setShowAccuracyMarker(value: Boolean) {
        viewModelScope.launch { app.preferences.setShowAccuracyMarker(value) }
    }

    fun setShowFixCloud(value: Boolean) {
        viewModelScope.launch { app.preferences.setShowFixCloud(value) }
    }

    fun setTrackSmoothing(value: Boolean) {
        viewModelScope.launch { app.preferences.setTrackSmoothingEnabled(value) }
    }

    fun setSmoothingStrength(value: Float) {
        viewModelScope.launch { app.preferences.setSmoothingStrength(value) }
    }

    fun setStationaryLock(value: Boolean) {
        viewModelScope.launch { app.preferences.setStationaryLockEnabled(value) }
    }

    fun setRecordingDensity(value: Float) {
        viewModelScope.launch { app.preferences.setRecordingDensity(value) }
    }

    fun setGnssOnly(value: Boolean) {
        viewModelScope.launch { app.preferences.setGnssOnly(value) }
    }

    fun setCompassTrueNorth(value: Boolean) {
        viewModelScope.launch { app.preferences.setCompassTrueNorth(value) }
    }

    fun setQnhHpa(value: Float) {
        viewModelScope.launch { app.preferences.setQnhHpa(value) }
    }

    fun setGoogleMapLayer(value: GoogleMapLayer) {
        viewModelScope.launch { app.preferences.setGoogleMapLayer(value) }
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

    fun onOsmMapFailed() {
        viewModelScope.launch {
            app.preferences.setUseOfflineMap(false)
        }
    }

    fun regions(): List<OsmRegion> = OsmCatalog.regions

    fun isDownloaded(region: OsmRegion): Boolean = app.osmMapStore.downloadedFile(region.id) != null

    fun shareSessions(sessionIds: Collection<Long>, format: TrackShareFormat, onReady: (Intent) -> Unit) {
        if (sessionIds.isEmpty()) {
            return
        }
        viewModelScope.launch {
            val chosen = uiState.value.sessions.filter { it.id in sessionIds }
            val items = chosen.map { session ->
                session to app.trackRepository.eventsFor(session.id)
            }.filter { it.second.isNotEmpty() }
            if (items.isEmpty()) {
                return@launch
            }
            val file = when (format) {
                TrackShareFormat.KMZ -> exporter.write(items)
                TrackShareFormat.GPX -> gpxExporter.write(items)
            }
            val mime = when (format) {
                TrackShareFormat.KMZ -> "application/vnd.google-earth.kmz"
                TrackShareFormat.GPX -> "application/gpx+xml"
            }
            val uri = FileProvider.getUriForFile(app, "${app.packageName}.files", file)
            onReady(
                Intent(Intent.ACTION_SEND).apply {
                    type = mime
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            )
        }
    }

    private fun elevationSamplesOf(events: List<GpsEventEntity>, qnhHpa: Float): List<ElevationSample> {
        return ElevationSeries.downsample(
            ElevationSeries.fromPoints(
                events.map { event ->
                    ElevationPoint(
                        latitude = event.latitude,
                        longitude = event.longitude,
                        gpsAltitude = event.altitude,
                        baroAltitude = BaroAltitude.displayedMeters(
                            event.pressureHpa,
                            event.baroAltitude,
                            qnhHpa
                        )
                    )
                }
            )
        )
    }
}
