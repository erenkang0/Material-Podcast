package com.material.podcast.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class Podcast(
    val id: String,
    val title: String,
    val author: String,
    val artworkUrl: String,
    val feedUrl: String,
    val genre: String,
    val episodeCount: Int,
    val description: String = "",
)

@Immutable
data class PodcastEpisode(
    val guid: String,
    val title: String,
    val description: String,
    val audioUrl: String,
    val artworkUrl: String,
    val publishedDate: String,
    val durationSeconds: Int,
    val podcastTitle: String,
    val podcastId: String,
    val podcastAuthor: String = "",
) {
    val durationLabel: String get() {
        val h = durationSeconds / 3600
        val m = (durationSeconds % 3600) / 60
        return when {
            h > 0 && m > 0 -> "${h}h ${m}m"
            h > 0 -> "${h}h"
            m > 0 -> "${m}m"
            else -> "<1m"
        }
    }
}

/**
 * A timestamped favorite moment within an episode, with an optional user note.
 * Carries enough of the episode to be replayed (and seeked to [positionMs]) on its own.
 */
@Immutable
data class FavoriteMoment(
    val id: String,
    val episodeGuid: String,
    val episodeTitle: String,
    val podcastTitle: String,
    val podcastId: String,
    val artworkUrl: String,
    val audioUrl: String,
    val positionMs: Long,
    val note: String,
    val createdAt: Long,
) {
    /** Reconstruct a minimal playable episode from the saved fields. */
    fun toEpisode() = PodcastEpisode(
        guid = episodeGuid,
        title = episodeTitle,
        description = "",
        audioUrl = audioUrl,
        artworkUrl = artworkUrl,
        publishedDate = "",
        durationSeconds = 0,
        podcastTitle = podcastTitle,
        podcastId = podcastId,
    )
}

/**
 * A user-editable Explore section: a display [name] plus the [query] that is searched
 * in the background to fill the row.
 */
@Immutable
data class ExploreCategory(
    val id: String,
    val name: String,
    val query: String,
)

/**
 * Where the user left off in an episode. Carries the whole episode so a "continue
 * listening" entry can be rendered and resumed from anywhere, plus when it was last touched.
 */
@Immutable
data class ResumePoint(
    val episode: PodcastEpisode,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long,
) {
    val fraction: Float get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}
