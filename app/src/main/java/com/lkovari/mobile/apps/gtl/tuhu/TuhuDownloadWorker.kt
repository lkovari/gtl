package com.lkovari.mobile.apps.gtl.tuhu

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.lkovari.mobile.apps.gtl.GtlApplication
import com.lkovari.mobile.apps.gtl.data.maps.MapDownloadNotice
import com.lkovari.mobile.apps.gtl.data.maps.MapDownloadStop
import com.lkovari.mobile.apps.gtl.data.maps.OsmDownloadBudget
import com.lkovari.mobile.apps.gtl.data.maps.OsmDownloadWorker
import com.lkovari.mobile.apps.gtl.diagnostics.AppErrorLog
import com.lkovari.mobile.apps.gtl.engine.DownloadBodyMode
import com.lkovari.mobile.apps.gtl.engine.DownloadResume
import com.lkovari.mobile.apps.gtl.engine.OsmMapFile
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CancellationException

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
        val title = inputData.getString(OsmDownloadWorker.KEY_TITLE) ?: TuhuCatalog.REGION_ID
        return try {
            MapDownloadNotice.promote(this, TuhuCatalog.REGION_ID, title)
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
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (stop: MapDownloadStop) {
            AppErrorLog.record("tuhu.download", stop)
            TuhuDownloadCleanup.purgeFailedAttempt(mapsDir, applicationContext.cacheDir)
            Result.failure(workDataOf(OsmDownloadWorker.KEY_REASON to stop.reason))
        } catch (error: IOException) {
            AppErrorLog.record("tuhu.download", error)
            Result.retry()
        } catch (error: Exception) {
            AppErrorLog.record("tuhu.download", error)
            TuhuDownloadCleanup.purgeFailedAttempt(mapsDir, applicationContext.cacheDir)
            Result.failure(workDataOf(OsmDownloadWorker.KEY_REASON to OsmDownloadBudget.ReasonOther))
        }
    }

    private suspend fun download(url: String, target: File) {
        var current = URL(url)
        var hops = 0
        while (hops <= TuhuDownloadPolicy.MaxRedirects) {
            if (!TuhuDownloadPolicy.isAllowedUrl(current)) {
                error("blocked url")
            }
            val opened = current.openConnection()
            if (opened !is HttpURLConnection) {
                error("expected http connection")
            }
            val connection = opened
            connection.setRequestProperty("User-Agent", USER_AGENT)
            DownloadResume.rangeHeader(if (target.isFile) target.length() else 0L)?.let { range ->
                connection.setRequestProperty("Range", range)
            }
            connection.connectTimeout = 60_000
            connection.readTimeout = 120_000
            connection.instanceFollowRedirects = false
            connection.connect()
            val code = connection.responseCode
            if (code in 300..399) {
                val location = connection.getHeaderField("Location")
                connection.disconnect()
                if (location.isNullOrBlank()) {
                    error("redirect without location")
                }
                current = URL(current, location)
                hops += 1
                continue
            }
            if (code !in 200..299) {
                connection.disconnect()
                throw MapDownloadStop(OsmDownloadBudget.ReasonOther)
            }
            val existing = if (target.isFile) target.length() else 0L
            val append = DownloadResume.bodyMode(code, existing) == DownloadBodyMode.Append
            val total = DownloadResume.fullSize(
                existingBytes = existing,
                responseCode = code,
                contentLength = connection.contentLengthLong,
                contentRangeTotal = DownloadResume.contentRangeTotal(
                    connection.getHeaderField("Content-Range")
                )
            )
            if (total > TuhuDownloadPolicy.MaxDownloadBytes) {
                connection.disconnect()
                throw MapDownloadStop(OsmDownloadBudget.ReasonTooLarge)
            }
            try {
                var copied = if (append) existing else 0L
                connection.inputStream.use { input ->
                    FileOutputStream(target, append).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var read = input.read(buffer)
                        while (read >= 0) {
                            output.write(buffer, 0, read)
                            copied += read
                            if (copied > TuhuDownloadPolicy.MaxDownloadBytes) {
                                throw MapDownloadStop(OsmDownloadBudget.ReasonTooLarge)
                            }
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
            } finally {
                connection.disconnect()
            }
            return
        }
        error("too many redirects")
    }

    companion object {
        private const val USER_AGENT = "GPS Track Logger"
    }
}
