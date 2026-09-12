package com.lkovari.mobile.apps.gtl.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lkovari.mobile.apps.gtl.R
import com.lkovari.mobile.apps.gtl.engine.ElevationSample
import com.lkovari.mobile.apps.gtl.engine.ElevationSeries
import com.lkovari.mobile.apps.gtl.engine.MeasurementSystem
import com.lkovari.mobile.apps.gtl.engine.Units
import com.lkovari.mobile.apps.gtl.ui.theme.CarmineTrack
import com.lkovari.mobile.apps.gtl.ui.theme.HudTeal
import com.lkovari.mobile.apps.gtl.ui.theme.MoonAmber

@Composable
fun ElevationProfile(
    samples: List<ElevationSample>,
    system: MeasurementSystem,
    modifier: Modifier = Modifier
) {
    if (samples.size < 2) {
        Text(
            text = stringResource(R.string.elevation_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(vertical = 8.dp)
        )
        return
    }
    val minGps = samples.minOf { it.gpsAltitude }
    val maxGps = samples.maxOf { it.gpsAltitude }
    val baroSamples = if (ElevationSeries.hasBaroLine(samples)) {
        samples.mapNotNull { sample ->
            val baro = sample.baroAltitude
            if (baro == null) {
                null
            } else {
                sample.distanceMeters to baro
            }
        }
    } else {
        emptyList()
    }
    val minAlt = if (baroSamples.isEmpty()) {
        minGps
    } else {
        minOf(minGps, baroSamples.minOf { it.second })
    }
    val maxAlt = if (baroSamples.isEmpty()) {
        maxGps
    } else {
        maxOf(maxGps, baroSamples.maxOf { it.second })
    }
    val span = (maxAlt - minAlt).coerceAtLeast(1.0)
    val maxDistance = samples.last().distanceMeters.coerceAtLeast(1.0)
    Column(modifier = modifier.fillMaxWidth()) {
        Box {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .padding(top = 18.dp, bottom = 8.dp)
            ) {
                val gpsPath = Path()
                val fillPath = Path()
                samples.forEachIndexed { index, sample ->
                    val x = (sample.distanceMeters / maxDistance).toFloat() * size.width
                    val y = size.height - ((sample.gpsAltitude - minAlt) / span).toFloat() * size.height
                    if (index == 0) {
                        gpsPath.moveTo(x, y)
                        fillPath.moveTo(x, size.height)
                        fillPath.lineTo(x, y)
                    } else {
                        gpsPath.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                }
                fillPath.lineTo(size.width, size.height)
                fillPath.close()
                drawPath(fillPath, HudTeal.copy(alpha = 0.22f), style = Fill)
                drawPath(
                    gpsPath,
                    CarmineTrack,
                    style = Stroke(width = 4f, cap = StrokeCap.Round)
                )
                if (baroSamples.size >= 2) {
                    val baroPath = Path()
                    baroSamples.forEachIndexed { index, pair ->
                        val x = (pair.first / maxDistance).toFloat() * size.width
                        val y = size.height - ((pair.second - minAlt) / span).toFloat() * size.height
                        if (index == 0) {
                            baroPath.moveTo(x, y)
                        } else {
                            baroPath.lineTo(x, y)
                        }
                    }
                    drawPath(
                        baroPath,
                        MoonAmber,
                        style = Stroke(
                            width = 3f,
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                        )
                    )
                }
                drawLine(
                    color = CarmineTrack.copy(alpha = 0.35f),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1f
                )
            }
            Text(
                text = Units.formatAltitude(maxGps, system),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.TopEnd)
            )
            Text(
                text = Units.formatAltitude(minGps, system),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
        Text(
            text = if (baroSamples.size >= 2) {
                stringResource(R.string.elevation_legend_both)
            } else {
                stringResource(R.string.elevation_legend_gps)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
