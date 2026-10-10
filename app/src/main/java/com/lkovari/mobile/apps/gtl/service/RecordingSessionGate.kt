package com.lkovari.mobile.apps.gtl.service

import kotlinx.coroutines.sync.Mutex

/**
 * Process-wide owner of the open track session.
 *
 * [TrackingForegroundService] claims the session while it records; [InterruptedRecordingRecovery]
 * closes an open session only when nobody owns it. Both run under [mutex], so a system restart of
 * the service and the UI-side recovery cannot both act on the same session.
 */
class RecordingSessionGate {
    val mutex = Mutex()

    /**
     * True from the moment the service accepts a start until it stops or gives up. Set on the
     * main thread without the lock so the claim is visible before the service's coroutine runs;
     * [InterruptedRecordingRecovery] reads it under [mutex].
     */
    @Volatile
    var serviceOwnsSession: Boolean = false
}
