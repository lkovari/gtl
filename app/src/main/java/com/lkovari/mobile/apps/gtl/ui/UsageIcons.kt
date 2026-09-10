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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.lkovari.mobile.apps.gtl.engine.UsageType
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

@Composable
fun rememberUsageMarkerBitmap(type: UsageType, size: Dp = 24.dp): Bitmap {
    val density = LocalDensity.current
    val sizePx = with(density) { size.roundToPx().coerceAtLeast(1) }
    val painter = rememberVectorPainter(image = usageIcon(type))
    return remember(type, sizePx, painter) {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val androidCanvas = android.graphics.Canvas(bitmap)
        val drawSize = Size(sizePx.toFloat(), sizePx.toFloat())
        CanvasDrawScope().draw(
            density = density,
            layoutDirection = LayoutDirection.Ltr,
            canvas = Canvas(androidCanvas),
            size = drawSize
        ) {
            with(painter) {
                draw(drawSize, colorFilter = ColorFilter.tint(UsageMarkerRed))
            }
        }
        bitmap
    }
}
