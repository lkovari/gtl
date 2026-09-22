package com.lkovari.mobile.apps.gtl.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TrackSessionEntity::class, GpsEventEntity::class],
    version = 6,
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

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE gps_events ADD COLUMN baroAltitude REAL")
                db.execSQL("ALTER TABLE gps_events ADD COLUMN pressureHpa REAL")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE gps_events_new (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "sessionId INTEGER NOT NULL, " +
                        "timestamp INTEGER NOT NULL, " +
                        "latitude REAL NOT NULL, " +
                        "longitude REAL NOT NULL, " +
                        "altitude REAL, " +
                        "speed REAL NOT NULL, " +
                        "bearing REAL NOT NULL, " +
                        "accuracy REAL NOT NULL, " +
                        "satellitesInFix INTEGER NOT NULL, " +
                        "ambientTemperature REAL, " +
                        "accelX REAL, " +
                        "accelY REAL, " +
                        "accelZ REAL, " +
                        "leanAngle REAL, " +
                        "usageType TEXT, " +
                        "isPlacemark INTEGER NOT NULL, " +
                        "eventKind TEXT NOT NULL, " +
                        "baroAltitude REAL, " +
                        "pressureHpa REAL, " +
                        "FOREIGN KEY(sessionId) REFERENCES track_sessions(id) ON DELETE CASCADE" +
                        ")"
                )
                db.execSQL(
                    "INSERT INTO gps_events_new (" +
                        "id, sessionId, timestamp, latitude, longitude, altitude, speed, bearing, " +
                        "accuracy, satellitesInFix, ambientTemperature, accelX, accelY, accelZ, " +
                        "leanAngle, usageType, isPlacemark, eventKind, baroAltitude, pressureHpa" +
                        ") SELECT " +
                        "id, sessionId, timestamp, latitude, longitude, altitude, speed, bearing, " +
                        "accuracy, satellitesInFix, ambientTemperature, accelX, accelY, accelZ, " +
                        "leanAngle, usageType, isPlacemark, eventKind, baroAltitude, pressureHpa " +
                        "FROM gps_events"
                )
                db.execSQL("DROP TABLE gps_events")
                db.execSQL("ALTER TABLE gps_events_new RENAME TO gps_events")
                db.execSQL("CREATE INDEX index_gps_events_sessionId ON gps_events (sessionId)")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE gps_events_new (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "sessionId INTEGER NOT NULL, " +
                        "timestamp INTEGER NOT NULL, " +
                        "latitude REAL NOT NULL, " +
                        "longitude REAL NOT NULL, " +
                        "altitude REAL, " +
                        "speed REAL, " +
                        "bearing REAL NOT NULL, " +
                        "accuracy REAL NOT NULL, " +
                        "satellitesInFix INTEGER NOT NULL, " +
                        "ambientTemperature REAL, " +
                        "accelX REAL, " +
                        "accelY REAL, " +
                        "accelZ REAL, " +
                        "leanAngle REAL, " +
                        "usageType TEXT, " +
                        "isPlacemark INTEGER NOT NULL, " +
                        "eventKind TEXT NOT NULL, " +
                        "baroAltitude REAL, " +
                        "pressureHpa REAL, " +
                        "FOREIGN KEY(sessionId) REFERENCES track_sessions(id) ON DELETE CASCADE" +
                        ")"
                )
                db.execSQL(
                    "INSERT INTO gps_events_new (" +
                        "id, sessionId, timestamp, latitude, longitude, altitude, speed, bearing, " +
                        "accuracy, satellitesInFix, ambientTemperature, accelX, accelY, accelZ, " +
                        "leanAngle, usageType, isPlacemark, eventKind, baroAltitude, pressureHpa" +
                        ") SELECT " +
                        "id, sessionId, timestamp, latitude, longitude, altitude, speed, bearing, " +
                        "accuracy, satellitesInFix, ambientTemperature, accelX, accelY, accelZ, " +
                        "leanAngle, usageType, isPlacemark, eventKind, baroAltitude, pressureHpa " +
                        "FROM gps_events"
                )
                db.execSQL("DROP TABLE gps_events")
                db.execSQL("ALTER TABLE gps_events_new RENAME TO gps_events")
                db.execSQL("CREATE INDEX index_gps_events_sessionId ON gps_events (sessionId)")
            }
        }

        fun create(context: Context): GtlDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                GtlDatabase::class.java,
                "gtl.db"
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6).build()
        }
    }
}
