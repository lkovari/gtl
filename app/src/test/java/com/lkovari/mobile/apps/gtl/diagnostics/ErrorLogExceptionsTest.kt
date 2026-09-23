package com.lkovari.mobile.apps.gtl.diagnostics

import android.database.SQLException
import java.io.File
import java.io.IOException
import org.junit.Assert.assertTrue
import org.junit.Test

class ErrorLogExceptionsTest {
    @Test
    fun ioExceptionKeepsMessageAndStack() {
        assertLogged("osm.download", IOException("download failed"))
    }

    @Test
    fun sqlExceptionKeepsMessageAndStack() {
        assertLogged("track.insert", SQLException("insert failed"))
    }

    @Test
    fun illegalArgumentExceptionWithoutMessageStillHasStack() {
        assertLogged("map.render", IllegalArgumentException())
    }

    @Test
    fun causeChainKeepsEveryException() {
        val error = IllegalStateException(
            "theme",
            IOException("theme file", IllegalArgumentException("bad zoom"))
        )
        val text = assertLogged("map.theme", error)
        assertTrue(text.contains("Caused by: java.io.IOException: theme file"))
        assertTrue(text.contains("Caused by: java.lang.IllegalArgumentException: bad zoom"))
    }

    @Test
    fun outOfMemoryErrorKeepsMessageAndStack() {
        assertLogged("map.destroy", OutOfMemoryError("tile bitmap"))
    }

    @Test
    fun differentExceptionsStayInOrder() {
        bindLog()
        val download = IOException("download failed")
        val insert = SQLException("insert failed")
        val theme = IllegalStateException("theme", IOException("theme file"))
        AppErrorLog.recordSync("osm.download", download)
        AppErrorLog.recordSync("track.insert", insert)
        AppErrorLog.recordSync("tuhu.download", theme)
        val text = AppErrorLog.read()
        val downloadAt = text.indexOf("osm.download")
        val insertAt = text.indexOf("track.insert")
        val themeAt = text.indexOf("tuhu.download")
        assertTrue(downloadAt >= 0 && downloadAt < insertAt && insertAt < themeAt)
        assertTrue(text.contains(download.stackTraceToString().trimEnd()))
        assertTrue(text.contains(insert.stackTraceToString().trimEnd()))
        assertTrue(text.contains(theme.stackTraceToString().trimEnd()))
    }

    @Test
    fun asyncRecordWritesTheIoException() {
        bindLog()
        val error = IOException("download failed")
        AppErrorLog.record("osm.download", error)
        val deadline = System.currentTimeMillis() + 2_000
        var text = ""
        while (System.currentTimeMillis() < deadline) {
            text = AppErrorLog.read()
            if (text.contains(error.stackTraceToString().trimEnd())) {
                break
            }
            Thread.sleep(10)
        }
        assertTrue(text.contains(Regex("""\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z  osm\.download""")))
        assertTrue(text.contains(error.stackTraceToString().trimEnd()))
    }

    private fun assertLogged(action: String, error: Throwable): String {
        bindLog()
        AppErrorLog.recordSync(action, error)
        val text = AppErrorLog.read()
        val quoted = Regex.escape(action)
        assertTrue(text.contains(Regex("""\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z  $quoted""")))
        assertTrue(text.contains(error.stackTraceToString().trimEnd()))
        return text
    }

    private fun bindLog() {
        val directory = File(System.getProperty("java.io.tmpdir"), "gtl-error-log-${System.nanoTime()}")
        directory.mkdirs()
        AppErrorLog.bind(directory)
        AppErrorLog.clear()
    }
}
