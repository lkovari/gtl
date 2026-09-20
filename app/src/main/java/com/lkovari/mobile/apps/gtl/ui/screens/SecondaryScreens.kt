package com.lkovari.mobile.apps.gtl.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lkovari.mobile.apps.gtl.R
import com.lkovari.mobile.apps.gtl.data.device.DeviceIdentity
import com.lkovari.mobile.apps.gtl.data.maps.OsmRegion
import com.lkovari.mobile.apps.gtl.domain.TrackShareFormat
import com.lkovari.mobile.apps.gtl.engine.BaroAltitude
import com.lkovari.mobile.apps.gtl.engine.DouglasPeucker
import com.lkovari.mobile.apps.gtl.engine.GpsAltitude
import com.lkovari.mobile.apps.gtl.engine.MeasurementSystem
import com.lkovari.mobile.apps.gtl.engine.OsmOfflineAvailability
import com.lkovari.mobile.apps.gtl.engine.UsageType
import com.lkovari.mobile.apps.gtl.ui.usageIcon
import com.lkovari.mobile.apps.gtl.ui.theme.CockpitPanel
import com.lkovari.mobile.apps.gtl.ui.theme.HudCyan
import com.lkovari.mobile.apps.gtl.ui.theme.MoonCream
import com.lkovari.mobile.apps.gtl.ui.theme.NightMuted
import com.lkovari.mobile.apps.gtl.ui.theme.TitleMagenta
import com.lkovari.mobile.apps.gtl.ui.components.ElevationProfile
import com.lkovari.mobile.apps.gtl.tuhu.TuhuDownloadRow
import com.lkovari.mobile.apps.gtl.tuhu.TuhuFeature
import com.lkovari.mobile.apps.gtl.tuhu.TuhuHelpSection
import com.lkovari.mobile.apps.gtl.tuhu.TuhuLayerActions
import com.lkovari.mobile.apps.gtl.tuhu.TuhuLayerControls
import com.lkovari.mobile.apps.gtl.tuhu.TuhuAboutSection
import com.lkovari.mobile.apps.gtl.viewmodel.GtlUiState
import com.lkovari.mobile.apps.gtl.viewmodel.GtlViewModel
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecondaryScaffold(
    pageTitle: String,
    onBack: () -> Unit,
    compactTopBar: Boolean = false,
    content: @Composable () -> Unit
) {
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.brand_title),
                            color = TitleMagenta,
                            style = if (compactTopBar) {
                                MaterialTheme.typography.titleMedium
                            } else {
                                MaterialTheme.typography.titleLarge
                            }
                        )
                        Text(
                            text = pageTitle,
                            style = if (compactTopBar) {
                                MaterialTheme.typography.labelSmall
                            } else {
                                MaterialTheme.typography.labelLarge
                            },
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
    var qnhValue by remember {
        mutableFloatStateOf(state.settings.qnhHpa)
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
    LaunchedEffect(state.settings.qnhHpa) {
        qnhValue = state.settings.qnhHpa
    }
    var appearanceOpen by rememberSaveable { mutableStateOf(true) }
    var osmOpen by rememberSaveable { mutableStateOf(true) }
    var tuhuOpen by rememberSaveable { mutableStateOf(true) }
    var recordingOpen by rememberSaveable { mutableStateOf(true) }
    var baroOpen by rememberSaveable { mutableStateOf(true) }
    SecondaryScaffold(stringResource(R.string.settings_title), onBack, compactTopBar = true) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
            ) {
                val short = maxHeight < 520.dp
                val lineTrim = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.Both
                )
                val titleStyle = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeightStyle = lineTrim
                )
                val labelStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeightStyle = lineTrim
                )
                val chipStyle = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeightStyle = lineTrim
                )
                val usageStyle = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 13.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeightStyle = lineTrim
                )
                val iconSize = if (short) 16.dp else 18.dp
                val chipHeight = if (short) 22.dp else 26.dp
                val switchScale = if (short) 0.58f else 0.68f
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(stringResource(R.string.settings_usage), style = titleStyle, maxLines = 1)
                            Column(
                                modifier = Modifier.fillMaxWidth().selectableGroup(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                UsageType.selectable.chunked(3).forEach { rowTypes ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        rowTypes.forEach { type ->
                                            val selected = state.settings.usageType == type
                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .selectable(
                                                        selected = selected,
                                                        onClick = { viewModel.setUsage(type) },
                                                        role = Role.RadioButton
                                                    ),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Icon(
                                                    imageVector = usageIcon(type),
                                                    contentDescription = stringResource(usageLabel(type)),
                                                    modifier = Modifier.size(iconSize),
                                                    tint = if (selected) TitleMagenta else MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = stringResource(usageLabel(type)),
                                                    style = usageStyle,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                stringResource(R.string.settings_units),
                                style = titleStyle,
                                maxLines = 1
                            )
                            MeasurementSystem.entries.forEach { system ->
                                FilterChip(
                                    selected = state.settings.measurementSystem == system,
                                    onClick = { viewModel.setUnits(system) },
                                    label = {
                                        Text(
                                            system.name.lowercase().replaceFirstChar { it.titlecase() },
                                            style = chipStyle,
                                            maxLines = 1
                                        )
                                    },
                                    modifier = Modifier.heightIn(max = chipHeight)
                                )
                            }
                        }
                    }
                    item {
                        AccordionSection(
                            title = stringResource(R.string.settings_group_appearance),
                            expanded = appearanceOpen,
                            onToggle = { appearanceOpen = !appearanceOpen },
                            titleStyle = titleStyle
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SettingSwitch(
                                    stringResource(R.string.settings_offline),
                                    OsmOfflineAvailability.effectiveUseOffline(
                                        state.settings.useOfflineMap,
                                        state.hasDownloadedOsmMap
                                    ),
                                    labelStyle,
                                    switchScale,
                                    enabled = OsmOfflineAvailability.canEnable(state.hasDownloadedOsmMap)
                                ) {
                                    viewModel.setUseOfflineMap(it)
                                }
                                SettingSliderGroup {
                                    SettingSwitch(
                                        stringResource(R.string.settings_optimize),
                                        state.settings.optimizationActive,
                                        labelStyle,
                                        switchScale
                                    ) {
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
                                            ),
                                            labelStyle = chipStyle
                                        )
                                    }
                                }
                                SettingSwitch(
                                    stringResource(R.string.settings_show_track),
                                    state.settings.showLastTrackOnMap,
                                    labelStyle,
                                    switchScale
                                ) {
                                    viewModel.setShowLastTrackOnMap(it)
                                }
                                SettingSwitch(
                                    stringResource(R.string.settings_keep_whole_track),
                                    state.settings.keepWholeTrackOnScreen,
                                    labelStyle,
                                    switchScale
                                ) {
                                    viewModel.setKeepWholeTrackOnScreen(it)
                                }
                                SettingSwitch(
                                    stringResource(R.string.settings_show_accuracy),
                                    state.settings.showAccuracyMarker,
                                    labelStyle,
                                    switchScale
                                ) {
                                    viewModel.setShowAccuracyMarker(it)
                                }
                                SettingSwitch(
                                    stringResource(R.string.settings_show_fix_cloud),
                                    state.settings.showFixCloud,
                                    labelStyle,
                                    switchScale
                                ) {
                                    viewModel.setShowFixCloud(it)
                                }
                                SettingSwitch(
                                    stringResource(R.string.settings_keep_screen_on),
                                    state.settings.keepScreenOnWhileLogging,
                                    labelStyle,
                                    switchScale
                                ) {
                                    viewModel.setKeepScreenOnWhileLogging(it)
                                }
                            }
                        }
                    }
                    if (state.osmMapInUse) {
                        item {
                            AccordionSection(
                                title = stringResource(R.string.settings_group_osm),
                                expanded = osmOpen,
                                onToggle = { osmOpen = !osmOpen },
                                titleStyle = titleStyle
                            ) {
                                OsmLayerControls(
                                    options = state.settings.osmRenderOptions(),
                                    hillshadingAvailable = state.osmHillshadingAvailable,
                                    labelStyle = labelStyle,
                                    switchScale = switchScale,
                                    actions = OsmLayerActions(
                                        setBuildings = { viewModel.setOsmBuildings(it) },
                                        setPoi = { viewModel.setOsmPoi(it) },
                                        setTransit = { viewModel.setOsmTransit(it) },
                                        setCycleways = { viewModel.setOsmCycleways(it) },
                                        setParks = { viewModel.setOsmParks(it) },
                                        setHillshading = { viewModel.setOsmHillshading(it) }
                                    )
                                )
                            }
                        }
                    }
                    if (TuhuFeature.showSettings(state.tuhuMapInUse)) {
                        item {
                            AccordionSection(
                                title = stringResource(R.string.tuhu_settings_group),
                                expanded = tuhuOpen,
                                onToggle = { tuhuOpen = !tuhuOpen },
                                titleStyle = titleStyle
                            ) {
                                TuhuLayerControls(
                                    options = state.tuhuRenderOptions,
                                    hillshadingAvailable = state.tuhuHillshadingAvailable,
                                    labelStyle = labelStyle,
                                    switchScale = switchScale,
                                    actions = TuhuLayerActions(
                                        setBlazes = { viewModel.setTuhuBlazes(it) },
                                        setPaths = { viewModel.setTuhuPaths(it) },
                                        setContours = { viewModel.setTuhuContours(it) },
                                        setContoursMinor = { viewModel.setTuhuContoursMinor(it) },
                                        setHikePoi = { viewModel.setTuhuHikePoi(it) },
                                        setParks = { viewModel.setTuhuParks(it) },
                                        setUrbanPoi = { viewModel.setTuhuUrbanPoi(it) },
                                        setHillshading = { viewModel.setTuhuHillshading(it) }
                                    )
                                )
                            }
                        }
                    }
                    item {
                        AccordionSection(
                            title = stringResource(R.string.settings_group_recording),
                            expanded = recordingOpen,
                            onToggle = { recordingOpen = !recordingOpen },
                            titleStyle = titleStyle
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SettingSwitch(
                                    stringResource(R.string.settings_gnss_only),
                                    state.settings.gnssOnly,
                                    labelStyle,
                                    switchScale
                                ) {
                                    viewModel.setGnssOnly(it)
                                }
                                SettingSliderGroup {
                                    SettingSwitch(
                                        stringResource(R.string.settings_track_smoothing),
                                        state.settings.trackSmoothingEnabled,
                                        labelStyle,
                                        switchScale
                                    ) {
                                        viewModel.setTrackSmoothing(it)
                                    }
                                    if (state.settings.trackSmoothingEnabled) {
                                        EndpointSlider(
                                            value = strengthValue,
                                            onValueChange = { strengthValue = it },
                                            onValueChangeFinished = {
                                                viewModel.setSmoothingStrength(strengthValue)
                                            },
                                            valueRange = 0f..1f,
                                            steps = 0,
                                            startLabel = stringResource(R.string.settings_smoothing_low),
                                            endLabel = stringResource(R.string.settings_smoothing_high),
                                            labelStyle = chipStyle
                                        )
                                    }
                                }
                                SettingSwitch(
                                    stringResource(R.string.settings_stationary_lock),
                                    state.settings.stationaryLockEnabled,
                                    labelStyle,
                                    switchScale
                                ) {
                                    viewModel.setStationaryLock(it)
                                }
                                SettingSliderGroup {
                                    Text(
                                        text = stringResource(R.string.settings_recording_density),
                                        style = labelStyle,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    EndpointSlider(
                                        value = densityValue,
                                        onValueChange = { densityValue = it },
                                        onValueChangeFinished = {
                                            viewModel.setRecordingDensity(densityValue)
                                        },
                                        valueRange = 0f..1f,
                                        steps = 0,
                                        startLabel = stringResource(R.string.settings_density_smart),
                                        endLabel = stringResource(R.string.settings_density_every_fix),
                                        labelStyle = chipStyle
                                    )
                                }
                            }
                        }
                    }
                    if (state.live.pressureAvailable) {
                        item {
                            val gpsFix = state.live.lastLocation
                            val canCalibrate = state.live.pressureHpa != null &&
                                gpsFix != null &&
                                gpsFix.hasAltitude() &&
                                GpsAltitude.isPlausible(gpsFix.altitude)
                            val canReset = state.settings.baroPressureOffsetHpa != 0f
                            AccordionSection(
                                title = stringResource(R.string.settings_group_baro),
                                expanded = baroOpen,
                                onToggle = { baroOpen = !baroOpen },
                                titleStyle = titleStyle
                            ) {
                                SettingSwitch(
                                    stringResource(R.string.settings_baro_auto_calibrate),
                                    state.settings.autoCalibrateBaroEnabled,
                                    labelStyle,
                                    switchScale
                                ) {
                                    viewModel.setAutoCalibrateBaroEnabled(it)
                                }
                                SettingSliderGroup {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.settings_qnh, qnhValue.toInt()),
                                            style = titleStyle,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        FilterChip(
                                            selected = false,
                                            onClick = { viewModel.calibrateBaroFromGps() },
                                            enabled = canCalibrate,
                                            label = {
                                                Text(
                                                    stringResource(R.string.settings_qnh_calibrate),
                                                    style = chipStyle,
                                                    maxLines = 1
                                                )
                                            },
                                            modifier = Modifier.heightIn(max = chipHeight)
                                        )
                                        FilterChip(
                                            selected = false,
                                            onClick = { viewModel.resetBaroPressureOffset() },
                                            enabled = canReset,
                                            label = {
                                                Text(
                                                    stringResource(R.string.settings_qnh_reset),
                                                    style = chipStyle,
                                                    maxLines = 1
                                                )
                                            },
                                            modifier = Modifier.heightIn(max = chipHeight)
                                        )
                                    }
                                    EndpointSlider(
                                        value = qnhValue,
                                        onValueChange = { qnhValue = it },
                                        onValueChangeFinished = { viewModel.setQnhHpa(qnhValue) },
                                        valueRange = BaroAltitude.MinQnhHpa..BaroAltitude.MaxQnhHpa,
                                        steps = 199,
                                        startLabel = BaroAltitude.MinQnhHpa.toInt().toString(),
                                        endLabel = BaroAltitude.MaxQnhHpa.toInt().toString(),
                                        labelStyle = chipStyle
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSliderGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        content = content
    )
}

@Composable
private fun EndpointSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    startLabel: String,
    endLabel: String,
    labelStyle: TextStyle,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = startLabel,
            style = labelStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(min = 36.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.weight(1f).height(12.dp).scale(0.75f)
        )
        Text(
            text = endLabel,
            style = labelStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(min = 36.dp)
        )
    }
}

