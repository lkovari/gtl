package com.lkovari.mobile.apps.gtl.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackSessionDao {
    @Insert
    suspend fun insert(session: TrackSessionEntity): Long

    @Update
    suspend fun update(session: TrackSessionEntity)

    @Query("SELECT * FROM track_sessions ORDER BY startedAt DESC")
    fun observeSessions(): Flow<List<TrackSessionEntity>>

    @Query("SELECT * FROM track_sessions WHERE id = :id")
    suspend fun getById(id: Long): TrackSessionEntity?

    @Query("SELECT * FROM track_sessions WHERE stoppedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun getOpenSession(): TrackSessionEntity?

    @Query("DELETE FROM track_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface GpsEventDao {
    @Insert
    suspend fun insert(event: GpsEventEntity): Long

    @Query("SELECT * FROM gps_events WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun observeBySession(sessionId: Long): Flow<List<GpsEventEntity>>

    @Query("SELECT * FROM gps_events WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun listBySession(sessionId: Long): List<GpsEventEntity>

    @Query("SELECT * FROM gps_events WHERE sessionId = :sessionId ORDER BY timestamp DESC LIMIT 1")
    suspend fun latestForSession(sessionId: Long): GpsEventEntity?
}
