package com.lkovari.mobile.apps.gtl.data.maps

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.lkovari.mobile.apps.gtl.GtlApplication
import com.lkovari.mobile.apps.gtl.engine.OsmMapFile
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class OsmDownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val regionId = inputData.getString(KEY_REGION_ID) ?: return Result.failure()
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        val target = File(applicationContext.filesDir, "maps/$regionId.map")
        target.parentFile?.mkdirs()
        val temp = File(target.parentFile, "$regionId.map.part")
        return try {
            download(url, temp)
            if (!OsmMapFile.isReadable(temp)) {
                temp.delete()
                return Result.failure()
            }
            if (!temp.renameTo(target)) {
                temp.copyTo(target, overwrite = true)
                temp.delete()
            }
            if (!OsmMapFile.isReadable(target)) {
                return Result.failure()
            }
            (applicationContext as? GtlApplication)?.osmMapStore?.notifyMapsChanged()
            Result.success(workDataOf(KEY_FILE to target.absolutePath))
        } catch (_: Exception) {
            temp.delete()
            Result.failure()
        }
    }

    private suspend fun download(url: String, target: File) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.setRequestProperty("User-Agent", USER_AGENT)
        connection.connectTimeout = 60_000
        connection.readTimeout = 120_000
        connection.instanceFollowRedirects = true
        connection.connect()
        if (connection.responseCode !in 200..299) {
            connection.disconnect()
            error("HTTP ${connection.responseCode}")
        }
        val total = connection.contentLengthLong
        val usable = target.parentFile?.usableSpace ?: 0L
        if (!OsmDownloadBudget.canStart(total, usable)) {
            connection.disconnect()
            error("download too large")
        }
        try {
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var copied = 0L
                    var lastSpaceCheck = 0L
                    var read = input.read(buffer)
                    while (read >= 0) {
                        output.write(buffer, 0, read)
                        copied += read
                        if (!OsmDownloadBudget.copiedAllowed(copied)) {
                            error("download too large")
                        }
                        if (OsmDownloadBudget.shouldRecheckSpace(copied, lastSpaceCheck)) {
                            lastSpaceCheck = copied
                            val space = target.parentFile?.usableSpace ?: 0L
                            if (!OsmDownloadBudget.spaceAllowsMore(space)) {
                                error("not enough space")
                            }
                        }
                        val progress = if (total > 0) {
                            ((copied * 100L) / total).toInt().coerceIn(0, 100)
                        } else {
                            0
                        }
                        setProgress(workDataOf(KEY_PROGRESS to progress, KEY_BYTES to copied, KEY_TOTAL to total))
                        read = input.read(buffer)
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val KEY_REGION_ID = "region_id"
        const val KEY_URL = "url"
        const val KEY_FILE = "file"
        const val KEY_PROGRESS = "progress"
        const val KEY_BYTES = "bytes"
        const val KEY_TOTAL = "total"
        private const val USER_AGENT = "GPS Track Logger"
    }
}
