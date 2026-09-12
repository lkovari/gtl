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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lkovari.mobile.apps.gtl.R
import com.lkovari.mobile.apps.gtl.engine.ConstellationCount
import com.lkovari.mobile.apps.gtl.engine.GnssConstellation
import com.lkovari.mobile.apps.gtl.engine.GnssSnapshot
import com.lkovari.mobile.apps.gtl.engine.SignalQuality
import com.lkovari.mobile.apps.gtl.ui.theme.AmberFix
import com.lkovari.mobile.apps.gtl.ui.theme.CarmineTrack
import com.lkovari.mobile.apps.gtl.ui.theme.Cockpit
import com.lkovari.mobile.apps.gtl.ui.theme.GnssLime
import com.lkovari.mobile.apps.gtl.ui.theme.HudCyan
import com.lkovari.mobile.apps.gtl.ui.theme.HudTeal
import com.lkovari.mobile.apps.gtl.ui.theme.MoonCream
import com.lkovari.mobile.apps.gtl.ui.theme.NightInk
import com.lkovari.mobile.apps.gtl.ui.theme.gnssConstellationColor
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ConstellationStrip(
    snapshot: GnssSnapshot?,
    provider: String? = null,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        ChipSpec("GPS L1", snapshot?.gpsL1, GnssConstellation.GPS),
        ChipSpec("GPS L5", snapshot?.gpsL5, GnssConstellation.GPS),
        ChipSpec("GAL", snapshot?.byConstellation?.get(GnssConstellation.GALILEO), GnssConstellation.GALILEO),
        ChipSpec("GLO", snapshot?.byConstellation?.get(GnssConstellation.GLONASS), GnssConstellation.GLONASS),
        ChipSpec("BDS", snapshot?.byConstellation?.get(GnssConstellation.BEIDOU), GnssConstellation.BEIDOU),
        ChipSpec("QZSS", snapshot?.byConstellation?.get(GnssConstellation.QZSS), GnssConstellation.QZSS),
        ChipSpec("NavIC", snapshot?.byConstellation?.get(GnssConstellation.IRNSS), GnssConstellation.IRNSS)
    )
    val rows = items.chunked(4)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        rows.forEachIndexed { rowIndex, rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowItems.forEach { spec ->
                    ConstellationChip(
                        label = spec.label,
                        used = spec.count?.usedInFix ?: 0,
                        inView = spec.count?.inView ?: 0,
                        labelColor = gnssConstellationColor(spec.constellation),
                        modifier = Modifier.weight(1f)
                    )
                }
                val empty = 4 - rowItems.size
                if (rowIndex == rows.lastIndex && empty > 0) {
                    ProviderChip(provider = provider, modifier = Modifier.weight(1f))
                    repeat(empty - 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                } else {
                    repeat(empty) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private data class ChipSpec(
    val label: String,
    val count: ConstellationCount?,
    val constellation: GnssConstellation
)

@Composable
private fun ConstellationChip(
    label: String,
    used: Int,
    inView: Int,
    labelColor: Color,
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
            color = labelColor,
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
private fun ProviderChip(provider: String?, modifier: Modifier = Modifier) {
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
            text = stringResource(R.string.gps_provider).uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.4.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = shortProvider(provider),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun shortProvider(raw: String?): String {
    if (raw.isNullOrBlank()) {
        return "—"
    }
    val lower = raw.lowercase()
    return when {
        lower == "gps" -> "GPS"
        "fused" in lower -> "Fused"
        "network" in lower -> "NET"
        "passive" in lower -> "PAS"
        raw.length <= 5 -> raw
        else -> raw.take(5)
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
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
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
                fontSize = 14.sp,
                color = qualityColor
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
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
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun HudMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    labelSuffix: String? = null
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(if (compact) 10.dp else 14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(
                horizontal = if (compact) 8.dp else 12.dp,
                vertical = if (compact) 4.dp else 12.dp
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label.uppercase(),
                style = if (compact) {
                    MaterialTheme.typography.labelSmall
                } else {
                    MaterialTheme.typography.labelLarge
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false
            )
            if (labelSuffix != null) {
                Text(
                    text = labelSuffix,
                    fontSize = if (compact) 9.sp else 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            fontSize = if (compact) 15.sp else 20.sp,
            lineHeight = if (compact) 18.sp else 24.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun CompassDial(azimuth: Float, referenceLabel: String, modifier: Modifier = Modifier) {
    val dark = MaterialTheme.colorScheme.background == Cockpit
    val bezel = if (dark) HudCyan else HudTeal
    val cardinal = if (dark) MoonCream else NightInk
    val tick = MaterialTheme.colorScheme.onSurfaceVariant
    val heading = ((azimuth % 360f) + 360f) % 360f
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxWidth().height(320.dp)) {
            val radius = size.minDimension / 2.15f
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(color = bezel.copy(alpha = 0.12f), radius = radius, center = center)
            drawCircle(color = bezel, radius = radius, center = center, style = Stroke(width = 5f))
            rotate(degrees = -heading, pivot = center) {
                for (deg in 0 until 360 step 10) {
                    val rad = Math.toRadians(deg.toDouble())
                    val major = deg % 30 == 0
                    val inner = if (major) radius - 22f else radius - 12f
                    val start = Offset(
                        center.x + (inner * sin(rad)).toFloat(),
                        center.y - (inner * cos(rad)).toFloat()
                    )
                    val end = Offset(
                        center.x + ((radius - 4f) * sin(rad)).toFloat(),
                        center.y - ((radius - 4f) * cos(rad)).toFloat()
                    )
                    val color = if (deg == 0) CarmineTrack else tick
                    drawLine(color, start, end, strokeWidth = if (major) 4.5f else 2f, cap = StrokeCap.Round)
                }
                val labelRadius = radius - 48f
                val paint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 34f
                    isFakeBoldText = true
                }
                drawIntoCanvas { canvas ->
                    val native = canvas.nativeCanvas
                    fun label(deg: Int, text: String, color: Color) {
                        paint.color = color.toArgb()
                        val rad = Math.toRadians(deg.toDouble())
                        val x = center.x + (labelRadius * sin(rad)).toFloat()
                        val y = center.y - (labelRadius * cos(rad)).toFloat() + 12f
                        native.drawText(text, x, y, paint)
                    }
                    label(0, "N", CarmineTrack)
                    label(90, "E", cardinal)
                    label(180, "S", cardinal)
                    label(270, "W", cardinal)
                }
            }
            val lubber = Path().apply {
                moveTo(center.x, center.y - radius + 6f)
                lineTo(center.x - 10f, center.y - radius + 28f)
                lineTo(center.x + 10f, center.y - radius + 28f)
                close()
            }
            drawPath(lubber, CarmineTrack)
            drawLine(
                bezel,
                Offset(center.x, center.y - 36f),
                Offset(center.x, center.y - radius + 32f),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = String.format(java.util.Locale.US, "%03.0f°", heading),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                fontSize = 36.sp,
                color = bezel
            )
            Text(
                text = referenceLabel,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                letterSpacing = 1.sp,
                color = bezel
            )
        }
    }
}
