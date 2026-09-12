package com.lkovari.mobile.apps.gtl.ui.components

import android.provider.Settings
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lkovari.mobile.apps.gtl.R
import com.lkovari.mobile.apps.gtl.engine.MapHudMode
import com.lkovari.mobile.apps.gtl.engine.MeasurementSystem
import com.lkovari.mobile.apps.gtl.engine.Units
import com.lkovari.mobile.apps.gtl.ui.theme.CarmineTrack
import com.lkovari.mobile.apps.gtl.ui.theme.Cockpit
import com.lkovari.mobile.apps.gtl.ui.theme.HudCyan
import com.lkovari.mobile.apps.gtl.ui.theme.NightInk
import com.lkovari.mobile.apps.gtl.ui.theme.NightMuted
import com.lkovari.mobile.apps.gtl.ui.theme.PaperGrid
import java.util.Locale

@Composable
fun MapHud(
    mode: MapHudMode,
    speedMps: Float?,
    system: MeasurementSystem,
    odometerMeters: Double,
    elapsedMillis: Long,
    accuracyMeters: Float?,
    satellitesInFix: Int,
    satellitesInView: Int,
    modifier: Modifier = Modifier
) {
    if (mode == MapHudMode.Hidden) {
        return
    }
    val dark = MaterialTheme.colorScheme.background == Cockpit
    val panel = if (dark) Cockpit.copy(alpha = 0.78f) else NightInk.copy(alpha = 0.72f)
    val number = if (dark) HudCyan else PaperGrid
    val muted = if (dark) NightMuted else PaperGrid.copy(alpha = 0.8f)
    val speedText = speedMps?.let { Units.hudSpeedNumber(it, system) } ?: "—"
    val unitText = Units.hudSpeedUnit(system)
    val accuracyText = accuracyMeters?.let {
        String.format(Locale.US, "%.1f m", it)
    } ?: "—"
    val gnssText = "$satellitesInFix/$satellitesInView"
    val compact = mode == MapHudMode.Compact
    Column(
        modifier = modifier
            .wrapContentWidth()
            .alpha(if (compact) 0.72f else 1f)
            .background(panel, RoundedCornerShape(12.dp))
            .padding(
                horizontal = if (compact) 12.dp else 14.dp,
                vertical = if (compact) 8.dp else 10.dp
            )
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (mode == MapHudMode.Full) {
                    RecBadge()
                }
                Column {
                    Text(
                        text = speedText,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        fontSize = if (compact) 28.sp else 44.sp,
                        color = number,
                        letterSpacing = (-0.6).sp
                    )
                    Text(
                        text = unitText,
                        style = MaterialTheme.typography.labelLarge,
                        color = muted
                    )
                }
            }
            if (mode == MapHudMode.Full) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = Units.formatDistance(odometerMeters, system),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        color = number
                    )
                    Text(
                        text = Units.formatDuration(elapsedMillis),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        color = number
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.map_hud_accuracy_gnss, accuracyText, gnssText),
            style = MaterialTheme.typography.labelMedium,
            color = muted,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun RecBadge() {
    val context = LocalContext.current
    val reduceMotion = remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    }
    val transition = rememberInfiniteTransition(label = "rec")
    val pulsed by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "recAlpha"
    )
    val alpha = if (reduceMotion) 1f else pulsed
    Text(
        text = stringResource(R.string.map_hud_rec),
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = CarmineTrack.copy(alpha = alpha),
        modifier = Modifier.padding(bottom = 6.dp)
    )
}
