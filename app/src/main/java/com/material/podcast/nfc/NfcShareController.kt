package com.material.podcast.nfc

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

/**
 * Central coordinator for the "tap to share a podcast over NFC" experience.
 *
 * Acts as the single source of truth shared by three otherwise-independent pieces:
 *  - the UI that *starts* a share (the overflow menu on a show, the centre share button),
 *  - the [NfcShareApduService] HCE service that *broadcasts* the payload to another phone,
 *  - [com.material.podcast.MainActivity], which renders the share sheet, runs NFC reader
 *    mode while a share is active, and routes a *received* payload back into navigation.
 *
 * The payload travels as an NDEF URI record carrying an `echoes://share?…` deep link, so a
 * phone without the app still resolves it through the system (and our intent-filter), while a
 * phone with the app jumps straight to the shared podcast.
 */
object NfcShareController {

    /** Everything needed to reconstruct (and display) the shared podcast on the far side. */
    data class SharePayload(
        val id: String,
        val title: String,
        val author: String,
        val artworkUrl: String,
        val feedUrl: String,
    )

    /** Non-null while the user is actively offering a podcast over NFC (drives the share sheet). */
    var activeShare by mutableStateOf<SharePayload?>(null)
        private set

    /** True while this device is listening (reader mode) for an incoming share. */
    var receiving by mutableStateOf(false)
        private set

    /**
     * Set to the podcast id once a share has been received and parsed; MainActivity observes
     * this, navigates to the show, then clears it. Kept separate from [activeShare] so a
     * receive never collides with an in-flight send.
     */
    var pendingOpenId by mutableStateOf<String?>(null)

    private const val SCHEME = "echoes"
    private const val HOST = "share"

    fun startShare(payload: SharePayload) {
        activeShare = payload
    }

    fun stopShare() {
        activeShare = null
    }

    fun setReceiving(value: Boolean) {
        receiving = value
    }

    /** Build the `echoes://share?…` deep link that encodes [payload]. */
    fun buildUri(payload: SharePayload): String = Uri.Builder()
        .scheme(SCHEME)
        .authority(HOST)
        .appendQueryParameter("id", payload.id)
        .appendQueryParameter("t", payload.title)
        .appendQueryParameter("a", payload.author)
        .appendQueryParameter("art", payload.artworkUrl)
        .appendQueryParameter("feed", payload.feedUrl)
        .build()
        .toString()

    /** Parse an `echoes://share?…` link back into a [SharePayload], or null if it isn't ours. */
    fun parseUri(raw: String): SharePayload? {
        return try {
            val uri = Uri.parse(raw)
            if (uri.scheme != SCHEME || uri.host != HOST) return null
            val id = uri.getQueryParameter("id").orEmpty()
            if (id.isBlank()) return null
            SharePayload(
                id = id,
                title = uri.getQueryParameter("t").orEmpty(),
                author = uri.getQueryParameter("a").orEmpty(),
                artworkUrl = uri.getQueryParameter("art").orEmpty(),
                feedUrl = uri.getQueryParameter("feed").orEmpty(),
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Called by reader mode (or the deep-link intent) when a share link arrives. Resolves it and
     * queues the podcast for opening. Returns true if the link was a valid Echoes share.
     */
    fun onReceived(raw: String): Boolean {
        val payload = parseUri(raw) ?: return false
        pendingOpenId = payload.id
        receiving = false
        return true
    }

    /**
     * The raw NDEF *message* bytes (a single URI record) for the active share, or null if no
     * share is in flight. [NfcShareApduService] wraps these in the NFC-Forum Type-4 file
     * structure when a reader selects our NDEF application.
     */
    fun currentNdefMessage(): ByteArray? {
        val payload = activeShare ?: return null
        return buildUriNdefMessage(buildUri(payload))
    }

    /**
     * Construct a minimal NDEF message containing one well-known URI ("U") record. We always use
     * the 0x00 ("no abbreviation") identifier code and put the full URI in the payload, since our
     * custom scheme isn't in the NFC URI prefix table.
     */
    private fun buildUriNdefMessage(uri: String): ByteArray {
        val uriBytes = uri.toByteArray(Charset.forName("US-ASCII"))
        val payload = ByteArray(uriBytes.size + 1)
        payload[0] = 0x00 // URI identifier code: no prefix abbreviation
        System.arraycopy(uriBytes, 0, payload, 1, uriBytes.size)

        val out = ByteArrayOutputStream()
        // NDEF record header: MB=1, ME=1, SR=1, TNF=0x01 (well-known) -> 0xD1
        out.write(0xD1)
        out.write(0x01)            // type length (1 byte: 'U')
        out.write(payload.size)    // payload length (short record, single byte)
        out.write('U'.code)        // type = "U" (URI)
        out.write(payload)
        return out.toByteArray()
    }
}
