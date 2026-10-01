package com.lkovari.mobile.apps.gtl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lkovari.mobile.apps.gtl.R
import com.lkovari.mobile.apps.gtl.engine.MeasurementSystem
import com.lkovari.mobile.apps.gtl.engine.SpeedBand
import com.lkovari.mobile.apps.gtl.engine.SpeedBands
import com.lkovari.mobile.apps.gtl.engine.Units
import com.lkovari.mobile.apps.gtl.engine.UsageType
import com.lkovari.mobile.apps.gtl.ui.theme.NightInk

@Composable
fun SpeedLegend(
    usage: UsageType,
    system: MeasurementSystem,
    expanded: Boolean,
    onToggle: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val edges = SpeedBands.edges(usage, system)
    val description = stringResource(R.string.map_speed_legend)
    val bands = listOf(
        SpeedBand.Slow,
        SpeedBand.Brisk,
        SpeedBand.Mid,
        SpeedBand.Fast,
        SpeedBand.Rapid,
        SpeedBand.High,
        SpeedBand.Max
    )
    val rows = bands.mapIndexed { index, band ->
        val label = when (index) {
            0 -> "0–${edges.first()}"
            bands.lastIndex -> "${edges.last()}+"
            else -> "${edges[index - 1]}–${edges[index]}"
        }
        band to label
    }
    val casing = modifier
        .semantics { contentDescription = description }
        .then(if (onToggle != null) Modifier.clickable(onClick = onToggle) else Modifier)
        .background(Color(SpeedBands.CasingArgb).copy(alpha = 0.86f), RoundedCornerShape(8.dp))
        .padding(horizontal = 8.dp, vertical = 6.dp)
    if (expanded) {
        Column(
            modifier = casing,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            rows.forEach { (band, label) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(SpeedBands.argb(band)), CircleShape)
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = NightInk
                    )
                }
            }
            Text(
                text = Units.hudSpeedUnit(system),
                style = MaterialTheme.typography.labelSmall,
                color = NightInk,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    } else {
        Row(
            modifier = casing,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            rows.forEach { (band, _) ->
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color(SpeedBands.argb(band)), CircleShape)
                )
            }
        }
    }
}
