package com.lkovari.mobile.apps.gtl.ui.screens

import android.graphics.drawable.BitmapDrawable
import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.lkovari.mobile.apps.gtl.R
import com.lkovari.mobile.apps.gtl.engine.FixCloudSample
import com.lkovari.mobile.apps.gtl.engine.FixCloudSnapshot
import com.lkovari.mobile.apps.gtl.engine.FixCloudStats
import com.lkovari.mobile.apps.gtl.engine.GeoPoint
import com.lkovari.mobile.apps.gtl.engine.LatLonBounds
import com.lkovari.mobile.apps.gtl.engine.TrackCameraBounds
import com.lkovari.mobile.apps.gtl.engine.UsageType
import com.lkovari.mobile.apps.gtl.engine.MapHudMode
import com.lkovari.mobile.apps.gtl.engine.MapHudVisibility
import com.lkovari.mobile.apps.gtl.ui.components.MapHud
import com.lkovari.mobile.apps.gtl.ui.rememberUsageMarkerBitmap
import com.lkovari.mobile.apps.gtl.ui.theme.AccuracyMarkerBorder
import com.lkovari.mobile.apps.gtl.ui.theme.AccuracyMarkerFill
import com.lkovari.mobile.apps.gtl.ui.theme.CarmineTrack
import com.lkovari.mobile.apps.gtl.ui.theme.FixCloudCepFill
import com.lkovari.mobile.apps.gtl.ui.theme.FixCloudCepStroke
import com.lkovari.mobile.apps.gtl.ui.theme.FixCloudDot
import com.lkovari.mobile.apps.gtl.ui.theme.UsageMarkerRed
import com.lkovari.mobile.apps.gtl.ui.usageIcon
import com.lkovari.mobile.apps.gtl.viewmodel.GtlUiState
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
import org.mapsforge.map.layer.Layer
import org.mapsforge.map.layer.overlay.Circle as ForgeCircle
import org.mapsforge.map.layer.overlay.Polyline as ForgePolyline
import org.mapsforge.map.layer.renderer.TileRendererLayer
import org.mapsforge.map.reader.MapFile
import org.mapsforge.map.rendertheme.internal.MapsforgeThemes
import java.io.FileInputStream

private const val AccuracyFillAlpha = 64
private const val FixCloudDotRadiusMeters = 1.25
private const val FixCloudCentroidRadiusMeters = 2.0
private const val FixCloudPausedAlpha = 0.35f

@Composable
fun MapPane(state: GtlUiState, onClearMap: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        val points = state.displayPoints
        if (state.settings.useOfflineMap && state.osmFile != null) {
            Box(modifier = Modifier.fillMaxSize()) {
                OsmMapView(
                    filePath = state.osmFile.absolutePath,
                    points = points,
                    location = state.live.lastLocation,
                    showAccuracyMarker = state.settings.showAccuracyMarker,
                    logging = state.live.logging,
                    keepWholeTrack = state.settings.keepWholeTrackOnScreen,
                    viewingSaved = state.selectedSessionId != null && !state.live.logging,
                    showFixCloud = state.settings.showFixCloud,
                    fixCloud = state.fixCloud,
                    usageType = state.mapUsageType
                )
                NorthIndicator(
                    mapBearingDegrees = 0f,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                )
            }
        } else if (!state.mapsKeyPresent) {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.map_missing_key), style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            GoogleMapContent(state, points)
        }
        if (state.selectedSessionId != null && !state.live.logging) {
            ClearMapButton(
                onClick = onClearMap,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
            )
        }
        val hudMode = MapHudVisibility.mode(
            logging = state.live.logging,
            selectedSessionId = state.selectedSessionId,
            hasFix = state.live.lastLocation != null
        )
        val showCloudPaused = state.settings.showFixCloud && !state.fixCloud.stats.active
        if (hudMode != MapHudMode.Hidden || showCloudPaused) {
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
                    speedMps = state.live.lastLocation?.speed,
                    system = state.settings.measurementSystem,
                    odometerMeters = state.stats.odometerMeters,
                    elapsedMillis = state.stats.elapsedMillis,
                    accuracyMeters = state.live.lastLocation?.accuracy,
                    satellitesInFix = state.live.gnss?.satellitesInFix ?: 0,
                    satellitesInView = state.live.gnss?.satellitesInView ?: 0
                )
            }
        }
    }
}

