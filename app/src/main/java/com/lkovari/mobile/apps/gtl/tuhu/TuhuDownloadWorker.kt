package com.lkovari.mobile.apps.gtl.tuhu

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.lkovari.mobile.apps.gtl.GtlApplication
import com.lkovari.mobile.apps.gtl.data.maps.OsmDownloadWorker
import com.lkovari.mobile.apps.gtl.engine.OsmMapFile
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class TuhuDownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val url = inputData.getString(OsmDownloadWorker.KEY_URL) ?: TuhuCatalog.ZIP_URL
        val mapsDir = File(applicationContext.filesDir, "maps")
        mapsDir.mkdirs()
        val zipPart = File(mapsDir, "${TuhuCatalog.REGION_ID}.zip.part")
        val extractDir = File(applicationContext.cacheDir, "tuhu-extract")
        val target = File(mapsDir, "${TuhuCatalog.REGION_ID}.map")
        val themeTarget = File(applicationContext.filesDir, "tuhu/theme.xml")
        return try {
            download(url, zipPart)
            extractDir.deleteRecursively()
            val extracted = TuhuZip.extract(zipPart, extractDir)
            val staging = File(mapsDir, "${TuhuCatalog.REGION_ID}.map.staging")
            extracted.mapFile.copyTo(staging, overwrite = true)
            if (!OsmMapFile.isReadable(staging)) {
                staging.delete()
                error("copied map is not readable")
            }
            if (target.exists()) {
                target.delete()
            }
            if (!staging.renameTo(target)) {
                staging.copyTo(target, overwrite = true)
                staging.delete()
            }
            val theme = extracted.themeFile
            if (theme != null) {
                themeTarget.parentFile?.mkdirs()
                theme.copyTo(themeTarget, overwrite = true)
            }
            zipPart.delete()
            extractDir.deleteRecursively()
            (applicationContext as? GtlApplication)?.osmMapStore?.notifyMapsChanged()
            Result.success(workDataOf(OsmDownloadWorker.KEY_FILE to target.absolutePath))
        } catch (_: Exception) {
            TuhuDownloadCleanup.purgeFailedAttempt(mapsDir, applicationContext.cacheDir)
            Result.failure()
        }
    }

    private suspend fun download(url: String, target: File) {
        val opened = URL(url).openConnection()
        if (opened !is HttpURLConnection) {
            error("expected http connection")
        }
        val connection = opened
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
                    setProgress(
                        workDataOf(
                            OsmDownloadWorker.KEY_PROGRESS to progress,
                            OsmDownloadWorker.KEY_BYTES to copied,
                            OsmDownloadWorker.KEY_TOTAL to total
                        )
                    )
                    read = input.read(buffer)
                }
            }
        }
        connection.disconnect()
    }

    companion object {
        private const val USER_AGENT = "GPS Track Logger"
    }
}
