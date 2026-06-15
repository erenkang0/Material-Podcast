package com.material.podcast.data.rss

import com.material.podcast.data.model.TranscriptCue

/**
 * Tolerant parser for WebVTT and SRT transcripts — the two text formats commonly referenced by
 * the podcast `<podcast:transcript>` tag. Produces a flat list of timed cues.
 */
object TranscriptParser {

    private val timeLine = Regex("""(\d{1,2}:)?\d{1,2}:\d{2}[.,]\d{1,3}\s*-->""")
    private val startTime = Regex("""((?:\d{1,2}:)?\d{1,2}:\d{2}[.,]\d{1,3})""")
    private val tag = Regex("""<[^>]*>""")

    fun parse(content: String): List<TranscriptCue> {
        if (content.isBlank()) return emptyList()
        val normalized = content.replace("\r\n", "\n").replace("\r", "\n")
        val blocks = normalized.split(Regex("\n[ \t]*\n"))
        val cues = ArrayList<TranscriptCue>()

        for (block in blocks) {
            val lines = block.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.isEmpty()) continue
            val timeIndex = lines.indexOfFirst { timeLine.containsMatchIn(it) }
            if (timeIndex < 0) continue
            val startMs = startTime.find(lines[timeIndex])?.value?.let { parseTimeMs(it) } ?: continue
            val text = lines.drop(timeIndex + 1).joinToString(" ").let { tag.replace(it, "") }.trim()
            if (text.isNotEmpty()) cues.add(TranscriptCue(startMs, text))
        }
        return cues
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
