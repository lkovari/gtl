package com.lkovari.mobile.apps.gtl.service

import android.database.SQLException
import com.lkovari.mobile.apps.gtl.data.db.TrackRepository
import com.lkovari.mobile.apps.gtl.diagnostics.AppErrorLog
import com.lkovari.mobile.apps.gtl.engine.EventKind
import kotlinx.coroutines.sync.withLock

/**
 * Closes a session that was left open because the process died while recording.
 *
 * Android normally restarts the sticky [TrackingForegroundService], which then resumes the open
 * session. Some vendor task killers (for example `com.pri.screenoff.killer`) mark the process as
 * bad, so that restart never happens and the session stays open with no writer. Without this
 * class the next Start appended to that session and drew a straight line across the gap.
 */
class InterruptedRecordingRecovery(
    private val repository: TrackRepository,
    private val gate: RecordingSessionGate
) {
    /**
     * Closes the open session at its last saved point when no service in this process owns it.
     *
     * @return the id of the closed session, or null when there was nothing to close.
     */
    suspend fun closeOrphanedSession(): Long? = gate.mutex.withLock {
        if (gate.serviceOwnsSession) {
            return@withLock null
        }
        val session = repository.openSession() ?: return@withLock null
        closeAtLastPoint(session.id)
        session.id
    }

    /** Writes a STOP marker at the last saved point (if any) and stops the session. */
    suspend fun closeAtLastPoint(sessionId: Long) {
        val latest = repository.latestEvent(sessionId)
        if (latest != null && latest.eventKind != EventKind.STOP.name) {
            try {
                repository.insertEvent(
                    latest.copy(
                        id = 0,
                        isPlacemark = true,
                        eventKind = EventKind.STOP.name
                    )
                )
            } catch (error: SQLException) {
                AppErrorLog.recordSync("track.stop", error)
            }
        }
        repository.stopSession(sessionId)
    }
}
