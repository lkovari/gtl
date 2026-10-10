package com.lkovari.mobile.apps.gtl.diagnostics

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import java.time.Instant

/**
 * Records why the previous process ended, so an interrupted recording can be traced from the
 * in-app error log without adb (`dumpsys activity exit-info`).
 */
object ProcessExitDiagnostics {
    fun recordLastExit(context: Context, sessionId: Long) {
        AppErrorLog.record("track.interrupted", RecordingInterruptedException(describe(context, sessionId)))
    }

    internal fun describe(context: Context, sessionId: Long): String {
        val exit = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) lastExit(context) else null
        return "session=$sessionId closed at last saved point; ${exit ?: "previous exit reason unavailable"}"
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun lastExit(context: Context): String? {
        val manager = context.getSystemService(ActivityManager::class.java) ?: return null
        val info = runCatching {
            manager.getHistoricalProcessExitReasons(context.packageName, 0, 1).firstOrNull()
        }.getOrNull() ?: return null
        return "previous exit at=${Instant.ofEpochMilli(info.timestamp)} reason=${info.reason} " +
            "status=${info.status} importance=${info.importance} description=${info.description}"
    }
}

/** Log record only: carries no stack trace, the message holds the facts. */
class RecordingInterruptedException(message: String) : Exception(message) {
    override fun fillInStackTrace(): Throwable = this
}
