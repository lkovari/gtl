package com.lkovari.mobile.apps.gtl.ui.screens

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.CameraPositionState
import com.lkovari.mobile.apps.gtl.data.maps.OsmRenderTheme
import com.lkovari.mobile.apps.gtl.engine.OsmRenderOptions
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.lkovari.mobile.apps.gtl.R
import com.lkovari.mobile.apps.gtl.diagnostics.AppErrorLog
import com.lkovari.mobile.apps.gtl.data.prefs.GoogleMapLayer
import com.lkovari.mobile.apps.gtl.engine.FixAcceptance
import com.lkovari.mobile.apps.gtl.engine.FixCloudSample
import com.lkovari.mobile.apps.gtl.engine.FixCloudSnapshot
import com.lkovari.mobile.apps.gtl.engine.FixCloudStats
import com.lkovari.mobile.apps.gtl.engine.GeoPoint
import com.lkovari.mobile.apps.gtl.engine.LatLonBounds
import com.lkovari.mobile.apps.gtl.engine.MapCameraMode
import com.lkovari.mobile.apps.gtl.engine.MapFitZoom
import com.lkovari.mobile.apps.gtl.engine.PostalAddress
import com.lkovari.mobile.apps.gtl.engine.OsmMapCamera
import com.lkovari.mobile.apps.gtl.engine.OsmMapViewRedraw
import com.lkovari.mobile.apps.gtl.engine.TrackCameraBounds
import com.lkovari.mobile.apps.gtl.engine.TapReadout
import com.lkovari.mobile.apps.gtl.engine.TrackEndpoints
import com.lkovari.mobile.apps.gtl.engine.UsageType
import com.lkovari.mobile.apps.gtl.engine.MapAddressLookup
import com.lkovari.mobile.apps.gtl.engine.MapHudMode
import com.lkovari.mobile.apps.gtl.engine.MapHudVisibility
import com.lkovari.mobile.apps.gtl.ui.components.MapHud
import com.lkovari.mobile.apps.gtl.ui.components.MapSearchOverlay
import com.lkovari.mobile.apps.gtl.ui.components.MapTapOverlay
import com.lkovari.mobile.apps.gtl.ui.drawLiveFixReticle
import com.lkovari.mobile.apps.gtl.ui.rememberUsageMarkerBitmap
import com.lkovari.mobile.apps.gtl.ui.theme.AccuracyMarkerBorder
import com.lkovari.mobile.apps.gtl.ui.theme.AccuracyMarkerFill
import com.lkovari.mobile.apps.gtl.ui.theme.CarmineTrack
import com.lkovari.mobile.apps.gtl.ui.theme.GnssLime
import com.lkovari.mobile.apps.gtl.ui.theme.HudCyan
import com.lkovari.mobile.apps.gtl.ui.theme.MoonCream
import com.lkovari.mobile.apps.gtl.ui.theme.FixCloudCepFill
import com.lkovari.mobile.apps.gtl.ui.theme.FixCloudCepStroke
import com.lkovari.mobile.apps.gtl.ui.theme.FixCloudDot
import com.lkovari.mobile.apps.gtl.ui.theme.UsageMarkerRed
import com.lkovari.mobile.apps.gtl.ui.usageIcon
import com.lkovari.mobile.apps.gtl.tuhu.TuhuFeature
import com.lkovari.mobile.apps.gtl.tuhu.TuhuLayerActions
import com.lkovari.mobile.apps.gtl.tuhu.TuhuLayerControls
import com.lkovari.mobile.apps.gtl.tuhu.TuhuRenderOptions
import com.lkovari.mobile.apps.gtl.tuhu.TuhuRenderTheme
import com.lkovari.mobile.apps.gtl.viewmodel.GtlUiState
import com.lkovari.mobile.apps.gtl.viewmodel.MapSearchUi
import org.mapsforge.core.graphics.Bitmap as ForgeBitmap
import org.mapsforge.core.graphics.Canvas
import org.mapsforge.core.graphics.Style
import org.mapsforge.core.model.BoundingBox
import org.mapsforge.core.model.LatLong
import org.mapsforge.core.model.MapPosition
import org.mapsforge.core.model.Point
import org.mapsforge.core.model.Rotation
import org.mapsforge.core.util.LatLongUtils
import org.mapsforge.core.util.MercatorProjection
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.util.AndroidUtil
import org.mapsforge.map.android.view.MapView
import org.mapsforge.map.view.InputListener
import org.mapsforge.map.layer.Layer
import org.mapsforge.map.layer.cache.TileCache
import org.mapsforge.map.layer.overlay.Circle as ForgeCircle
import org.mapsforge.map.layer.overlay.Polyline as ForgePolyline
import org.mapsforge.map.layer.renderer.TileRendererLayer
import org.mapsforge.map.reader.MapFile
import org.mapsforge.map.rendertheme.internal.MapsforgeThemes
import java.io.File

private const val AccuracyFillAlpha = 64
private const val FixCloudDotRadiusMeters = 1.25
private const val FixCloudCentroidRadiusMeters = 2.0
private const val FixCloudPausedAlpha = 0.35f

