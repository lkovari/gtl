package com.lkovari.mobile.apps.gtl.diagnostics

import android.content.Context
import java.io.File

object RecordingInterruptNotice {
    private const val FileName = "interrupted-session"

    fun mark(context: Context, sessionId: Long) {
        val directory = File(context.filesDir, "diagnostics")
        directory.mkdirs()
        File(directory, FileName).writeText(sessionId.toString())
    }

    fun pending(context: Context): Long? {
        val file = File(context.filesDir, "diagnostics/$FileName")
        if (!file.isFile) {
            return null
        }
        return file.readText().trim().toLongOrNull()
    }

    fun clear(context: Context) {
        File(context.filesDir, "diagnostics/$FileName").delete()
    }
}
