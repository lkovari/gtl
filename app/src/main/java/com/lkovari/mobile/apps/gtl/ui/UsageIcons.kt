package com.lkovari.mobile.apps.gtl.ui

import android.graphics.Bitmap
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.lkovari.mobile.apps.gtl.engine.UsageType
import com.lkovari.mobile.apps.gtl.ui.theme.HudCyan
import com.lkovari.mobile.apps.gtl.ui.theme.UsageMarkerRed

fun usageIcon(type: UsageType): ImageVector {
    return when (type) {
        UsageType.AIRCRAFT -> Icons.Filled.Flight
        UsageType.WATERCRAFT -> Icons.Filled.DirectionsBoat
        UsageType.FOUR_WHEELERS -> Icons.Filled.DirectionsCar
        UsageType.TWO_WHEELERS -> Icons.Filled.TwoWheeler
        UsageType.BICYCLE -> Icons.Filled.DirectionsBike
        UsageType.RUNNER, UsageType.WALKING_HIKE, UsageType.PEDESTRIAN ->
            Icons.AutoMirrored.Filled.DirectionsRun
    }
}

fun DrawScope.drawLiveFixReticle() {
    val r = size.minDimension / 2f
    drawCircle(color = HudCyan.copy(alpha = 0.22f), radius = r)
    drawCircle(
        color = HudCyan,
        radius = (r - 1.5f).coerceAtLeast(1f),
        style = Stroke(width = 2.5f)
    )
    val tick = size.minDimension * 0.16f
    val cx = size.width / 2f
    val cy = size.height / 2f
    val inset = 1.5f
    drawLine(HudCyan, Offset(cx, inset), Offset(cx, inset + tick), strokeWidth = 2f)
    drawLine(
        HudCyan,
        Offset(cx, size.height - inset),
        Offset(cx, size.height - inset - tick),
        strokeWidth = 2f
    )
    drawLine(HudCyan, Offset(inset, cy), Offset(inset + tick, cy), strokeWidth = 2f)
    drawLine(
        HudCyan,
        Offset(size.width - inset, cy),
        Offset(size.width - inset - tick, cy),
        strokeWidth = 2f
    )
}

@Composable
fun rememberUsageMarkerBitmap(
    type: UsageType,
    liveFix: Boolean = false,
    size: Dp = if (liveFix) 32.dp else 24.dp
): Bitmap {
    val density = LocalDensity.current
    val sizePx = with(density) { size.roundToPx().coerceAtLeast(1) }
    val painter = rememberVectorPainter(image = usageIcon(type))
    return remember(type, sizePx, painter, liveFix) {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val androidCanvas = android.graphics.Canvas(bitmap)
        val drawSize = Size(sizePx.toFloat(), sizePx.toFloat())
        CanvasDrawScope().draw(
            density = density,
            layoutDirection = LayoutDirection.Ltr,
            canvas = Canvas(androidCanvas),
            size = drawSize
        ) {
            if (liveFix) {
                drawLiveFixReticle()
                val inset = sizePx * 0.22f
                val iconSize = Size(drawSize.width - inset * 2f, drawSize.height - inset * 2f)
                translate(left = inset, top = inset) {
                    with(painter) {
                        draw(iconSize, colorFilter = ColorFilter.tint(UsageMarkerRed))
                    }
                }
            } else {
                with(painter) {
                    draw(drawSize, colorFilter = ColorFilter.tint(UsageMarkerRed))
                }
            }
        }
        bitmap
    }
}
