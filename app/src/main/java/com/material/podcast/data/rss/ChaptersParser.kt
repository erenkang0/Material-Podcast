package com.material.podcast.data.rss

import com.material.podcast.data.model.Chapter
import org.json.JSONArray
import org.json.JSONObject

/**
 * Parses embedded chapter markers shipped with an episode. The primary format is the
 * Podcast Namespace JSON chapters file referenced by `<podcast:chapters>`:
 * https://github.com/Podcastindex-org/podcast-namespace/blob/main/chapters/jsonChapters.md
 *
 * ```json
 * { "version": "1.2.0", "chapters": [
 *     { "startTime": 0,    "title": "Intro",  "img": "https://..." },
 *     { "startTime": 73.5, "title": "Topic 1" }
 * ] }
 * ```
 *
 * The parser is tolerant of real-world variations:
 *  - `startTime` in seconds (fractional allowed) or already in milliseconds.
 *  - Title under `title` or `name`; artwork under `img`, `image` or `url`.
 *  - Chapters wrapped in `chapters`, `chapter` or `items` arrays, or a bare JSON array.
 *  - `toc:false` markers (hidden, e.g. ID3/PSC-style mid-roll markers) are skipped.
 */
object ChaptersParser {

    fun parse(json: String): List<Chapter> {
        if (json.isBlank()) return emptyList()
        return try {
            val trimmed = json.trimStart('﻿', ' ', '\t', '\n', '\r')
            val arr: JSONArray = if (trimmed.startsWith("[")) {
                JSONArray(trimmed)
            } else {
                val root = JSONObject(trimmed)
                root.optJSONArray("chapters")
                    ?: root.optJSONArray("chapter")
                    ?: root.optJSONArray("items")
                    ?: return emptyList()
            }

            val out = ArrayList<Chapter>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val startMs = startMs(obj)
                if (startMs < 0) continue
                val title = firstString(obj, "title", "name").trim()
                if (title.isEmpty()) continue
                // A chapter flagged toc:false is a hidden marker — keep it out of the list.
                if (obj.has("toc") && !obj.optBoolean("toc", true)) continue
                out.add(
                    Chapter(
                        startMs = startMs,
                        title = title,
                        imageUrl = firstString(obj, "img", "image", "url", "artwork").trim(),
                    ),
                )
            }
            // De-duplicate identical start times and keep chronological order.
            out.sortedBy { it.startMs }.distinctBy { it.startMs }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Resolve a chapter start to milliseconds, accepting seconds or millisecond inputs. */
    private fun startMs(obj: JSONObject): Long {
        for (key in arrayOf("startTime", "start", "startSec")) {
            if (obj.has(key)) {
                val v = obj.optDouble(key, Double.NaN)
                if (!v.isNaN() && v >= 0) return (v * 1000).toLong()
            }
        }
        for (key in arrayOf("startMs", "startTimeMs", "offsetMs")) {
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
}
