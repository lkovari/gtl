package com.lkovari.mobile.apps.gtl.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ColorOutlineLight = Color(0xFFB7C7BE)
private val ColorOutlineDark = Color(0xFF35505A)

private val LightColors = lightColorScheme(
    primary = HudTeal,
    onPrimary = PaperGrid,
    secondary = AmberFix,
    onSecondary = NightInk,
    tertiary = CarmineTrack,
    onTertiary = PaperGrid,
    background = ChartSage,
    onBackground = NightInk,
    surface = PaperGrid,
    onSurface = NightInk,
    surfaceVariant = ChartSage,
    onSurfaceVariant = MutedInk,
    outline = ColorOutlineLight,
    error = CarmineTrack,
    onError = PaperGrid
)

private val DarkColors = darkColorScheme(
    primary = HudCyan,
    onPrimary = Cockpit,
    secondary = MoonAmber,
    onSecondary = Cockpit,
    tertiary = CarmineTrack,
    onTertiary = MoonCream,
    background = Cockpit,
    onBackground = MoonCream,
    surface = CockpitPanel,
    onSurface = MoonCream,
    surfaceVariant = CockpitPanel,
    onSurfaceVariant = NightMuted,
    outline = ColorOutlineDark,
    error = MoonAmber,
    onError = Cockpit
)

fun gtlWash(dark: Boolean): Brush {
    return if (dark) {
        Brush.verticalGradient(listOf(Cockpit, CockpitPanel))
    } else {
        Brush.verticalGradient(listOf(ChartSage, PaperGrid))
    }
}

@Composable
fun GtlTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val view = LocalView.current
    val context = LocalContext.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = context as? Activity ?: return@SideEffect
            val window = activity.window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = !darkTheme
            window.statusBarColor = (if (darkTheme) Cockpit else HudTeal).toArgb()
            window.navigationBarColor = (if (darkTheme) Cockpit else PaperGrid).toArgb()
        }
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}
