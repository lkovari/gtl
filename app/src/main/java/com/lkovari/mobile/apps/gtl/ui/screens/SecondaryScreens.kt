package com.lkovari.mobile.apps.gtl.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.lkovari.mobile.apps.gtl.R
import com.lkovari.mobile.apps.gtl.data.device.DeviceIdentity
import com.lkovari.mobile.apps.gtl.data.maps.OsmRegion
import com.lkovari.mobile.apps.gtl.engine.DouglasPeucker
import com.lkovari.mobile.apps.gtl.engine.MeasurementSystem
import com.lkovari.mobile.apps.gtl.engine.UsageType
import com.lkovari.mobile.apps.gtl.ui.theme.CockpitPanel
import com.lkovari.mobile.apps.gtl.ui.theme.HudCyan
import com.lkovari.mobile.apps.gtl.ui.theme.MoonCream
import com.lkovari.mobile.apps.gtl.ui.theme.NightMuted
import com.lkovari.mobile.apps.gtl.ui.theme.TitleMagenta
import com.lkovari.mobile.apps.gtl.viewmodel.GtlUiState
import com.lkovari.mobile.apps.gtl.viewmodel.GtlViewModel
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecondaryScaffold(pageTitle: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(state: GtlUiState, viewModel: GtlViewModel, onBack: () -> Unit) {
    var dpValue by remember {
        mutableFloatStateOf(state.settings.optimizationTolerance.toFloat())
    }
    var strengthValue by remember {
        mutableFloatStateOf(state.settings.smoothingStrengthValue)
    }
    var densityValue by remember {
        mutableFloatStateOf(state.settings.recordingDensityValue)
    }
    LaunchedEffect(state.settings.optimizationTolerance) {
        dpValue = state.settings.optimizationTolerance.toFloat()
    }
    LaunchedEffect(state.settings.smoothingStrengthValue) {
        strengthValue = state.settings.smoothingStrengthValue
    }
    LaunchedEffect(state.settings.recordingDensityValue) {
        densityValue = state.settings.recordingDensityValue
    }
    SecondaryScaffold(stringResource(R.string.settings_title), onBack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text(stringResource(R.string.settings_usage), style = MaterialTheme.typography.titleMedium)
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
                            .padding(vertical = 1.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = usageIcon(type),
                            contentDescription = stringResource(usageLabel(type)),
                            modifier = Modifier.size(26.dp),
                            tint = if (selected) TitleMagenta else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(usageLabel(type)),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(stringResource(R.string.settings_units), style = MaterialTheme.typography.titleMedium)
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
            if (state.settings.optimizationActive) {
                EndpointSlider(
                    value = dpValue,
                    onValueChange = { dpValue = it },
                    onValueChangeFinished = {
                        viewModel.setOptimizationTolerance(dpValue.toDouble())
                    },
                    valueRange = DouglasPeucker.MinToleranceMeters.toFloat()..
                        DouglasPeucker.MaxToleranceMeters.toFloat(),
                    steps = 18,
                    startLabel = stringResource(
                        R.string.settings_meters,
                        DouglasPeucker.MinToleranceMeters.toInt()
                    ),
                    endLabel = stringResource(
                        R.string.settings_meters,
                        DouglasPeucker.MaxToleranceMeters.toInt()
                    )
                )
            }
            SettingSwitch(stringResource(R.string.settings_show_track), state.settings.showLastTrackOnMap) {
                viewModel.setShowLastTrackOnMap(it)
            }
            SettingSwitch(stringResource(R.string.settings_keep_whole_track), state.settings.keepWholeTrackOnScreen) {
                viewModel.setKeepWholeTrackOnScreen(it)
            }
            SettingSwitch(stringResource(R.string.settings_show_accuracy), state.settings.showAccuracyMarker) {
                viewModel.setShowAccuracyMarker(it)
            }
            SettingSwitch(stringResource(R.string.settings_gnss_only), state.settings.gnssOnly) {
                viewModel.setGnssOnly(it)
            }
            SettingSwitch(stringResource(R.string.settings_track_smoothing), state.settings.trackSmoothingEnabled) {
                viewModel.setTrackSmoothing(it)
            }
            if (state.settings.trackSmoothingEnabled) {
                EndpointSlider(
                    value = strengthValue,
                    onValueChange = { strengthValue = it },
                    onValueChangeFinished = { viewModel.setSmoothingStrength(strengthValue) },
                    valueRange = 0f..1f,
                    steps = 0,
                    startLabel = stringResource(R.string.settings_smoothing_low),
                    endLabel = stringResource(R.string.settings_smoothing_high)
                )
            }
            SettingSwitch(stringResource(R.string.settings_stationary_lock), state.settings.stationaryLockEnabled) {
                viewModel.setStationaryLock(it)
            }
            Text(
                text = stringResource(R.string.settings_recording_density),
                style = MaterialTheme.typography.bodyMedium
            )
            EndpointSlider(
                value = densityValue,
                onValueChange = { densityValue = it },
                onValueChangeFinished = { viewModel.setRecordingDensity(densityValue) },
                valueRange = 0f..1f,
                steps = 0,
                startLabel = stringResource(R.string.settings_density_smart),
                endLabel = stringResource(R.string.settings_density_every_fix)
            )
        }
    }
}

@Composable
private fun EndpointSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    startLabel: String,
    endLabel: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = startLabel,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            modifier = Modifier.widthIn(min = 36.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.weight(1f).height(28.dp)
        )
        Text(
            text = endLabel,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            modifier = Modifier.widthIn(min = 36.dp)
        )
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
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 36.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f).padding(end = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2
        )
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
    onShare: (Set<Long>) -> Unit,
    onShowOnMap: (Long) -> Unit
) {
    val format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    val sessionIds = state.sessions.map { it.id }.toSet()
    val visibleSelected = selectedIds.intersect(sessionIds)
    val allSelected = sessionIds.isNotEmpty() && visibleSelected.size == sessionIds.size
    SecondaryScaffold(stringResource(R.string.tracks_title), onBack) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = allSelected,
                        onCheckedChange = { checked ->
                            selectedIds = if (checked) sessionIds else emptySet()
                        },
                        enabled = sessionIds.isNotEmpty()
                    )
                    Text(stringResource(R.string.tracks_select_all), style = MaterialTheme.typography.bodyLarge)
                }
                Button(
                    onClick = { onShare(visibleSelected) },
                    enabled = visibleSelected.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Text(stringResource(R.string.tracks_share_selected))
                }
            }
            items(state.sessions) { session ->
                val checked = session.id in visibleSelected
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { on ->
                                selectedIds = if (on) {
                                    visibleSelected + session.id
                                } else {
                                    visibleSelected - session.id
                                }
                            }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(format.format(Date(session.startedAt)), style = MaterialTheme.typography.titleLarge)
                            Text(
                                "${session.usageType} · ${session.measurementSystem}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.padding(start = 48.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
    var expandedId by rememberSaveable { mutableStateOf(HelpSectionUsage) }
    SecondaryScaffold(stringResource(R.string.help_title), onBack) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                HelpAccordionSection(
                    title = stringResource(R.string.settings_usage),
                    expanded = expandedId == HelpSectionUsage,
                    onToggle = { expandedId = toggleHelpSection(expandedId, HelpSectionUsage) }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.help_body), style = MaterialTheme.typography.bodyLarge)
                        Text(stringResource(R.string.help_usage_toggles), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            item {
                HelpAccordionSection(
                    title = stringResource(R.string.settings_title),
                    expanded = expandedId == HelpSectionSettings,
                    onToggle = { expandedId = toggleHelpSection(expandedId, HelpSectionSettings) }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.help_settings_presets), style = MaterialTheme.typography.bodyLarge)
                        Text(stringResource(R.string.help_settings_controls), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            item {
                HelpAccordionSection(
                    title = stringResource(R.string.help_logging_title),
                    expanded = expandedId == HelpSectionLogging,
                    onToggle = { expandedId = toggleHelpSection(expandedId, HelpSectionLogging) }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.help_usage_simplify), style = MaterialTheme.typography.bodyLarge)
                        Text(stringResource(R.string.help_usage_spacing), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            item {
                HelpAccordionSection(
                    title = stringResource(R.string.help_gps_title),
                    expanded = expandedId == HelpSectionGps,
                    onToggle = { expandedId = toggleHelpSection(expandedId, HelpSectionGps) }
                ) {
                    Text(stringResource(R.string.help_gps_body), style = MaterialTheme.typography.bodyLarge)
                }
            }
            item {
                HelpAccordionSection(
                    title = stringResource(R.string.help_route_title),
                    expanded = expandedId == HelpSectionRoute,
                    onToggle = { expandedId = toggleHelpSection(expandedId, HelpSectionRoute) }
                ) {
                    Text(stringResource(R.string.help_route_body), style = MaterialTheme.typography.bodyLarge)
                }
            }
            item {
                HelpAccordionSection(
                    title = stringResource(R.string.help_map_title),
                    expanded = expandedId == HelpSectionMap,
                    onToggle = { expandedId = toggleHelpSection(expandedId, HelpSectionMap) }
                ) {
                    Text(stringResource(R.string.help_map_body), style = MaterialTheme.typography.bodyLarge)
                }
            }
            item {
                HelpAccordionSection(
                    title = stringResource(R.string.help_compass_title),
                    expanded = expandedId == HelpSectionCompass,
                    onToggle = { expandedId = toggleHelpSection(expandedId, HelpSectionCompass) }
                ) {
                    Text(stringResource(R.string.help_compass_body), style = MaterialTheme.typography.bodyLarge)
                }
            }
            item {
                HelpAccordionSection(
                    title = stringResource(R.string.help_kml_title),
                    expanded = expandedId == HelpSectionKmz,
                    onToggle = { expandedId = toggleHelpSection(expandedId, HelpSectionKmz) }
                ) {
                    Text(stringResource(R.string.help_kml_body), style = MaterialTheme.typography.bodyLarge)
                }
            }
            item {
                HelpAccordionSection(
                    title = stringResource(R.string.help_privacy_policy),
                    expanded = expandedId == HelpSectionPrivacy,
                    onToggle = { expandedId = toggleHelpSection(expandedId, HelpSectionPrivacy) }
                ) {
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
            item {
                HelpAccordionSection(
                    title = stringResource(R.string.help_trackpoint_title),
                    expanded = expandedId == HelpSectionTrackpoint,
                    onToggle = { expandedId = toggleHelpSection(expandedId, HelpSectionTrackpoint) }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = stringResource(R.string.help_trackpoint_intro),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        StoredTrackpointTable()
                    }
                }
            }
        }
    }
}

private const val HelpSectionUsage = "usage"
private const val HelpSectionSettings = "settings"
private const val HelpSectionLogging = "logging"
private const val HelpSectionGps = "gps"
private const val HelpSectionRoute = "route"
private const val HelpSectionMap = "map"
private const val HelpSectionCompass = "compass"
private const val HelpSectionKmz = "kmz"
private const val HelpSectionPrivacy = "privacy"
private const val HelpSectionTrackpoint = "trackpoint"

private fun toggleHelpSection(expandedId: String, sectionId: String): String {
    return if (expandedId == sectionId) "" else sectionId
}

@Composable
private fun HelpAccordionSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge
                )
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = title
                )
            }
            if (expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun StoredTrackpointTable() {
    val fields = stringArrayResource(R.array.help_trackpoint_fields)
    val meanings = stringArrayResource(R.array.help_trackpoint_meanings)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CockpitPanel,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
            fields.forEachIndexed { index, field ->
                if (index > 0) {
                    HorizontalDivider(color = NightMuted.copy(alpha = 0.28f))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = field,
                        modifier = Modifier.weight(0.46f),
                        color = HudCyan,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = meanings.getOrElse(index) { "" },
                        modifier = Modifier.weight(0.54f),
                        color = MoonCream,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val deviceName = remember { DeviceIdentity.displayName(context) }
    SecondaryScaffold(stringResource(R.string.about_title), onBack) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${com.lkovari.mobile.apps.gtl.BuildConfig.VERSION_NAME}  ·  com.lkovari.mobile.apps.gtl")
            Text("${stringResource(R.string.about_device)}: $deviceName")
            Text(stringResource(R.string.about_author))
            Text(stringResource(R.string.about_body))
        }
    }
}
