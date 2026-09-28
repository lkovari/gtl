package com.lkovari.mobile.apps.gtl.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.lkovari.mobile.apps.gtl.engine.MeasurementSystem
import com.lkovari.mobile.apps.gtl.engine.SpeedBands
import com.lkovari.mobile.apps.gtl.engine.SpeedSample
import com.lkovari.mobile.apps.gtl.engine.UsageType

@Composable
fun SpeedSparkline(
    samples: List<SpeedSample>,
    usage: UsageType,
    system: MeasurementSystem,
    modifier: Modifier = Modifier
) {
    val drawable = samples.count { it.speedMps != null } >= 2
    if (!drawable) {
        return
    }
    val maxDistance = samples.last().distanceMeters.coerceAtLeast(1.0)
    val maxSpeed = samples.mapNotNull { it.speedMps }.maxOrNull()?.coerceAtLeast(0.1f) ?: return
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        for (index in 1 until samples.size) {
            val previous = samples[index - 1]
            val current = samples[index]
            val previousSpeed = previous.speedMps ?: continue
            val currentSpeed = current.speedMps ?: continue
            val x0 = (previous.distanceMeters / maxDistance).toFloat() * size.width
            val x1 = (current.distanceMeters / maxDistance).toFloat() * size.width
            val y0 = size.height - (previousSpeed / maxSpeed) * size.height
            val y1 = size.height - (currentSpeed / maxSpeed) * size.height
            drawLine(
                color = Color(SpeedBands.argb(SpeedBands.of(currentSpeed, usage, system))),
                start = Offset(x0, y0),
                end = Offset(x1, y1),
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )
        }
    }
}
