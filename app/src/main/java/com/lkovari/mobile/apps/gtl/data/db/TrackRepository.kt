package com.lkovari.mobile.apps.gtl.data.db

import com.lkovari.mobile.apps.gtl.data.sync.RemoteTrackSync
import com.lkovari.mobile.apps.gtl.engine.EventKind
import com.lkovari.mobile.apps.gtl.engine.MeasurementSystem
import com.lkovari.mobile.apps.gtl.engine.TrackSample
import com.lkovari.mobile.apps.gtl.engine.UsageType
import kotlinx.coroutines.flow.Flow

class TrackRepository(
    private val database: GtlDatabase,
    private val remoteTrackSync: RemoteTrackSync
) {
    private val sessions = database.trackSessionDao()
    private val events = database.gpsEventDao()

    fun observeSessions(): Flow<List<TrackSessionEntity>> = sessions.observeSessions()

    fun observeEvents(sessionId: Long): Flow<List<GpsEventEntity>> = events.observeBySession(sessionId)

    suspend fun startSession(usageType: UsageType, measurementSystem: MeasurementSystem): Long {
        return sessions.insert(
            TrackSessionEntity(
                startedAt = System.currentTimeMillis(),
                stoppedAt = null,
                usageType = usageType.name,
                measurementSystem = measurementSystem.name
            )
        )
    }

    suspend fun stopSession(sessionId: Long) {
        val existing = sessions.getById(sessionId) ?: return
        sessions.update(existing.copy(stoppedAt = System.currentTimeMillis()))
        remoteTrackSync.uploadSession(existing, events.listBySession(sessionId))
    }

    suspend fun openSession(): TrackSessionEntity? = sessions.getOpenSession()

    suspend fun latestEvent(sessionId: Long): GpsEventEntity? = events.latestForSession(sessionId)

    suspend fun insertEvent(event: GpsEventEntity): Long = events.insert(event)

    suspend fun eventsFor(sessionId: Long): List<GpsEventEntity> = events.listBySession(sessionId)

    suspend fun deleteSession(sessionId: Long) {
        sessions.deleteById(sessionId)
    }

    fun toSamples(items: List<GpsEventEntity>): List<TrackSample> {
        return items.map { event ->
            TrackSample(
                timestampMillis = event.timestamp,
                latitude = event.latitude,
                longitude = event.longitude,
                altitude = event.altitude,
                speedMps = event.speed,
                bearing = event.bearing,
                ambientTemperature = event.ambientTemperature,
                eventKind = runCatching { EventKind.valueOf(event.eventKind) }.getOrDefault(EventKind.MOVE)
            )
        }
    }
}
