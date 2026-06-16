package com.material.podcast.media

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat

class DownloadNotificationManager(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "echoes_downloads"
        private const val CHANNEL_NAME = "İndirmeler"
        private const val SUMMARY_ID = 1
        private const val DONE_ID = 2
    }

    private val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // guid -> (title, progress 0..1)
    private val active = mutableMapOf<String, Pair<String, Float>>()
    private var completedCount = 0

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
        active[guid] = Pair(title, progress)
        val count = active.size
        val avgProgress = if (active.isEmpty()) 0f else active.values.map { it.second }.average().toFloat()
        val sub = buildString {
            if (queuedCount > 0) append("$queuedCount sırada")
        }
        val contentText = buildString {
            append("$count bölüm indiriliyor")
            if (sub.isNotEmpty()) append(" · $sub")
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(if (count == 1) title else "Echoes İndirme")
            .setContentText(if (count == 1) "İndiriliyor…" else contentText)
            .setProgress(100, (avgProgress * 100).toInt().coerceIn(0, 100), avgProgress <= 0f)
            .setOngoing(true)
            .setSilent(true)
            .build()
        nm.notify(SUMMARY_ID, notification)
    }

    fun showComplete(guid: String, title: String) {
        active.remove(guid)
        completedCount++
        if (active.isEmpty()) {
            nm.cancel(SUMMARY_ID)
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle(if (completedCount == 1) title else "$completedCount bölüm indirildi")
                .setContentText("İndirme tamamlandı")
                .setAutoCancel(true)
                .build()
            nm.notify(DONE_ID, notification)
            completedCount = 0
        }
        // If other downloads still active, the ongoing notification updates naturally
        // via the next updateProgress call from a concurrent download.
    }

    fun showFailed(guid: String, title: String) {
        active.remove(guid)
        if (active.isEmpty()) {
            nm.cancel(SUMMARY_ID)
        }
    }

    fun cancel(guid: String) {
        active.remove(guid)
        if (active.isEmpty()) nm.cancel(SUMMARY_ID)
    }
}
