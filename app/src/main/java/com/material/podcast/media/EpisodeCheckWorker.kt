package com.material.podcast.media

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.material.podcast.EchoesApplication
import com.material.podcast.MainActivity
import com.material.podcast.R
import com.material.podcast.data.store.LibraryStore

/**
 * Periodically checks every followed podcast's RSS feed for a newly published episode and,
 * when one appears, posts a per-podcast notification. The latest seen episode GUID is
 * remembered in [LibraryStore] so each new episode only notifies once.
 */
class EpisodeCheckWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        LibraryStore.init(applicationContext)
        ensureChannel()

        val repo = EchoesApplication.instance.repository
        val followed = LibraryStore.followedPodcasts.toList()
        if (followed.isEmpty()) return Result.success()

        followed.forEach { podcast ->
            try {
                val episodes = repo.getEpisodes(podcast)
                val latest = episodes.firstOrNull() ?: return@forEach
                val previous = LibraryStore.getLastKnownEpisodeGuid(podcast.id)
                if (previous == null) {
                    // First time we see this show — just record, don't spam.
                    LibraryStore.setLastKnownEpisodeGuid(podcast.id, latest.guid)
                } else if (previous != latest.guid) {
                    LibraryStore.setLastKnownEpisodeGuid(podcast.id, latest.guid)
                    if (LibraryStore.isNotifyEnabled(podcast.id)) {
                        notify(podcast.id.hashCode(), podcast.title, latest.title)
                    }
                }
            } catch (_: Exception) {
                // Ignore a single feed failure; try again next cycle.
            }
        }
        return Result.success()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Yeni bölümler",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "Takip ettiğin podcastlerin yeni bölümleri" }
            val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun notify(id: Int, podcastTitle: String, episodeTitle: String) {
        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val intent = Intent(applicationContext, MainActivity::class.java)
            .apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP }
        val pending = PendingIntent.getActivity(
            applicationContext, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("Yeni bölüm: $podcastTitle")
            .setContentText(episodeTitle)
            .setStyle(NotificationCompat.BigTextStyle().bigText(episodeTitle))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(id, notification)
    }

    companion object {
        private const val CHANNEL_ID = "new_episodes"
    }
}
