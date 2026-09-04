package com.lkovari.mobile.apps.gtl.ui.screens

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.lkovari.mobile.apps.gtl.R
import com.lkovari.mobile.apps.gtl.engine.GeoPoint
import com.lkovari.mobile.apps.gtl.ui.theme.CarmineTrack
import com.lkovari.mobile.apps.gtl.viewmodel.GtlUiState
import org.mapsforge.core.model.LatLong
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.util.AndroidUtil
import org.mapsforge.map.android.view.MapView
import org.mapsforge.map.layer.overlay.Polyline as ForgePolyline
import org.mapsforge.map.layer.renderer.TileRendererLayer
import org.mapsforge.map.reader.MapFile
import org.mapsforge.map.rendertheme.internal.MapsforgeThemes
import java.io.FileInputStream

@Composable
fun MapPane(state: GtlUiState) {
    val points = state.displayPoints
    if (state.settings.useOfflineMap && state.osmFile != null) {
        OsmMapView(filePath = state.osmFile.absolutePath, points = points)
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

@Composable
private fun OsmMapView(filePath: String, points: List<GeoPoint>) {
    val context = LocalContext.current
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
            }
        }
    )
    DisposableEffect(filePath) {
        onDispose { }
    }
}
