package com.material.podcast.media

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.material.podcast.data.model.PodcastEpisode
import com.material.podcast.data.store.LibraryStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

enum class DownloadStatus { Queued, Downloading, Completed, Failed }

data class DownloadState(val status: DownloadStatus, val progress: Float = 0f)

/**
 * Downloads episode audio to internal storage for offline playback. Progress is exposed
 * reactively via [states] (keyed by episode guid); completed downloads are recorded in
 * [LibraryStore] so they survive restarts, with the audio kept on disk.
 */
class DownloadManager(private val appContext: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val notifManager = DownloadNotificationManager(appContext)

    val states: SnapshotStateMap<String, DownloadState> = mutableStateMapOf()

    // Ordered queue of pending downloads (waiting for an active slot).
    private val pendingQueue = ArrayDeque<PodcastEpisode>()
    private val maxConcurrent = 3

    private val dir: File get() = File(appContext.filesDir, "downloads").apply { mkdirs() }

    private fun fileName(guid: String) = guid.hashCode().toString() + ".audio"
    fun fileFor(guid: String): File = File(dir, fileName(guid))
    fun isDownloaded(guid: String): Boolean = fileFor(guid).exists()
    fun localUriOrNull(guid: String): String? =
        fileFor(guid).takeIf { it.exists() }?.let { Uri.fromFile(it).toString() }

    fun download(episode: PodcastEpisode) {
        val guid = episode.guid
        if (isDownloaded(guid)) return
        val current = states[guid]?.status
        if (current == DownloadStatus.Downloading || current == DownloadStatus.Queued) return

        val activeCount = states.values.count { it.status == DownloadStatus.Downloading }
        if (activeCount >= maxConcurrent) {
            if (pendingQueue.none { it.guid == guid }) pendingQueue.addLast(episode)
            states[guid] = DownloadState(DownloadStatus.Queued, 0f)
        } else {
            startDownload(episode)
        }
    }

    private fun startDownload(episode: PodcastEpisode) {
        val guid = episode.guid
        states[guid] = DownloadState(DownloadStatus.Downloading, 0f)
        scope.launch {
            val target = fileFor(guid)
            val tmp = File(dir, "${fileName(guid)}.part")
            try {
                val req = Request.Builder().url(episode.audioUrl).build()
                client.newCall(req).execute().use { resp ->
                    val body = resp.body
                    if (!resp.isSuccessful || body == null) { fail(guid, episode.title); return@launch }
                    val total = body.contentLength()
                    body.byteStream().use { input ->
                        tmp.outputStream().use { output ->
                            val buf = ByteArray(64 * 1024)
                            var downloaded = 0L
                            var read: Int
                            while (input.read(buf).also { read = it } != -1) {
                                output.write(buf, 0, read)
                                downloaded += read
                                val progress = if (total > 0) (downloaded.toFloat() / total).coerceIn(0f, 1f) else 0f
                                states[guid] = DownloadState(DownloadStatus.Downloading, progress)
                                val activeCount = states.values.count { it.status == DownloadStatus.Downloading }
                                notifManager.updateProgress(guid, episode.title, progress, total, activeCount, pendingQueue.size)
                            }
                        }
                    }
                }
                if (tmp.renameTo(target)) {
                    states[guid] = DownloadState(DownloadStatus.Completed, 1f)
                    notifManager.showComplete(guid, episode.title)
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        LibraryStore.addDownload(episode)
                    }
                } else {
                    tmp.delete()
                    fail(guid, episode.title)
                }
            } catch (_: Exception) {
                tmp.delete()
                fail(guid, episode.title)
            } finally {
                startNextQueued()
            }
        }
    }

    private fun startNextQueued() {
        val next = pendingQueue.removeFirstOrNull() ?: return
        states.remove(next.guid)
        startDownload(next)
    }

    fun delete(guid: String) {
        fileFor(guid).delete()
        states.remove(guid)
        pendingQueue.removeAll { it.guid == guid }
        notifManager.cancel(guid)
        LibraryStore.removeDownload(guid)
    }

    private fun fail(guid: String, title: String) {
        states[guid] = DownloadState(DownloadStatus.Failed)
        notifManager.showFailed(guid, title)
    }
}
