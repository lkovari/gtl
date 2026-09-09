package com.lkovari.mobile.apps.gtl.ui.screens

import android.location.Location
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.lkovari.mobile.apps.gtl.R
import com.lkovari.mobile.apps.gtl.engine.GeoPoint
import com.lkovari.mobile.apps.gtl.engine.LatLonBounds
import com.lkovari.mobile.apps.gtl.engine.TrackCameraBounds
import com.lkovari.mobile.apps.gtl.ui.theme.AccuracyMarkerBorder
import com.lkovari.mobile.apps.gtl.ui.theme.AccuracyMarkerFill
import com.lkovari.mobile.apps.gtl.ui.theme.CarmineTrack
import com.lkovari.mobile.apps.gtl.viewmodel.GtlUiState
import org.mapsforge.core.graphics.Style
import org.mapsforge.core.model.BoundingBox
import org.mapsforge.core.model.LatLong
import org.mapsforge.core.model.MapPosition
import org.mapsforge.core.util.LatLongUtils
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.util.AndroidUtil
import org.mapsforge.map.android.view.MapView
import org.mapsforge.map.layer.overlay.Circle as ForgeCircle
import org.mapsforge.map.layer.overlay.Polyline as ForgePolyline
import org.mapsforge.map.layer.renderer.TileRendererLayer
import org.mapsforge.map.reader.MapFile
import org.mapsforge.map.rendertheme.internal.MapsforgeThemes
import java.io.FileInputStream

private const val AccuracyFillAlpha = 64

@Composable
fun MapPane(state: GtlUiState) {
    val points = state.displayPoints
    if (state.settings.useOfflineMap && state.osmFile != null) {
        OsmMapView(
            filePath = state.osmFile.absolutePath,
            points = points,
            location = state.live.lastLocation,
            showAccuracyMarker = state.settings.showAccuracyMarker,
            logging = state.live.logging,
            keepWholeTrack = state.settings.keepWholeTrackOnScreen
        )
        return
    }
    if (!state.mapsKeyPresent) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.map_missing_key), style = MaterialTheme.typography.bodyLarge)
        }
        return
    }
    val live = state.live.lastLocation?.let { LatLng(it.latitude, it.longitude) }
    val start = live
        ?: points.lastOrNull()?.let { LatLng(it.latitude, it.longitude) }
        ?: LatLng(47.4979, 19.0402)
    val camera = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(start, 16f)
    }
    val latLngs = points.map { LatLng(it.latitude, it.longitude) }
    val accuracy = state.live.lastLocation?.accuracy ?: 0f
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = camera,
        uiSettings = MapUiSettings(zoomControlsEnabled = true, compassEnabled = true)
    ) {
        if (latLngs.size >= 2) {
            Polyline(points = latLngs, color = CarmineTrack, width = 10f)
        }
        latLngs.firstOrNull()?.let { Marker(state = MarkerState(it), title = "Start") }
        if (latLngs.size > 1) {
            Marker(state = MarkerState(latLngs.last()), title = "Now")
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
    }
    var centeredOnce by remember { mutableStateOf(false) }
    val keepWhole = state.settings.keepWholeTrackOnScreen
    val liveLat = if (keepWhole && !state.live.logging) null else live?.latitude
    val liveLon = if (keepWhole && !state.live.logging) null else live?.longitude
    LaunchedEffect(
        liveLat,
        liveLon,
        latLngs.size,
        latLngs.firstOrNull(),
        latLngs.lastOrNull(),
        state.live.logging,
        keepWhole
    ) {
        if (keepWhole) {
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

private class OsmMapOverlays {
    var polyline: ForgePolyline? = null
    var start: ForgeCircle? = null
    var end: ForgeCircle? = null
    var accuracy: ForgeCircle? = null
    var center: ForgeCircle? = null
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
    keepWholeTrack: Boolean
) {
    val overlays = remember(filePath) { OsmMapOverlays() }
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
                mapView.model.mapViewPosition.setZoomLevel(14.toByte())
                mapView
            },
            update = { mapView ->
                if (keepWholeTrack) {
                    val extra = if (logging) {
                        location?.let { GeoPoint(it.latitude, it.longitude) }
                    } else {
                        null
                    }
                    val fitKey = osmFitKey(keepWholeTrack, logging, points, location)
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
                updateOsmTrack(mapView, overlays, points)
                updateOsmAccuracy(mapView, overlays, location, showAccuracyMarker)
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

private fun updateOsmTrack(mapView: MapView, overlays: OsmMapOverlays, points: List<GeoPoint>) {
    val latLongs = points.map { LatLong(it.latitude, it.longitude) }
    overlays.polyline?.setPoints(latLongs)
    overlays.polyline?.requestRedraw()
    val graphic = AndroidGraphicFactory.INSTANCE
    fun endpoint(existing: ForgeCircle?, position: LatLong?): ForgeCircle? {
        if (position == null) {
            existing?.isVisible = false
            existing?.requestRedraw()
            return existing
        }
        if (existing == null) {
            val fill = graphic.createPaint()
            fill.setColor(graphic.createColor(255, 0xC1, 0x3B, 0x2E))
            fill.setStyle(Style.FILL)
            val circle = ForgeCircle(position, 8f, fill, null)
            mapView.layerManager.layers.add(circle)
            return circle
        }
        existing.setLatLong(position)
        existing.isVisible = true
        existing.requestRedraw()
        return existing
    }
    overlays.start = endpoint(overlays.start, latLongs.firstOrNull())
    overlays.end = endpoint(overlays.end, latLongs.lastOrNull()?.takeIf { latLongs.size > 1 })
}

private fun updateOsmAccuracy(
    mapView: MapView,
    overlays: OsmMapOverlays,
    location: Location?,
    showAccuracyMarker: Boolean
) {
    if (location == null || !showAccuracyMarker || location.accuracy <= 0f) {
        overlays.accuracy?.isVisible = false
        overlays.center?.isVisible = false
        overlays.accuracy?.requestRedraw()
        overlays.center?.requestRedraw()
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
        val centerFill = graphic.createPaint()
        centerFill.setColor(graphic.createColor(255, 0x14, 0x14, 0xFC))
        centerFill.setStyle(Style.FILL)
        val centerCircle = ForgeCircle(latLong, 0.25f, centerFill, null)
        mapView.layerManager.layers.add(centerCircle)
        overlays.center = centerCircle
    } else {
        accuracy.setLatLong(latLong)
        accuracy.setRadius(location.accuracy)
        accuracy.isVisible = true
        accuracy.requestRedraw()
        overlays.center?.setLatLong(latLong)
        overlays.center?.isVisible = true
        overlays.center?.requestRedraw()
    }
}