@Composable
fun MapPane(
    state: GtlUiState,
    onClearMap: () -> Unit,
    modifier: Modifier = Modifier.fillMaxSize(),
    mapActive: Boolean = true,
    onOsmFailed: () -> Unit = {},
    onGoogleMapLayer: (GoogleMapLayer) -> Unit = {},
    onOsmLayers: OsmLayerActions? = null,
    onTuhuLayers: TuhuLayerActions? = null,
    onSearchQuery: (String) -> Unit = {},
    onClearSearch: () -> Unit = {},
    mapSearch: StateFlow<MapSearchUi>
) {
    Box(modifier = modifier) {
        val points = state.displayPoints
        val osmFile = state.osmFile
        var locateRequest by remember { mutableIntStateOf(0) }
        var tapPoint by remember { mutableStateOf<GeoPoint?>(null) }
        var menuOpen by remember { mutableStateOf(false) }
        val tapAnchor = remember { TapAnchor() }
        var coordinateText by remember { mutableStateOf<String?>(null) }
        var postalAddress by remember { mutableStateOf<PostalAddress?>(null) }
        var distanceTarget by remember { mutableStateOf<GeoPoint?>(null) }
        var cameraHold by remember { mutableStateOf(false) }
        var searchFocus by remember { mutableStateOf<MapSearchFocus?>(null) }
        var searchOpen by remember { mutableStateOf(false) }
        var searchQuery by remember { mutableStateOf("") }
        val mapPath = osmFile?.absolutePath
        var trackedMapPath by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(mapPath) {
            val switchedFile = trackedMapPath != null && mapPath != null && trackedMapPath != mapPath
            if (mapPath != null) {
                trackedMapPath = mapPath
            }
            cameraHold = false
            searchFocus = null
            if (switchedFile) {
                tapPoint = null
                menuOpen = false
                coordinateText = null
                postalAddress = null
                tapAnchor.screen = null
                distanceTarget = null
            }
            if (searchOpen) {
                searchOpen = false
                searchQuery = ""
                onClearSearch()
            }
        }
        val viewingSaved = MapCameraMode.finishedTrackOnMap(state.live.logging, points.size)
        val onMapTap = { point: GeoPoint ->
            tapPoint = point
            menuOpen = true
            coordinateText = null
            postalAddress = null
            tapAnchor.screen = null
        }
        if (state.showingOsmMap && osmFile != null) {
            Box(modifier = Modifier.fillMaxSize()) {
                OsmMapView(
                    filePath = osmFile.absolutePath,
                    points = points,
                    location = state.live.lastLocation,
                    showAccuracyMarker = state.settings.showAccuracyMarker,
                    logging = state.live.logging,
                    keepWholeTrack = state.settings.keepWholeTrackOnScreen,
                    viewingSaved = viewingSaved,
                    showFixCloud = state.settings.showFixCloud,
                    fixCloud = state.fixCloud,
                    usageType = state.mapUsageType,
                    osmRenderOptions = state.settings.osmRenderOptions().forMap(state.osmHillshadingAvailable),
                    tuhuRenderOptions = state.tuhuRenderOptions.forMap(state.tuhuHillshadingAvailable),
                    mapActive = mapActive,
                    locateRequest = locateRequest,
                    cameraHold = cameraHold,
                    searchFocus = searchFocus,
                    onOsmFailed = onOsmFailed,
                    tapAnchor = tapPoint,
                    onTap = onMapTap,
                    onAnchorScreen = { tapAnchor.screen = it },
                    onUserPan = {
                        if (cameraHold) {
                            cameraHold = false
                        }
                    }
                )
                NorthIndicator(
                    mapBearingDegrees = 0f,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                )
                Text(
                    text = stringResource(R.string.map_osm_attribution),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = 56.dp)
                        .background(Color.White.copy(alpha = 0.78f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF333333)
                )
                if (TuhuFeature.showMapControls(osmFile.absolutePath) && onTuhuLayers != null) {
                    TuhuLayerButton(
                        options = state.tuhuRenderOptions,
                        hillshadingAvailable = state.tuhuHillshadingAvailable,
                        actions = onTuhuLayers,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 60.dp, bottom = 12.dp)
                    )
                } else if (onOsmLayers != null) {
                    OsmLayerButton(
                        options = state.settings.osmRenderOptions(),
                        hillshadingAvailable = state.osmHillshadingAvailable,
                        actions = onOsmLayers,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 60.dp, bottom = 12.dp)
                    )
                }
            }
        } else if (!state.mapsKeyPresent) {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(
                        if (state.settings.useOfflineMap) R.string.osm_map_unavailable else R.string.map_missing_key
                    ),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            GoogleMapContent(
                state,
                points,
                locateRequest,
                onGoogleMapLayer,
                tapPoint,
                onMapTap,
                onAnchorScreen = { tapAnchor.screen = it }
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (viewingSaved) {
                ClearMapButton(onClick = onClearMap)
            }
            LocateMeButton(
                enabled = state.live.lastLocation != null,
                onClick = {
                    cameraHold = false
                    locateRequest++
                }
            )
            if (state.showingOsmMap) {
                SearchMapButton(
                    onClick = {
                        if (searchOpen) {
                            searchOpen = false
                            searchQuery = ""
                            onClearSearch()
                        } else {
                            searchOpen = true
                        }
                    }
                )
                if (searchOpen) {
                    MapSearchSlot(
                        search = mapSearch,
                        query = searchQuery,
                        onQueryChange = { value ->
                            searchQuery = value
                            onSearchQuery(value)
                        },
                        measurement = state.settings.measurementSystem,
                        onNavigate = { hit ->
                            cameraHold = true
                            distanceTarget = GeoPoint(hit.latitude, hit.longitude)
                            val token = (searchFocus?.token ?: 0) + 1
                            searchFocus = MapSearchFocus(
                                latitude = hit.latitude,
                                longitude = hit.longitude,
                                zoom = hit.kind.zoom(),
                                token = token
                            )
                            searchOpen = false
                            searchQuery = ""
                            onClearSearch()
                        },
                        onClose = {
                            searchOpen = false
                            searchQuery = ""
                            onClearSearch()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 56.dp)
                    )
                }
            }
        }
        val hudMode = MapHudVisibility.mode(
            logging = state.live.logging,
            selectedSessionId = state.selectedSessionId,
            hasFix = state.live.lastLocation != null
        )
        val showCloudPaused = state.settings.showFixCloud && !state.fixCloud.stats.active
        val distancePrefix = stringResource(R.string.map_tap_distance_prefix)
        val liveFix = state.live.lastLocation
        val distanceText = distanceTarget?.let { target ->
            if (liveFix == null) {
                stringResource(R.string.map_tap_no_fix)
            } else {
                TapReadout.formatStraightLine(
                    FixAcceptance.haversineMeters(
                        liveFix.latitude,
                        liveFix.longitude,
                        target.latitude,
                        target.longitude
                    ),
                    state.settings.measurementSystem,
                    distancePrefix
                )
            }
        }
        if (hudMode != MapHudMode.Hidden || showCloudPaused || distanceText != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (showCloudPaused) {
                    Text(
                        text = stringResource(R.string.map_fix_cloud_paused),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
                MapHud(
                    mode = hudMode,
                    speedMps = state.live.lastLocation?.takeIf { it.hasSpeed() }?.speed,
                    system = state.settings.measurementSystem,
                    odometerMeters = state.stats.odometerMeters,
                    elapsedMillis = state.stats.elapsedMillis,
                    accuracyMeters = state.live.lastLocation?.accuracy,
                    satellitesInFix = state.live.gnss?.satellitesInFix ?: 0,
                    satellitesInView = state.live.gnss?.satellitesInView ?: 0,
                    distanceText = distanceText,
                    onClearDistance = { distanceTarget = null }
                )
            }
        }
        TapMenuHost(
            anchor = tapAnchor,
            tapPoint = tapPoint,
            menuOpen = menuOpen,
            coordinateText = coordinateText,
            postalAddress = postalAddress,
            onDistance = {
                distanceTarget = tapPoint
                menuOpen = false
            },
            onCoordinate = {
                val point = tapPoint
                if (point != null) {
                    coordinateText = TapReadout.formatCoordinate(point.latitude, point.longitude)
                    postalAddress = null
                    menuOpen = false
                }
            },
            onAddress = {
                val point = tapPoint
                if (point != null) {
                    postalAddress = MapAddressLookup.lookup(point.latitude, point.longitude)
                    coordinateText = null
                    menuOpen = false
                }
            },
            onClose = {
                coordinateText = null
                postalAddress = null
            }
        )
    }
}

