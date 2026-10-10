package com.lkovari.mobile.apps.gtl.service

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.lkovari.mobile.apps.gtl.data.db.GpsEventEntity
import com.lkovari.mobile.apps.gtl.data.db.GtlDatabase
import com.lkovari.mobile.apps.gtl.data.db.TrackRepository
import com.lkovari.mobile.apps.gtl.data.sync.NoOpRemoteTrackSync
import com.lkovari.mobile.apps.gtl.engine.EventKind
import com.lkovari.mobile.apps.gtl.engine.MeasurementSystem
import com.lkovari.mobile.apps.gtl.engine.UsageType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = android.app.Application::class)
class InterruptedRecordingRecoveryTest {
    private lateinit var database: GtlDatabase
    private lateinit var repository: TrackRepository
    private lateinit var gate: RecordingSessionGate
    private lateinit var recovery: InterruptedRecordingRecovery

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            GtlDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = TrackRepository(database, NoOpRemoteTrackSync())
        gate = RecordingSessionGate()
        recovery = InterruptedRecordingRecovery(repository, gate)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun orphanedSessionIsClosedAtLastSavedPoint() = runBlocking {
        val id = repository.startSession(UsageType.RUNNER, MeasurementSystem.METRIC)
        repository.insertEvent(event(id, 1_000L, 47.4489, 19.2160, EventKind.START))
        repository.insertEvent(event(id, 2_000L, 47.4513, 19.2148, EventKind.MOVE))

        assertEquals(id, recovery.closeOrphanedSession())

        assertNull(repository.openSession())
        assertNotNull(repository.sessionById(id)?.stoppedAt)
        val events = repository.eventsFor(id)
        assertEquals(3, events.size)
        val stop = events.last()
        assertEquals(EventKind.STOP.name, stop.eventKind)
        assertEquals(2_000L, stop.timestamp)
        assertEquals(47.4513, stop.latitude, 1e-9)
        assertEquals(19.2148, stop.longitude, 1e-9)
    }

    @Test
    fun sessionOwnedByRunningServiceIsLeftOpen() = runBlocking {
        val id = repository.startSession(UsageType.RUNNER, MeasurementSystem.METRIC)
        repository.insertEvent(event(id, 1_000L, 47.0, 19.0, EventKind.START))
        gate.serviceOwnsSession = true

        assertNull(recovery.closeOrphanedSession())

        assertEquals(id, repository.openSession()?.id)
        assertEquals(1, repository.eventsFor(id).size)
    }

    @Test
    fun nothingToCloseWhenNoSessionIsOpen() = runBlocking {
        val id = repository.startSession(UsageType.RUNNER, MeasurementSystem.METRIC)
        repository.stopSession(id)

        assertNull(recovery.closeOrphanedSession())
    }

    @Test
    fun emptyOrphanedSessionIsClosedWithoutMarker() = runBlocking {
        val id = repository.startSession(UsageType.RUNNER, MeasurementSystem.METRIC)

        assertEquals(id, recovery.closeOrphanedSession())

        assertNull(repository.openSession())
        assertEquals(0, repository.eventsFor(id).size)
    }

    @Test
    fun closingTwiceDoesNotAddSecondStop() = runBlocking {
        val id = repository.startSession(UsageType.RUNNER, MeasurementSystem.METRIC)
        repository.insertEvent(event(id, 1_000L, 47.0, 19.0, EventKind.START))

        recovery.closeAtLastPoint(id)
        recovery.closeAtLastPoint(id)

        assertEquals(1, repository.eventsFor(id).count { it.eventKind == EventKind.STOP.name })
    }

    private fun event(sessionId: Long, timestamp: Long, lat: Double, lon: Double, kind: EventKind) =
        GpsEventEntity(
            sessionId = sessionId,
            timestamp = timestamp,
            latitude = lat,
            longitude = lon,
            altitude = 130.0,
            speed = 2.5f,
            bearing = 0f,
            accuracy = 4f,
            satellitesInFix = 8,
            ambientTemperature = null,
            accelX = null,
            accelY = null,
            accelZ = null,
            leanAngle = null,
            usageType = UsageType.RUNNER.name,
            isPlacemark = kind != EventKind.MOVE,
            eventKind = kind.name
        )
}
