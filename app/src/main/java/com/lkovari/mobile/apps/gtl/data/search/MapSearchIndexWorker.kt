package com.lkovari.mobile.apps.gtl.data.search

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lkovari.mobile.apps.gtl.GtlApplication
import java.io.File

class MapSearchIndexWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val path = inputData.getString(KEY_PATH) ?: return Result.failure()
        val generation = inputData.getInt(KEY_GENERATION, -1)
        val app = applicationContext as? GtlApplication ?: return Result.failure()
        return app.mapSearch.runIndex(File(path), generation)
    }

    companion object {
        const val KEY_PATH = "path"
        const val KEY_GENERATION = "generation"
    }
}
