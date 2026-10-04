package com.lkovari.mobile.apps.gtl.data.db

import androidx.room.withTransaction
import com.lkovari.mobile.apps.gtl.data.sync.RemoteTrackSync
import com.lkovari.mobile.apps.gtl.engine.EventKind
import com.lkovari.mobile.apps.gtl.engine.MeasurementSystem
import com.lkovari.mobile.apps.gtl.engine.SavedTrackCard
import com.lkovari.mobile.apps.gtl.engine.SavedTrackCards
import com.lkovari.mobile.apps.gtl.engine.TrackPreviewPolyline
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
        val snapshot = database.withTransaction {
            val existing = sessions.getById(sessionId) ?: return@withTransaction null
            val stored = events.listBySession(sessionId)
            sessions.update(existing.withCard(stored, System.currentTimeMillis()))
            existing to stored
        } ?: return
        remoteTrackSync.uploadSession(snapshot.first, snapshot.second)
    }

    suspend fun sessionsNeedingCard(): List<TrackSessionEntity> = sessions.sessionsNeedingCard()

    suspend fun backfillCard(sessionId: Long) {
        database.withTransaction {
            val existing = sessions.getById(sessionId) ?: return@withTransaction
            if (existing.stoppedAt == null || existing.previewPolyline != null) {
                return@withTransaction
            }
            val stored = events.listBySession(sessionId)
            sessions.update(existing.withCard(stored, existing.stoppedAt))
        }
    }

    private fun TrackSessionEntity.withCard(stored: List<GpsEventEntity>, stoppedAtMillis: Long): TrackSessionEntity {
        val card = SavedTrackCards.from(toSamples(stored))
        return copy(
            stoppedAt = stoppedAtMillis,
            distanceMeters = card.odometerMeters,
            durationMs = card.elapsedMillis,
            avgSpeedMps = card.averageSpeedMps,
            maxSpeedMps = card.maxSpeedMps,
            previewPolyline = TrackPreviewPolyline.encode(card.preview)
        )
    }

    suspend fun openSession(): TrackSessionEntity? = sessions.getOpenSession()

    suspend fun sessionById(id: Long): TrackSessionEntity? = sessions.getById(id)

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

fun TrackSessionEntity.toStoredCard(): SavedTrackCard? {
    val distance = distanceMeters ?: return null
    val duration = durationMs ?: return null
    val average = avgSpeedMps ?: return null
    val fastest = maxSpeedMps ?: return null
    val preview = previewPolyline ?: return null
    return SavedTrackCard(
        odometerMeters = distance,
        elapsedMillis = duration,
        averageSpeedMps = average,
        maxSpeedMps = fastest,
        preview = TrackPreviewPolyline.decode(preview)
    )
}
