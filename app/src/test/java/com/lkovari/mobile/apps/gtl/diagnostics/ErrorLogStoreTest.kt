package com.lkovari.mobile.apps.gtl.diagnostics

import java.io.File
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ErrorLogStoreTest {
    @Test
    fun recordContainsTimestampActionAndFullStack() {
        val error = IllegalStateException("outer", IllegalArgumentException("root"))
        val text = ErrorLogStore.format(
            action = "map.render",
            error = error,
            at = Instant.parse("2026-09-23T12:54:01.123Z")
        )
        assertTrue(text.startsWith("2026-09-23T12:54:01.123Z  map.render\n"))
        assertTrue(text.contains("java.lang.IllegalStateException: outer"))
        assertTrue(text.contains("at com.lkovari.mobile.apps.gtl.diagnostics.ErrorLogStoreTest"))
        assertTrue(text.contains("Caused by: java.lang.IllegalArgumentException: root"))
    }

    @Test
    fun rolloverKeepsThePreviousFileThenTheCurrentOne() {
        val directory = tempDirectory()
        val first = ErrorLogStore.format("map.render", IllegalStateException("one"), Instant.EPOCH)
        val second = ErrorLogStore.format("track.insert", IllegalStateException("two"), Instant.EPOCH)
        val cap = first.toByteArray(Charsets.UTF_8).size
        ErrorLogStore.append(directory, first, maxBytes = cap)
        ErrorLogStore.append(directory, second, maxBytes = cap)
        val previous = File(directory, ErrorLogStore.PreviousName).readText(Charsets.UTF_8)
        val current = File(directory, ErrorLogStore.CurrentName).readText(Charsets.UTF_8)
        assertTrue(previous.contains("map.render"))
        assertFalse(previous.contains("track.insert"))
        assertTrue(current.contains("track.insert"))
        assertFalse(current.contains("map.render"))
        val joined = ErrorLogStore.read(directory)
        assertTrue(joined.indexOf("map.render") < joined.indexOf("track.insert"))
    }

    @Test
    fun clearRemovesBothFiles() {
        val directory = tempDirectory()
        val record = ErrorLogStore.format("osm.download", IllegalStateException("x"), Instant.EPOCH)
        ErrorLogStore.append(directory, record, maxBytes = record.toByteArray(Charsets.UTF_8).size)
        ErrorLogStore.append(directory, record, maxBytes = record.toByteArray(Charsets.UTF_8).size)
        ErrorLogStore.clear(directory)
        assertEquals("", ErrorLogStore.read(directory))
        assertFalse(File(directory, ErrorLogStore.CurrentName).exists())
        assertFalse(File(directory, ErrorLogStore.PreviousName).exists())
    }

    @Test
    fun failedWriteDoesNotEscape() {
        val blocked = File(tempDirectory(), "blocked")
        blocked.writeText("x")
        AppErrorLog.bind(blocked)
        AppErrorLog.recordSync("map.render", IllegalStateException("boom"))
    }

    private fun tempDirectory(): File {
        return File(System.getProperty("java.io.tmpdir"), "gtl-error-log-${System.nanoTime()}").apply {
            mkdirs()
        }
    }
}
