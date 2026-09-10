package com.lkovari.mobile.apps.gtl.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TrackSessionEntity::class, GpsEventEntity::class],
    version = 3,
    exportSchema = false
)
abstract class GtlDatabase : RoomDatabase() {
    abstract fun trackSessionDao(): TrackSessionDao
    abstract fun gpsEventDao(): GpsEventDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE gps_events ADD COLUMN leanAngle REAL")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE gps_events ADD COLUMN usageType TEXT")
                db.execSQL(
                    "UPDATE gps_events SET usageType = (" +
                        "SELECT usageType FROM track_sessions " +
                        "WHERE track_sessions.id = gps_events.sessionId" +
                        ")"
                )
            }
        }

        fun create(context: Context): GtlDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                GtlDatabase::class.java,
                "gtl.db"
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
        }
    }
}
