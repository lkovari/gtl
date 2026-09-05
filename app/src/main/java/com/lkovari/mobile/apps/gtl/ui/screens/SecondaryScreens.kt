package com.lkovari.mobile.apps.gtl.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.lkovari.mobile.apps.gtl.R
import com.lkovari.mobile.apps.gtl.data.device.DeviceIdentity
import com.lkovari.mobile.apps.gtl.data.maps.OsmRegion
import com.lkovari.mobile.apps.gtl.engine.MeasurementSystem
import com.lkovari.mobile.apps.gtl.engine.UsageType
import com.lkovari.mobile.apps.gtl.ui.theme.TitleMagenta
import com.lkovari.mobile.apps.gtl.viewmodel.GtlUiState
import com.lkovari.mobile.apps.gtl.viewmodel.GtlViewModel
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecondaryScaffold(pageTitle: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.brand_title),
                            color = TitleMagenta,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = pageTitle,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            content()
        }
    }
}

@Composable
fun SettingsScreen(state: GtlUiState, viewModel: GtlViewModel, onBack: () -> Unit) {
    SecondaryScaffold(stringResource(R.string.settings_title), onBack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(stringResource(R.string.settings_usage), style = MaterialTheme.typography.titleLarge)
            Row(
                modifier = Modifier.fillMaxWidth().selectableGroup(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Top
            ) {
                UsageType.selectable.forEach { type ->
                    val selected = state.settings.usageType == type
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .selectable(
                                selected = selected,
                                onClick = { viewModel.setUsage(type) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        RadioButton(selected = selected, onClick = { viewModel.setUsage(type) })
                        Icon(
                            imageVector = usageIcon(type),
                            contentDescription = stringResource(usageLabel(type)),
                            tint = if (selected) TitleMagenta else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(usageLabel(type)),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1
                        )
                    }
                }
            }
            Text(stringResource(R.string.settings_units), style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MeasurementSystem.entries.forEach { system ->
                    FilterChip(
                        selected = state.settings.measurementSystem == system,
                        onClick = { viewModel.setUnits(system) },
                        label = { Text(system.name.lowercase().replaceFirstChar { it.titlecase() }) }
                    )
                }
            }
            SettingSwitch(stringResource(R.string.settings_offline), state.settings.useOfflineMap) {
                viewModel.setUseOfflineMap(it)
            }
            SettingSwitch(stringResource(R.string.settings_optimize), state.settings.optimizationActive) {
                viewModel.setOptimization(it)
            }
            SettingSwitch(stringResource(R.string.settings_show_track), state.settings.showLastTrackOnMap) {
                viewModel.setShowLastTrackOnMap(it)
            }
            SettingSwitch(stringResource(R.string.settings_show_accuracy), state.settings.showAccuracyMarker) {
                viewModel.setShowAccuracyMarker(it)
            }
            Text(stringResource(R.string.settings_filters), style = MaterialTheme.typography.bodyMedium)
            Text("${stringResource(R.string.settings_min_distance)}: ${state.settings.minDistanceMeters} m")
            Text("${stringResource(R.string.settings_min_time)}: ${state.settings.minTimeMillis} ms")
            Text("${stringResource(R.string.settings_min_accuracy)}: ${state.settings.minAccuracyMeters} m")
            Text("${stringResource(R.string.settings_min_sats)}: ${state.settings.minSatellites}")
        }
    }
}

private fun usageIcon(type: UsageType): ImageVector {
    return when (type) {
        UsageType.AIRCRAFT -> Icons.Filled.Flight
        UsageType.WATERCRAFT -> Icons.Filled.DirectionsBoat
        UsageType.FOUR_WHEELERS -> Icons.Filled.DirectionsCar
        UsageType.TWO_WHEELERS -> Icons.Filled.TwoWheeler
        UsageType.RUNNER -> Icons.AutoMirrored.Filled.DirectionsRun
        UsageType.WALKING_HIKE, UsageType.PEDESTRIAN -> Icons.AutoMirrored.Filled.DirectionsRun
    }
}

private fun usageLabel(type: UsageType): Int {
    return when (type) {
        UsageType.AIRCRAFT -> R.string.usage_aircraft
        UsageType.WATERCRAFT -> R.string.usage_watercraft
        UsageType.FOUR_WHEELERS -> R.string.usage_four_wheelers
        UsageType.TWO_WHEELERS -> R.string.usage_two_wheelers
        UsageType.RUNNER, UsageType.WALKING_HIKE, UsageType.PEDESTRIAN -> R.string.usage_runner
    }
}

@Composable
fun LocationSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    SecondaryScaffold(stringResource(R.string.action_location_settings), onBack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.location_settings_body),
                style = MaterialTheme.typography.bodyLarge
            )
            Button(
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
            ) {
                Text(stringResource(R.string.location_settings_open))
            }
        }
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun OsmDownloadScreen(viewModel: GtlViewModel, onBack: () -> Unit) {
    SecondaryScaffold(stringResource(R.string.osm_title), onBack) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            items(viewModel.regions()) { region ->
                OsmRow(region, viewModel)
            }
        }
    }
}

