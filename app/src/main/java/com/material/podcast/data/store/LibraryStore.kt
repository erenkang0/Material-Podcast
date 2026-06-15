package com.material.podcast.data.store

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.material.podcast.data.model.FavoriteMoment
import com.material.podcast.data.model.Podcast
import com.material.podcast.data.model.PodcastEpisode

/**
 * Single source of truth for everything the user keeps on-device: followed podcasts,
 * liked episodes, timestamped favorite moments and downloaded episodes.
 *
 * Backed by SharedPreferences + Gson, so all of it survives app restarts and is never
 * silently dropped. Collections are [SnapshotStateList]s, so Compose recomposes on change.
 */
object LibraryStore {

    private const val PREFS = "echoes_library"
    private const val KEY_FOLLOWS = "follows"
    private const val KEY_LIKES = "likes"
    private const val KEY_MOMENTS = "moments"
    private const val KEY_DOWNLOADS = "downloads"

    private val gson = Gson()
    private lateinit var prefs: SharedPreferences

    val followedPodcasts: SnapshotStateList<Podcast> = mutableStateListOf()
    val likedEpisodes: SnapshotStateList<PodcastEpisode> = mutableStateListOf()
    val moments: SnapshotStateList<FavoriteMoment> = mutableStateListOf()
    val downloads: SnapshotStateList<PodcastEpisode> = mutableStateListOf()

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        followedPodcasts.addAll(load(KEY_FOLLOWS, object : TypeToken<List<Podcast>>() {}))
        likedEpisodes.addAll(load(KEY_LIKES, object : TypeToken<List<PodcastEpisode>>() {}))
        moments.addAll(load(KEY_MOMENTS, object : TypeToken<List<FavoriteMoment>>() {}))
        downloads.addAll(load(KEY_DOWNLOADS, object : TypeToken<List<PodcastEpisode>>() {}))
    }

    private fun <T> load(key: String, type: TypeToken<List<T>>): List<T> {
        val json = prefs.getString(key, null) ?: return emptyList()
        return try {
            gson.fromJson(json, type.type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun persist(key: String, value: List<*>) {
        if (!::prefs.isInitialized) return
        prefs.edit().putString(key, gson.toJson(value)).apply()
    }

    // ---- Followed podcasts -------------------------------------------------

    fun isFollowed(podcastId: String) = followedPodcasts.any { it.id == podcastId }

    fun toggleFollow(podcast: Podcast) {
        val idx = followedPodcasts.indexOfFirst { it.id == podcast.id }
        if (idx >= 0) followedPodcasts.removeAt(idx) else followedPodcasts.add(0, podcast)
        persist(KEY_FOLLOWS, followedPodcasts)
    }

    // ---- Liked episodes ----------------------------------------------------

    fun isLiked(guid: String) = likedEpisodes.any { it.guid == guid }

    fun toggleLike(episode: PodcastEpisode) {
        val idx = likedEpisodes.indexOfFirst { it.guid == episode.guid }
        if (idx >= 0) likedEpisodes.removeAt(idx) else likedEpisodes.add(0, episode)
        persist(KEY_LIKES, likedEpisodes)
    }

    // ---- Favorite moments / notes -----------------------------------------

    fun momentsFor(guid: String) = moments.filter { it.episodeGuid == guid }

    fun hasNotes(guid: String) = moments.any { it.episodeGuid == guid }

    fun addMoment(moment: FavoriteMoment) {
        moments.add(0, moment)
        persist(KEY_MOMENTS, moments)
    }

    fun removeMoment(id: String) {
        moments.removeIf { it.id == id }
        persist(KEY_MOMENTS, moments)
    }

    // ---- Downloads ---------------------------------------------------------

    fun isDownloaded(guid: String) = downloads.any { it.guid == guid }

    fun addDownload(episode: PodcastEpisode) {
        if (isDownloaded(episode.guid)) return
        downloads.add(0, episode)
        persist(KEY_DOWNLOADS, downloads)
    }

    fun removeDownload(guid: String) {
        downloads.removeIf { it.guid == guid }
        persist(KEY_DOWNLOADS, downloads)
    }
}
