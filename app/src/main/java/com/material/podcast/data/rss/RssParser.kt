package com.material.podcast.data.rss

import android.text.Html
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale

class RssParser {

    fun parse(inputStream: InputStream): List<RssEpisode> {
        val episodes = mutableListOf<RssEpisode>()
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(inputStream, null)

        var inItem = false
        var guid = ""
        var title = ""
        var description = ""
        var enclosureUrl = ""
        var pubDate = ""
        var durationStr = ""
        var imageUrl: String? = null
        var transcriptUrl: String? = null
        var chaptersUrl: String? = null
        var channelImageUrl: String? = null
        var inChannelImage = false
        var inItemImage = false

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            val tag = parser.name ?: ""
            when (event) {
                XmlPullParser.START_TAG -> when {
                    tag == "item" -> {
                        inItem = true
                        guid = ""; title = ""; description = ""
                        enclosureUrl = ""; pubDate = ""; durationStr = ""
                        imageUrl = null; transcriptUrl = null; chaptersUrl = null
                    }
                    !inItem && tag == "image" -> inChannelImage = true
                    inItem && tag == "image" -> inItemImage = true

                    inItem && tag == "title" -> title = safeNextText(parser)
                    inItem && (tag == "description" || tag == "itunes:summary") -> {
                        val text = safeNextText(parser)
                        if (description.isEmpty()) description = stripHtml(text)
                    }
                    inItem && tag == "enclosure" -> {
                        val type = parser.getAttributeValue(null, "type") ?: ""
                        if (type.startsWith("audio") || enclosureUrl.isEmpty()) {
                            val url = parser.getAttributeValue(null, "url") ?: ""
                            if (url.isNotEmpty()) enclosureUrl = url
                        }
                    }
                    inItem && tag == "pubDate" -> pubDate = safeNextText(parser)
                    inItem && tag == "itunes:duration" -> durationStr = safeNextText(parser)
                    inItem && tag == "guid" -> guid = safeNextText(parser)
                    inItem && tag == "itunes:image" -> {
                        imageUrl = parser.getAttributeValue(null, "href") ?: imageUrl
                    }
                    inItem && tag == "podcast:chapters" -> {
                        val url = parser.getAttributeValue(null, "url")
                        if (url != null && chaptersUrl == null) chaptersUrl = url
                    }
                    inItem && tag == "podcast:transcript" -> {
                        val url = parser.getAttributeValue(null, "url")
                        val type = parser.getAttributeValue(null, "type") ?: ""
                        // Prefer a text format we can parse (VTT/SRT); take the first otherwise.
                        if (url != null && (transcriptUrl == null ||
                                type.contains("vtt") || type.contains("srt"))
                        ) {
                            transcriptUrl = url
                        }
                    }
                    !inItem && inChannelImage && tag == "url" -> {
                        channelImageUrl = safeNextText(parser)
                    }
                    !inItem && tag == "itunes:image" -> {
                        channelImageUrl = parser.getAttributeValue(null, "href") ?: channelImageUrl
                    }
                }

                XmlPullParser.END_TAG -> when {
                    tag == "image" && inChannelImage && !inItem -> inChannelImage = false
                    tag == "image" && inItemImage && inItem -> inItemImage = false
                    tag == "item" && inItem -> {
                        inItem = false
                        if (enclosureUrl.isNotEmpty() && title.isNotEmpty()) {
                            episodes.add(
                                RssEpisode(
                                    guid = guid.ifEmpty { enclosureUrl },
                                    title = title,
                                    description = description,
                                    enclosureUrl = enclosureUrl,
                                    pubDate = formatDate(pubDate),
                                    durationSeconds = parseDuration(durationStr),
                                    imageUrl = imageUrl ?: channelImageUrl,
                                    transcriptUrl = transcriptUrl,
                                    chaptersUrl = chaptersUrl,
                                )
                            )
                        }
                        if (episodes.size >= 100) return episodes
                    }
                }
            }
            event = parser.next()
        }
        return episodes
    }

    private fun safeNextText(parser: XmlPullParser): String {
        return try { parser.nextText().trim() } catch (e: Exception) { "" }
    }

    private fun parseDuration(s: String): Int {
        if (s.isBlank()) return 0
        return try {
            when {
                s.contains(":") -> {
                    val parts = s.split(":")
                    when (parts.size) {
                        3 -> parts[0].toInt() * 3600 + parts[1].toInt() * 60 + parts[2].toInt()
                        2 -> parts[0].toInt() * 60 + parts[1].toInt()
                        else -> s.toIntOrNull() ?: 0
                    }
                }
                else -> s.toIntOrNull() ?: 0
            }
        } catch (e: Exception) { 0 }
    }

    private fun formatDate(pubDate: String): String {
        if (pubDate.isBlank()) return ""
        val formats = listOf(
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "EEE, dd MMM yyyy HH:mm:ss zzz",
            "dd MMM yyyy HH:mm:ss Z",
        )
        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.ENGLISH)
                val date = sdf.parse(pubDate) ?: continue
                return SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH).format(date)
            } catch (e: Exception) { continue }
        }
        return pubDate.take(16)
    }

    private fun stripHtml(html: String): String =
        Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT).toString().trim()
}
