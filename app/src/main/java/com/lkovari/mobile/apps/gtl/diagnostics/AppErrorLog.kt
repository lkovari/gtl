package com.lkovari.mobile.apps.gtl.diagnostics

import android.content.Context
import java.io.File
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object AppErrorLog {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()

    @Volatile
    private var directory: File? = null

    fun install(context: Context) {
        bind(File(context.filesDir, "diagnostics"))
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        if (previous is UncaughtErrorLogHandler) {
            return
        }
        Thread.setDefaultUncaughtExceptionHandler(UncaughtErrorLogHandler(previous))
    }

    fun record(action: String, error: Throwable) {
        scope.launch {
            recordSync(action, error)
        }
    }

    fun recordSync(action: String, error: Throwable) {
        val dir = directory ?: return
        try {
            synchronized(lock) {
                ErrorLogStore.append(dir, ErrorLogStore.format(action, error, Instant.now()))
            }
        } catch (_: Exception) {
        }
    }

    fun read(): String {
        val dir = directory ?: return ""
        return try {
            synchronized(lock) {
                ErrorLogStore.read(dir)
            }
        } catch (_: Exception) {
            ""
        }
    }

    fun clear() {
        val dir = directory ?: return
        try {
            synchronized(lock) {
                ErrorLogStore.clear(dir)
            }
        } catch (_: Exception) {
        }
    }

    internal fun bind(directory: File) {
        this.directory = directory
    }

    private class UncaughtErrorLogHandler(
        private val previous: Thread.UncaughtExceptionHandler?
    ) : Thread.UncaughtExceptionHandler {
        override fun uncaughtException(thread: Thread, error: Throwable) {
            recordSync("uncaught", error)
            previous?.uncaughtException(thread, error)
        }
    }
}
