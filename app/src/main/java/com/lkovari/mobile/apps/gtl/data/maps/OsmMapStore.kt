package com.lkovari.mobile.apps.gtl.data.maps

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.lkovari.mobile.apps.gtl.engine.OsmMapFile
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

data class OsmDownloadState(
    val regionId: String?,
    val running: Boolean,
    val progress: Int,
    val failed: Boolean,
    val totalBytes: Long = 0L,
    val failureReason: String? = null
)

class OsmMapStore(private val context: Context) {
    private val workManager = WorkManager.getInstance(context.applicationContext)
    private val mapsDir = File(context.applicationContext.filesDir, "maps")
    private val _downloadedRevision = MutableStateFlow(0)
    val downloadedRevision: StateFlow<Int> = _downloadedRevision.asStateFlow()
    private val _regionSizes = MutableStateFlow<Map<String, Long>>(emptyMap())
    val regionSizes: StateFlow<Map<String, Long>> = _regionSizes.asStateFlow()

    fun downloadedFile(regionId: String): File? {
        val file = File(mapsDir, "$regionId.map")
        return file.takeIf { OsmMapFile.isReadable(it) }
    }

    fun listDownloaded(): List<File> {
        if (!mapsDir.exists()) {
            return emptyList()
        }
        return mapsDir.listFiles { file -> file.extension == "map" }
            ?.filter { OsmMapFile.isReadable(it) }
            .orEmpty()
    }

    fun hasDownloadedMap(): Boolean {
        return listDownloaded().isNotEmpty()
    }

    fun observeHasDownloadedMap(): Flow<Boolean> {
        return combine(
            downloadedRevision,
            workManager.getWorkInfosByTagFlow(WORK_TAG)
        ) { _, _ ->
            hasDownloadedMap()
        }
    }

    fun enqueue(region: OsmRegion, title: String) {
        val request = OneTimeWorkRequestBuilder<OsmDownloadWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setInputData(
                workDataOf(
                    OsmDownloadWorker.KEY_REGION_ID to region.id,
                    OsmDownloadWorker.KEY_URL to region.url,
                    OsmDownloadWorker.KEY_TITLE to title
                )
            )
            .addTag(WORK_TAG)
            .build()
        workManager.enqueueUniqueWork(workName(region.id), ExistingWorkPolicy.REPLACE, request)
    }

    suspend fun probeSizes(regions: List<OsmRegion>) {
        val gate = Semaphore(4)
        val found = coroutineScope {
            regions.map { region ->
                async {
                    gate.withPermit {
                        withContext(Dispatchers.IO) {
                            region.id to MapFileDownloader.headLength(region.url)
                        }
                    }
                }
            }.awaitAll().mapNotNull { (id, length) ->
                if (length == null) null else id to length
            }.toMap()
        }
        if (found.isNotEmpty()) {
            _regionSizes.value = _regionSizes.value + found
        }
    }

    fun delete(regionId: String) {
        workManager.cancelUniqueWork(workName(regionId))
        File(mapsDir, "$regionId.map.part").delete()
        File(mapsDir, "$regionId.map").delete()
        notifyMapsChanged()
    }

    fun notifyMapsChanged() {
        _downloadedRevision.value = _downloadedRevision.value + 1
    }

    fun observe(regionId: String): Flow<OsmDownloadState> {
        return workManager.getWorkInfosForUniqueWorkFlow(workName(regionId)).map { infos ->
            val info = infos.firstOrNull()
            val progress = info?.progress?.getInt(OsmDownloadWorker.KEY_PROGRESS, 0) ?: 0
            val totalBytes = info?.progress?.getLong(OsmDownloadWorker.KEY_TOTAL, 0L) ?: 0L
            val failed = info?.state == WorkInfo.State.FAILED
            OsmDownloadState(
                regionId = regionId,
                running = info?.state == WorkInfo.State.RUNNING || info?.state == WorkInfo.State.ENQUEUED,
                progress = progress,
                failed = failed,
                totalBytes = totalBytes,
                failureReason = if (failed) {
                    info.outputData.getString(OsmDownloadWorker.KEY_REASON)
                } else {
                    null
                }
            )
        }
    }

    private fun workName(regionId: String): String = "osm-download-$regionId"

    companion object {
        const val WORK_TAG = "osm-download"
    }
}
