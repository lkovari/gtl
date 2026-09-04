package com.lkovari.mobile.apps.gtl.data.maps

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

data class OsmDownloadState(
    val regionId: String?,
    val running: Boolean,
    val progress: Int,
    val failed: Boolean
)

class OsmMapStore(private val context: Context) {
    private val workManager = WorkManager.getInstance(context.applicationContext)
    private val mapsDir = File(context.applicationContext.filesDir, "maps")

    fun downloadedFile(regionId: String): File? {
        val file = File(mapsDir, "$regionId.map")
        return if (file.exists() && file.length() > 0L) file else null
    }

    fun listDownloaded(): List<File> {
        if (!mapsDir.exists()) {
            return emptyList()
        }
        return mapsDir.listFiles { file -> file.extension == "map" }?.toList().orEmpty()
    }

    fun enqueue(region: OsmRegion) {
        val request = OneTimeWorkRequestBuilder<OsmDownloadWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInputData(
                workDataOf(
                    OsmDownloadWorker.KEY_REGION_ID to region.id,
                    OsmDownloadWorker.KEY_URL to region.url
                )
            )
            .addTag(WORK_TAG)
            .build()
        workManager.enqueueUniqueWork(workName(region.id), ExistingWorkPolicy.KEEP, request)
    }

    fun observe(regionId: String): Flow<OsmDownloadState> {
        return workManager.getWorkInfosForUniqueWorkFlow(workName(regionId)).map { infos ->
            val info = infos.firstOrNull()
            val progress = info?.progress?.getInt(OsmDownloadWorker.KEY_PROGRESS, 0) ?: 0
            OsmDownloadState(
                regionId = regionId,
                running = info?.state == WorkInfo.State.RUNNING || info?.state == WorkInfo.State.ENQUEUED,
                progress = progress,
                failed = info?.state == WorkInfo.State.FAILED
            )
        }
    }

    private fun workName(regionId: String): String = "osm-download-$regionId"

    companion object {
        const val WORK_TAG = "osm-download"
    }
}
