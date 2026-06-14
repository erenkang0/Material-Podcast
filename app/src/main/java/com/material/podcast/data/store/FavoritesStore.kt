package com.material.podcast.data.store

import androidx.compose.runtime.mutableStateListOf
import com.material.podcast.data.model.Podcast

object FavoritesStore {
    val podcasts = mutableStateListOf<Podcast>()

    fun isFavorite(podcastId: String) = podcasts.any { it.id == podcastId }

    fun toggle(podcast: Podcast) {
        val idx = podcasts.indexOfFirst { it.id == podcast.id }
        if (idx >= 0) podcasts.removeAt(idx) else podcasts.add(0, podcast)
    }
}
