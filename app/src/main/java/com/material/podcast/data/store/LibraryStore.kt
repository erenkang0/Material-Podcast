package com.material.podcast.data.store

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.material.podcast.data.model.ExploreCategory
import com.material.podcast.data.model.FavoriteMoment
import com.material.podcast.data.model.Podcast
import com.material.podcast.data.model.PodcastEpisode
import com.material.podcast.data.model.ResumePoint

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
    private const val KEY_CATEGORIES = "categories"
    private const val KEY_RESUME = "resume"
    private const val KEY_RECENT = "recent"
    private const val KEY_CURRENT_QUEUE = "current_queue"

    const val MAX_CATEGORIES = 10
    private const val MAX_RESUME = 40
    private const val MAX_RECENT = 20

    /** Seeded the first time the app runs; fully editable thereafter. */
    val defaultCategories: List<ExploreCategory> = listOf(
        ExploreCategory("tech", "Teknoloji", "technology podcast"),
        ExploreCategory("science", "Bilim & Eğitim", "science education podcast"),
        ExploreCategory("culture", "Toplum & Kültür", "society culture podcast"),
        ExploreCategory("comedy", "Komedi", "comedy podcast"),
        ExploreCategory("news", "Haberler", "news podcast"),
    )

    private val gson = Gson()
    private lateinit var prefs: SharedPreferences

    val followedPodcasts: SnapshotStateList<Podcast> = mutableStateListOf()
    val likedEpisodes: SnapshotStateList<PodcastEpisode> = mutableStateListOf()
    val moments: SnapshotStateList<FavoriteMoment> = mutableStateListOf()
    val downloads: SnapshotStateList<PodcastEpisode> = mutableStateListOf()
    val categories: SnapshotStateList<ExploreCategory> = mutableStateListOf()
    val resumePoints: SnapshotStateList<ResumePoint> = mutableStateListOf()
    val recentPodcasts: SnapshotStateList<Podcast> = mutableStateListOf()

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        followedPodcasts.addAll(load(KEY_FOLLOWS, object : TypeToken<List<Podcast>>() {}))
        likedEpisodes.addAll(load(KEY_LIKES, object : TypeToken<List<PodcastEpisode>>() {}))
        moments.addAll(load(KEY_MOMENTS, object : TypeToken<List<FavoriteMoment>>() {}))
        downloads.addAll(load(KEY_DOWNLOADS, object : TypeToken<List<PodcastEpisode>>() {}))
        resumePoints.addAll(load(KEY_RESUME, object : TypeToken<List<ResumePoint>>() {}))
        recentPodcasts.addAll(load(KEY_RECENT, object : TypeToken<List<Podcast>>() {}))

        val savedCategories = load(KEY_CATEGORIES, object : TypeToken<List<ExploreCategory>>() {})
        categories.addAll(savedCategories.ifEmpty { defaultCategories })
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

    // ---- Explore categories ------------------------------------------------

    fun setCategories(newCategories: List<ExploreCategory>) {
        categories.clear()
        categories.addAll(newCategories.take(MAX_CATEGORIES))
        persist(KEY_CATEGORIES, categories)
    }

    fun resetCategories() = setCategories(defaultCategories)

    // ---- Resume / continue listening ---------------------------------------

    fun saveProgress(episode: PodcastEpisode, positionMs: Long, durationMs: Long) {
        if (positionMs < 5_000L) return // ignore the very start
        // Treat near-finished as completed: drop the resume point.
        if (durationMs > 0 && positionMs > durationMs - 15_000L) {
            if (resumePoints.removeIf { it.episode.guid == episode.guid }) persist(KEY_RESUME, resumePoints)
            return
        }
        resumePoints.removeIf { it.episode.guid == episode.guid }
        resumePoints.add(0, ResumePoint(episode, positionMs, durationMs, System.currentTimeMillis()))
        while (resumePoints.size > MAX_RESUME) resumePoints.removeAt(resumePoints.lastIndex)
        persist(KEY_RESUME, resumePoints)
    }

    fun lastResume(): ResumePoint? = resumePoints.firstOrNull()

    fun resumeForPodcast(podcastId: String): ResumePoint? =
        resumePoints.firstOrNull { it.episode.podcastId == podcastId }

    fun positionFor(guid: String): Long =
        resumePoints.firstOrNull { it.episode.guid == guid }?.positionMs ?: 0L

    // ---- Recently viewed podcasts ------------------------------------------

    fun recordView(podcast: Podcast) {
        val idx = recentPodcasts.indexOfFirst { it.id == podcast.id }
        if (idx >= 0) recentPodcasts.removeAt(idx)
        recentPodcasts.add(0, podcast)
        while (recentPodcasts.size > MAX_RECENT) recentPodcasts.removeAt(recentPodcasts.lastIndex)
        persist(KEY_RECENT, recentPodcasts)
    }

    // ---- Active playback queue (for restoring after process death) ---------

    fun saveCurrentQueue(queue: List<PodcastEpisode>) = persist(KEY_CURRENT_QUEUE, queue)

    fun getCurrentQueue(): List<PodcastEpisode> =
        load(KEY_CURRENT_QUEUE, object : TypeToken<List<PodcastEpisode>>() {})

    // ---- Full JSON backup / restore ----------------------------------------

    /** Serialize the entire on-device library to a single JSON document. */
    fun exportJson(): String {
        val snapshot = mapOf(
            "version" to 1,
            KEY_FOLLOWS to followedPodcasts.toList(),
            KEY_LIKES to likedEpisodes.toList(),
            KEY_MOMENTS to moments.toList(),
            KEY_DOWNLOADS to downloads.toList(),
            KEY_CATEGORIES to categories.toList(),
            KEY_RESUME to resumePoints.toList(),
            KEY_RECENT to recentPodcasts.toList(),
        )
        return gson.toJson(snapshot)
    }

    /** Replace the library from a previously exported JSON document. Returns true on success. */
    fun importJson(json: String): Boolean {
        return try {
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val map: Map<String, Any> = gson.fromJson(json, type) ?: return false

            fun <T> section(key: String, token: TypeToken<List<T>>): List<T> {
                val element = map[key] ?: return emptyList()
                return gson.fromJson(gson.toJson(element), token.type) ?: emptyList()
            }

            followedPodcasts.replaceAllPersist(KEY_FOLLOWS,
                section(KEY_FOLLOWS, object : TypeToken<List<Podcast>>() {}))
            likedEpisodes.replaceAllPersist(KEY_LIKES,
                section(KEY_LIKES, object : TypeToken<List<PodcastEpisode>>() {}))
            moments.replaceAllPersist(KEY_MOMENTS,
                section(KEY_MOMENTS, object : TypeToken<List<FavoriteMoment>>() {}))
            downloads.replaceAllPersist(KEY_DOWNLOADS,
                section(KEY_DOWNLOADS, object : TypeToken<List<PodcastEpisode>>() {}))
            resumePoints.replaceAllPersist(KEY_RESUME,
                section(KEY_RESUME, object : TypeToken<List<ResumePoint>>() {}))
            recentPodcasts.replaceAllPersist(KEY_RECENT,
                section(KEY_RECENT, object : TypeToken<List<Podcast>>() {}))
            val cats = section(KEY_CATEGORIES, object : TypeToken<List<ExploreCategory>>() {})
            if (cats.isNotEmpty()) categories.replaceAllPersist(KEY_CATEGORIES, cats)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun <T> SnapshotStateList<T>.replaceAllPersist(key: String, items: List<T>) {
        clear()
        addAll(items)
        persist(key, this)
    }
}
