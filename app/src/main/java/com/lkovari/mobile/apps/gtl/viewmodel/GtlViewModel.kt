package com.lkovari.mobile.apps.gtl.viewmodel

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lkovari.mobile.apps.gtl.GtlApplication
import com.lkovari.mobile.apps.gtl.diagnostics.AppErrorLog
import com.lkovari.mobile.apps.gtl.diagnostics.RecordingInterruptNotice
import com.lkovari.mobile.apps.gtl.data.db.GpsEventEntity
import com.lkovari.mobile.apps.gtl.data.db.TrackSessionEntity
import com.lkovari.mobile.apps.gtl.data.db.toStoredCard
import com.lkovari.mobile.apps.gtl.data.maps.OsmCatalog
import com.lkovari.mobile.apps.gtl.data.maps.OsmDownloadState
import com.lkovari.mobile.apps.gtl.data.maps.OsmRegion
import com.lkovari.mobile.apps.gtl.data.prefs.GoogleMapLayer
import com.lkovari.mobile.apps.gtl.data.prefs.GtlSettings
import com.lkovari.mobile.apps.gtl.domain.GpxExportUseCase
import com.lkovari.mobile.apps.gtl.domain.KmlExportUseCase
import com.lkovari.mobile.apps.gtl.domain.TrackShareFailure
import com.lkovari.mobile.apps.gtl.domain.TrackShareFormat
import com.lkovari.mobile.apps.gtl.data.sensor.AndroidBaroAltitude
import com.lkovari.mobile.apps.gtl.data.sensor.freshPressures
import com.lkovari.mobile.apps.gtl.engine.AppTheme
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
import com.lkovari.mobile.apps.gtl.engine.GnssSnapshot
import com.lkovari.mobile.apps.gtl.engine.GpsAltitude
import com.lkovari.mobile.apps.gtl.engine.TrackPresentation
import com.lkovari.mobile.apps.gtl.engine.TrackPresentationKey
import com.lkovari.mobile.apps.gtl.engine.MapDisplayUsage
import com.lkovari.mobile.apps.gtl.engine.MapSearch
import com.lkovari.mobile.apps.gtl.engine.MapSearchHit
import com.lkovari.mobile.apps.gtl.engine.MapTrackVisibility
import com.lkovari.mobile.apps.gtl.engine.MeasurementSystem
import com.lkovari.mobile.apps.gtl.engine.OsmHillshading
import com.lkovari.mobile.apps.gtl.engine.SpeedRun
import com.lkovari.mobile.apps.gtl.engine.SpeedSample
import com.lkovari.mobile.apps.gtl.engine.SpeedSeries
import com.lkovari.mobile.apps.gtl.engine.SpeedTrack
import com.lkovari.mobile.apps.gtl.engine.TrackVertex
import com.lkovari.mobile.apps.gtl.engine.OsmMapFile
import com.lkovari.mobile.apps.gtl.engine.OsmMapLocale
import com.lkovari.mobile.apps.gtl.engine.OsmOfflineAvailability
import com.lkovari.mobile.apps.gtl.engine.OfflineMapUse
import com.lkovari.mobile.apps.gtl.engine.TrackInspectDump
import com.lkovari.mobile.apps.gtl.engine.TrackInspectEvent
import com.lkovari.mobile.apps.gtl.engine.SavedTrackCard
import com.lkovari.mobile.apps.gtl.engine.SavedTrackCards
import com.lkovari.mobile.apps.gtl.engine.TrackStats
import com.lkovari.mobile.apps.gtl.engine.TrackStatsCalculator
import com.lkovari.mobile.apps.gtl.engine.ThemeMode
import com.lkovari.mobile.apps.gtl.engine.ThemePosition
import com.lkovari.mobile.apps.gtl.engine.UsageType
import com.lkovari.mobile.apps.gtl.service.LiveTrackingState
import com.lkovari.mobile.apps.gtl.service.TrackingForegroundService
import com.lkovari.mobile.apps.gtl.tuhu.TuhuDeletePolicy
import com.lkovari.mobile.apps.gtl.tuhu.TuhuFeature
import com.lkovari.mobile.apps.gtl.tuhu.TuhuRenderOptions
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
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.time.Instant

private data class OsmTuhuBits(
    val mapCleared: Boolean,
    val hasDownloadedOsmMap: Boolean,
    val tuhuRenderOptions: TuhuRenderOptions,
    val tuhuMapDownloaded: Boolean
)

private data class StartupSettings(
    val settings: GtlSettings,
    val loaded: Boolean
)

private data class MapUiBits(
    val requestedTab: Int?,
    val selectedSessionId: Long?,
    val fixCloud: FixCloudSnapshot,
    val mainTab: Int,
    val mapCleared: Boolean,
    val hasDownloadedOsmMap: Boolean,
    val tuhuRenderOptions: TuhuRenderOptions,
    val tuhuMapDownloaded: Boolean
)

data class MapSearchUi(
    val indexing: Boolean = false,
    val ready: Boolean = false,
    val failed: Boolean = false,
    val truncated: Boolean = false,
    val searching: Boolean = false,
    val hits: List<MapSearchHit> = emptyList()
)

