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

enum class DownloadStatus { Downloading, Completed, Failed }

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

    val states: SnapshotStateMap<String, DownloadState> = mutableStateMapOf()

    private val dir: File get() = File(appContext.filesDir, "downloads").apply { mkdirs() }

    private fun fileName(guid: String) = guid.hashCode().toString() + ".audio"
    fun fileFor(guid: String): File = File(dir, fileName(guid))
    fun isDownloaded(guid: String): Boolean = fileFor(guid).exists()
    fun localUriOrNull(guid: String): String? =
        fileFor(guid).takeIf { it.exists() }?.let { Uri.fromFile(it).toString() }

    fun download(episode: PodcastEpisode) {
        val guid = episode.guid
        if (isDownloaded(guid) || states[guid]?.status == DownloadStatus.Downloading) return
        states[guid] = DownloadState(DownloadStatus.Downloading, 0f)
        scope.launch {
            val target = fileFor(guid)
            val tmp = File(dir, "${fileName(guid)}.part")
            try {
                val req = Request.Builder().url(episode.audioUrl).build()
                client.newCall(req).execute().use { resp ->
                    val body = resp.body
                    if (!resp.isSuccessful || body == null) { fail(guid); return@launch }
                    val total = body.contentLength()
                    body.byteStream().use { input ->
                        tmp.outputStream().use { output ->
                            val buf = ByteArray(64 * 1024)
                            var downloaded = 0L
                            var read: Int
                            while (input.read(buf).also { read = it } != -1) {
                                output.write(buf, 0, read)
                                downloaded += read
                                if (total > 0) {
                                    states[guid] = DownloadState(
                                        DownloadStatus.Downloading,
                                        (downloaded.toFloat() / total).coerceIn(0f, 1f),
                                    )
                                }
                            }
                        }
                    }
                }
                if (tmp.renameTo(target)) {
                    states[guid] = DownloadState(DownloadStatus.Completed, 1f)
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        LibraryStore.addDownload(episode)
                    }
                } else {
                    tmp.delete()
                    fail(guid)
                }
            } catch (_: Exception) {
                tmp.delete()
                fail(guid)
            }
        }
    }

    fun delete(guid: String) {
        fileFor(guid).delete()
        states.remove(guid)
        LibraryStore.removeDownload(guid)
    }

    private fun fail(guid: String) {
        states[guid] = DownloadState(DownloadStatus.Failed)
    }
}
