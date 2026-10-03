package com.lkovari.mobile.apps.gtl.engine

enum class LocationStartAction {
    StartLogging,
    PreciseRequired,
    RequestPermission,
    OpenAppSettings,
    PermissionDenied
}

fun locationStartTap(
    fineGranted: Boolean,
    coarseGranted: Boolean,
    askedBefore: Boolean,
    canAskAgain: Boolean
): LocationStartAction {
    if (fineGranted) {
        return LocationStartAction.StartLogging
    }
    if (coarseGranted) {
        return LocationStartAction.PreciseRequired
    }
    if (askedBefore && !canAskAgain) {
        return LocationStartAction.OpenAppSettings
    }
    return LocationStartAction.RequestPermission
}

fun locationPermissionResult(
    fineGranted: Boolean,
    coarseGranted: Boolean,
    canAskAgain: Boolean
): LocationStartAction {
    if (fineGranted) {
        return LocationStartAction.StartLogging
    }
    if (coarseGranted) {
        return LocationStartAction.PreciseRequired
    }
    if (!canAskAgain) {
        return LocationStartAction.OpenAppSettings
    }
    return LocationStartAction.PermissionDenied
}
