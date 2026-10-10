package com.lkovari.mobile.apps.gtl.diagnostics

import androidx.test.platform.app.InstrumentationRegistry
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = android.app.Application::class)
class ProcessExitDiagnosticsTest {
    @Test
    fun describesSessionWhenExitReasonsAreUnavailable() {
        val text = ProcessExitDiagnostics.describe(InstrumentationRegistry.getInstrumentation().targetContext, 42L)

        assertEquals("session=42 closed at last saved point; previous exit reason unavailable", text)
    }

    @Test
    fun interruptedRecordIsMessageOnlyWithoutStackTrace() {
        val record = ErrorLogStore.format(
            "track.interrupted",
            RecordingInterruptedException("session=7 closed"),
            Instant.parse("2026-10-10T07:00:15Z")
        )

        assertEquals(
            "2026-10-10T07:00:15.000Z  track.interrupted\n" +
                "com.lkovari.mobile.apps.gtl.diagnostics.RecordingInterruptedException: session=7 closed\n",
            record
        )
        assertFalse(record.contains("\tat "))
    }
}
