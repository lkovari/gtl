package com.lkovari.mobile.apps.gtl.data.db

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.lkovari.mobile.apps.gtl.GtlApplication

class TrackCardBackfillWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as GtlApplication
        if (app.trackingState.state.value.logging) {
            return Result.retry()
        }
        val pending = app.trackRepository.sessionsNeedingCard()
        for (session in pending) {
            if (app.trackingState.state.value.logging) {
                return Result.retry()
            }
            app.trackRepository.backfillCard(session.id)
        }
        return Result.success()
    }

    companion object {
        private const val WorkName = "track-card-backfill"

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<TrackCardBackfillWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(WorkName, ExistingWorkPolicy.KEEP, request)
        }
    }
}
