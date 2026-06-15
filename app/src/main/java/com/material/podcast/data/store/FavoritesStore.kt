package com.material.podcast.data.store

import com.material.podcast.data.model.Podcast

/**
 * Backwards-compatible facade over [LibraryStore] for followed podcasts.
 * All state is now persisted on-device by [LibraryStore].
 */
object FavoritesStore {
    val podcasts get() = LibraryStore.followedPodcasts

    fun isFavorite(podcastId: String) = LibraryStore.isFollowed(podcastId)

    fun toggle(podcast: Podcast) = LibraryStore.toggleFollow(podcast)
}
