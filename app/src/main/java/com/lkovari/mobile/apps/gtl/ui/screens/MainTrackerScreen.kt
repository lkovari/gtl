package com.lkovari.mobile.apps.gtl.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.GeomagneticField
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lkovari.mobile.apps.gtl.R
import com.lkovari.mobile.apps.gtl.engine.CompassHeading
import com.lkovari.mobile.apps.gtl.engine.ElevationPoint
import com.lkovari.mobile.apps.gtl.engine.ElevationSeries
import com.lkovari.mobile.apps.gtl.engine.Units
import com.lkovari.mobile.apps.gtl.ui.components.CompassDial
import com.lkovari.mobile.apps.gtl.ui.components.ConstellationStrip
import com.lkovari.mobile.apps.gtl.ui.components.ElevationProfile
import com.lkovari.mobile.apps.gtl.ui.components.HudMetric
import com.lkovari.mobile.apps.gtl.ui.components.SnrMeter
import com.lkovari.mobile.apps.gtl.ui.theme.AmberFix
import com.lkovari.mobile.apps.gtl.ui.theme.StartBlue
import com.lkovari.mobile.apps.gtl.ui.theme.TitleMagenta
import com.lkovari.mobile.apps.gtl.ui.theme.TrackingOrange
import com.lkovari.mobile.apps.gtl.ui.theme.gtlWash
import com.lkovari.mobile.apps.gtl.viewmodel.GtlUiState
import com.lkovari.mobile.apps.gtl.viewmodel.GtlViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTrackerScreen(
    state: GtlUiState,
    viewModel: GtlViewModel,
    onOpenSettings: () -> Unit,
    onOpenMaps: () -> Unit,
    onOpenTracks: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenLocationSettings: () -> Unit
) {
    var tab by remember { mutableIntStateOf(state.mainTab) }
    var menu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val previewLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            viewModel.startPreview()
        }
    }
    val startLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            viewModel.startLogging()
        }
    }
    LaunchedEffect(Unit) {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (fine || coarse) {
            viewModel.startPreview()
        } else {
            previewLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
    LaunchedEffect(state.requestedTab) {
        val requested = state.requestedTab
        if (requested != null) {
            tab = requested
            viewModel.setMainTab(requested)
            viewModel.consumeRequestedTab()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.brand_title),
                        color = TitleMagenta
                    )
                },
                actions = {
                    val logging = state.live.logging
                    Button(
                        onClick = {
                            if (logging) {
                                viewModel.stopLogging()
                            } else {
                                val fine = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                                if (fine) {
                                    viewModel.startLogging()
                                } else {
                                    val needed = buildList {
                                        add(Manifest.permission.ACCESS_FINE_LOCATION)
                                        add(Manifest.permission.ACCESS_COARSE_LOCATION)
                                        if (Build.VERSION.SDK_INT >= 33) {
                                            add(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    }
                                    startLauncher.launch(needed.toTypedArray())
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (logging) TrackingOrange else StartBlue,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            if (logging) {
                                stringResource(R.string.action_stop)
                            } else {
                                stringResource(R.string.action_start)
                            }
                        )
                    }
                    IconButton(onClick = { menu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.menu))
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.settings_title)) }, onClick = {
                            menu = false
                            onOpenSettings()
                        })
                        DropdownMenuItem(text = { Text(stringResource(R.string.osm_title)) }, onClick = {
                            menu = false
                            onOpenMaps()
                        })
                        DropdownMenuItem(text = { Text(stringResource(R.string.tracks_title)) }, onClick = {
                            menu = false
                            onOpenTracks()
                        })
                        DropdownMenuItem(text = { Text(stringResource(R.string.help_title)) }, onClick = {
                            menu = false
                            onOpenHelp()
                        })
                        DropdownMenuItem(text = { Text(stringResource(R.string.about_title)) }, onClick = {
                            menu = false
                            onOpenAbout()
                        })
                        DropdownMenuItem(text = { Text(stringResource(R.string.action_location_settings)) }, onClick = {
                            menu = false
                            onOpenLocationSettings()
                        })
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = {
                        tab = 0
                        viewModel.setMainTab(0)
                    },
                    icon = { Icon(Icons.Default.SatelliteAlt, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_gps)) }
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = {
                        tab = 1
                        viewModel.setMainTab(1)
                    },
                    icon = { Icon(Icons.Default.Speed, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_route)) }
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = {
                        tab = 2
                        viewModel.setMainTab(2)
                    },
                    icon = { Icon(Icons.Default.Map, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_map)) }
                )
                NavigationBarItem(
                    selected = tab == 3,
                    onClick = {
                        tab = 3
                        viewModel.setMainTab(3)
                    },
                    icon = { Icon(Icons.Default.Explore, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_compass)) }
                )
            }
        }
    ) { padding ->
        val dark = MaterialTheme.colorScheme.background == com.lkovari.mobile.apps.gtl.ui.theme.Cockpit
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(gtlWash(dark))
                .padding(padding)
        ) {
            when (tab) {
                0 -> GpsPane(state)
                1 -> RoutePane(state)
                2 -> MapPane(state, onClearMap = { viewModel.clearShownTrack() })
                else -> CompassPane(
                    state = state,
                    onTrueNorth = { viewModel.setCompassTrueNorth(it) }
                )
            }
        }
    }
}

