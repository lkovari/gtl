package com.lkovari.mobile.apps.gtl.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [TrackSessionEntity::class, GpsEventEntity::class],
    version = 1,
    exportSchema = false
)
abstract class GtlDatabase : RoomDatabase() {
    abstract fun trackSessionDao(): TrackSessionDao
    abstract fun gpsEventDao(): GpsEventDao

    companion object {
        fun create(context: Context): GtlDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                GtlDatabase::class.java,
                "gtl.db"
            ).build()
        }
    }
}
