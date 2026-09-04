package com.lkovari.mobile.apps.gtl.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lkovari.mobile.apps.gtl.R
import com.lkovari.mobile.apps.gtl.engine.GnssConstellation
import com.lkovari.mobile.apps.gtl.engine.GnssSnapshot
import com.lkovari.mobile.apps.gtl.engine.SignalQuality
import com.lkovari.mobile.apps.gtl.ui.theme.AmberFix
import com.lkovari.mobile.apps.gtl.ui.theme.CarmineTrack
import com.lkovari.mobile.apps.gtl.ui.theme.GnssLime
import com.lkovari.mobile.apps.gtl.ui.theme.HudTeal
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ConstellationStrip(snapshot: GnssSnapshot?, modifier: Modifier = Modifier) {
    val items = listOf(
        "GPS L1" to snapshot?.gpsL1,
        "GPS L5" to snapshot?.gpsL5,
        "GAL" to snapshot?.byConstellation?.get(GnssConstellation.GALILEO),
        "GLO" to snapshot?.byConstellation?.get(GnssConstellation.GLONASS),
        "BDS" to snapshot?.byConstellation?.get(GnssConstellation.BEIDOU),
        "QZSS" to snapshot?.byConstellation?.get(GnssConstellation.QZSS),
        "NavIC" to snapshot?.byConstellation?.get(GnssConstellation.IRNSS)
    )
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items.chunked(4).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowItems.forEach { (label, count) ->
                    ConstellationChip(
                        label = label,
                        used = count?.usedInFix ?: 0,
                        inView = count?.inView ?: 0,
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(4 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ConstellationChip(
    label: String,
    used: Int,
    inView: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            textAlign = TextAlign.Center
        )
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "99/99",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 1,
                softWrap = false,
                color = Color.Transparent
            )
            Text(
                text = "$used/$inView",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center,
                color = if (used > 0) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
fun SnrMeter(snapshot: GnssSnapshot?, modifier: Modifier = Modifier) {
    val quality = snapshot?.signalQuality ?: SignalQuality.NONE
    val cn0 = snapshot?.usedAverageCn0 ?: snapshot?.averageCn0 ?: 0.0
    val qualityColor = when (quality) {
        SignalQuality.EXCELLENT -> GnssLime
        SignalQuality.GOOD -> HudTeal
        SignalQuality.FAIR -> AmberFix
        SignalQuality.POOR, SignalQuality.NONE -> CarmineTrack
    }
    val qualityLabel = when (quality) {
        SignalQuality.EXCELLENT -> stringResource(R.string.snr_excellent)
        SignalQuality.GOOD -> stringResource(R.string.snr_good)
        SignalQuality.FAIR -> stringResource(R.string.snr_fair)
        SignalQuality.POOR -> stringResource(R.string.snr_poor)
        SignalQuality.NONE -> stringResource(R.string.snr_none)
    }
    val fraction = (cn0 / 45.0).coerceIn(0.0, 1.0).toFloat()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.gps_snr).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = qualityLabel,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = qualityColor
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .background(qualityColor)
            )
        }
        Text(
            text = if (snapshot == null || quality == SignalQuality.NONE) {
                stringResource(R.string.snr_waiting)
            } else {
                stringResource(
                    R.string.snr_detail,
                    String.format(java.util.Locale.US, "%.1f", cn0),
                    snapshot.satellitesInFix,
                    snapshot.satellitesInView
                )
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            minLines = 2
        )
    }
}

@Composable
fun HudMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun CompassDial(azimuth: Float, modifier: Modifier = Modifier) {
    val needle = MaterialTheme.colorScheme.tertiary
    val ring = MaterialTheme.colorScheme.primary
    val tick = MaterialTheme.colorScheme.onSurfaceVariant
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxWidth().height(260.dp)) {
            val radius = size.minDimension / 2.2f
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(color = ring.copy(alpha = 0.25f), radius = radius, center = center)
            drawCircle(color = ring, radius = radius, center = center, style = Stroke(width = 6f))
            for (deg in 0 until 360 step 10) {
                val rad = Math.toRadians(deg.toDouble())
                val inner = if (deg % 90 == 0) radius - 18f else radius - 10f
                val start = Offset(
                    center.x + (inner * sin(rad)).toFloat(),
                    center.y - (inner * cos(rad)).toFloat()
                )
                val end = Offset(
                    center.x + (radius * sin(rad)).toFloat(),
                    center.y - (radius * cos(rad)).toFloat()
                )
                drawLine(tick, start, end, strokeWidth = if (deg % 90 == 0) 4f else 2f, cap = StrokeCap.Round)
            }
            val heading = Math.toRadians(azimuth.toDouble())
            val tip = Offset(
                center.x + ((radius - 28f) * sin(heading)).toFloat(),
                center.y - ((radius - 28f) * cos(heading)).toFloat()
            )
            drawLine(needle, center, tip, strokeWidth = 8f, cap = StrokeCap.Round)
            drawCircle(Color.White, 8f, center)
        }
    }
}
