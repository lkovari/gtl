package com.lkovari.mobile.apps.gtl.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class LocationStartTest {
    @Test
    fun fineLocationStartsLogging() {
        assertEquals(
            LocationStartAction.StartLogging,
            locationStartTap(
                fineGranted = true,
                coarseGranted = false,
                askedBefore = false,
                canAskAgain = false
            )
        )
    }

    @Test
    fun coarseWithoutFineAsksForPrecise() {
        assertEquals(
            LocationStartAction.PreciseRequired,
            locationStartTap(
                fineGranted = false,
                coarseGranted = true,
                askedBefore = true,
                canAskAgain = false
            )
        )
    }

    @Test
    fun firstTapRequestsPermission() {
        assertEquals(
            LocationStartAction.RequestPermission,
            locationStartTap(
                fineGranted = false,
                coarseGranted = false,
                askedBefore = false,
                canAskAgain = false
            )
        )
    }

    @Test
    fun deniedOnceAsksAgain() {
        assertEquals(
            LocationStartAction.RequestPermission,
            locationStartTap(
                fineGranted = false,
                coarseGranted = false,
                askedBefore = true,
                canAskAgain = true
            )
        )
    }

    @Test
    fun permanentDenialOpensAppSettings() {
        assertEquals(
            LocationStartAction.OpenAppSettings,
            locationStartTap(
                fineGranted = false,
                coarseGranted = false,
                askedBefore = true,
                canAskAgain = false
            )
        )
    }

    @Test
    fun resultStartsWhenFineIsGranted() {
        assertEquals(
            LocationStartAction.StartLogging,
            locationPermissionResult(fineGranted = true, coarseGranted = true, canAskAgain = false)
        )
    }

    @Test
    fun resultNamesPreciseWhenOnlyCoarseIsGranted() {
        assertEquals(
            LocationStartAction.PreciseRequired,
            locationPermissionResult(fineGranted = false, coarseGranted = true, canAskAgain = true)
        )
    }

    @Test
    fun resultOpensSettingsWhenTheSystemWillNotAskAgain() {
        assertEquals(
            LocationStartAction.OpenAppSettings,
            locationPermissionResult(fineGranted = false, coarseGranted = false, canAskAgain = false)
        )
    }

    @Test
    fun resultToastsWhenTheUserCanBeAskedAgain() {
        assertEquals(
            LocationStartAction.PermissionDenied,
            locationPermissionResult(fineGranted = false, coarseGranted = false, canAskAgain = true)
        )
    }
}
