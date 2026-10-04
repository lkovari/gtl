package com.lkovari.mobile.apps.gtl.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = android.app.Application::class)
class GtlDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        GtlDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate6To7KeepsPointsAndLeavesCardColumnsNull() {
        helper.createDatabase(TestDb, 6).apply {
            execSQL(
                "INSERT INTO track_sessions (startedAt, stoppedAt, usageType, measurementSystem) " +
                    "VALUES (1000, 2000, 'HIKE', 'METRIC')"
            )
            execSQL(
                "INSERT INTO gps_events (" +
                    "sessionId, timestamp, latitude, longitude, altitude, speed, bearing, accuracy, " +
                    "satellitesInFix, isPlacemark, eventKind" +
                    ") VALUES (1, 1500, 47.5, 19.04, 120.0, 1.5, 90.0, 4.0, 8, 0, 'MOVE')"
            )
            close()
        }
        val migrated = helper.runMigrationsAndValidate(TestDb, 7, true, GtlDatabase.MIGRATION_6_7)
        migrated.query(
            "SELECT distanceMeters, durationMs, avgSpeedMps, maxSpeedMps, previewPolyline, altitude, latitude " +
                "FROM track_sessions JOIN gps_events ON gps_events.sessionId = track_sessions.id"
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertTrue(cursor.isNull(0))
            assertTrue(cursor.isNull(1))
            assertTrue(cursor.isNull(2))
            assertTrue(cursor.isNull(3))
            assertTrue(cursor.isNull(4))
            assertEquals(120.0, cursor.getDouble(5), 0.001)
            assertEquals(47.5, cursor.getDouble(6), 0.001)
        }
        migrated.query(
            "SELECT name FROM sqlite_master WHERE type = 'index' AND tbl_name = 'gps_events'"
        ).use { cursor ->
            val names = mutableListOf<String>()
            while (cursor.moveToNext()) {
                names.add(cursor.getString(0))
            }
            assertTrue(names.contains("index_gps_events_sessionId_timestamp"))
            assertFalse(names.contains("index_gps_events_sessionId"))
        }
        migrated.close()
    }

    private companion object {
        const val TestDb = "migration-6-7"
    }
}
