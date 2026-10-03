package com.lkovari.mobile.apps.gtl.data.maps

import com.lkovari.mobile.apps.gtl.engine.DownloadBodyMode
import com.lkovari.mobile.apps.gtl.engine.DownloadResume
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class MapDownloadStop(val reason: String) : Exception(reason)

object MapFileDownloader {
    const val USER_AGENT = "GPS Track Logger"

    suspend fun download(
        url: String,
        target: File,
        fallbackUrl: String?,
        onProgress: suspend (copied: Long, total: Long) -> Unit
    ) {
        try {
            transfer(url, target, onProgress)
        } catch (stop: MapDownloadStop) {
            if (stop.reason == OsmDownloadBudget.ReasonOther && fallbackUrl != null) {
                transfer(fallbackUrl, target, onProgress)
            } else {
                throw stop
            }
        }
    }

    private suspend fun transfer(
        url: String,
        target: File,
        onProgress: suspend (copied: Long, total: Long) -> Unit
    ) {
        val existing = if (target.isFile) target.length() else 0L
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.setRequestProperty("User-Agent", USER_AGENT)
        DownloadResume.rangeHeader(existing)?.let { range ->
            connection.setRequestProperty("Range", range)
        }
        connection.connectTimeout = 60_000
        connection.readTimeout = 120_000
        connection.instanceFollowRedirects = true
        try {
            connection.connect()
            val code = connection.responseCode
            if (code !in 200..299) {
                throw MapDownloadStop(OsmDownloadBudget.ReasonOther)
            }
            val append = DownloadResume.bodyMode(code, existing) == DownloadBodyMode.Append
            val total = DownloadResume.fullSize(
                existingBytes = existing,
                responseCode = code,
                contentLength = connection.contentLengthLong,
                contentRangeTotal = DownloadResume.contentRangeTotal(
                    connection.getHeaderField("Content-Range")
                )
            )
            val usable = target.parentFile?.usableSpace ?: 0L
            OsmDownloadBudget.rejection(total, usable, existing)?.let { reason ->
                throw MapDownloadStop(reason)
            }
            var copied = if (append) existing else 0L
            var lastSpaceCheck = 0L
            connection.inputStream.use { input ->
                FileOutputStream(target, append).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var read = input.read(buffer)
                    while (read >= 0) {
                        output.write(buffer, 0, read)
                        copied += read
                        if (!OsmDownloadBudget.copiedAllowed(copied)) {
                            throw MapDownloadStop(OsmDownloadBudget.ReasonTooLarge)
                        }
                        if (OsmDownloadBudget.shouldRecheckSpace(copied, lastSpaceCheck)) {
                            lastSpaceCheck = copied
                            val space = target.parentFile?.usableSpace ?: 0L
                            if (!OsmDownloadBudget.spaceAllowsMore(space)) {
                                throw MapDownloadStop(OsmDownloadBudget.ReasonNoSpace)
                            }
                        }
                        onProgress(copied, total)
                        read = input.read(buffer)
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    fun headLength(url: String): Long? {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "HEAD"
        connection.setRequestProperty("User-Agent", USER_AGENT)
        connection.connectTimeout = 20_000
        connection.readTimeout = 20_000
        connection.instanceFollowRedirects = true
        return try {
            connection.connect()
            val length = connection.contentLengthLong
            if (connection.responseCode in 200..299 && length >= 0L) length else null
        } catch (error: IOException) {
            null
        } finally {
            connection.disconnect()
        }
    }
}
