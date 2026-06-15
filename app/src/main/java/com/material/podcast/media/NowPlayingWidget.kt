package com.material.podcast.media

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.material.podcast.MainActivity
import com.material.podcast.R
import com.material.podcast.data.store.LibraryStore

/**
 * A compact home-screen widget showing where the listener left off; tapping it opens the app.
 * Refreshed periodically by the OS and pushed by the player as progress is saved.
 */
class NowPlayingWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateOne(context, manager, it) }
    }

    companion object {
        /** Rebuild every placed instance of the widget (safe to call from anywhere). */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(ComponentName(context, NowPlayingWidget::class.java))
            ids.forEach { updateOne(context, manager, it) }
        }

        private fun updateOne(context: Context, manager: AppWidgetManager, id: Int) {
            LibraryStore.init(context)
            val views = RemoteViews(context.packageName, R.layout.widget_now_playing)
            val resume = LibraryStore.lastResume()
            if (resume != null) {
                views.setTextViewText(R.id.widget_title, resume.episode.title)
                views.setTextViewText(R.id.widget_subtitle, resume.episode.podcastTitle)
            } else {
                views.setTextViewText(R.id.widget_title, "Dinlemeye başla")
                views.setTextViewText(R.id.widget_subtitle, "Keşfet'ten bir bölüm seç")
            }
            val intent = Intent(context, MainActivity::class.java)
                .apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP }
            val pending = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, pending)
            manager.updateAppWidget(id, views)
        }
    }
}
