package com.lkovari.mobile.apps.gtl.data.qnh

import android.location.Location
import com.lkovari.mobile.apps.gtl.data.db.TrackRepository
import com.lkovari.mobile.apps.gtl.data.prefs.GtlPreferences
import com.lkovari.mobile.apps.gtl.diagnostics.AppErrorLog
import com.lkovari.mobile.apps.gtl.engine.QnhSelection
import com.lkovari.mobile.apps.gtl.service.RecordingSessionGate
import com.lkovari.mobile.apps.gtl.service.TrackingStateHolder
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield

class QnhRefresh(
    private val preferences: GtlPreferences,
    private val trackingState: TrackingStateHolder,
    private val gate: RecordingSessionGate,
    private val repository: TrackRepository,
    private val pressureAvailable: () -> Boolean,
    private val fineLocationGranted: () -> Boolean,
    private val client: QnhClient = QnhClient(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val nowMillis: () -> Long = System::currentTimeMillis
) {
    private val generation = AtomicInteger(0)
    private val recovery = CompletableDeferred<Unit>()

    @Volatile
    var sliderHeld: Boolean = false

    @Volatile
    private var job: Job? = null

    fun holdSlider() {
        sliderHeld = true
    }

    fun releaseSliderHold() {
        sliderHeld = false
    }

    fun onRecoveryFinished() {
        if (!recovery.isCompleted) {
            recovery.complete(Unit)
        }
    }

    fun arm() {
        begin(generation.get(), startup = true)
    }

    fun refreshNow() {
        sliderHeld = false
        begin(generation.incrementAndGet(), startup = false)
    }

    suspend fun awaitIdle() {
        var seen: Job? = null
        while (true) {
            val running = job
            if (running == null) {
                return
            }
            if (running.isCompleted) {
                if (running === seen) {
                    return
                }
                seen = running
                continue
            }
            running.join()
            seen = running
        }
    }

    private fun begin(token: Int, startup: Boolean) {
        job = scope.launch {
            try {
                runFetch(token, startup)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                logFailure("unreachable")
            }
        }
    }

    private suspend fun runFetch(token: Int, startup: Boolean) {
        if (!pressureAvailable()) {
            return
        }
        if (startup) {
            withContext(Dispatchers.Main.immediate) { yield() }
            if (stale(token) || gate.serviceOwnsSession) {
                return
            }
            withTimeoutOrNull(FIX_WAIT_MILLIS) { recovery.await() }
            if (stale(token) || gate.serviceOwnsSession) {
                return
            }
            if (repository.openSession() != null) {
                return
            }
            if (preferences.settings.first().qnhManual) {
                return
            }
        } else {
            if (recordingOpen()) {
                return
            }
            preferences.clearQnhManual()
        }
        val fix = awaitFix(token) ?: return
        if (stale(token) || recordingOpen()) {
            return
        }
        val lookup = withContext(Dispatchers.IO) {
            client.lookup(fix.latitude, fix.longitude)
        }
        if (stale(token)) {
            return
        }
        val settings = preferences.settings.first()
        val choice = QnhSelection.select(
            modelHpa = lookup.modelHpa,
            metars = lookup.metars,
            latitude = lookup.latitude,
            longitude = lookup.longitude,
            nowMillis = nowMillis(),
            usage = settings.usageType
        )
        if (choice == null) {
            logFailure(lookup.failure ?: "empty")
            return
        }
        if (!QnhSelection.mayApplyAutomatic(sliderHeld || settings.qnhManual, recordingOpen())) {
            return
        }
        preferences.applyAutomaticQnh(
            hpa = choice.hpa,
            origin = choice.origin,
            icao = choice.icao,
            distanceKm = choice.distanceKm,
            latitude = lookup.latitude,
            longitude = lookup.longitude,
            fetchedAtMillis = nowMillis(),
            blocked = { stale(token) || sliderHeld || gate.serviceOwnsSession }
        )
    }

    private suspend fun awaitFix(token: Int): Location? {
        val deadline = System.currentTimeMillis() + FIX_WAIT_MILLIS
        while (System.currentTimeMillis() < deadline) {
            if (stale(token) || gate.serviceOwnsSession) {
                return null
            }
            val location = trackingState.state.value.lastLocation
            if (fineLocationGranted() && usable(location)) {
                return location
            }
            delay(250)
        }
        return null
    }

    private suspend fun recordingOpen(): Boolean {
        return gate.serviceOwnsSession || repository.openSession() != null
    }

    private fun stale(token: Int): Boolean {
        return generation.get() != token
    }

    private fun usable(location: Location?): Boolean {
        return location != null && location.latitude.isFinite() && location.longitude.isFinite()
    }

    private fun logFailure(reason: String) {
        AppErrorLog.record("qnh.fetch", QnhFetchException(reason))
    }

    private companion object {
        const val FIX_WAIT_MILLIS = 15_000L
    }
}
