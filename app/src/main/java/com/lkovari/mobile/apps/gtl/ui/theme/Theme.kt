package com.lkovari.mobile.apps.gtl.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

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
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}