private class TapAnchor {
    var screen by mutableStateOf<Offset?>(null)
}

@Composable
private fun MapSearchSlot(
    search: StateFlow<MapSearchUi>,
    query: String,
    onQueryChange: (String) -> Unit,
    measurement: com.lkovari.mobile.apps.gtl.engine.MeasurementSystem,
    onNavigate: (com.lkovari.mobile.apps.gtl.engine.MapSearchHit) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier
) {
    val ui by search.collectAsStateWithLifecycle()
    MapSearchOverlay(
        query = query,
        onQueryChange = onQueryChange,
        hits = ui.hits,
        indexing = ui.indexing,
        failed = ui.failed,
        truncated = ui.truncated,
        searching = ui.searching,
        measurement = measurement,
        onNavigate = onNavigate,
        onClose = onClose,
        modifier = modifier
    )
}

@Composable
private fun TapMenuHost(
    anchor: TapAnchor,
    tapPoint: GeoPoint?,
    menuOpen: Boolean,
    coordinateText: String?,
    postalAddress: PostalAddress?,
    onDistance: () -> Unit,
    onCoordinate: () -> Unit,
    onAddress: () -> Unit,
    onClose: () -> Unit
) {
    val screen = anchor.screen ?: return
    if (tapPoint == null || (!menuOpen && coordinateText == null && postalAddress == null)) {
        return
    }
    MapTapOverlay(
        screen = screen,
        menuOpen = menuOpen,
        showAddress = MapAddressLookup.supported(),
        coordinateLatitude = if (coordinateText != null) tapPoint.latitude else null,
        coordinateLongitude = if (coordinateText != null) tapPoint.longitude else null,
        address = postalAddress,
        onDistance = onDistance,
        onCoordinate = onCoordinate,
        onAddress = onAddress,
        onClose = onClose
    )
}

@Composable
private fun GoogleMapContent(
    state: GtlUiState,
    points: List<GeoPoint>,
    locateRequest: Int,
    onGoogleMapLayer: (GoogleMapLayer) -> Unit,
    tapAnchor: GeoPoint?,
    onTap: (GeoPoint) -> Unit,
    onAnchorScreen: (Offset) -> Unit
) {
    val live = state.live.lastLocation?.let { LatLng(it.latitude, it.longitude) }
    val start = live
        ?: points.lastOrNull()?.let { LatLng(it.latitude, it.longitude) }
        ?: LatLng(47.4979, 19.0402)
    val camera = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(start, 16f)
    }
    val latLngs = points.map { LatLng(it.latitude, it.longitude) }
    val accuracy = state.live.lastLocation?.accuracy ?: 0f
    val cloudAlpha = if (state.fixCloud.stats.active) 1f else FixCloudPausedAlpha
    Box(modifier = Modifier.fillMaxSize()) {
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = camera,
        properties = MapProperties(mapType = state.settings.googleMapLayer.toComposeType()),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = true,
            compassEnabled = false,
            mapToolbarEnabled = false
        ),
        onMapClick = { latLng ->
            onTap(GeoPoint(latLng.latitude, latLng.longitude))
        }
    ) {
        if (latLngs.size >= 2) {
            Polyline(points = latLngs, color = CarmineTrack, width = 10f)
        }
        val startPoint = TrackEndpoints.start(points)
        if (startPoint != null) {
            val startLatLng = LatLng(startPoint.latitude, startPoint.longitude)
            val startState = remember { MarkerState(startLatLng) }
            SideEffect { startState.position = startLatLng }
            MarkerComposable(
                keys = arrayOf("track-start"),
                state = startState,
                title = stringResource(R.string.map_track_start),
                anchor = Offset(0.5f, 0.5f),
                zIndex = 1f
            ) {
                TrackEndDot(start = true)
            }
        }
        val endPoint = TrackEndpoints.end(points, state.live.logging)
        if (endPoint != null) {
            val endLatLng = LatLng(endPoint.latitude, endPoint.longitude)
            val endState = remember { MarkerState(endLatLng) }
            SideEffect { endState.position = endLatLng }
            MarkerComposable(
                keys = arrayOf("track-end"),
                state = endState,
                title = stringResource(R.string.map_track_end),
                anchor = Offset(0.5f, 0.5f),
                zIndex = 1f
            ) {
                TrackEndDot(start = false)
            }
        }
        val usagePosition = live ?: latLngs.lastOrNull()
        if (usagePosition != null) {
            val markerState = remember { MarkerState(usagePosition) }
            SideEffect { markerState.position = usagePosition }
            MarkerComposable(
                keys = arrayOf(state.mapUsageType, live != null),
                state = markerState,
                rotation = 0f,
                flat = false,
                anchor = Offset(0.5f, 0.5f)
            ) {
                CurrentPositionMarker(
                    usageType = state.mapUsageType,
                    liveFix = live != null
                )
            }
        }
        if (state.settings.showAccuracyMarker && live != null && accuracy > 0f) {
            Circle(
                center = live,
                radius = accuracy.toDouble(),
                fillColor = AccuracyMarkerFill.copy(alpha = AccuracyFillAlpha / 255f),
                strokeColor = AccuracyMarkerBorder,
                strokeWidth = 2f
            )
        }
        if (state.settings.showFixCloud && state.fixCloud.samples.size >= 2) {
            state.fixCloud.samples.forEach { sample ->
                Circle(
                    center = LatLng(sample.latitude, sample.longitude),
                    radius = FixCloudDotRadiusMeters,
                    fillColor = FixCloudDot.copy(alpha = cloudAlpha),
                    strokeColor = FixCloudDot.copy(alpha = cloudAlpha),
                    strokeWidth = 1f
                )
            }
            val stats = state.fixCloud.stats
            val cep = stats.cep95Meters
            val centroidLat = stats.centroidLatitude
            val centroidLon = stats.centroidLongitude
            if (cep != null && centroidLat != null && centroidLon != null) {
                val centroid = LatLng(centroidLat, centroidLon)
                Circle(
                    center = centroid,
                    radius = cep,
                    fillColor = FixCloudCepFill.copy(alpha = (AccuracyFillAlpha / 255f) * cloudAlpha),
                    strokeColor = FixCloudCepStroke.copy(alpha = cloudAlpha),
                    strokeWidth = 2f
                )
                Circle(
                    center = centroid,
                    radius = FixCloudCentroidRadiusMeters,
                    fillColor = FixCloudDot.copy(alpha = cloudAlpha),
                    strokeColor = FixCloudDot.copy(alpha = cloudAlpha),
                    strokeWidth = 1f
                )
            }
        }
    }
    NorthIndicator(
        mapBearingDegrees = camera.position.bearing,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(10.dp)
    )
    MapLayerButton(
        selected = state.settings.googleMapLayer,
        onSelect = onGoogleMapLayer,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 60.dp, bottom = 12.dp)
    )
    }
    LaunchedEffect(tapAnchor, camera.position) {
        val point = tapAnchor ?: return@LaunchedEffect
        val projection = camera.projection ?: return@LaunchedEffect
        val pixels = projection.toScreenLocation(LatLng(point.latitude, point.longitude))
        onAnchorScreen(Offset(pixels.x.toFloat(), pixels.y.toFloat()))
    }
    var centeredOnce by remember { mutableStateOf(false) }
    val keepWhole = state.settings.keepWholeTrackOnScreen
    val viewingSaved = MapCameraMode.finishedTrackOnMap(state.live.logging, points.size)
    val fitTrack = keepWhole || viewingSaved
    val liveLat = if (fitTrack && !state.live.logging) null else live?.latitude
    val liveLon = if (fitTrack && !state.live.logging) null else live?.longitude
    LaunchedEffect(
        liveLat,
        liveLon,
        latLngs.size,
        latLngs.firstOrNull(),
        latLngs.lastOrNull(),
        state.live.logging,
        keepWhole,
        viewingSaved,
        state.selectedSessionId,
        state.mapUsageType
    ) {
        if (fitTrack) {
            val extra = if (state.live.logging) {
                state.live.lastLocation?.let { GeoPoint(it.latitude, it.longitude) }
            } else {
                null
            }
            val bounds = TrackCameraBounds.of(points, extra)
            if (bounds != null) {
                animateToTrackBounds(camera, bounds)
                centeredOnce = true
            }
        } else if (state.live.logging && latLngs.isNotEmpty()) {
            val last = latLngs.last()
            camera.animate(CameraUpdateFactory.newLatLngZoom(last, 16f))
            centeredOnce = true
        } else if (!centeredOnce && live != null) {
            camera.animate(CameraUpdateFactory.newLatLngZoom(live, 16f))
            centeredOnce = true
        } else if (!centeredOnce && latLngs.isNotEmpty()) {
            val last = latLngs.last()
            camera.animate(CameraUpdateFactory.newLatLngZoom(last, 16f))
            centeredOnce = true
        }
    }
    LaunchedEffect(locateRequest) {
        if (locateRequest == 0) {
            return@LaunchedEffect
        }
        val target = live ?: return@LaunchedEffect
        camera.animate(CameraUpdateFactory.newLatLng(target))
    }
}

