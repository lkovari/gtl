package com.lkovari.mobile.apps.gtl.data.sensor

import android.os.SystemClock
import com.lkovari.mobile.apps.gtl.engine.BaroFeedStatus
import com.lkovari.mobile.apps.gtl.engine.BaroFreshness
import com.lkovari.mobile.apps.gtl.engine.BaroFreshnessState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

/**
 * Emits the latest pressure in hPa, or null while the sensor is silent or stuck.
 * A stale or frozen sensor is re-registered with back-off, since some HALs only
 * recover after the listener is removed and added again.
 */
fun PressureSource.freshPressures(): Flow<Float?> = channelFlow {
    if (!isAvailable) {
        return@channelFlow
    }
    var attempt = 0
    while (true) {
        val since = SystemClock.elapsedRealtimeNanos()
        val tracker = AtomicReference(BaroFreshnessState())
        val listener = launch {
            samples().collect { sample ->
                tracker.set(BaroFreshness.observe(tracker.get(), sample.hpa, sample.receivedNanos))
                if (BaroFreshness.status(tracker.get(), sample.receivedNanos, since) == BaroFeedStatus.Live) {
                    attempt = 0
                    send(sample.hpa)
                }
            }
        }
        while (true) {
            delay(CheckIntervalMillis)
            val status = BaroFreshness.status(tracker.get(), SystemClock.elapsedRealtimeNanos(), since)
            if (status == BaroFeedStatus.Stale || status == BaroFeedStatus.Frozen) {
                break
            }
        }
        send(null)
        listener.cancel()
        delay(RestartBackoffMillis[attempt.coerceAtMost(RestartBackoffMillis.lastIndex)])
        attempt += 1
    }
}

private const val CheckIntervalMillis = 1_000L
private val RestartBackoffMillis = longArrayOf(1_000L, 10_000L, 30_000L, 60_000L)
