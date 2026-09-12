package com.lkovari.mobile.apps.gtl.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lkovari.mobile.apps.gtl.R
import com.lkovari.mobile.apps.gtl.engine.GnssSnapshot
import com.lkovari.mobile.apps.gtl.engine.SkyplotMarker
import com.lkovari.mobile.apps.gtl.engine.SkyplotMarkers
import com.lkovari.mobile.apps.gtl.engine.SkyplotProjection
import com.lkovari.mobile.apps.gtl.ui.theme.CarmineTrack
import com.lkovari.mobile.apps.gtl.ui.theme.Cockpit
import com.lkovari.mobile.apps.gtl.ui.theme.HudCyan
import com.lkovari.mobile.apps.gtl.ui.theme.HudTeal
import com.lkovari.mobile.apps.gtl.ui.theme.MoonCream
import com.lkovari.mobile.apps.gtl.ui.theme.NightInk
import com.lkovari.mobile.apps.gtl.ui.theme.gnssConstellationColor

@Composable
fun GnssSkyplot(snapshot: GnssSnapshot?, modifier: Modifier = Modifier) {
    val dark = MaterialTheme.colorScheme.background == Cockpit
    val bezel = if (dark) HudCyan else HudTeal
    val cardinal = if (dark) MoonCream else NightInk
    val l5Stroke = if (dark) MoonCream else NightInk
    val markers = remember(snapshot?.satellites) {
        SkyplotMarkers.from(snapshot?.satellites.orEmpty())
    }
    val titleStyle = cornerLabelStyle(MaterialTheme.typography.labelLarge)
    val legendStyle = cornerLabelStyle(
        MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp)
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val circleSize = minOf(maxWidth, 196.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(circleSize)
            ) {
                Canvas(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(circleSize)
                ) {
                    val radius = size.minDimension / 2f - 3.5f
                    val center = Offset(size.width / 2f, size.height / 2f)
                    drawCircle(color = bezel.copy(alpha = 0.10f), radius = radius, center = center)
                    for (elevation in listOf(0f, 30f, 60f)) {
                        val ring = SkyplotProjection.offset(0f, elevation, center.x, center.y, radius)
                        if (ring != null) {
                            val ringRadius = (center.y - ring.y).coerceAtLeast(1f)
                            val stroke = if (elevation == 0f) 3.5f else 1.6f
                            drawCircle(
                                color = bezel.copy(alpha = if (elevation == 0f) 1f else 0.55f),
                                radius = ringRadius,
                                center = center,
                                style = Stroke(width = stroke)
                            )
                        }
                    }
                    drawCardinals(center, radius, cardinal)
                    for (marker in markers) {
                        val point = SkyplotProjection.offset(
                            marker.azimuthDegrees,
                            marker.elevationDegrees,
                            center.x,
                            center.y,
                            radius
                        ) ?: continue
                        drawMarker(marker, Offset(point.x, point.y), l5Stroke)
                    }
                }
                Text(
                    text = stringResource(R.string.gps_skyplot).uppercase(),
                    style = titleStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.TopStart)
                )
                LegendItem(
                    filled = false,
                    dual = false,
                    label = stringResource(R.string.gps_skyplot_legend_in_view),
                    color = bezel,
                    l5Stroke = l5Stroke,
                    textStyle = legendStyle,
                    modifier = Modifier.align(Alignment.TopEnd),
                    verticalAlignment = Alignment.Top
                )
                LegendItem(
                    filled = true,
                    dual = false,
                    label = stringResource(R.string.gps_skyplot_legend_used),
                    color = bezel,
                    l5Stroke = l5Stroke,
                    textStyle = legendStyle,
                    modifier = Modifier.align(Alignment.BottomStart),
                    verticalAlignment = Alignment.Bottom
                )
                LegendItem(
                    filled = true,
                    dual = true,
                    label = stringResource(R.string.gps_skyplot_legend_l5),
                    color = bezel,
                    l5Stroke = l5Stroke,
                    textStyle = legendStyle,
                    modifier = Modifier.align(Alignment.BottomEnd),
                    verticalAlignment = Alignment.Bottom
                )
            }
        }
    }
}

@Composable
private fun LegendItem(
    filled: Boolean,
    dual: Boolean,
    label: String,
    color: Color,
    l5Stroke: Color,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically
) {
    Row(
        modifier = modifier,
        verticalAlignment = verticalAlignment,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Canvas(modifier = Modifier.size(12.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2.4f
            if (filled) {
                drawCircle(color = color, radius = radius, center = center, style = Fill)
            } else {
                drawCircle(color = color, radius = radius, center = center, style = Stroke(width = 2.2f))
            }
            if (dual) {
                drawCircle(color = l5Stroke, radius = radius * 0.45f, center = center, style = Stroke(width = 2f))
            }
        }
        Text(
            text = label,
            style = textStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun cornerLabelStyle(style: TextStyle): TextStyle {
    return style.copy(
        lineHeight = style.fontSize,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.Both
        )
    )
}

private fun DrawScope.drawCardinals(center: Offset, radius: Float, cardinal: Color) {
    val labelRadius = radius * 0.78f
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
        textAlign = android.graphics.Paint.Align.CENTER
        textSize = (radius * 0.12f).coerceIn(16f, 26f)
        isFakeBoldText = true
    }
    drawIntoCanvas { canvas ->
        val native = canvas.nativeCanvas
        fun label(azimuth: Float, text: String, color: Color) {
            val point = SkyplotProjection.offset(azimuth, 0f, center.x, center.y, labelRadius)
            if (point == null) {
                return
            }
            paint.color = color.toArgb()
            native.drawText(text, point.x, point.y + paint.textSize * 0.35f, paint)
        }
        label(0f, "N", CarmineTrack)
        label(90f, "E", cardinal)
        label(180f, "S", cardinal)
        label(270f, "W", cardinal)
    }
}

private fun DrawScope.drawMarker(marker: SkyplotMarker, center: Offset, l5Stroke: Color) {
    val color = gnssConstellationColor(marker.constellation)
    val radius = 8.5f
    if (marker.usedInFix) {
        drawCircle(color = color, radius = radius, center = center, style = Fill)
    } else {
        drawCircle(color = color, radius = radius, center = center, style = Stroke(width = 2.6f))
    }
    if (marker.hasL5) {
        drawCircle(
            color = l5Stroke,
            radius = radius * 0.42f,
            center = center,
            style = Stroke(width = 2.2f)
        )
    }
}
