package com.lkovari.mobile.apps.gtl.tuhu

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.lkovari.mobile.apps.gtl.data.maps.OsmDownloadState
import com.lkovari.mobile.apps.gtl.data.maps.OsmDownloadWorker
import com.lkovari.mobile.apps.gtl.data.maps.OsmMapStore
import kotlinx.coroutines.flow.Flow
import java.io.File

class TuhuMapStore(
    private val context: Context,
    private val osmMapStore: OsmMapStore
) {
    private val workManager = WorkManager.getInstance(context.applicationContext)

    fun downloadedFile(): File? = osmMapStore.downloadedFile(TuhuCatalog.REGION_ID)

    fun themeFile(): File? {
        val file = File(context.applicationContext.filesDir, "tuhu/theme.xml")
        return file.takeIf { it.isFile }
    }

    fun enqueue() {
        val request = OneTimeWorkRequestBuilder<TuhuDownloadWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setInputData(
                workDataOf(
                    OsmDownloadWorker.KEY_REGION_ID to TuhuCatalog.REGION_ID,
                    OsmDownloadWorker.KEY_URL to TuhuCatalog.ZIP_URL
                )
            )
            .addTag(OsmMapStore.WORK_TAG)
            .build()
        workManager.enqueueUniqueWork(workName(), ExistingWorkPolicy.REPLACE, request)
    }

    fun delete() {
        workManager.cancelUniqueWork(workName())
        osmMapStore.delete(TuhuCatalog.REGION_ID)
        File(context.applicationContext.filesDir, "maps/${TuhuCatalog.REGION_ID}.zip.part").delete()
        File(context.applicationContext.cacheDir, "tuhu-extract").deleteRecursively()
        val theme = File(context.applicationContext.filesDir, "tuhu/theme.xml")
        theme.delete()
        theme.parentFile?.delete()
    }

    fun observe(): Flow<OsmDownloadState> = osmMapStore.observe(TuhuCatalog.REGION_ID)

    private fun workName(): String = "osm-download-${TuhuCatalog.REGION_ID}"
}
