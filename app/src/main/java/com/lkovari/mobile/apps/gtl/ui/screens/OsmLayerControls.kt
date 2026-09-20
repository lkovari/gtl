package com.lkovari.mobile.apps.gtl.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.lkovari.mobile.apps.gtl.R
import com.lkovari.mobile.apps.gtl.engine.OsmRenderOptions

class OsmLayerActions(
    val setBuildings: (Boolean) -> Unit,
    val setPoi: (Boolean) -> Unit,
    val setTransit: (Boolean) -> Unit,
    val setCycleways: (Boolean) -> Unit,
    val setParks: (Boolean) -> Unit,
    val setHillshading: (Boolean) -> Unit
)

@Composable
fun OsmLayerControls(
    options: OsmRenderOptions,
    hillshadingAvailable: Boolean,
    labelStyle: TextStyle,
    switchScale: Float,
    actions: OsmLayerActions,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SettingSwitch(
            stringResource(R.string.settings_osm_buildings),
            options.buildings,
            labelStyle,
            switchScale
        ) {
            actions.setBuildings(it)
        }
        SettingSwitch(
            stringResource(R.string.settings_osm_poi),
            options.poi,
            labelStyle,
            switchScale
        ) {
            actions.setPoi(it)
        }
        Text(
            text = stringResource(R.string.settings_osm_poi_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SettingSwitch(
            stringResource(R.string.settings_osm_transit),
            options.transit,
            labelStyle,
            switchScale
        ) {
            actions.setTransit(it)
        }
        Text(
            text = stringResource(R.string.settings_osm_transit_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SettingSwitch(
            stringResource(R.string.settings_osm_cycleways),
            options.cycleways,
            labelStyle,
            switchScale
        ) {
            actions.setCycleways(it)
        }
        Text(
            text = stringResource(R.string.settings_osm_cycleways_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SettingSwitch(
            stringResource(R.string.settings_osm_parks),
            options.parks,
            labelStyle,
            switchScale
        ) {
            actions.setParks(it)
        }
        Text(
            text = stringResource(R.string.settings_osm_parks_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SettingSwitch(
            stringResource(R.string.settings_osm_hillshading),
            options.hillshading && hillshadingAvailable,
            labelStyle,
            switchScale,
            enabled = hillshadingAvailable
        ) {
            actions.setHillshading(it)
        }
        Text(
            text = stringResource(
                if (hillshadingAvailable) {
                    R.string.settings_osm_hillshading_hint
                } else {
                    R.string.settings_osm_hillshading_unavailable
                }
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