private suspend fun animateToTrackBounds(camera: CameraPositionState, bounds: LatLonBounds) {
    val southWest = LatLng(bounds.minLatitude, bounds.minLongitude)
    if (bounds.isDegenerate) {
        camera.animate(CameraUpdateFactory.newLatLngZoom(southWest, 16f))
        return
    }
    val latLngBounds = LatLngBounds(southWest, LatLng(bounds.maxLatitude, bounds.maxLongitude))
    try {
        camera.animate(CameraUpdateFactory.newLatLngBounds(latLngBounds, 80))
    } catch (_: IllegalStateException) {
        camera.animate(CameraUpdateFactory.newLatLngZoom(latLngBounds.center, 16f))
    }
}

@Composable
private fun TrackEndDot(start: Boolean) {
    val fill = if (start) GnssLime else CarmineTrack
    Box(
        modifier = Modifier
            .size(20.dp)
            .background(fill, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (start) "S" else "E",
            color = MoonCream,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CurrentPositionMarker(usageType: UsageType, liveFix: Boolean) {
    val glyph = if (liveFix) 32.dp else 24.dp
    Box(
        modifier = Modifier.size(glyph),
        contentAlignment = Alignment.Center
    ) {
        if (liveFix) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLiveFixReticle()
            }
        }
        Icon(
            imageVector = usageIcon(usageType),
            contentDescription = null,
            tint = UsageMarkerRed,
            modifier = Modifier.size(if (liveFix) 18.dp else 24.dp)
        )
    }
}

@Composable
private fun ClearMapButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(40.dp)
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                CircleShape
            )
    ) {
        Icon(
            imageVector = Icons.Filled.CleaningServices,
            contentDescription = stringResource(R.string.map_clear_track),
            tint = UsageMarkerRed,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun SearchMapButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(40.dp)
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                CircleShape
            )
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = stringResource(R.string.map_search),
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun LocateMeButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .size(40.dp)
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                CircleShape
            )
            .border(
                width = 1.5.dp,
                color = if (enabled) HudCyan else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f),
                shape = CircleShape
            )
    ) {
        Icon(
            imageVector = Icons.Filled.GpsFixed,
            contentDescription = stringResource(R.string.map_locate_me),
            tint = if (enabled) HudCyan else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun NorthIndicator(mapBearingDegrees: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(40.dp)
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                CircleShape
            )
            .rotate(-mapBearingDegrees),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Navigation,
            contentDescription = stringResource(R.string.map_north),
            tint = UsageMarkerRed,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = "N",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 3.dp)
        )
    }
}

@Composable
private fun MapLayerButton(
    selected: GoogleMapLayer,
    onSelect: (GoogleMapLayer) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier
                .size(40.dp)
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Filled.Layers,
                contentDescription = stringResource(R.string.map_layers),
                tint = UsageMarkerRed,
                modifier = Modifier.size(22.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            GoogleMapLayer.entries.forEach { layer ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(layer.labelRes()),
                            fontWeight = if (layer == selected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onSelect(layer)
                        expanded = false
                    },
                    trailingIcon = if (layer == selected) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null
                            )
                        }
                    } else {
                        null
                    }
                )
            }
        }
    }
}