data class GtlUiState(
    val settings: GtlSettings,
    val settingsLoaded: Boolean,
    val live: LiveTrackingState,
    val events: List<GpsEventEntity>,
    val stats: TrackStats,
    val displayPoints: List<GeoPoint>,
    val sessions: List<TrackSessionEntity>,
    val mapsKeyPresent: Boolean,
    val osmFile: File?,
    val hasDownloadedOsmMap: Boolean,
    val requestedTab: Int?,
    val selectedSessionId: Long?,
    val fixCloud: FixCloudSnapshot,
    val mapUsageType: UsageType,
    val mainTab: Int,
    val tuhuRenderOptions: TuhuRenderOptions = TuhuRenderOptions.defaults(),
    val tuhuMapDownloaded: Boolean = false,
    val tuhuHillshadingAvailable: Boolean = false,
    val osmHillshadingAvailable: Boolean = false,
    val speedUsage: UsageType = UsageType.TWO_WHEELERS,
    val speedLegendVisible: Boolean = false,
    val speedRuns: List<SpeedRun> = emptyList(),
    val speedSamples: List<SpeedSample> = emptyList(),
    val darkTheme: Boolean = false
) {
    val showingOsmMap: Boolean
        get() = OsmOfflineAvailability.effectiveUseOffline(settings.useOfflineMap, hasDownloadedOsmMap) &&
            osmFile != null

    val osmMapInUse: Boolean
        get() = showingOsmMap && !TuhuFeature.isTuhuMap(settings.selectedMapFile)

    val tuhuMapInUse: Boolean
        get() = showingOsmMap && TuhuFeature.isTuhuMap(settings.selectedMapFile)
}

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
    private var previewBearing: Float? = null
    private var previewBearingAtMillis: Long = 0L
    private var pressureJob: Job? = null
    private val fixCloudBuffer = FixCloudBuffer()
    private val fixCloudView = MutableStateFlow(FixCloudSnapshot.Empty)
    private val savedElevationSessionId = MutableStateFlow<Long?>(null)
    private val savedElevationSamples = MutableStateFlow<List<ElevationSample>>(emptyList())
    private var lastObservedNanos = Long.MIN_VALUE
    private var lastGnssOnly: Boolean? = null
    private val mapSearchUi = MutableStateFlow(MapSearchUi())
    val mapSearch: StateFlow<MapSearchUi> = mapSearchUi.asStateFlow()
    private var searchJob: Job? = null
    private val themeMinute = MutableStateFlow(0L)
    private val recordingInterruptedState = MutableStateFlow(false)
    val recordingInterrupted: StateFlow<Boolean> = recordingInterruptedState.asStateFlow()

    private val startupSettings: StateFlow<StartupSettings> = app.preferences.settings
        .map { StartupSettings(settings = it, loaded = true) }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            StartupSettings(settings = GtlSettings.placeholder(), loaded = false)
        )

    val settings: StateFlow<GtlSettings> = startupSettings
        .map { it.settings }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            GtlSettings.placeholder()
        )

    val settingsLoaded: StateFlow<Boolean> = startupSettings
        .map { it.loaded }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val live: StateFlow<LiveTrackingState> = app.trackingState.state

    val savedElevationId: StateFlow<Long?> = savedElevationSessionId
    val savedElevation: StateFlow<List<ElevationSample>> = savedElevationSamples
    private val inspectDumpText = MutableStateFlow<String?>(null)
    val inspectDump: StateFlow<String?> = inspectDumpText

    val sessions: StateFlow<List<TrackSessionEntity>> = app.trackRepository.observeSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val savedTrackCards: StateFlow<Map<Long, SavedTrackCard>> = combine(
        sessions,
        live.map { state -> state.logging to state.sessionId }.distinctUntilChanged()
    ) { sessionList, recording ->
        sessionList to recording
    }.flatMapLatest { (sessionList, recording) ->
        val (loggingNow, openId) = recording
        val liveId = if (loggingNow) openId else null
        val closedCards = sessionList.mapNotNull { session ->
            if (session.id == liveId) {
                null
            } else {
                session.toStoredCard()?.let { card -> session.id to card }
            }
        }.toMap()
        if (liveId == null) {
            flowOf(closedCards)
        } else {
            app.trackRepository.observeEvents(liveId).map { events ->
                val liveCard = withContext(Dispatchers.Default) {
                    SavedTrackCards.from(app.trackRepository.toSamples(events))
                }
                closedCards + (liveId to liveCard)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    init {
        observeLoggingPreview()
        observeFixCloud()
        observeSavedElevationQnh()
        observeOsmDownloadAvailability()
        observeActiveMapSearch()
        observeThemeClock()
        observeThemeAnchor()
        viewModelScope.launch(Dispatchers.IO) {
            recordingInterruptedState.value = RecordingInterruptNotice.pending(app) != null
        }
    }

    fun dismissRecordingInterrupted() {
        viewModelScope.launch(Dispatchers.IO) {
            RecordingInterruptNotice.clear(app)
            recordingInterruptedState.value = false
        }
    }

    fun startPreview() {
        if (app.trackingState.state.value.logging) {
            return
        }
        listenGnss()
        listenLocation()
        listenCompass()
        listenTemperature()
        listenPressure()
    }

    private fun stopPreview() {
        gnssJob?.cancel()
        gnssJob = null
        locationJob?.cancel()
        locationJob = null
        compassJob?.cancel()
        compassJob = null
        temperatureJob?.cancel()
        temperatureJob = null
        previewBearing = null
        previewBearingAtMillis = 0L
        pressureJob?.cancel()
        pressureJob = null
    }

    private fun observeLoggingPreview() {
        viewModelScope.launch {
            app.trackingState.state.map { it.logging }.distinctUntilChanged().collect { logging ->
                if (logging) {
                    stopPreview()
                } else {
                    startPreview()
                }
            }
        }
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
                client.locations(1000L, 0f, gnssOnly, recording = false).collect { location ->
                    app.trackingState.acceptFix(location, settings.value.usageType) {
                        it.copy(provider = location.provider)
                    }
                    val current = app.trackingState.state.value
                    val speed = current.displaySpeedMps ?: 0f
                    val bearingLean = if (location.hasBearing() && previewBearing != null) {
                        BikeLeanAngle.fromBearingChange(
                            speed,
                            previewBearing ?: 0f,
                            location.bearing,
                            location.time - previewBearingAtMillis
                        )
                    } else {
                        null
                    }
                    if (location.hasBearing()) {
                        previewBearing = location.bearing
                        previewBearingAtMillis = location.time
                    }
                    val lean = BikeLeanAngle.liveDegrees(speed, current.yawRateRadPerSec, bearingLean)
                    app.trackingState.update { it.copy(leanAngle = lean) }
                }
            }
        }
    }

    private fun listenCompass() {
        compassJob?.cancel()
        compassJob = viewModelScope.launch {
            app.compassSource.samples().collectLatest { sample ->
                app.trackingState.update {
                    val yaw = sample.yawRateRadPerSec
                    val lean = if (yaw != null) {
                        BikeLeanAngle.fromYawRate(it.displaySpeedMps, yaw)
                    } else {
                        it.leanAngle
                    }
                    it.copy(
                        azimuthDegrees = sample.azimuthDegrees,
                        compassAccuracy = sample.accuracy,
                        yawRateRadPerSec = yaw,
                        leanAngle = lean
                    )
                }
            }
        }
    }

    private fun listenPressure() {
        pressureJob?.cancel()
        pressureJob = viewModelScope.launch {
            settings.map { it.qnhHpa to it.baroPressureOffsetHpa }.distinctUntilChanged().collectLatest { (qnh, offset) ->
                app.trackingState.update {
                    it.copy(pressureAvailable = app.pressureSource.isAvailable)
                }
                app.pressureSource.freshPressures().collect { value ->
                    app.trackingState.acceptPressure(value, qnh, offset)
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
            settings.map { it.qnhHpa to it.baroPressureOffsetHpa }.distinctUntilChanged().collectLatest { (qnh, offset) ->
                val id = savedElevationSessionId.value ?: return@collectLatest
                val events = app.trackRepository.eventsFor(id)
                savedElevationSamples.value = elevationSamplesOf(events, qnh, offset)
            }
        }
    }

    private fun observeOsmDownloadAvailability() {
        viewModelScope.launch {
            app.osmMapStore.observeHasDownloadedMap().collect { hasMap ->
                if (settings.value.useOfflineMap &&
                    !OsmOfflineAvailability.effectiveUseOffline(true, hasMap)
                ) {
                    app.preferences.setUseOfflineMap(false)
                }
            }
        }
    }

    private val activeEvents = combine(
        live.map { it.sessionId }.distinctUntilChanged(),
        selectedSessionId,
        settings.map { it.showLastTrackOnMap }.distinctUntilChanged(),
        sessions,
        mapCleared
    ) { liveSessionId, selected, showLastTrack, sessionList, cleared ->
        liveSessionId
            ?: selected
            ?: if (!cleared && showLastTrack) sessionList.firstOrNull()?.id else null
    }.distinctUntilChanged().flatMapLatest { sessionId ->
        if (sessionId == null) {
            flowOf(emptyList())
        } else {
            app.trackRepository.observeEvents(sessionId)
        }
    }

    private val liveUi = live.distinctUntilChanged { previous, next ->
        previous.uiTick() == next.uiTick()
    }

    private val mapFiles: StateFlow<MapFileSnapshot> = combine(
        settings.map { it.selectedMapFile }.distinctUntilChanged(),
        app.osmMapStore.downloadedRevision
    ) { path, _ -> path }
        .map { path ->
            withContext(Dispatchers.IO) {
                val osm = if (path.isNotBlank()) {
                    File(path).takeIf { OsmMapFile.isReadable(it) }
                } else {
                    null
                }
                val tuhu = app.tuhuMapStore.downloadedFile()
                MapFileSnapshot(
                    osmFile = osm,
                    osmHillshadingAvailable = osm != null && OsmHillshading.available(osm),
                    tuhuHillshadingAvailable = tuhu != null && OsmHillshading.available(tuhu),
                    tuhuMapDownloaded = tuhu != null
                )
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            MapFileSnapshot(
                osmFile = null,
                osmHillshadingAvailable = false,
                tuhuHillshadingAvailable = false,
                tuhuMapDownloaded = false
            )
        )

    private val trackPresentation: StateFlow<DrawnTrack> = combine(
        combine(
            activeEvents,
            live.map { it.logging to it.sessionId }.distinctUntilChanged(),
            selectedSessionId,
            mapCleared
        ) { events, liveBits, selected, cleared ->
            TrackDrawHead(
                events = events,
                logging = liveBits.first,
                liveSessionId = liveBits.second,
                selectedSessionId = selected,
                mapCleared = cleared
            )
        },
        settings.map { it.toTrackSlice() }.distinctUntilChanged(),
        sessions
    ) { head, slice, sessionList ->
        TrackDrawRequest(
            events = head.events,
            logging = head.logging,
            liveSessionId = head.liveSessionId,
            selectedSessionId = head.selectedSessionId,
            mapCleared = head.mapCleared,
            sessionList = sessionList,
            slice = slice
        )
    }.map { request ->
        request to request.presentationKey()
    }.distinctUntilChanged { previous, next ->
        TrackPresentation.same(previous.second, next.second)
    }.map { (request, _) ->
        presentTrack(request)
    }.flowOn(Dispatchers.Default).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        DrawnTrack(
            events = emptyList(),
            stats = TrackStatsCalculator.compute(emptyList()),
            displayPoints = emptyList(),
            mapUsageType = UsageType.TWO_WHEELERS,
            speedUsage = UsageType.TWO_WHEELERS,
            speedLegendVisible = false,
            speedRuns = emptyList(),
            speedSamples = emptyList()
        )
    )

    val uiState: StateFlow<GtlUiState> = combine(
        combine(startupSettings, themeMinute) { startup, _ -> startup },
        liveUi,
        trackPresentation,
        sessions,
        combine(
            combine(
                requestedTab,
                selectedSessionId,
                fixCloudView,
                mainTab,
                combine(
                    mapCleared,
                    app.osmMapStore.observeHasDownloadedMap(),
                    app.tuhuPreferences.options
                ) { cleared, hasMap, tuhuOptions ->
                    OsmTuhuBits(
                        mapCleared = cleared,
                        hasDownloadedOsmMap = hasMap,
                        tuhuRenderOptions = tuhuOptions,
                        tuhuMapDownloaded = false
                    )
                }
            ) { tab, selected, cloud, persistedTab, osmTuhu ->
                MapUiBits(
                    requestedTab = tab,
                    selectedSessionId = selected,
                    fixCloud = cloud,
                    mainTab = persistedTab,
                    mapCleared = osmTuhu.mapCleared,
                    hasDownloadedOsmMap = osmTuhu.hasDownloadedOsmMap,
                    tuhuRenderOptions = osmTuhu.tuhuRenderOptions,
                    tuhuMapDownloaded = osmTuhu.tuhuMapDownloaded
                )
            },
            mapFiles
        ) { bits, files ->
            bits to files
        }
    ) { startup, liveState, drawn, sessionList, bitsAndFiles ->
        val prefs = startup.settings
        val mapBits = bitsAndFiles.first
        val files = bitsAndFiles.second
        GtlUiState(
            settings = prefs,
            settingsLoaded = startup.loaded,
            live = liveState,
            events = drawn.events,
            stats = drawn.stats,
            displayPoints = drawn.displayPoints,
            sessions = sessionList,
            mapsKeyPresent = com.lkovari.mobile.apps.gtl.BuildConfig.MAPS_API_KEY.isNotBlank(),
            osmFile = files.osmFile,
            hasDownloadedOsmMap = mapBits.hasDownloadedOsmMap,
            requestedTab = mapBits.requestedTab,
            selectedSessionId = mapBits.selectedSessionId,
            fixCloud = mapBits.fixCloud,
            mapUsageType = drawn.mapUsageType,
            mainTab = mapBits.mainTab,
            tuhuRenderOptions = mapBits.tuhuRenderOptions,
            tuhuMapDownloaded = files.tuhuMapDownloaded,
            tuhuHillshadingAvailable = files.tuhuHillshadingAvailable,
            osmHillshadingAvailable = files.osmHillshadingAvailable,
            speedUsage = drawn.speedUsage,
            speedLegendVisible = drawn.speedLegendVisible,
            speedRuns = drawn.speedRuns,
            speedSamples = drawn.speedSamples,
            darkTheme = AppTheme.isDark(
                prefs.themeMode,
                liveState.lastLocation?.latitude ?: prefs.themeLatitude,
                liveState.lastLocation?.longitude ?: prefs.themeLongitude,
                Instant.now()
            )
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        GtlUiState(
            settings = settings.value,
            settingsLoaded = false,
            live = live.value,
            events = emptyList(),
            stats = TrackStatsCalculator.compute(emptyList()),
            displayPoints = emptyList(),
            sessions = emptyList(),
            mapsKeyPresent = com.lkovari.mobile.apps.gtl.BuildConfig.MAPS_API_KEY.isNotBlank(),
            osmFile = null,
            hasDownloadedOsmMap = false,
            requestedTab = null,
            selectedSessionId = null,
            fixCloud = FixCloudSnapshot.Empty,
            mapUsageType = settings.value.usageType,
            mainTab = 0,
            speedUsage = settings.value.usageType
        )
    )

    val locationPermissionAsked: StateFlow<Boolean> = app.preferences.locationPermissionAsked
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private fun presentTrack(request: TrackDrawRequest): DrawnTrack {
        val events = request.events
        val slice = request.slice
        val selected = request.selectedSessionId
        val cleared = request.mapCleared
        val followSettings = MapDisplayUsage.followsSettings(request.logging, selected)
        val samples = app.trackRepository.toSamples(events)
        val routeStats = TrackStatsCalculator.compute(samples)
        val viewingId = request.liveSessionId
            ?: selected
            ?: if (!cleared && slice.showLastTrackOnMap) request.sessionList.firstOrNull()?.id else null
        val viewed = request.sessionList.find { it.id == viewingId }
        val mapUsage = MapDisplayUsage.of(
            logging = request.logging,
            followSettings = followSettings,
            settingsUsage = slice.usageType,
            sessionUsageName = viewed?.usageType
        )
        val simplify = MapDisplayUsage.simplify(
            usage = mapUsage,
            logging = request.logging,
            followSettings = followSettings,
            settingsActive = slice.optimizationActive,
            settingsTolerance = slice.optimizationTolerance
        )
        val vertices = events.map { event ->
            TrackVertex(event.latitude, event.longitude, event.altitude, event.speed)
        }
        val simplifyOn = simplify.first && vertices.size > 4
        val keep = if (simplifyOn) {
            DouglasPeucker.keepIndices(
                vertices.map { GeoPoint(it.latitude, it.longitude, it.altitude) },
                DouglasPeucker.clampTolerance(simplify.second)
            )
        } else {
            BooleanArray(vertices.size) { true }
        }
        val display = vertices.mapIndexedNotNull { index, vertex ->
            if (keep.getOrElse(index) { false }) {
                GeoPoint(vertex.latitude, vertex.longitude, vertex.altitude)
            } else {
                null
            }
        }
        val trackVisible = MapTrackVisibility.visible(
            request.logging,
            slice.showLastTrackOnMap,
            selected,
            cleared
        )
        val mapPoints = if (trackVisible) display else emptyList()
        val speedUsage = viewed?.usageType
            ?.let { runCatching { UsageType.valueOf(it) }.getOrNull() }
            ?: slice.usageType
        val speedRuns = if (trackVisible) {
            SpeedTrack.runs(vertices, keep, speedUsage, slice.measurementSystem)
        } else {
            emptyList()
        }
        return DrawnTrack(
            events = events,
            stats = routeStats,
            displayPoints = mapPoints,
            mapUsageType = mapUsage,
            speedUsage = speedUsage,
            speedLegendVisible = trackVisible,
            speedRuns = speedRuns,
            speedSamples = SpeedSeries.downsample(SpeedSeries.fromVertices(vertices))
        )
    }

    fun acceptDisclaimer() {
        viewModelScope.launch { app.preferences.setDisclaimerAccepted(true) }
    }

    fun markLocationPermissionAsked() {
        viewModelScope.launch { app.preferences.setLocationPermissionAsked() }
    }

    fun startLogging() {
        selectedSessionId.value = null
        mapCleared.value = false
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
            if (id == app.trackingState.state.value.sessionId) {
                return@launch
            }
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
            savedElevationSamples.value = elevationSamplesOf(
                events,
                settings.value.qnhHpa,
                settings.value.baroPressureOffsetHpa
            )
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
        viewModelScope.launch {
            if (!value) {
                app.preferences.setUseOfflineMap(false)
                return@launch
            }
            val maps = app.osmMapStore.listDownloaded()
            if (!OsmOfflineAvailability.canEnable(maps.isNotEmpty())) {
                return@launch
            }
            val selected = settings.value.selectedMapFile
            val readable = selected.isNotBlank() && OsmMapFile.isReadable(File(selected))
            if (!readable) {
                app.preferences.setSelectedMapFile(maps.first().absolutePath)
            }
            app.preferences.setUseOfflineMap(true)
        }
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

    fun setSpeedScaleAlwaysOpen(value: Boolean) {
        viewModelScope.launch { app.preferences.setSpeedScaleAlwaysOpen(value) }
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

    fun calibrateBaroFromGps() {
        viewModelScope.launch {
            val live = app.trackingState.state.value
            val pressure = live.pressureHpa ?: return@launch
            if (!BaroAltitude.isPlausiblePressureHpa(pressure)) {
                return@launch
            }
            val location = live.lastLocation ?: return@launch
            if (!location.hasAltitude()) {
                return@launch
            }
            if (!GpsAltitude.isPlausible(location.altitude)) {
                return@launch
            }
            val offset = BaroAltitude.calibrationOffsetHpa(
                pressure,
                location.altitude,
                settings.value.qnhHpa
            ) ?: return@launch
            app.preferences.setBaroPressureOffsetHpa(offset)
        }
    }

    fun resetBaroPressureOffset() {
        viewModelScope.launch { app.preferences.setBaroPressureOffsetHpa(0f) }
    }

    fun setAutoCalibrateBaroEnabled(value: Boolean) {
        viewModelScope.launch { app.preferences.setAutoCalibrateBaroEnabled(value) }
    }

    fun setGoogleMapLayer(value: GoogleMapLayer) {
        viewModelScope.launch { app.preferences.setGoogleMapLayer(value) }
    }

    fun setThemeAutomatic(enabled: Boolean) {
        viewModelScope.launch {
            val mode = if (enabled) {
                ThemeMode.AUTOMATIC
            } else if (uiState.value.darkTheme) {
                ThemeMode.DARK
            } else {
                ThemeMode.LIGHT
            }
            app.preferences.setThemeMode(mode)
        }
    }

    fun setThemeMode(value: ThemeMode) {
        viewModelScope.launch { app.preferences.setThemeMode(value) }
    }

    fun setOsmBuildings(value: Boolean) {
        viewModelScope.launch { app.preferences.setOsmBuildings(value) }
    }

    fun setOsmPoi(value: Boolean) {
        viewModelScope.launch { app.preferences.setOsmPoi(value) }
    }

    fun setOsmTransit(value: Boolean) {
        viewModelScope.launch { app.preferences.setOsmTransit(value) }
    }

    fun setOsmCycleways(value: Boolean) {
        viewModelScope.launch { app.preferences.setOsmCycleways(value) }
    }

    fun setOsmParks(value: Boolean) {
        viewModelScope.launch { app.preferences.setOsmParks(value) }
    }

    fun setOsmHillshading(value: Boolean) {
        viewModelScope.launch { app.preferences.setOsmHillshading(value) }
    }

    fun setTuhuBlazes(value: Boolean) {
        viewModelScope.launch { app.tuhuPreferences.setBlazes(value) }
    }

    fun setTuhuPaths(value: Boolean) {
        viewModelScope.launch { app.tuhuPreferences.setPaths(value) }
    }

    fun setTuhuContours(value: Boolean) {
        viewModelScope.launch { app.tuhuPreferences.setContours(value) }
    }

    fun setTuhuContoursMinor(value: Boolean) {
        viewModelScope.launch { app.tuhuPreferences.setContoursMinor(value) }
    }

    fun setTuhuHikePoi(value: Boolean) {
        viewModelScope.launch { app.tuhuPreferences.setHikePoi(value) }
    }

    fun setTuhuParks(value: Boolean) {
        viewModelScope.launch { app.tuhuPreferences.setParks(value) }
    }

    fun setTuhuUrbanPoi(value: Boolean) {
        viewModelScope.launch { app.tuhuPreferences.setUrbanPoi(value) }
    }

    fun setTuhuHillshading(value: Boolean) {
        viewModelScope.launch { app.tuhuPreferences.setHillshading(value) }
    }

    val regionSizes: StateFlow<Map<String, Long>> = app.osmMapStore.regionSizes

    fun probeRegionSizes() {
        viewModelScope.launch { app.osmMapStore.probeSizes(OsmCatalog.regions) }
    }

    fun downloadRegion(region: OsmRegion, title: String) {
        app.osmMapStore.enqueue(region, title)
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

    fun toggleDownloadedMap(region: OsmRegion) {
        val file = app.osmMapStore.downloadedFile(region.id) ?: return
        val prefs = settings.value
        if (OfflineMapUse.isInUse(prefs.useOfflineMap, prefs.selectedMapFile, file.absolutePath)) {
            viewModelScope.launch {
                app.preferences.setUseOfflineMap(false)
            }
        } else {
            selectDownloadedMap(region)
        }
    }

    fun isRegionInUse(region: OsmRegion): Boolean {
        val file = app.osmMapStore.downloadedFile(region.id) ?: return false
        val prefs = settings.value
        return OfflineMapUse.isInUse(prefs.useOfflineMap, prefs.selectedMapFile, file.absolutePath)
    }

    fun observeDownloadedRevision(): StateFlow<Int> = app.osmMapStore.downloadedRevision

    fun searchPlaces(query: String) {
        searchJob?.cancel()
        if (!MapSearch.accepts(query)) {
            mapSearchUi.update { it.copy(hits = emptyList(), searching = false) }
            return
        }
        mapSearchUi.update { it.copy(searching = true) }
        searchJob = viewModelScope.launch {
            var published = false
            try {
                delay(300)
                val liveFix = live.value.lastLocation
                val stored = app.mapSearch.origin()
                val latitude = liveFix?.latitude ?: stored?.first
                val longitude = liveFix?.longitude ?: stored?.second
                if (latitude == null || longitude == null) {
                    mapSearchUi.update { it.copy(hits = emptyList(), searching = false) }
                    published = true
                    return@launch
                }
                val hits = app.mapSearch.search(query, latitude, longitude)
                if (!isActive) {
                    return@launch
                }
                mapSearchUi.update { it.copy(hits = hits, searching = false) }
                published = true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                AppErrorLog.record("place.search", error)
                if (isActive) {
                    mapSearchUi.update { it.copy(hits = emptyList(), searching = false) }
                    published = true
                }
            } finally {
                if (!published && isActive) {
                    mapSearchUi.update { it.copy(hits = emptyList(), searching = false) }
                }
            }
        }
    }

    fun clearPlaceSearch() {
        searchJob?.cancel()
        mapSearchUi.update { it.copy(hits = emptyList(), searching = false) }
    }

    private fun observeActiveMapSearch() {
        viewModelScope.launch {
            combine(
                settings,
                app.osmMapStore.observeHasDownloadedMap(),
                app.osmMapStore.downloadedRevision
            ) { prefs, hasMap, _ ->
                prefs.selectedMapFile to
                    OsmOfflineAvailability.effectiveUseOffline(prefs.useOfflineMap, hasMap)
            }.distinctUntilChanged().collect { (path, offline) ->
                searchJob?.cancel()
                mapSearchUi.update { it.copy(hits = emptyList(), searching = false) }
                val file = withContext(Dispatchers.IO) {
                    val candidate = path.takeIf { it.isNotBlank() }?.let { File(it) }
                    candidate?.takeIf { offline && OsmMapFile.isReadable(it) }
                }
                app.mapSearch.activate(file)
            }
        }
        viewModelScope.launch {
            app.mapSearch.status.collect { status ->
                mapSearchUi.update {
                    it.copy(
                        indexing = status.indexing,
                        ready = status.ready,
                        failed = status.failed,
                        truncated = status.truncated
                    )
                }
            }
        }
    }

    private fun observeThemeClock() {
        viewModelScope.launch {
            while (isActive) {
                delay(60_000)
                themeMinute.value = themeMinute.value + 1
            }
        }
    }

    private fun observeThemeAnchor() {
        viewModelScope.launch {
            live.map { state ->
                val location = state.lastLocation
                location?.latitude to location?.longitude
            }.distinctUntilChanged().collect { (latitude, longitude) ->
                if (latitude == null || longitude == null) {
                    return@collect
                }
                val prefs = settings.value
                if (!ThemePosition.shouldStore(
                        prefs.themeLatitude,
                        prefs.themeLongitude,
                        latitude,
                        longitude
                    )
                ) {
                    return@collect
                }
                app.preferences.rememberThemePosition(latitude, longitude)
            }
        }
    }

    fun deleteDownloadedMap(region: OsmRegion) {
        val file = app.osmMapStore.downloadedFile(region.id)
        val selectedPath = settings.value.selectedMapFile
        val deletingSelected = file != null && file.absolutePath == selectedPath
        val localeCountry = app.resources.configuration.locales[0].country
        app.osmMapStore.delete(region.id)
        val mapsRemain = app.osmMapStore.hasDownloadedMap()
        val localeMatch = OsmMapLocale.countryMatches(region.countryCode, localeCountry)
        viewModelScope.launch {
            if (file != null) {
                app.mapSearch.delete(file)
            }
            if (deletingSelected) {
                app.preferences.setSelectedMapFile("")
            }
            if (OsmOfflineAvailability.forceGoogleAfterDelete(deletingSelected, localeMatch, mapsRemain)) {
                app.preferences.setUseOfflineMap(false)
            }
        }
    }

    fun onOsmMapFailed() {
        viewModelScope.launch {
            app.preferences.setUseOfflineMap(false)
        }
    }

    fun regions(): List<OsmRegion> = OsmCatalog.regions

    fun isDownloaded(region: OsmRegion): Boolean = app.osmMapStore.downloadedFile(region.id) != null

    fun downloadTuhu(title: String) {
        app.tuhuMapStore.enqueue(title)
    }

    fun observeTuhuDownload(): kotlinx.coroutines.flow.Flow<OsmDownloadState> {
        return app.tuhuMapStore.observe()
    }

    fun selectTuhuMap() {
        val file = app.tuhuMapStore.downloadedFile() ?: return
        viewModelScope.launch {
            app.preferences.setSelectedMapFile(file.absolutePath)
            app.preferences.setUseOfflineMap(true)
        }
    }

    fun toggleTuhuMap() {
        val file = app.tuhuMapStore.downloadedFile() ?: return
        val prefs = settings.value
        if (OfflineMapUse.isInUse(prefs.useOfflineMap, prefs.selectedMapFile, file.absolutePath)) {
            viewModelScope.launch {
                app.preferences.setUseOfflineMap(false)
            }
        } else {
            selectTuhuMap()
        }
    }

    fun isTuhuInUse(): Boolean {
        val file = app.tuhuMapStore.downloadedFile() ?: return false
        val prefs = settings.value
        return OfflineMapUse.isInUse(prefs.useOfflineMap, prefs.selectedMapFile, file.absolutePath)
    }

    fun deleteTuhuMap() {
        val file = app.tuhuMapStore.downloadedFile()
        val selectedPath = settings.value.selectedMapFile
        val deletingSelected = file != null && file.absolutePath == selectedPath
        app.tuhuMapStore.delete()
        viewModelScope.launch {
            if (file != null) {
                app.mapSearch.delete(file)
            }
            if (TuhuDeletePolicy.forceGoogle(deletingSelected)) {
                app.preferences.setSelectedMapFile("")
                app.preferences.setUseOfflineMap(false)
            }
        }
    }

    fun isTuhuDownloaded(): Boolean = app.tuhuMapStore.downloadedFile() != null

    fun shareSessions(
        sessionIds: Collection<Long>,
        format: TrackShareFormat,
        onReady: (Intent) -> Unit,
        onFailed: (TrackShareFailure) -> Unit
    ) {
        if (sessionIds.isEmpty()) {
            onFailed(TrackShareFailure.Empty)
            return
        }
        viewModelScope.launch {
            val chosen = uiState.value.sessions.filter { it.id in sessionIds }
            val items = chosen.map { session ->
                session to app.trackRepository.eventsFor(session.id)
            }.filter { it.second.isNotEmpty() }
            if (items.isEmpty()) {
                onFailed(TrackShareFailure.Empty)
                return@launch
            }
            var file: File? = null
            try {
                file = when (format) {
                    TrackShareFormat.KMZ -> exporter.write(
                        items,
                        settings.value.qnhHpa,
                        settings.value.baroPressureOffsetHpa
                    )
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
            } catch (error: IOException) {
                file?.delete()
                onFailed(TrackShareFailure.Write)
            } catch (error: IllegalArgumentException) {
                file?.delete()
                onFailed(TrackShareFailure.Write)
            }
        }
    }

    private fun elevationSamplesOf(
        events: List<GpsEventEntity>,
        qnhHpa: Float,
        offsetHpa: Float
    ): List<ElevationSample> {
        return ElevationSeries.downsample(
            ElevationSeries.fromPoints(
                events.map { event ->
                    ElevationPoint(
                        latitude = event.latitude,
                        longitude = event.longitude,
                        gpsAltitude = event.altitude,
                        baroAltitude = AndroidBaroAltitude.displayedMeters(
                            event.pressureHpa,
                            event.baroAltitude,
                            qnhHpa,
                            offsetHpa,
                            event.altitude
                        )
                    )
                }
            )
        )
    }
}

private data class TrackSettingsSlice(
    val usageType: UsageType,
    val optimizationActive: Boolean,
    val optimizationTolerance: Double,
    val showLastTrackOnMap: Boolean,
    val measurementSystem: MeasurementSystem
)

private data class TrackDrawHead(
    val events: List<GpsEventEntity>,
    val logging: Boolean,
    val liveSessionId: Long?,
    val selectedSessionId: Long?,
    val mapCleared: Boolean
)

private data class TrackDrawRequest(
    val events: List<GpsEventEntity>,
    val logging: Boolean,
    val liveSessionId: Long?,
    val selectedSessionId: Long?,
    val mapCleared: Boolean,
    val sessionList: List<TrackSessionEntity>,
    val slice: TrackSettingsSlice
) {
    fun presentationKey(): TrackPresentationKey {
        val last = events.lastOrNull()
        val viewedId = liveSessionId
            ?: selectedSessionId
            ?: if (!mapCleared && slice.showLastTrackOnMap) sessionList.firstOrNull()?.id else null
        val viewedUsage = sessionList.find { it.id == viewedId }?.usageType
        return TrackPresentation.key(
            eventCount = events.size,
            lastEventMillis = last?.timestamp ?: 0L,
            lastLatitude = last?.latitude ?: 0.0,
            lastLongitude = last?.longitude ?: 0.0,
            usageName = slice.usageType.name,
            toleranceMeters = slice.optimizationTolerance.toDouble(),
            optimizationActive = slice.optimizationActive,
            showLastTrackOnMap = slice.showLastTrackOnMap,
            selectedSessionId = selectedSessionId,
            viewingSessionId = viewedId,
            logging = logging,
            mapCleared = mapCleared,
            measurementSystemName = slice.measurementSystem.name,
            sessionUsageName = viewedUsage
        )
    }
}

private data class DrawnTrack(
    val events: List<GpsEventEntity>,
    val stats: TrackStats,
    val displayPoints: List<GeoPoint>,
    val mapUsageType: UsageType,
    val speedUsage: UsageType,
    val speedLegendVisible: Boolean,
    val speedRuns: List<SpeedRun>,
    val speedSamples: List<SpeedSample>
)

private data class MapFileSnapshot(
    val osmFile: File?,
    val osmHillshadingAvailable: Boolean,
    val tuhuHillshadingAvailable: Boolean,
    val tuhuMapDownloaded: Boolean
)

private data class LiveUiTick(
    val logging: Boolean,
    val sessionId: Long?,
    val acceptedFixCount: Int,
    val rejectedFixCount: Int,
    val poorGps: Boolean,
    val loggingError: Boolean,
    val gpsOff: Boolean,
    val hasLocation: Boolean,
    val latitude: Double?,
    val longitude: Double?,
    val altitude: Double?,
    val accuracy: Float?,
    val bearing: Float?,
    val time: Long?,
    val displaySpeedMps: Float?,
    val provider: String?,
    val gnss: GnssSnapshot?,
    val temperatureAvailable: Boolean,
    val pressureAvailable: Boolean
)

private fun GtlSettings.toTrackSlice(): TrackSettingsSlice {
    return TrackSettingsSlice(
        usageType = usageType,
        optimizationActive = optimizationActive,
        optimizationTolerance = optimizationTolerance,
        showLastTrackOnMap = showLastTrackOnMap,
        measurementSystem = measurementSystem
    )
}

private fun LiveTrackingState.uiTick(): LiveUiTick {
    val location = lastLocation
    return LiveUiTick(
        logging = logging,
        sessionId = sessionId,
        acceptedFixCount = acceptedFixCount,
        rejectedFixCount = rejectedFixCount,
        poorGps = poorGps,
        loggingError = loggingError,
        gpsOff = gpsOff,
        hasLocation = location != null,
        latitude = location?.latitude,
        longitude = location?.longitude,
        altitude = if (location != null && location.hasAltitude()) location.altitude else null,
        accuracy = if (location != null && location.hasAccuracy()) location.accuracy else null,
        bearing = if (location != null && location.hasBearing()) location.bearing else null,
        time = location?.time,
        displaySpeedMps = displaySpeedMps,
        provider = provider,
        gnss = gnss,
        temperatureAvailable = temperatureAvailable,
        pressureAvailable = pressureAvailable
    )
}