@Composable
private fun GpsPane(state: GtlUiState) {
    val location = state.live.lastLocation
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ConstellationStrip(state.live.gnss)
        SnrMeter(state.live.gnss)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            HudMetric(
                stringResource(R.string.gps_latitude),
                location?.let { String.format(Locale.US, "%.6f", it.latitude) } ?: "—",
                Modifier.weight(1f)
            )
            HudMetric(
                stringResource(R.string.gps_longitude),
                location?.let { String.format(Locale.US, "%.6f", it.longitude) } ?: "—",
                Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            HudMetric(
                stringResource(R.string.gps_accuracy),
                location?.let { String.format(Locale.US, "%.1f m", it.accuracy) } ?: "—",
                Modifier.weight(1f)
            )
            HudMetric(
                stringResource(R.string.route_altitude),
                location?.let { Units.formatAltitude(it.altitude, state.settings.measurementSystem) } ?: "—",
                Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            HudMetric(
                stringResource(R.string.gps_provider),
                state.live.provider ?: "—",
                Modifier.weight(1f)
            )
            HudMetric(
                stringResource(R.string.gps_status),
                if (state.live.logging) stringResource(R.string.status_logging) else stringResource(R.string.status_idle),
                Modifier.weight(1f)
            )
        }
        if (state.settings.showFixCloud) {
            FixCloudMetrics(state)
        }
        HudMetric(
            stringResource(R.string.gps_temperature),
            if (state.live.temperatureAvailable) {
                Units.formatTemperature(state.live.temperatureCelsius)
            } else {
                Units.formatTemperature(0f)
            }
        )
    }
}

