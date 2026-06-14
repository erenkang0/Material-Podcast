package com.material.podcast.data.rss

data class RssEpisode(
    val guid: String,
    val title: String,
    val description: String,
    val enclosureUrl: String,
    val pubDate: String,
    val durationSeconds: Int,
    val imageUrl: String?,
)