private fun usageLabel(type: UsageType): Int {
    return when (type) {
        UsageType.AIRCRAFT -> R.string.usage_aircraft
        UsageType.WATERCRAFT -> R.string.usage_watercraft
        UsageType.FOUR_WHEELERS -> R.string.usage_four_wheelers
        UsageType.TWO_WHEELERS -> R.string.usage_two_wheelers
        UsageType.BICYCLE -> R.string.usage_bicycle
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
internal fun SettingSwitch(
    label: String,
    checked: Boolean,
    labelStyle: TextStyle,
    switchScale: Float,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 22.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f).padding(end = 8.dp),
            style = labelStyle,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.38f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Box(
            modifier = Modifier.size(width = 42.dp, height = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            Switch(
                checked = checked,
                onCheckedChange = onChange,
                enabled = enabled,
                modifier = Modifier.scale(switchScale)
            )
        }
    }
}

@Composable
private fun OsmActionButton(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
        modifier = Modifier.height(28.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

@Composable
fun OsmDownloadScreen(viewModel: GtlViewModel, onBack: () -> Unit) {
    SecondaryScaffold(stringResource(R.string.osm_title), onBack) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            if (TuhuFeature.showDownloadRow()) {
                item {
                    TuhuDownloadRow(viewModel)
                }
            }
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
    val mapsRevision by viewModel.observeDownloadedRevision().collectAsState()
    val prefs by viewModel.settings.collectAsState()
    val downloaded = remember(mapsRevision, download.running, download.failed, region.id) {
        viewModel.isDownloaded(region)
    }
    val inUse = remember(downloaded, prefs.useOfflineMap, prefs.selectedMapFile, mapsRevision, region.id) {
        downloaded && viewModel.isRegionInUse(region)
    }
    var pendingDelete by rememberSaveable { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(region.label, style = MaterialTheme.typography.titleLarge)
                Text(
                    if (downloaded) stringResource(R.string.osm_downloaded) else region.id,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 28.dp) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (downloaded) {
                        OsmActionButton(
                            stringResource(if (inUse) R.string.osm_in_use else R.string.osm_can_use)
                        ) {
                            viewModel.toggleDownloadedMap(region)
                        }
                        OsmActionButton(stringResource(R.string.action_delete)) {
                            pendingDelete = true
                        }
                    } else {
                        OsmActionButton(
                            stringResource(R.string.osm_download),
                            enabled = !download.running
                        ) {
                            viewModel.downloadRegion(region)
                        }
                    }
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
    if (pendingDelete) {
        AlertDialog(
            onDismissRequest = { pendingDelete = false },
            title = { Text(stringResource(R.string.osm_delete_title)) },
            text = { Text(stringResource(R.string.osm_delete_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteDownloadedMap(region)
                        pendingDelete = false
                    }
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TracksScreen(
    state: GtlUiState,
    viewModel: GtlViewModel,
    onBack: () -> Unit,
    onShare: (Set<Long>, TrackShareFormat) -> Unit,
    onShowOnMap: (Long) -> Unit
) {
    val inspectDump by viewModel.inspectDump.collectAsStateWithLifecycle()
    val dump = inspectDump
    if (dump != null) {
        BackHandler { viewModel.closeInspect() }
        SessionInspectScreen(
            text = dump,
            onBack = { viewModel.closeInspect() }
        )
        return
    }
    val format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    val sessionIds = state.sessions.map { it.id }.toSet()
    val visibleSelected = selectedIds.intersect(sessionIds)
    val allSelected = sessionIds.isNotEmpty() && visibleSelected.size == sessionIds.size
    var sharePicker by remember { mutableStateOf(false) }
    var pendingDeleteId by remember { mutableStateOf<Long?>(null) }
    val elevationSessionId by viewModel.savedElevationId.collectAsStateWithLifecycle()
    val elevationSamples by viewModel.savedElevation.collectAsStateWithLifecycle()
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
                    onClick = { sharePicker = true },
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
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .onTripleTap(session.id) {
                                    viewModel.inspectSession(session.id)
                                }
                        ) {
                            Text(format.format(Date(session.startedAt)), style = MaterialTheme.typography.titleLarge)
                            Text(
                                "${session.usageType} · ${session.measurementSystem}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 48.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(onClick = { onShowOnMap(session.id) }) {
                            Text(stringResource(R.string.action_show_on_map))
                        }
                        Button(onClick = { viewModel.toggleSavedElevation(session.id) }) {
                            Text(stringResource(R.string.tracks_elevation))
                        }
                        Button(onClick = { pendingDeleteId = session.id }) {
                            Text(stringResource(R.string.action_delete))
                        }
                    }
                    if (elevationSessionId == session.id) {
                        val units = runCatching {
                            MeasurementSystem.valueOf(session.measurementSystem)
                        }.getOrDefault(MeasurementSystem.METRIC)
                        ElevationProfile(
                            samples = elevationSamples,
                            system = units,
                            modifier = Modifier.padding(start = 48.dp, top = 8.dp)
                        )
                    }
                }
            }
        }
    }
    if (pendingDeleteId != null) {
        val deleteId = pendingDeleteId
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text(stringResource(R.string.tracks_delete_title)) },
            text = { Text(stringResource(R.string.tracks_delete_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (deleteId != null) {
                            viewModel.deleteSession(deleteId)
                            selectedIds = visibleSelected - deleteId
                        }
                        pendingDeleteId = null
                    }
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
    if (sharePicker) {
        AlertDialog(
            onDismissRequest = { sharePicker = false },
            title = { Text(stringResource(R.string.tracks_share_as)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            sharePicker = false
                            onShare(visibleSelected, TrackShareFormat.KMZ)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.tracks_share_kmz))
                    }
                    Button(
                        onClick = {
                            sharePicker = false
                            onShare(visibleSelected, TrackShareFormat.GPX)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.tracks_share_gpx))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { sharePicker = false }) {
                    Text(stringResource(R.string.action_back))
                }
            }
        )
    }
}

private fun Modifier.onTripleTap(key: Any, onTripleTap: () -> Unit): Modifier = pointerInput(key) {
    var count = 0
    var lastAt = 0L
    detectTapGestures {
        val now = SystemClock.elapsedRealtime()
        count = if (now - lastAt <= 500L) count + 1 else 1
        lastAt = now
        if (count >= 3) {
            count = 0
            onTripleTap()
        }
    }
}

@Composable
private fun SessionInspectScreen(text: String, onBack: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    val vertical = rememberScrollState()
    val horizontal = rememberScrollState()
    SecondaryScaffold("gps_events", onBack) {
        Column(modifier = Modifier.fillMaxSize()) {
            TextButton(onClick = { clipboard.setText(AnnotatedString(text)) }) {
                Text("Copy")
            }
            SelectionContainer(modifier = Modifier.weight(1f)) {
                Text(
                    text = text,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .verticalScroll(vertical)
                        .horizontalScroll(horizontal)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun HelpScreen(onBack: () -> Unit, tuhuMapDownloaded: Boolean = false) {
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
                AccordionSection(
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
                AccordionSection(
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
                AccordionSection(
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
                AccordionSection(
                    title = stringResource(R.string.help_gps_title),
                    expanded = expandedId == HelpSectionGps,
                    onToggle = { expandedId = toggleHelpSection(expandedId, HelpSectionGps) }
                ) {
                    Text(stringResource(R.string.help_gps_body), style = MaterialTheme.typography.bodyLarge)
                }
            }
            item {
                AccordionSection(
                    title = stringResource(R.string.help_route_title),
                    expanded = expandedId == HelpSectionRoute,
                    onToggle = { expandedId = toggleHelpSection(expandedId, HelpSectionRoute) }
                ) {
                    Text(stringResource(R.string.help_route_body), style = MaterialTheme.typography.bodyLarge)
                }
            }
            item {
                AccordionSection(
                    title = stringResource(R.string.help_map_title),
                    expanded = expandedId == HelpSectionMap,
                    onToggle = { expandedId = toggleHelpSection(expandedId, HelpSectionMap) }
                ) {
                    Text(stringResource(R.string.help_map_body), style = MaterialTheme.typography.bodyLarge)
                }
            }
            if (TuhuFeature.showHelp(tuhuMapDownloaded)) {
                item {
                    AccordionSection(
                        title = stringResource(R.string.tuhu_help_title),
                        expanded = expandedId == HelpSectionTuhu,
                        onToggle = { expandedId = toggleHelpSection(expandedId, HelpSectionTuhu) }
                    ) {
                        TuhuHelpSection()
                    }
                }
            }
            item {
                AccordionSection(
                    title = stringResource(R.string.help_compass_title),
                    expanded = expandedId == HelpSectionCompass,
                    onToggle = { expandedId = toggleHelpSection(expandedId, HelpSectionCompass) }
                ) {
                    Text(stringResource(R.string.help_compass_body), style = MaterialTheme.typography.bodyLarge)
                }
            }
            item {
                AccordionSection(
                    title = stringResource(R.string.help_kml_title),
                    expanded = expandedId == HelpSectionKmz,
                    onToggle = { expandedId = toggleHelpSection(expandedId, HelpSectionKmz) }
                ) {
                    Text(stringResource(R.string.help_kml_body), style = MaterialTheme.typography.bodyLarge)
                }
            }
            item {
                AccordionSection(
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
                AccordionSection(
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
private const val HelpSectionTuhu = "tuhu"
private const val HelpSectionCompass = "compass"
private const val HelpSectionKmz = "kmz"
private const val HelpSectionPrivacy = "privacy"
private const val HelpSectionTrackpoint = "trackpoint"

private fun toggleHelpSection(expandedId: String, sectionId: String): String {
    return if (expandedId == sectionId) "" else sectionId
}

@Composable
private fun AccordionSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    titleStyle: TextStyle = MaterialTheme.typography.titleLarge,
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
                    style = titleStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
    val copyrightUrl = stringResource(R.string.about_osm_copyright_url)
    val websiteUrl = stringResource(R.string.about_osm_website_url)
    SecondaryScaffold(stringResource(R.string.about_title), onBack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("${com.lkovari.mobile.apps.gtl.BuildConfig.VERSION_NAME}  ·  com.lkovari.mobile.apps.gtl")
            Text("${stringResource(R.string.about_device)}: $deviceName")
            Text(stringResource(R.string.about_author))
            Text(stringResource(R.string.about_body))
            Text(
                text = stringResource(R.string.about_osm_title),
                style = MaterialTheme.typography.titleLarge
            )
            Text(stringResource(R.string.about_osm_body))
            AboutLink(stringResource(R.string.about_osm_website), websiteUrl)
            AboutLink(stringResource(R.string.about_osm_copyright), copyrightUrl)
            TuhuAboutSection()
        }
    }
}

@Composable
private fun AboutLink(label: String, url: String) {
    val context = LocalContext.current
    Text(
        text = label,
        color = TitleMagenta,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.clickable {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    )
}
