package com.lkovari.mobile.apps.gtl.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class LaunchScreenTest {
    @Test
    fun holdsUntilTheStoredChoiceIsKnown() {
        assertEquals(LaunchScreen.Hold, launchScreen(settingsLoaded = false, disclaimerAccepted = false))
        assertEquals(LaunchScreen.Hold, launchScreen(settingsLoaded = false, disclaimerAccepted = true))
    }

    @Test
    fun firstLaunchAfterInstallShowsDisclaimer() {
        assertEquals(LaunchScreen.Disclaimer, launchScreen(settingsLoaded = true, disclaimerAccepted = false))
    }

    @Test
    fun acceptedChoiceOpensTheTracker() {
        assertEquals(LaunchScreen.Tracker, launchScreen(settingsLoaded = true, disclaimerAccepted = true))
    }
}
