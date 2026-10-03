package com.lkovari.mobile.apps.gtl.data.maps

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import com.lkovari.mobile.apps.gtl.R
import com.lkovari.mobile.apps.gtl.diagnostics.AppErrorLog
import kotlinx.coroutines.CancellationException

object MapDownloadNotice {
    private const val CHANNEL_ID = "map_downloads"

    fun notificationId(key: String): Int {
        return 41_000 + (key.hashCode() and 0x0FFF)
    }

    suspend fun promote(worker: CoroutineWorker, key: String, title: String) {
        val context = worker.applicationContext
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.download_notification, title))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                notificationId(key),
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(notificationId(key), notification)
        }
        try {
            worker.setForeground(info)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            AppErrorLog.record("map.download.foreground", error)
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.download_channel),
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
    }
}