@Composable
private fun FixCloudMetrics(state: GtlUiState) {
    val stats = state.fixCloud.stats
    val n = stats.sampleCount
    val dash = "—"
    val rms = stats.rmsMeters?.let { String.format(Locale.US, "%.1f m", it) } ?: dash
    val cep95 = stats.cep95Meters?.let { String.format(Locale.US, "%.1f m", it) } ?: dash
    val reported = stats.reportedAccuracyMedianMeters?.let { String.format(Locale.US, "%.1f m", it) } ?: dash
    val caption = when {
        !stats.active -> stringResource(R.string.gps_fix_cloud_paused)
        n < 8 -> stringResource(R.string.gps_fix_cloud_wait)
        else -> stringResource(R.string.gps_fix_cloud_standing)
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            CompactMetric(stringResource(R.string.gps_fix_cloud_n), n.toString(), Modifier.weight(1f))
            CompactMetric(stringResource(R.string.gps_fix_cloud_rms), rms, Modifier.weight(1f))
            CompactMetric(stringResource(R.string.gps_fix_cloud_cep95), cep95, Modifier.weight(1f))
            CompactMetric(stringResource(R.string.gps_fix_cloud_reported), reported, Modifier.weight(1f))
        }
        Text(
            text = caption,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CompactMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

@Composable
private fun RoutePane(state: GtlUiState) {
    val stats = state.stats
    val units = state.settings.measurementSystem
    val location = state.live.lastLocation
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            HudMetric(stringResource(R.string.route_elapsed), Units.formatDuration(stats.elapsedMillis), Modifier.weight(1f))
            HudMetric(stringResource(R.string.route_odometer), Units.formatDistance(stats.odometerMeters, units), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            HudMetric(stringResource(R.string.route_moving), Units.formatDuration(stats.movingMillis), Modifier.weight(1f))
            HudMetric(stringResource(R.string.route_waiting), Units.formatDuration(stats.waitingMillis), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            HudMetric(
                stringResource(R.string.route_speed),
                location?.let { Units.formatSpeed(it.speed, units) } ?: "—",
                Modifier.weight(1f)
            )
            HudMetric(
                stringResource(R.string.route_avg_speed),
                Units.formatSpeed(stats.averageSpeedMps, units),
                Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            HudMetric(
                stringResource(R.string.route_altitude),
                location?.let { Units.formatAltitude(it.altitude, units) } ?: "—",
                Modifier.weight(1f)
            )
            HudMetric(
                stringResource(R.string.route_bearing),
                location?.let { String.format(Locale.US, "%.0f°", it.bearing) } ?: "—",
                Modifier.weight(1f)
            )
        }
        HudMetric(
            stringResource(R.string.route_lean),
            state.live.leanAngle?.let { String.format(Locale.US, "%.0f°", it) } ?: "—"
        )
        stats.temperatureRange?.let { range ->
            HudMetric(
                stringResource(R.string.route_temp_range),
                "${Units.formatTemperature(range.minCelsius)} / ${Units.formatTemperature(range.maxCelsius)}"
            )
        }
        val elevation = remember(state.events) {
            ElevationSeries.downsample(
                ElevationSeries.fromPoints(
                    state.events.map { event ->
                        ElevationPoint(
                            latitude = event.latitude,
                            longitude = event.longitude,
                            gpsAltitude = event.altitude,
                            baroAltitude = event.baroAltitude
                        )
                    }
                )
            )
        }
        ElevationProfile(samples = elevation, system = units)
    }
}

@Composable
private fun CompassPane(state: GtlUiState, onTrueNorth: (Boolean) -> Unit) {
    val location = state.live.lastLocation
    val declination = remember(
        location?.latitude,
        location?.longitude,
        location?.altitude,
        location?.time
    ) {
        location?.let { loc ->
            val time = if (loc.time > 0L) loc.time else System.currentTimeMillis()
            GeomagneticField(
                loc.latitude.toFloat(),
                loc.longitude.toFloat(),
                if (loc.hasAltitude()) loc.altitude.toFloat() else 0f,
                time
            ).declination
        }
    }
    val shown = CompassHeading.display(
        magneticDegrees = state.live.azimuthDegrees ?: 0f,
        wantTrue = state.settings.compassTrueNorth,
        declinationDegrees = declination
    )
    val reference = if (shown.trueNorth) {
        stringResource(R.string.compass_true)
    } else {
        stringResource(R.string.compass_mag)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = !state.settings.compassTrueNorth,
                onClick = { onTrueNorth(false) },
                label = { Text(stringResource(R.string.compass_mag)) }
            )
            FilterChip(
                selected = state.settings.compassTrueNorth,
                onClick = { onTrueNorth(true) },
                label = { Text(stringResource(R.string.compass_true)) }
            )
        }
        if (shown.missingFix) {
            Text(
                text = stringResource(R.string.compass_no_gps),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        CompassDial(
            azimuth = shown.degrees,
            referenceLabel = reference,
            modifier = Modifier.padding(top = 12.dp)
        )
        if (CompassHeading.needsFigureEight(state.live.compassAccuracy)) {
            Text(
                text = stringResource(R.string.compass_figure_eight),
                style = MaterialTheme.typography.bodyLarge,
                color = AmberFix,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}