@Composable
private fun OsmLayerButton(
    options: OsmRenderOptions,
    hillshadingAvailable: Boolean,
    actions: OsmLayerActions,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier
                .size(40.dp)
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Filled.Layers,
                contentDescription = stringResource(R.string.map_layers),
                tint = UsageMarkerRed,
                modifier = Modifier.size(22.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            OsmLayerControls(
                options = options,
                hillshadingAvailable = hillshadingAvailable,
                labelStyle = MaterialTheme.typography.bodyMedium,
                switchScale = 0.75f,
                actions = actions,
                modifier = Modifier
                    .width(280.dp)
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun TuhuLayerButton(
    options: TuhuRenderOptions,
    hillshadingAvailable: Boolean,
    actions: TuhuLayerActions,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier
                .size(40.dp)
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Filled.Layers,
                contentDescription = stringResource(R.string.map_layers),
                tint = UsageMarkerRed,
                modifier = Modifier.size(22.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            TuhuLayerControls(
                options = options,
                hillshadingAvailable = hillshadingAvailable,
                labelStyle = MaterialTheme.typography.bodyMedium,
                switchScale = 0.75f,
                actions = actions,
                modifier = Modifier
                    .width(280.dp)
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

private fun GoogleMapLayer.toComposeType(): MapType {
    return when (this) {
        GoogleMapLayer.NORMAL -> MapType.NORMAL
        GoogleMapLayer.SATELLITE -> MapType.SATELLITE
        GoogleMapLayer.HYBRID -> MapType.HYBRID
        GoogleMapLayer.TERRAIN -> MapType.TERRAIN
    }
}

private fun GoogleMapLayer.labelRes(): Int {
    return when (this) {
        GoogleMapLayer.NORMAL -> R.string.map_layer_normal
        GoogleMapLayer.SATELLITE -> R.string.map_layer_satellite
        GoogleMapLayer.HYBRID -> R.string.map_layer_hybrid
        GoogleMapLayer.TERRAIN -> R.string.map_layer_terrain
    }
}

private class GtlOsmMapView(context: Context) : MapView(context) {
    var mapActive: Boolean = false
    var onMapTap: ((Double, Double) -> Unit)? = null
    var onAnchorScreen: ((Float, Float) -> Unit)? = null
    var onUserPan: (() -> Unit)? = null
    var suppressUserPan: Boolean = false
    var anchorLatitude: Double? = null
    var anchorLongitude: Double? = null

    init {
        addInputListener(object : InputListener {
            override fun onMoveEvent() {
                publishAnchor()
                if (!suppressUserPan) {
                    onUserPan?.invoke()
                }
                if (mapActive) {
                    repaint()
                }
            }

            override fun onZoomEvent() {
                publishAnchor()
                requestVisibleTiles()
            }
        })
    }

    fun publishAnchor() {
        val latitude = anchorLatitude ?: return
        val longitude = anchorLongitude ?: return
        val pixels = mapViewProjection.toPixels(LatLong(latitude, longitude)) ?: return
        onAnchorScreen?.invoke(pixels.x.toFloat(), pixels.y.toFloat())
    }

    override fun repaint() {
        super.repaint()
        if (OsmMapViewRedraw.mustPostAncestorInvalidate(Looper.myLooper() == Looper.getMainLooper())) {
            post { invalidateComposeParents() }
        } else {
            invalidateComposeParents()
        }
    }

    private fun invalidateComposeParents() {
        var ancestor = parent
        while (ancestor is View) {
            ancestor.invalidate()
            ancestor = ancestor.parent
        }
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        if (OsmMapViewRedraw.shouldRedrawLayers(width, height, oldWidth, oldHeight)) {
            post { requestVisibleTiles() }
        }
    }

    fun requestVisibleTiles() {
        if (!OsmMapViewRedraw.shouldRequestTiles(width, height)) {
            return
        }
        layerManager.redrawLayers()
        repaint()
    }
}

private class OsmMapOverlays {
    var polyline: ForgePolyline? = null
    var accuracy: ForgeCircle? = null
    var cloud: FixCloudLayer? = null
    var usage: UsagePositionLayer? = null
    var ends: TrackEndsLayer? = null
    var renderer: TileRendererLayer? = null
    var tileCache: TileCache? = null
    var lastRenderOptions: OsmRenderOptions? = null
    var lastTuhuRenderOptions: TuhuRenderOptions? = null
    var usageBitmapType: UsageType? = null
    var usageLiveFix: Boolean? = null
    var didInitialCenter = false
    var lastFitKey: String? = null
    var layersReady = false
    var mapBounds: LatLonBounds? = null
    var mapStart: GeoPoint? = null
    var mapStartZoom: Int? = null
    var lastLocateRequest: Int = 0
    var lastFocusToken: Int = 0
}

private data class MapSearchFocus(
    val latitude: Double,
    val longitude: Double,
    val zoom: Int,
    val token: Int
)

@Composable
private fun OsmMapView(
    filePath: String,
    points: List<GeoPoint>,
    location: Location?,
    showAccuracyMarker: Boolean,
    logging: Boolean,
    keepWholeTrack: Boolean,
    viewingSaved: Boolean,
    showFixCloud: Boolean,
    fixCloud: FixCloudSnapshot,
    usageType: UsageType,
    osmRenderOptions: OsmRenderOptions,
    tuhuRenderOptions: TuhuRenderOptions,
    mapActive: Boolean,
    locateRequest: Int,
    cameraHold: Boolean,
    searchFocus: MapSearchFocus?,
    onOsmFailed: () -> Unit,
    tapAnchor: GeoPoint?,
    onTap: (GeoPoint) -> Unit,
    onAnchorScreen: (Offset) -> Unit,
    onUserPan: () -> Unit
) {
    val overlays = remember(filePath) { OsmMapOverlays() }
    val usageBitmap = rememberUsageMarkerBitmap(usageType, liveFix = location != null)
    key(filePath) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val mapView = GtlOsmMapView(ctx)
                mapView.mapScaleBar.isVisible = true
                mapView.setBuiltInZoomControls(true)
                mapView.setZoomLevelMin(MapFitZoom.Min.toByte())
                mapView.setZoomLevelMax(MapFitZoom.Max.toByte())
                try {
                    attachOsmLayers(mapView, overlays, filePath, osmRenderOptions, tuhuRenderOptions)
                    applyOsmMapCamera(mapView, overlays, location, points)
                    overlays.didInitialCenter = true
                    mapView.post { mapView.requestVisibleTiles() }
                } catch (error: Throwable) {
                    AppErrorLog.record("map.render", error)
                    Handler(Looper.getMainLooper()).post { onOsmFailed() }
                }
                mapView
            },
            update = { view ->
                val mapView = view as? GtlOsmMapView ?: return@AndroidView
                mapView.mapActive = mapActive
                mapView.onMapTap = { latitude, longitude -> onTap(GeoPoint(latitude, longitude)) }
                mapView.onAnchorScreen = { x, y -> onAnchorScreen(Offset(x, y)) }
                mapView.onUserPan = onUserPan
                mapView.anchorLatitude = tapAnchor?.latitude
                mapView.anchorLongitude = tapAnchor?.longitude
                if (!overlays.layersReady) {
                    return@AndroidView
                }
                applyOsmRenderOptions(mapView, overlays, filePath, osmRenderOptions, tuhuRenderOptions)
                if (!mapActive) {
                    return@AndroidView
                }
                val focus = searchFocus
                if (focus != null && focus.token != overlays.lastFocusToken) {
                    overlays.lastFocusToken = focus.token
                    mapView.suppressUserPan = true
                    mapView.model.mapViewPosition.setCenter(LatLong(focus.latitude, focus.longitude))
                    mapView.model.mapViewPosition.setZoomLevel(focus.zoom.toByte(), false)
                    mapView.post { mapView.suppressUserPan = false }
                    overlays.didInitialCenter = true
                }
                if (locateRequest != overlays.lastLocateRequest) {
                    overlays.lastLocateRequest = locateRequest
                    val target = OsmMapCamera.locateCenter(location?.latitude, location?.longitude)
                    if (target != null) {
                        mapView.suppressUserPan = true
                        mapView.model.mapViewPosition.center = LatLong(target.latitude, target.longitude)
                        mapView.post { mapView.suppressUserPan = false }
                    }
                }
                val cameraMode = MapCameraMode.of(logging, keepWholeTrack, viewingSaved)
                if (!cameraHold && cameraMode == MapCameraMode.FitTrack) {
                    val extra = if (logging) {
                        location?.takeIf { loc ->
                            val bounds = overlays.mapBounds
                            bounds != null && OsmMapCamera.contains(bounds, loc.latitude, loc.longitude)
                        }?.let { GeoPoint(it.latitude, it.longitude) }
                    } else {
                        null
                    }
                    val fitKey = osmFitKey(keepWholeTrack || viewingSaved, logging, points, location)
                    if (fitKey != overlays.lastFitKey) {
                        val bounds = TrackCameraBounds.of(points, extra)
                        if (bounds != null && fitOsmToBounds(mapView, bounds)) {
                            overlays.lastFitKey = fitKey
                            overlays.didInitialCenter = true
                        }
                    }
                } else if (!cameraHold && cameraMode == MapCameraMode.FollowLive) {
                    followOsmLiveCamera(mapView, overlays, location, points, logging)
                }
                updateOsmTrack(overlays, points)
                updateOsmTrackEnds(overlays, points, logging)
                updateOsmAccuracy(mapView, overlays, location, showAccuracyMarker)
                updateOsmFixCloud(overlays, showFixCloud, fixCloud)
                updateOsmUsage(mapView, overlays, location, points, usageType, usageBitmap)
                mapView.requestVisibleTiles()
            },
            onRelease = { view ->
                overlays.usage?.bitmap?.decrementRefCount()
                overlays.usage?.bitmap = null
                overlays.usageBitmapType = null
                overlays.usageLiveFix = null
                overlays.polyline = null
                overlays.accuracy = null
                overlays.cloud = null
                overlays.ends = null
                overlays.usage = null
                overlays.renderer = null
                overlays.tileCache = null
                overlays.lastRenderOptions = null
                overlays.lastTuhuRenderOptions = null
                overlays.layersReady = false
                overlays.lastLocateRequest = 0
                overlays.lastFocusToken = 0
                try {
                    view.destroyAll()
                } catch (error: Throwable) {
                    AppErrorLog.record("map.destroy", error)
                }
            }
        )
    }
}

private fun attachOsmLayers(
    mapView: GtlOsmMapView,
    overlays: OsmMapOverlays,
    filePath: String,
    options: OsmRenderOptions,
    tuhuOptions: TuhuRenderOptions
) {
    val tileCache = AndroidUtil.createTileCache(
        mapView.context,
        "gtl-osm",
        mapView.model.displayModel.tileSize,
        1f,
        mapView.model.frameBufferModel.overdrawFactor
    )
    val mapFile = MapFile(File(filePath))
    val renderer = TileRendererLayer(
        tileCache,
        mapFile,
        mapView.model.mapViewPosition,
        AndroidGraphicFactory.INSTANCE
    )
    applyOsmXmlTheme(mapView, renderer, filePath, options, tuhuOptions)
    mapView.layerManager.layers.add(renderer)
    overlays.renderer = renderer
    overlays.tileCache = tileCache
    overlays.lastRenderOptions = options
    overlays.lastTuhuRenderOptions = tuhuOptions
    val graphic = AndroidGraphicFactory.INSTANCE
    val stroke = graphic.createPaint()
    stroke.setColor(graphic.createColor(255, 0xC1, 0x3B, 0x2E))
    stroke.setStyle(Style.STROKE)
    stroke.strokeWidth = 10f
    val polyline = ForgePolyline(stroke, graphic)
    mapView.layerManager.layers.add(polyline)
    overlays.polyline = polyline
    val cloud = FixCloudLayer()
    mapView.layerManager.layers.add(cloud)
    overlays.cloud = cloud
    val ends = TrackEndsLayer()
    mapView.layerManager.layers.add(ends)
    overlays.ends = ends
    val usage = UsagePositionLayer()
    mapView.layerManager.layers.add(usage)
    overlays.usage = usage
    mapView.layerManager.layers.add(MapTapLayer(mapView))
    val box = mapFile.boundingBox()
    overlays.mapBounds = LatLonBounds(box.minLatitude, box.minLongitude, box.maxLatitude, box.maxLongitude)
    val start = mapFile.startPosition()
    overlays.mapStart = GeoPoint(start.latitude, start.longitude)
    overlays.mapStartZoom = MapFitZoom.clamp((mapFile.startZoomLevel() ?: 8).toInt())
    overlays.layersReady = true
}

private fun applyOsmXmlTheme(
    mapView: GtlOsmMapView,
    renderer: TileRendererLayer,
    filePath: String,
    options: OsmRenderOptions,
    tuhuOptions: TuhuRenderOptions
) {
    try {
        val theme = if (TuhuFeature.isActive(filePath)) {
            TuhuRenderTheme.create(
                mapView.context.assets,
                tuhuOptions,
                File(mapView.context.filesDir, "tuhu/theme.xml")
            )
        } else {
            OsmRenderTheme.create(mapView.context.assets, options)
        }
        renderer.setXmlRenderTheme(theme)
    } catch (error: Throwable) {
        AppErrorLog.record("map.theme", error)
        renderer.setXmlRenderTheme(MapsforgeThemes.DEFAULT)
    }
}

private fun applyOsmRenderOptions(
    mapView: GtlOsmMapView,
    overlays: OsmMapOverlays,
    filePath: String,
    options: OsmRenderOptions,
    tuhuOptions: TuhuRenderOptions
) {
    val renderer = overlays.renderer ?: return
    val tuhuActive = TuhuFeature.isActive(filePath)
    val unchanged = if (tuhuActive) {
        overlays.lastTuhuRenderOptions == tuhuOptions
    } else {
        overlays.lastRenderOptions == options
    }
    if (unchanged) {
        return
    }
    applyOsmXmlTheme(mapView, renderer, filePath, options, tuhuOptions)
    overlays.tileCache?.purge()
    overlays.lastRenderOptions = options
    overlays.lastTuhuRenderOptions = tuhuOptions
    mapView.requestVisibleTiles()
}

private fun applyOsmMapCamera(
    mapView: GtlOsmMapView,
    overlays: OsmMapOverlays,
    location: Location?,
    points: List<GeoPoint>
) {
    val bounds = overlays.mapBounds ?: return
    val start = overlays.mapStart ?: return
    val locLat = location?.latitude ?: points.lastOrNull()?.latitude
    val locLon = location?.longitude ?: points.lastOrNull()?.longitude
    val center = OsmMapCamera.initialCenter(
        bounds,
        start.latitude,
        start.longitude,
        locLat,
        locLon
    )
    mapView.model.mapViewPosition.setCenter(LatLong(center.latitude, center.longitude))
    val gpsInside = locLat != null && locLon != null && OsmMapCamera.contains(bounds, locLat, locLon)
    val zoom = OsmMapCamera.initialZoom(gpsInside, overlays.mapStartZoom ?: 8)
    mapView.model.mapViewPosition.setZoomLevel(zoom.toByte(), false)
}

private fun followOsmLiveCamera(
    mapView: GtlOsmMapView,
    overlays: OsmMapOverlays,
    location: Location?,
    points: List<GeoPoint>,
    logging: Boolean
) {
    if (!overlays.didInitialCenter) {
        applyOsmMapCamera(mapView, overlays, location, points)
        overlays.didInitialCenter = true
        return
    }
    val last = points.lastOrNull()
    val bounds = overlays.mapBounds ?: return
    val follow = OsmMapCamera.followCenter(
        bounds,
        logging && last != null,
        last?.latitude,
        last?.longitude,
        location?.latitude,
        location?.longitude
    ) ?: return
    mapView.model.mapViewPosition.center = LatLong(follow.latitude, follow.longitude)
}

private fun osmFitKey(
    keepWholeTrack: Boolean,
    logging: Boolean,
    points: List<GeoPoint>,
    location: Location?
): String {
    val first = points.firstOrNull()
    val last = points.lastOrNull()
    return if (keepWholeTrack && logging) {
        "${points.size}:${last?.latitude}:${last?.longitude}:${location?.latitude}:${location?.longitude}"
    } else {
        "${points.size}:${first?.latitude}:${first?.longitude}:${last?.latitude}:${last?.longitude}"
    }
}

private fun fitOsmToBounds(mapView: MapView, bounds: LatLonBounds): Boolean {
    val center = LatLong(
        (bounds.minLatitude + bounds.maxLatitude) / 2.0,
        (bounds.minLongitude + bounds.maxLongitude) / 2.0
    )
    val dimension = mapView.model.mapViewDimension.dimension
    val width = dimension?.width
    val height = dimension?.height
    if (!MapFitZoom.canFit(width, height) || dimension == null) {
        mapView.model.mapViewPosition.center = center
        return false
    }
    if (bounds.isDegenerate) {
        mapView.model.mapViewPosition.center = center
        mapView.model.mapViewPosition.setZoomLevel(16.toByte(), false)
        return true
    }
    return try {
        val box = BoundingBox(
            bounds.minLatitude,
            bounds.minLongitude,
            bounds.maxLatitude,
            bounds.maxLongitude
        )
        val zoom = MapFitZoom.clamp(
            LatLongUtils.zoomForBounds(dimension, box, mapView.model.displayModel.tileSize).toInt()
        ).toByte()
        mapView.model.mapViewPosition.setMapPosition(MapPosition(center, zoom), false)
        true
    } catch (_: RuntimeException) {
        mapView.model.mapViewPosition.center = center
        false
    }
}

private fun updateOsmTrack(overlays: OsmMapOverlays, points: List<GeoPoint>) {
    val latLongs = points.map { LatLong(it.latitude, it.longitude) }
    overlays.polyline?.setPoints(latLongs)
    overlays.polyline?.requestRedraw()
}

private fun updateOsmTrackEnds(overlays: OsmMapOverlays, points: List<GeoPoint>, logging: Boolean) {
    val layer = overlays.ends ?: return
    val start = TrackEndpoints.start(points)
    val end = TrackEndpoints.end(points, logging)
    layer.startLatitude = start?.latitude
    layer.startLongitude = start?.longitude
    layer.endLatitude = end?.latitude
    layer.endLongitude = end?.longitude
    layer.requestRedraw()
}

private fun updateOsmAccuracy(
    mapView: MapView,
    overlays: OsmMapOverlays,
    location: Location?,
    showAccuracyMarker: Boolean
) {
    if (location == null || !showAccuracyMarker || location.accuracy <= 0f) {
        overlays.accuracy?.isVisible = false
        overlays.accuracy?.requestRedraw()
        return
    }
    val latLong = LatLong(location.latitude, location.longitude)
    val accuracy = overlays.accuracy
    if (accuracy == null) {
        val graphic = AndroidGraphicFactory.INSTANCE
        val fill = graphic.createPaint()
        fill.setColor(graphic.createColor(AccuracyFillAlpha, 0x66, 0x66, 0xFF))
        fill.setStyle(Style.FILL)
        val stroke = graphic.createPaint()
        stroke.setColor(graphic.createColor(255, 0x14, 0x14, 0xFC))
        stroke.setStyle(Style.STROKE)
        stroke.strokeWidth = 2f
        val accuracyCircle = ForgeCircle(latLong, location.accuracy, fill, stroke)
        mapView.layerManager.layers.add(accuracyCircle)
        overlays.accuracy = accuracyCircle
        overlays.usage?.let { usage ->
            mapView.layerManager.layers.remove(usage)
            mapView.layerManager.layers.add(usage)
        }
    } else {
        accuracy.setLatLong(latLong)
        accuracy.setRadius(location.accuracy)
        accuracy.isVisible = true
        accuracy.requestRedraw()
    }
}

private fun updateOsmFixCloud(
    overlays: OsmMapOverlays,
    showFixCloud: Boolean,
    snapshot: FixCloudSnapshot
) {
    val layer = overlays.cloud ?: return
    layer.enabled = showFixCloud
    layer.samples = snapshot.samples
    layer.stats = snapshot.stats
    layer.requestRedraw()
}

private fun updateOsmUsage(
    mapView: MapView,
    overlays: OsmMapOverlays,
    location: Location?,
    points: List<GeoPoint>,
    usageType: UsageType,
    androidBitmap: android.graphics.Bitmap
) {
    val layer = overlays.usage ?: return
    val liveFix = location != null
    if (overlays.usageBitmapType != usageType || overlays.usageLiveFix != liveFix) {
        layer.bitmap?.decrementRefCount()
        val drawable = BitmapDrawable(mapView.resources, androidBitmap)
        val forgeBitmap = AndroidGraphicFactory.convertToBitmap(drawable)
        forgeBitmap.incrementRefCount()
        layer.bitmap = forgeBitmap
        overlays.usageBitmapType = usageType
        overlays.usageLiveFix = liveFix
    }
    val lat = location?.latitude ?: points.lastOrNull()?.latitude
    val lon = location?.longitude ?: points.lastOrNull()?.longitude
    if (lat == null || lon == null) {
        layer.latitude = null
        layer.longitude = null
        layer.requestRedraw()
        return
    }
    layer.latitude = lat
    layer.longitude = lon
    layer.requestRedraw()
}

private class MapTapLayer(
    private val host: GtlOsmMapView
) : Layer() {
    override fun draw(
        boundingBox: BoundingBox,
        zoomLevel: Byte,
        canvas: Canvas,
        topLeftPoint: Point,
        _rotation: Rotation
    ) {
    }

    override fun onTap(tapLatLong: LatLong, layerXY: Point?, tapXY: Point): Boolean {
        val listener = host.onMapTap ?: return false
        listener.invoke(tapLatLong.latitude, tapLatLong.longitude)
        host.onAnchorScreen?.invoke(tapXY.x.toFloat(), tapXY.y.toFloat())
        return true
    }
}

private class UsagePositionLayer : Layer() {
    var latitude: Double? = null
    var longitude: Double? = null
    var bitmap: ForgeBitmap? = null

    override fun draw(
        boundingBox: BoundingBox,
        zoomLevel: Byte,
        canvas: Canvas,
        topLeftPoint: Point,
        _rotation: Rotation
    ) {
        val lat = latitude
        val lon = longitude
        val bmp = bitmap
        if (lat == null || lon == null || bmp == null || bmp.isDestroyed) {
            return
        }
        val mapSize = MercatorProjection.getMapSize(zoomLevel, displayModel.tileSize)
        val pixelX = MercatorProjection.longitudeToPixelX(lon, mapSize) - topLeftPoint.x
        val pixelY = MercatorProjection.latitudeToPixelY(lat, mapSize) - topLeftPoint.y
        val left = (pixelX - bmp.width / 2.0).toInt()
        val top = (pixelY - bmp.height / 2.0).toInt()
        canvas.drawBitmap(bmp, left, top)
    }
}

private class TrackEndsLayer : Layer() {
    var startLatitude: Double? = null
    var startLongitude: Double? = null
    var endLatitude: Double? = null
    var endLongitude: Double? = null

    override fun draw(
        boundingBox: BoundingBox,
        zoomLevel: Byte,
        canvas: Canvas,
        topLeftPoint: Point,
        _rotation: Rotation
    ) {
        val graphic = AndroidGraphicFactory.INSTANCE
        val mapSize = MercatorProjection.getMapSize(zoomLevel, displayModel.tileSize)
        val startLat = startLatitude
        val startLon = startLongitude
        if (startLat != null && startLon != null) {
            drawEndDot(canvas, graphic, mapSize, topLeftPoint, startLat, startLon, 111, 175, 78)
        }
        val endLat = endLatitude
        val endLon = endLongitude
        if (endLat != null && endLon != null) {
            drawEndDot(canvas, graphic, mapSize, topLeftPoint, endLat, endLon, 193, 59, 46)
        }
    }

    private fun drawEndDot(
        canvas: Canvas,
        graphic: org.mapsforge.core.graphics.GraphicFactory,
        mapSize: Long,
        topLeftPoint: Point,
        latitude: Double,
        longitude: Double,
        red: Int,
        green: Int,
        blue: Int
    ) {
        val fill = graphic.createPaint()
        fill.setColor(graphic.createColor(255, red, green, blue))
        fill.setStyle(Style.FILL)
        val pixelX = MercatorProjection.longitudeToPixelX(longitude, mapSize) - topLeftPoint.x
        val pixelY = MercatorProjection.latitudeToPixelY(latitude, mapSize) - topLeftPoint.y
        canvas.drawCircle(pixelX.toInt(), pixelY.toInt(), 12, fill)
    }
}

private class FixCloudLayer : Layer() {
    var enabled: Boolean = false
    var samples: List<FixCloudSample> = emptyList()
    var stats: FixCloudStats = FixCloudStats.Empty

    override fun draw(
        boundingBox: BoundingBox,
        zoomLevel: Byte,
        canvas: Canvas,
        topLeftPoint: Point,
        _rotation: Rotation
    ) {
        if (!enabled || samples.size < 2) {
            return
        }
        val graphic = AndroidGraphicFactory.INSTANCE
        val mapSize = MercatorProjection.getMapSize(zoomLevel, displayModel.tileSize)
        val alpha = if (stats.active) 255 else (255 * FixCloudPausedAlpha).toInt()
        val fill = graphic.createPaint()
        fill.setColor(graphic.createColor(alpha, 0xF4, 0x8F, 0xB1))
        fill.setStyle(Style.FILL)
        samples.forEach { sample ->
            drawMetersCircle(
                canvas,
                topLeftPoint,
                mapSize,
                sample.latitude,
                sample.longitude,
                FixCloudDotRadiusMeters.toFloat(),
                fill,
                null
            )
        }
        val cep = stats.cep95Meters
        val centroidLat = stats.centroidLatitude
        val centroidLon = stats.centroidLongitude
        if (cep != null && centroidLat != null && centroidLon != null) {
            val cepFill = graphic.createPaint()
            cepFill.setColor(graphic.createColor((AccuracyFillAlpha * alpha) / 255, 0xE9, 0x1E, 0x63))
            cepFill.setStyle(Style.FILL)
            val cepStroke = graphic.createPaint()
            cepStroke.setColor(graphic.createColor(alpha, 0xC2, 0x18, 0x5B))
            cepStroke.setStyle(Style.STROKE)
            cepStroke.strokeWidth = 2f
            drawMetersCircle(
                canvas,
                topLeftPoint,
                mapSize,
                centroidLat,
                centroidLon,
                cep.toFloat(),
                cepFill,
                cepStroke
            )
            drawMetersCircle(
                canvas,
                topLeftPoint,
                mapSize,
                centroidLat,
                centroidLon,
                FixCloudCentroidRadiusMeters.toFloat(),
                fill,
                null
            )
        }
    }

    private fun drawMetersCircle(
        canvas: Canvas,
        topLeftPoint: Point,
        mapSize: Long,
        latitude: Double,
        longitude: Double,
        radiusMeters: Float,
        fill: org.mapsforge.core.graphics.Paint,
        stroke: org.mapsforge.core.graphics.Paint?
    ) {
        val pixelX = MercatorProjection.longitudeToPixelX(longitude, mapSize) - topLeftPoint.x
        val pixelY = MercatorProjection.latitudeToPixelY(latitude, mapSize) - topLeftPoint.y
        val radiusPx = radiusMeters / MercatorProjection.calculateGroundResolution(latitude, mapSize)
        val x = pixelX.toInt()
        val y = pixelY.toInt()
        val r = radiusPx.toInt().coerceAtLeast(1)
        canvas.drawCircle(x, y, r, fill)
        if (stroke != null) {
            canvas.drawCircle(x, y, r, stroke)
        }
    }
}