@Composable
private fun GoogleMapContent(state: GtlUiState, points: List<GeoPoint>) {
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
        uiSettings = MapUiSettings(zoomControlsEnabled = true, compassEnabled = false)
    ) {
        if (latLngs.size >= 2) {
            Polyline(points = latLngs, color = CarmineTrack, width = 10f)
        }
        val usagePosition = live ?: latLngs.lastOrNull()
        if (usagePosition != null) {
            val markerState = remember { MarkerState(usagePosition) }
            SideEffect { markerState.position = usagePosition }
            MarkerComposable(
                keys = arrayOf(state.mapUsageType),
                state = markerState,
                rotation = 0f,
                flat = false,
                anchor = Offset(0.5f, 0.5f)
            ) {
                Icon(
                    imageVector = usageIcon(state.mapUsageType),
                    contentDescription = null,
                    tint = UsageMarkerRed,
                    modifier = Modifier.size(24.dp)
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
    }
    var centeredOnce by remember { mutableStateOf(false) }
    val keepWhole = state.settings.keepWholeTrackOnScreen
    val viewingSaved = state.selectedSessionId != null && !state.live.logging
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

private class OsmMapOverlays {
    var polyline: ForgePolyline? = null
    var accuracy: ForgeCircle? = null
    var cloud: FixCloudLayer? = null
    var usage: UsagePositionLayer? = null
    var usageBitmapType: UsageType? = null
    var didInitialCenter = false
    var lastFitKey: String? = null
}

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
    usageType: UsageType
) {
    val overlays = remember(filePath) { OsmMapOverlays() }
    val usageBitmap = rememberUsageMarkerBitmap(usageType)
    key(filePath) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val mapView = MapView(ctx)
                mapView.mapScaleBar.isVisible = true
                mapView.setBuiltInZoomControls(true)
                val tileCache = AndroidUtil.createTileCache(
                    ctx,
                    "gtl-osm",
                    mapView.model.displayModel.tileSize,
                    1f,
                    mapView.model.frameBufferModel.overdrawFactor
                )
                val mapFile = MapFile(FileInputStream(filePath))
                val renderer = TileRendererLayer(
                    tileCache,
                    mapFile,
                    mapView.model.mapViewPosition,
                    AndroidGraphicFactory.INSTANCE
                )
                renderer.setXmlRenderTheme(MapsforgeThemes.DEFAULT)
                mapView.layerManager.layers.add(renderer)
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
                val usage = UsagePositionLayer()
                mapView.layerManager.layers.add(usage)
                overlays.usage = usage
                mapView.model.mapViewPosition.setZoomLevel(14.toByte())
                mapView
            },
            update = { mapView ->
                if (keepWholeTrack || viewingSaved) {
                    val extra = if (logging) {
                        location?.let { GeoPoint(it.latitude, it.longitude) }
                    } else {
                        null
                    }
                    val fitKey = osmFitKey(keepWholeTrack || viewingSaved, logging, points, location)
                    if (fitKey != overlays.lastFitKey) {
                        val bounds = TrackCameraBounds.of(points, extra)
                        if (bounds != null) {
                            fitOsmToBounds(mapView, bounds)
                            overlays.lastFitKey = fitKey
                            overlays.didInitialCenter = true
                        }
                    }
                } else if (logging && points.isNotEmpty()) {
                    val last = points.last()
                    mapView.model.mapViewPosition.center = LatLong(last.latitude, last.longitude)
                    overlays.didInitialCenter = true
                } else if (!overlays.didInitialCenter) {
                    if (points.isNotEmpty()) {
                        val last = points.last()
                        mapView.model.mapViewPosition.center = LatLong(last.latitude, last.longitude)
                        overlays.didInitialCenter = true
                    } else if (location != null) {
                        mapView.model.mapViewPosition.center = LatLong(location.latitude, location.longitude)
                        overlays.didInitialCenter = true
                    }
                }
                updateOsmTrack(overlays, points)
                updateOsmAccuracy(mapView, overlays, location, showAccuracyMarker)
                updateOsmFixCloud(overlays, showFixCloud, fixCloud)
                updateOsmUsage(mapView, overlays, location, points, usageType, usageBitmap)
            }
        )
    }
    DisposableEffect(filePath) {
        onDispose { }
    }
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

private fun fitOsmToBounds(mapView: MapView, bounds: LatLonBounds) {
    val center = LatLong(
        (bounds.minLatitude + bounds.maxLatitude) / 2.0,
        (bounds.minLongitude + bounds.maxLongitude) / 2.0
    )
    if (bounds.isDegenerate) {
        mapView.model.mapViewPosition.center = center
        mapView.model.mapViewPosition.setZoomLevel(16.toByte())
        return
    }
    val box = BoundingBox(
        bounds.minLatitude,
        bounds.minLongitude,
        bounds.maxLatitude,
        bounds.maxLongitude
    )
    val dimension = mapView.model.mapViewDimension.dimension
    val zoom = if (dimension.width > 0 && dimension.height > 0) {
        LatLongUtils.zoomForBounds(dimension, box, mapView.model.displayModel.tileSize)
    } else {
        14.toByte()
    }
    mapView.model.mapViewPosition.mapPosition = MapPosition(center, zoom)
}

private fun updateOsmTrack(overlays: OsmMapOverlays, points: List<GeoPoint>) {
    val latLongs = points.map { LatLong(it.latitude, it.longitude) }
    overlays.polyline?.setPoints(latLongs)
    overlays.polyline?.requestRedraw()
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
    if (overlays.usageBitmapType != usageType) {
        layer.bitmap?.decrementRefCount()
        val drawable = BitmapDrawable(mapView.resources, androidBitmap)
        val forgeBitmap = AndroidGraphicFactory.convertToBitmap(drawable)
        forgeBitmap.incrementRefCount()
        layer.bitmap = forgeBitmap
        overlays.usageBitmapType = usageType
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
