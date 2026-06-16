package com.material.podcast.data.rss

import com.material.podcast.data.model.TranscriptCue
import org.json.JSONArray
import org.json.JSONObject

/**
 * Tolerant transcript parser. Handles the common formats referenced by the podcast
 * `<podcast:transcript>` tag:
 *
 *  - **WebVTT** (`WEBVTT` header, `00:00:01.000 --> 00:00:04.000` cues)
 *  - **SRT** (numeric index, `00:00:01,000 --> 00:00:04,000` cues)
 *  - **JSON** — both the Podcast Namespace segment format
 *    (`{ "segments": [ { "startTime": 1.0, "body": "..." } ] }`) and generic
 *    `[ { "start": 1.0, "text": "..." } ]` arrays.
 *  - **Plain text** — falls back to one (untimed) cue per non-empty line.
 *
 * Produces a flat list of timed cues sorted by start time. Cues without a usable
 * timestamp are still surfaced (with `startMs = -1`) so the text is never lost.
 */
object TranscriptParser {

    private val timeArrow = Regex("""(\d{1,2}:)?\d{1,2}:\d{2}[.,]\d{1,3}\s*-->""")
    private val timeStamp = Regex("""((?:\d{1,2}:)?\d{1,2}:\d{2}[.,]\d{1,3})""")
    private val tag = Regex("""<[^>]*>""")

    fun parse(content: String): List<TranscriptCue> {
        if (content.isBlank()) return emptyList()
        val trimmed = content.trimStart('﻿', ' ', '\t', '\n', '\r')

        val cues = when {
            trimmed.startsWith("{") || trimmed.startsWith("[") -> parseJson(trimmed)
            timeArrow.containsMatchIn(trimmed) -> parseCueBlocks(trimmed)
            looksLikeHtml(trimmed) -> parseHtml(trimmed)
            else -> parsePlain(trimmed)
        }
        // Sort timed cues by position but keep untimed ones in their original order at the end.
        return if (cues.any { it.startMs >= 0 }) {
            cues.sortedBy { if (it.startMs < 0) Long.MAX_VALUE else it.startMs }
        } else {
            cues
        }
    }

    /** WebVTT / SRT: split into blocks separated by blank lines, find the `-->` line. */
    private fun parseCueBlocks(content: String): List<TranscriptCue> {
        val normalized = content.replace("\r\n", "\n").replace("\r", "\n")
        val blocks = normalized.split(Regex("\n[ \t]*\n"))
        val cues = ArrayList<TranscriptCue>()

        for (block in blocks) {
            val lines = block.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.isEmpty()) continue
            if (lines.first().equals("WEBVTT", ignoreCase = true)) continue
            val timeIndex = lines.indexOfFirst { timeArrow.containsMatchIn(it) }
            if (timeIndex < 0) continue
            val startMs = timeStamp.find(lines[timeIndex])?.value?.let { parseTimeMs(it) } ?: continue
            val text = lines.drop(timeIndex + 1)
                .joinToString(" ")
                .let { tag.replace(it, "") }
                .replace(Regex("\\s+"), " ")
                .trim()
            if (text.isNotEmpty()) cues.add(TranscriptCue(startMs, text))
        }
        return cues
    }

    private fun parseJson(content: String): List<TranscriptCue> {
        return try {
            val arr: JSONArray = when {
                content.startsWith("[") -> JSONArray(content)
                else -> {
                    val root = JSONObject(content)
                    root.optJSONArray("segments")
                        ?: root.optJSONArray("cues")
                        ?: root.optJSONArray("results")
                        ?: return parsePlain(content)
                }
            }
            val cues = ArrayList<TranscriptCue>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val text = firstString(obj, "body", "text", "transcript", "content").let {
                    tag.replace(it, "").replace(Regex("\\s+"), " ").trim()
                }
                if (text.isEmpty()) continue
                val startMs = startMsFromJson(obj)
                cues.add(TranscriptCue(startMs, text))
            }
            cues
        } catch (_: Exception) {
            parsePlain(content)
        }
    }

    /** Read a start time expressed in seconds (`startTime`/`start`) or in ms (`startMs`/`offset`). */
    private fun startMsFromJson(obj: JSONObject): Long {
        for (key in arrayOf("startTime", "start", "from", "begin")) {
            if (obj.has(key)) {
                val v = obj.optDouble(key, Double.NaN)
                if (!v.isNaN()) return (v * 1000).toLong()
            }
        }
        for (key in arrayOf("startMs", "offset", "offsetMs")) {
            if (obj.has(key)) {
                val v = obj.optLong(key, -1L)
                if (v >= 0) return v
            }
        }
        return -1L
    }

    private fun firstString(obj: JSONObject, vararg keys: String): String {
        for (key in keys) {
            val v = obj.optString(key, "")
            if (v.isNotBlank()) return v
        }
        return ""
    }

    private fun looksLikeHtml(content: String): Boolean {
        val head = content.take(400)
        return head.startsWith("<") ||
            head.contains("<!doctype", ignoreCase = true) ||
            content.contains("</p>", ignoreCase = true) ||
            content.contains("<br", ignoreCase = true) ||
            content.contains("</div>", ignoreCase = true)
    }

    /**
     * HTML transcript (many `<podcast:transcript type="text/html">` documents): turn block-level
     * boundaries into paragraph breaks, strip the remaining tags and decode entities, then surface
     * one untimed cue per paragraph so the text is readable instead of one giant blob.
     */
    private fun parseHtml(content: String): List<TranscriptCue> {
        val withBreaks = content.replace(
            Regex("(?i)</p>|<br\\s*/?>|</div>|</li>|</h[1-6]>|</tr>"),
            "\n",
        )
        val stripped = tag.replace(withBreaks, "")
        return decodeEntities(stripped)
            .replace("\r\n", "\n").replace("\r", "\n")
            .split("\n")
            .map { it.replace(Regex("[ \t]+"), " ").trim() }
            .filter { it.isNotEmpty() }
            .map { TranscriptCue(-1L, it) }
    }

    private fun decodeEntities(s: String): String = s
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
        .replace("&mdash;", "—")
        .replace("&ndash;", "–")
        .replace("&hellip;", "…")
        .replace("&rsquo;", "'")
        .replace("&lsquo;", "'")
        .replace("&ldquo;", "\"")
        .replace("&rdquo;", "\"")

    /** Plain text: one cue per non-empty line, untimed. */
    private fun parsePlain(content: String): List<TranscriptCue> {
        return content.replace("\r\n", "\n").replace("\r", "\n")
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { TranscriptCue(-1L, tag.replace(it, "").trim()) }
            .filter { it.text.isNotEmpty() }
    }

    private fun parseTimeMs(t: String): Long {
        return try {
            val cleaned = t.replace(',', '.')
            val dot = cleaned.split(".")
            val hms = dot[0].split(":").map { it.toLong() }
            val secs = when (hms.size) {
                3 -> hms[0] * 3600 + hms[1] * 60 + hms[2]
                2 -> hms[0] * 60 + hms[1]
                else -> hms[0]
            }
            val millis = dot.getOrNull(1)?.padEnd(3, '0')?.take(3)?.toLong() ?: 0L
            secs * 1000 + millis
        } catch (e: Exception) {
            0L
        }
    }
}