@Composable
private fun OsmRow(region: OsmRegion, viewModel: GtlViewModel) {
    val download by viewModel.observeDownload(region.id).collectAsState(
        initial = com.lkovari.mobile.apps.gtl.data.maps.OsmDownloadState(region.id, false, 0, false)
    )
    val downloaded = viewModel.isDownloaded(region)
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(region.label, style = MaterialTheme.typography.titleLarge)
                Text(
                    if (downloaded) stringResource(R.string.osm_downloaded) else region.id,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (downloaded) {
                Button(onClick = { viewModel.selectDownloadedMap(region) }) {
                    Text(stringResource(R.string.osm_use))
                }
            } else {
                Button(onClick = { viewModel.downloadRegion(region) }, enabled = !download.running) {
                    Text(stringResource(R.string.osm_download))
                }
            }
        }
        if (download.running) {
            LinearProgressIndicator(
                progress = { download.progress / 100f },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            Text("${download.progress} %", style = MaterialTheme.typography.labelMedium)
        }
        if (download.failed) {
            Text(stringResource(R.string.osm_failed), color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun TracksScreen(
    state: GtlUiState,
    viewModel: GtlViewModel,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onShowOnMap: (Long) -> Unit
) {
    val format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
    SecondaryScaffold(stringResource(R.string.tracks_title), onBack) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            item {
                Button(onClick = onShare, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Text(stringResource(R.string.action_share_kml))
                }
            }
            items(state.sessions) { session ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectSession(session.id) }
                        .padding(vertical = 10.dp)
                ) {
                    Text(format.format(Date(session.startedAt)), style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${session.usageType} · ${session.measurementSystem}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onShowOnMap(session.id) }) {
                            Text(stringResource(R.string.action_show_on_map))
                        }
                        Button(onClick = { viewModel.deleteSession(session.id) }) {
                            Text(stringResource(R.string.action_delete))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HelpScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val privacyUrl = stringResource(R.string.help_privacy_url)
    SecondaryScaffold(stringResource(R.string.help_title), onBack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.help_body),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(stringResource(R.string.help_gps_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.help_gps_body), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.help_route_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.help_route_body), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.help_map_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.help_map_body), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.help_compass_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.help_compass_body), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(R.string.help_privacy_policy),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = privacyUrl,
                color = TitleMagenta,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.clickable {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(privacyUrl)))
                }
            )
        }
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val deviceName = remember { DeviceIdentity.displayName(context) }
    SecondaryScaffold(stringResource(R.string.about_title), onBack) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("2.0.0  ·  com.lkovari.mobile.apps.gtl")
            Text("${stringResource(R.string.about_device)}: $deviceName")
            Text(stringResource(R.string.about_author))
            Text(stringResource(R.string.about_body))
        }
    }
}
