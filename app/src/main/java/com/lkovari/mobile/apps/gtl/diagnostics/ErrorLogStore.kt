package com.lkovari.mobile.apps.gtl.diagnostics

import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

internal object ErrorLogStore {
    const val MaxBytes = 256 * 1024
    const val CurrentName = "errors.log"
    const val PreviousName = "errors.log.1"

    private val stamp = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT)
        .withZone(ZoneOffset.UTC)

    fun format(action: String, error: Throwable, at: Instant): String {
        val stack = error.stackTraceToString().trimEnd()
        return "${stamp.format(at)}  $action\n$stack\n"
    }

    fun append(directory: File, record: String, maxBytes: Int = MaxBytes) {
        directory.mkdirs()
        val current = File(directory, CurrentName)
        val body = record.toByteArray(Charsets.UTF_8)
        val existing = if (current.isFile) current.length() else 0L
        val separator = if (existing > 0L) 1 else 0
        if (existing > 0L && existing + separator + body.size > maxBytes) {
            rotate(directory)
            write(File(directory, CurrentName), body)
        } else if (existing > 0L) {
            write(current, byteArrayOf('\n'.code.toByte()) + body)
        } else {
            write(current, body)
        }
    }

    fun read(directory: File): String {
        val previous = textOf(File(directory, PreviousName))
        val current = textOf(File(directory, CurrentName))
        return when {
            previous.isEmpty() -> current
            current.isEmpty() -> previous
            previous.endsWith("\n") -> previous + "\n" + current
            else -> "$previous\n\n$current"
        }
    }

    fun clear(directory: File) {
        File(directory, PreviousName).delete()
        File(directory, CurrentName).delete()
    }

    private fun rotate(directory: File) {
        val current = File(directory, CurrentName)
        val previous = File(directory, PreviousName)
        if (previous.exists()) {
            previous.delete()
        }
        if (!current.renameTo(previous)) {
            current.copyTo(previous, overwrite = true)
            current.delete()
        }
    }

    private fun write(file: File, bytes: ByteArray) {
        FileOutputStream(file, true).use { stream ->
            stream.write(bytes)
        }
    }

    private fun textOf(file: File): String {
        if (!file.isFile) {
            return ""
        }
        return file.readText(Charsets.UTF_8)
    }
}
