package com.material.podcast.nfc

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.material.podcast.data.model.PodcastEpisode

/**
 * Coordinates "playable moment" sharing — the other half of [NfcShareController].
 *
 * A Moment Card encodes not just *which* podcast but an exact second to start from, so opening
 * the `echoes://moment?…` link (via NFC, a chat app, anywhere) drops the recipient straight into
 * playback at that instant. The payload is self-contained (it carries the audio URL) so the
 * recipient can play even without already following the show.
 */
object MomentShareController {

    data class MomentPayload(
        val guid: String,
        val title: String,
        val podcastTitle: String,
        val artworkUrl: String,
        val audioUrl: String,
        val startMs: Long,
        val quote: String,
    )

    /** Set when a moment link arrives; the Compose layer observes this, plays it, then clears it. */
    var pendingMoment by mutableStateOf<MomentPayload?>(null)

    private const val SCHEME = "echoes"
    private const val HOST = "moment"

    fun buildUri(p: MomentPayload): String = Uri.Builder()
        .scheme(SCHEME)
        .authority(HOST)
        .appendQueryParameter("g", p.guid)
        .appendQueryParameter("t", p.title)
        .appendQueryParameter("p", p.podcastTitle)
        .appendQueryParameter("art", p.artworkUrl)
        .appendQueryParameter("au", p.audioUrl)
        .appendQueryParameter("ms", p.startMs.toString())
        .appendQueryParameter("q", p.quote.take(280))
        .build()
        .toString()

    fun parseUri(raw: String): MomentPayload? {
        return try {
            val uri = Uri.parse(raw)
            if (uri.scheme != SCHEME || uri.host != HOST) return null
            val guid = uri.getQueryParameter("g").orEmpty()
            val audio = uri.getQueryParameter("au").orEmpty()
            if (guid.isBlank() || audio.isBlank()) return null
            MomentPayload(
                guid = guid,
                title = uri.getQueryParameter("t").orEmpty(),
                podcastTitle = uri.getQueryParameter("p").orEmpty(),
                artworkUrl = uri.getQueryParameter("art").orEmpty(),
                audioUrl = audio,
                startMs = uri.getQueryParameter("ms")?.toLongOrNull() ?: 0L,
                quote = uri.getQueryParameter("q").orEmpty(),
            )
        } catch (_: Exception) {
            null
        }
    }

    fun onReceived(raw: String): Boolean {
        val payload = parseUri(raw) ?: return false
        pendingMoment = payload
        return true
    }

    fun toEpisode(p: MomentPayload): PodcastEpisode = PodcastEpisode(
        guid = p.guid,
        title = p.title,
        description = "",
        audioUrl = p.audioUrl,
        artworkUrl = p.artworkUrl,
        publishedDate = "",
        durationSeconds = 0,
        podcastTitle = p.podcastTitle,
        podcastId = "",
    )
}
