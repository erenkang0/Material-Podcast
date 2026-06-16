package com.material.podcast.media

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.transformer.Composition
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.material.podcast.EchoesApplication
import com.material.podcast.data.model.PodcastEpisode
import java.io.File

/**
 * Extracts a short clip ("snip") of the current episode between two timestamps using the Media3
 * [Transformer], then hands back a shareable file. Must be invoked from the main thread.
 */
object SnipExporter {

    /** Export [episode] between [startMs] and [endMs]; [onResult] receives the file or null on failure. */
    fun export(
        context: Context,
        episode: PodcastEpisode,
        startMs: Long,
        endMs: Long,
        onResult: (File?) -> Unit,
    ) {
        val source = EchoesApplication.instance.downloadManager.localUriOrNull(episode.guid)
            ?: episode.audioUrl
        if (source.isBlank() || endMs <= startMs) {
            onResult(null)
            return
        }

        val clip = MediaItem.Builder()
            .setUri(source)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(startMs)
                    .setEndPositionMs(endMs)
                    .build(),
            )
            .build()

        val dir = File(context.cacheDir, "snips").apply { mkdirs() }
        // Note: Media3 Transformer cannot mux true MP3; we output an AAC stream in an MP4
        // container with an `.m4a` extension (MIME audio/mp4), which is the broadly
        // compatible audio format recognized by WhatsApp and other messaging apps.
        val output = File(dir, "snip_${System.currentTimeMillis()}.m4a")

        val transformer = Transformer.Builder(context)
            // Drop any video track so the result is an audio-only clip; podcasts are
            // already audio-only but this guarantees a pure audio file for sharing.
            .setRemoveVideo(true)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, result: ExportResult) {
                    onResult(output)
                }

                override fun onError(
                    composition: Composition,
                    result: ExportResult,
                    exception: ExportException,
                ) {
                    onResult(null)
                }
            })
            .build()

        try {
            transformer.start(clip, output.absolutePath)
        } catch (e: Exception) {
            onResult(null)
        }
    }

    /** Fire a share sheet for a previously exported snip [file]. */
    fun share(context: Context, file: File, episode: PodcastEpisode) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            // m4a's MIME — more reliably recognized as shareable audio than "audio/*".
            type = "audio/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "${episode.title} — ${episode.podcastTitle} · Echoes")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, "Snip'i paylaş").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }
}
