package com.lkovari.mobile.apps.gtl.data.search

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "map_places",
    indices = [
        Index("path"),
        Index(
            value = ["mapKey", "nameFold", "kind", "gridLat", "gridLon"],
            unique = true
        )
    ]
)
data class MapPlaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mapKey: String,
    val path: String,
    val name: String,
    val nameFold: String,
    val kind: String,
    val latitude: Double,
    val longitude: Double,
    val gridLat: Int,
    val gridLon: Int
)

@Fts4(contentEntity = MapPlaceEntity::class)
@Entity(tableName = "map_places_fts")
data class MapPlaceFts(
    val nameFold: String
)

@Entity(tableName = "map_index_state")
data class MapIndexStateEntity(
    @PrimaryKey val mapKey: String,
    val path: String,
    val done: Boolean,
    val truncated: Boolean,
    val attempt: Int,
    val nextAttemptAtMillis: Long,
    val subIndex: Int,
    val tileX: Long,
    val tileY: Long,
    val originLatitude: Double,
    val originLongitude: Double
)
