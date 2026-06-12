package com.material.podcast.data

data class Podcast(
    val id: String,
    val title: String,
    val author: String,
    val category: String,
    val duration: String,
    val colorHex: Long,
    val episodeCount: Int,
    val isSubscribed: Boolean = false
)

data class Episode(
    val id: String,
    val podcastTitle: String,
    val title: String,
    val duration: String,
    val progress: Float,
    val colorHex: Long,
    val isDownloaded: Boolean = false
)

object MockData {
    val featuredPodcasts = listOf(
        Podcast("1", "Cosmos Unfolded", "Dr. Sarah Chen", "Science", "45 min", 0xFF7C4DFF, 128),
        Podcast("2", "Dark Frequencies", "Marcus Webb", "True Crime", "62 min", 0xFFE040FB, 89),
        Podcast("3", "The Human Code", "AI Labs Collective", "Technology", "38 min", 0xFFFF6B6B, 203),
        Podcast("4", "Midnight Philosophy", "Elena Vasquez", "Philosophy", "55 min", 0xFF00BCD4, 67),
        Podcast("5", "Neon Jungle", "Various Artists", "Music", "28 min", 0xFFFFD740, 312),
    )

    val recentEpisodes = listOf(
        Episode("e1", "Cosmos Unfolded", "The Fabric of Time", "45:22", 0.65f, 0xFF7C4DFF),
        Episode("e2", "Dark Frequencies", "Episode 89: The Vanishing", "1:02:14", 0.3f, 0xFFE040FB),
        Episode("e3", "The Human Code", "GPT-7 and Beyond", "38:05", 0.0f, 0xFFFF6B6B),
        Episode("e4", "Midnight Philosophy", "Consciousness as Code", "55:30", 0.88f, 0xFF00BCD4),
    )

    val categories = listOf(
        "Science", "True Crime", "Technology", "Philosophy",
        "Music", "Business", "Health", "Comedy", "History", "Sports"
    )

    val trendingPodcasts = listOf(
        Podcast("t1", "Neural Pathways", "Dr. Kim Park", "Science", "42 min", 0xFF7C4DFF, 45),
        Podcast("t2", "Sonic Tapestry", "The Collective", "Music", "35 min", 0xFFFFD740, 78),
        Podcast("t3", "Future Shock", "Tech Tomorrow", "Technology", "50 min", 0xFF00BCD4, 156),
        Podcast("t4", "True Darkness", "Crime Wave Media", "True Crime", "68 min", 0xFFE040FB, 92),
        Podcast("t5", "Mind Garden", "Wellness Co.", "Health", "25 min", 0xFF69F0AE, 234),
    )

    val libraryPodcasts = listOf(
        Podcast("l1", "Cosmos Unfolded", "Dr. Sarah Chen", "Science", "45 min", 0xFF7C4DFF, 128, true),
        Podcast("l2", "Dark Frequencies", "Marcus Webb", "True Crime", "62 min", 0xFFE040FB, 89, true),
        Podcast("l3", "The Human Code", "AI Labs Collective", "Technology", "38 min", 0xFFFF6B6B, 203, true),
        Podcast("l4", "Midnight Philosophy", "Elena Vasquez", "Philosophy", "55 min", 0xFF00BCD4, 67, true),
    )
}
