package com.lkovari.mobile.apps.gtl.data.maps

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
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
            if (target.exists()) {
                target.delete()
            }
            temp.renameTo(target)
            Result.success(workDataOf(KEY_FILE to target.absolutePath))
        } catch (_: Exception) {
            temp.delete()
            Result.failure()
        }
    }

    private suspend fun download(url: String, target: File) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 30_000
        connection.readTimeout = 30_000
        connection.instanceFollowRedirects = true
        connection.connect()
        if (connection.responseCode !in 200..299) {
            connection.disconnect()
            error("HTTP ${connection.responseCode}")
        }
        val total = connection.contentLengthLong
        connection.inputStream.use { input ->
            target.outputStream().use { output ->
                val buffer = ByteArray(64 * 1024)
                var copied = 0L
                var read = input.read(buffer)
                while (read >= 0) {
                    output.write(buffer, 0, read)
                    copied += read
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
        connection.disconnect()
    }

    companion object {
        const val KEY_REGION_ID = "region_id"
        const val KEY_URL = "url"
        const val KEY_FILE = "file"
        const val KEY_PROGRESS = "progress"
        const val KEY_BYTES = "bytes"
        const val KEY_TOTAL = "total"
    }
}
