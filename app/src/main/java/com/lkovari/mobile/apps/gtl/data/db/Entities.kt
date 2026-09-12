package com.lkovari.mobile.apps.gtl.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "track_sessions")
data class TrackSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val stoppedAt: Long?,
    val usageType: String,
    val measurementSystem: String
)

@Entity(
    tableName = "gps_events",
    foreignKeys = [
        ForeignKey(
            entity = TrackSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class GpsEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val speed: Float,
    val bearing: Float,
    val accuracy: Float,
    val satellitesInFix: Int,
    val ambientTemperature: Float?,
    val accelX: Float?,
    val accelY: Float?,
    val accelZ: Float?,
    val leanAngle: Float?,
    val usageType: String? = null,
    val isPlacemark: Boolean,
    val eventKind: String,
    val baroAltitude: Double? = null,
    val pressureHpa: Float? = null
)
