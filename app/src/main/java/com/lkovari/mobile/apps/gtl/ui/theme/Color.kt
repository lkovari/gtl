package com.lkovari.mobile.apps.gtl.ui.theme

import androidx.compose.ui.graphics.Color
import com.lkovari.mobile.apps.gtl.engine.GnssConstellation

val ChartSage = Color(0xFFD8E4D4)
val PaperGrid = Color(0xFFF4F7F1)
val NightInk = Color(0xFF102027)
val MutedInk = Color(0xFF4A5C63)
val HudTeal = Color(0xFF1F8A80)
val DeepTeal = Color(0xFF0F5C58)
val AmberFix = Color(0xFFD39A2A)
val CarmineTrack = Color(0xFFC13B2E)
val AccuracyMarkerFill = Color(0xFF6666FF)
val AccuracyMarkerBorder = Color(0xFF1414FC)
val FixCloudDot = Color(0xFFF48FB1)
val FixCloudCepStroke = Color(0xFFC2185B)
val FixCloudCepFill = Color(0xFFE91E63)
val UsageMarkerRed = Color(0xFFE53935)
val StartBlue = Color(0xFF1565C0)
val TrackingOrange = Color(0xFFEF6C00)
val TitleMagenta = Color(0xFFC2185B)
val GnssLime = Color(0xFF6FAF4E)

val Cockpit = Color(0xFF07141C)
val CockpitPanel = Color(0xFF102530)
val HudCyan = Color(0xFF3ECFCF)
val MoonAmber = Color(0xFFE8A838)
val MoonCream = Color(0xFFE7F0EA)
val NightMuted = Color(0xFF8AA0A8)

fun gnssConstellationColor(constellation: GnssConstellation): Color {
    return when (constellation) {
        GnssConstellation.GPS -> StartBlue
        GnssConstellation.GALILEO -> GnssLime
        GnssConstellation.GLONASS -> CarmineTrack
        GnssConstellation.BEIDOU -> MoonAmber
        GnssConstellation.QZSS -> TitleMagenta
        GnssConstellation.IRNSS -> HudCyan
        GnssConstellation.SBAS, GnssConstellation.UNKNOWN -> NightMuted
    }
}
