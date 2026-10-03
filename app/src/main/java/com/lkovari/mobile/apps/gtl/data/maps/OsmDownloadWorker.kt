package com.lkovari.mobile.apps.gtl.data.maps

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.lkovari.mobile.apps.gtl.GtlApplication
import com.lkovari.mobile.apps.gtl.diagnostics.AppErrorLog
import com.lkovari.mobile.apps.gtl.engine.OsmMapFile
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CancellationException

class OsmDownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val regionId = inputData.getString(KEY_REGION_ID) ?: return Result.failure()
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        val title = inputData.getString(KEY_TITLE) ?: regionId
        val target = File(applicationContext.filesDir, "maps/$regionId.map")
        target.parentFile?.mkdirs()
        val temp = File(target.parentFile, "$regionId.map.part")
        return try {
            MapDownloadNotice.promote(this, regionId, title)
            MapFileDownloader.download(
                url = url,
                target = temp,
                fallbackUrl = OsmCatalog.fallbackUrl(url),
                onProgress = { copied, total ->
                    val progress = if (total > 0L) {
                        ((copied * 100L) / total).toInt().coerceIn(0, 100)
                    } else {
                        0
                    }
                    setProgress(
                        workDataOf(
                            KEY_PROGRESS to progress,
                            KEY_BYTES to copied,
                            KEY_TOTAL to total
                        )
                    )
                }
            )
            if (!OsmMapFile.isReadable(temp)) {
                temp.delete()
                return failure(OsmDownloadBudget.ReasonOther)
            }
            if (!temp.renameTo(target)) {
                temp.copyTo(target, overwrite = true)
                temp.delete()
            }
            if (!OsmMapFile.isReadable(target)) {
                return failure(OsmDownloadBudget.ReasonOther)
            }
            (applicationContext as? GtlApplication)?.osmMapStore?.notifyMapsChanged()
            Result.success(workDataOf(KEY_FILE to target.absolutePath))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (stop: MapDownloadStop) {
            temp.delete()
            AppErrorLog.record("osm.download", stop)
            failure(stop.reason)
        } catch (error: IOException) {
            AppErrorLog.record("osm.download", error)
            Result.retry()
        } catch (error: Exception) {
            AppErrorLog.record("osm.download", error)
            temp.delete()
            failure(OsmDownloadBudget.ReasonOther)
        }
    }

    private fun failure(reason: String): Result {
        return Result.failure(workDataOf(KEY_REASON to reason))
    }

    companion object {
        const val KEY_REGION_ID = "region_id"
        const val KEY_URL = "url"
        const val KEY_TITLE = "title"
        const val KEY_FILE = "file"
        const val KEY_PROGRESS = "progress"
        const val KEY_BYTES = "bytes"
        const val KEY_TOTAL = "total"
        const val KEY_REASON = "reason"
    }
}
