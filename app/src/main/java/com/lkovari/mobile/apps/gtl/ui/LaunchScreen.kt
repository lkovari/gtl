package com.lkovari.mobile.apps.gtl.ui

internal enum class LaunchScreen {
    Hold,
    Disclaimer,
    Tracker
}

internal fun launchScreen(settingsLoaded: Boolean, disclaimerAccepted: Boolean): LaunchScreen {
    if (!settingsLoaded) {
        return LaunchScreen.Hold
    }
    return if (disclaimerAccepted) LaunchScreen.Tracker else LaunchScreen.Disclaimer
}
