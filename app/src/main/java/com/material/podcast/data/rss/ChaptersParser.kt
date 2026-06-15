package com.material.podcast.data.rss

import com.material.podcast.data.model.Chapter
import org.json.JSONObject

/**
 * Parses the Podcast Namespace JSON chapters format:
 * https://github.com/Podcastindex-org/podcast-namespace/blob/main/chapters/jsonChapters.md
 *
 * ```json
 * { "version": "1.2.0", "chapters": [
 *     { "startTime": 0,    "title": "Intro",  "img": "https://..." },
 *     { "startTime": 73.5, "title": "Topic 1" }
 * ] }
 * ```
 * `startTime` is in seconds (may be fractional).
 */
object ChaptersParser {

    fun parse(json: String): List<Chapter> {
        if (json.isBlank()) return emptyList()
        return try {
            val root = JSONObject(json)
            val arr = root.optJSONArray("chapters") ?: return emptyList()
            val out = ArrayList<Chapter>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val startSec = obj.optDouble("startTime", -1.0)
                if (startSec < 0) continue
                val title = obj.optString("title").trim()
                if (title.isEmpty()) continue
                // A chapter flagged toc:false is a hidden marker — skip it from the list.
                if (obj.has("toc") && !obj.optBoolean("toc", true)) continue
                out.add(
                    Chapter(
                        startMs = (startSec * 1000).toLong(),
                        title = title,
                        imageUrl = obj.optString("img").trim(),
                    ),
                )
            }
            out.sortedBy { it.startMs }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
