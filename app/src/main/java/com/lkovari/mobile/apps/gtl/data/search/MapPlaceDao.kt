package com.lkovari.mobile.apps.gtl.data.search

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface MapPlaceDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(rows: List<MapPlaceEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertState(state: MapIndexStateEntity)

    @Query("SELECT * FROM map_index_state WHERE mapKey = :mapKey")
    suspend fun state(mapKey: String): MapIndexStateEntity?

    @Query("SELECT COUNT(*) FROM map_places WHERE mapKey = :mapKey")
    suspend fun count(mapKey: String): Int

    @Query("DELETE FROM map_places WHERE path = :path")
    suspend fun deletePlaces(path: String)

    @Query("DELETE FROM map_index_state WHERE path = :path")
    suspend fun deleteState(path: String)

    @Query("DELETE FROM map_places WHERE path != :path")
    suspend fun deletePlacesExcept(path: String)

    @Query("DELETE FROM map_index_state WHERE path != :path")
    suspend fun deleteStateExcept(path: String)

    @Transaction
    suspend fun deletePath(path: String) {
        deletePlaces(path)
        deleteState(path)
    }

    @Transaction
    suspend fun deleteExcept(path: String) {
        deletePlacesExcept(path)
        deleteStateExcept(path)
    }

    @Query(
        """
        SELECT * FROM map_places
        WHERE mapKey = :mapKey AND id IN (
            SELECT rowid FROM map_places_fts WHERE map_places_fts MATCH :match
        )
        LIMIT :limit
        """
    )
    suspend fun match(mapKey: String, match: String, limit: Int): List<MapPlaceEntity>
}
