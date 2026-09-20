package com.lkovari.mobile.apps.gtl.tuhu

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
import com.lkovari.mobile.apps.gtl.ui.screens.SettingSwitch

class TuhuLayerActions(
    val setBlazes: (Boolean) -> Unit,
    val setPaths: (Boolean) -> Unit,
    val setContours: (Boolean) -> Unit,
    val setContoursMinor: (Boolean) -> Unit,
    val setHikePoi: (Boolean) -> Unit,
    val setParks: (Boolean) -> Unit,
    val setUrbanPoi: (Boolean) -> Unit,
    val setHillshading: (Boolean) -> Unit
)

@Composable
fun TuhuLayerControls(
    options: TuhuRenderOptions,
    hillshadingAvailable: Boolean,
    labelStyle: TextStyle,
    switchScale: Float,
    actions: TuhuLayerActions,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SettingSwitch(
            stringResource(R.string.tuhu_blazes),
            options.blazes,
            labelStyle,
            switchScale
        ) {
            actions.setBlazes(it)
        }
        Text(
            text = stringResource(R.string.tuhu_blazes_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SettingSwitch(
            stringResource(R.string.tuhu_paths),
            options.paths,
            labelStyle,
            switchScale
        ) {
            actions.setPaths(it)
        }
        Text(
            text = stringResource(R.string.tuhu_paths_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SettingSwitch(
            stringResource(R.string.tuhu_contours),
            options.contours,
            labelStyle,
            switchScale
        ) {
            actions.setContours(it)
        }
        Text(
            text = stringResource(R.string.tuhu_contours_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SettingSwitch(
            stringResource(R.string.tuhu_contours_minor),
            options.contoursMinor,
            labelStyle,
            switchScale
        ) {
            actions.setContoursMinor(it)
        }
        Text(
            text = stringResource(R.string.tuhu_contours_hint_minor),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SettingSwitch(
            stringResource(R.string.tuhu_hike_poi),
            options.hikePoi,
            labelStyle,
            switchScale
        ) {
            actions.setHikePoi(it)
        }
        Text(
            text = stringResource(R.string.tuhu_hike_poi_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SettingSwitch(
            stringResource(R.string.tuhu_parks),
            options.parks,
            labelStyle,
            switchScale
        ) {
            actions.setParks(it)
        }
        Text(
            text = stringResource(R.string.tuhu_parks_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SettingSwitch(
            stringResource(R.string.tuhu_urban_poi),
            options.urbanPoi,
            labelStyle,
            switchScale
        ) {
            actions.setUrbanPoi(it)
        }
        Text(
            text = stringResource(R.string.tuhu_urban_poi_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SettingSwitch(
            stringResource(R.string.tuhu_hillshading),
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
                    R.string.tuhu_hillshading_hint
                } else {
                    R.string.tuhu_hillshading_unavailable
                }
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
