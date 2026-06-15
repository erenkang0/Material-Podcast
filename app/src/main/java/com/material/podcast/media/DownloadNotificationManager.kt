package com.material.podcast.media

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat

class DownloadNotificationManager(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "echoes_downloads"
        private const val CHANNEL_NAME = "İndirmeler"
    }

    private val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Bölüm indirme bildirimleri"
            setShowBadge(false)
        }
        nm.createNotificationChannel(channel)
    }

    fun updateProgress(
        guid: String,
        title: String,
        progress: Float,
        totalBytes: Long,
        activeCount: Int,
        queuedCount: Int,
    ) {
        val totalMB = if (totalBytes > 0) "%.1f MB".format(totalBytes / 1_048_576f) else ""
        val sub = buildString {
            if (totalMB.isNotEmpty()) append(totalMB)
            if (activeCount > 1) { if (isNotEmpty()) append(" • "); append("$activeCount aktif") }
            if (queuedCount > 0) { if (isNotEmpty()) append(" • "); append("$queuedCount sırada") }
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(if (sub.isNotEmpty()) "İndiriliyor — $sub" else "İndiriliyor…")
            .setProgress(100, (progress * 100).toInt().coerceIn(0, 100), progress <= 0f)
            .setOngoing(true)
            .setSilent(true)
            .build()
        nm.notify(guid.hashCode(), notification)
    }

    fun showComplete(guid: String, title: String) {
        nm.cancel(guid.hashCode())
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText("İndirildi")
            .setAutoCancel(true)
            .build()
        nm.notify(guid.hashCode() + 500_000, notification)
    }

    fun showFailed(guid: String, title: String) {
        nm.cancel(guid.hashCode())
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(title)
            .setContentText("İndirilemedi")
            .setAutoCancel(true)
            .build()
        nm.notify(guid.hashCode() + 1_000_000, notification)
    }

    fun cancel(guid: String) {
        nm.cancel(guid.hashCode())
    }
}
