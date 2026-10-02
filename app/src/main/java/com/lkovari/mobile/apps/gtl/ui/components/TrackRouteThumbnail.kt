package com.lkovari.mobile.apps.gtl.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.lkovari.mobile.apps.gtl.engine.GeoPoint
import com.lkovari.mobile.apps.gtl.engine.TrackPreview
import com.lkovari.mobile.apps.gtl.ui.theme.Cockpit
import com.lkovari.mobile.apps.gtl.ui.theme.HudCyan
import com.lkovari.mobile.apps.gtl.ui.theme.MoonCream
import com.lkovari.mobile.apps.gtl.ui.theme.TitleMagenta

@Composable
fun TrackRouteThumbnail(
    points: List<GeoPoint>,
    modifier: Modifier = Modifier
) {
    val laid = TrackPreview.layout(points)
    Box(
        modifier
            .size(76.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Cockpit)
    ) {
        Canvas(Modifier.matchParentSize()) {
            if (laid.isEmpty()) {
                return@Canvas
            }
            val pad = size.minDimension * 0.16f
            val widthSpan = size.width - pad * 2
            val heightSpan = size.height - pad * 2
            fun place(x: Float, y: Float): Offset {
                return Offset(pad + x * widthSpan, pad + y * heightSpan)
            }
            if (laid.size == 1) {
                val only = laid.first()
                drawCircle(
                    color = HudCyan,
                    radius = 4.dp.toPx(),
                    center = place(only.x, only.y)
                )
                return@Canvas
            }
            val path = Path()
            laid.forEachIndexed { index, point ->
                val at = place(point.x, point.y)
                if (index == 0) {
                    path.moveTo(at.x, at.y)
                } else {
                    path.lineTo(at.x, at.y)
                }
            }
            drawPath(
                path = path,
                color = TitleMagenta,
                style = Stroke(
                    width = 3.2.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
            val start = laid.first()
            val end = laid.last()
            drawCircle(color = HudCyan, radius = 3.6.dp.toPx(), center = place(start.x, start.y))
            drawCircle(color = MoonCream, radius = 3.2.dp.toPx(), center = place(end.x, end.y))
        }
    }
}
