package com.lkovari.mobile.apps.gtl.data.search

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MapPlaceEntity::class, MapPlaceFts::class, MapIndexStateEntity::class],
    version = 2,
    exportSchema = true
)
abstract class MapSearchDatabase : RoomDatabase() {
    abstract fun places(): MapPlaceDao

    companion object {
        fun create(context: Context): MapSearchDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                MapSearchDatabase::class.java,
                "map-search.db"
            ).fallbackToDestructiveMigration(true).build()
        }
    }
}
