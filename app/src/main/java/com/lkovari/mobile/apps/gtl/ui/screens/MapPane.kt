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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.lkovari.mobile.apps.gtl.R
import com.lkovari.mobile.apps.gtl.engine.GeoPoint
import com.lkovari.mobile.apps.gtl.ui.theme.AccuracyMarkerBorder
import com.lkovari.mobile.apps.gtl.ui.theme.AccuracyMarkerFill
import com.lkovari.mobile.apps.gtl.ui.theme.CarmineTrack
import com.lkovari.mobile.apps.gtl.viewmodel.GtlUiState
import org.mapsforge.core.graphics.Style
import org.mapsforge.core.model.LatLong
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.util.AndroidUtil
import org.mapsforge.map.android.view.MapView
import org.mapsforge.map.layer.overlay.Circle as ForgeCircle
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
            showAccuracyMarker = state.settings.showAccuracyMarker
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
    LaunchedEffect(live?.latitude, live?.longitude, latLngs.size, state.live.logging) {
        if (state.live.logging && latLngs.isNotEmpty()) {
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

private class OsmAccuracyLayers {
    var accuracy: ForgeCircle? = null
    var center: ForgeCircle? = null
}

@Composable
private fun OsmMapView(
    filePath: String,
    points: List<GeoPoint>,
    location: Location?,
    showAccuracyMarker: Boolean
) {
    val overlays = remember(filePath) { OsmAccuracyLayers() }
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
            mapView.model.mapViewPosition.setZoomLevel(14.toByte())
            mapView
        },
        update = { mapView ->
            if (points.isNotEmpty()) {
                val last = points.last()
                mapView.model.mapViewPosition.center = LatLong(last.latitude, last.longitude)
            } else if (location != null) {
                mapView.model.mapViewPosition.center = LatLong(location.latitude, location.longitude)
            }
            updateOsmAccuracy(mapView, overlays, location, showAccuracyMarker)
        }
    )
    DisposableEffect(filePath) {
        onDispose { }
    }
}

private fun updateOsmAccuracy(
    mapView: MapView,
    overlays: OsmAccuracyLayers,
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
