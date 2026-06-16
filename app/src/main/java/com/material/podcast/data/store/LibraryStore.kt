package com.material.podcast.data.store

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.material.podcast.data.model.ExploreCategory
import com.material.podcast.data.model.FavoriteMoment
import com.material.podcast.data.model.ListenStats
import com.material.podcast.data.model.Playlist
import com.material.podcast.data.model.Podcast
import com.material.podcast.data.model.PodcastEpisode
import com.material.podcast.data.model.ResumePoint
import java.time.LocalDate

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
    private const val KEY_STATS = "listen_stats"
    private const val KEY_PLAYLISTS = "playlists"
    private const val KEY_HISTORY = "play_history"
    private const val KEY_HEAT = "replay_heat"

    /** Resolution of the per-episode replay heatmap (buckets across the whole episode). */
    const val HEAT_BUCKETS = 48
    private const val HEAT_MAX_COUNT = 30 // cap so a single obsessively-replayed spot can't dwarf all

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
    val playlists: SnapshotStateList<Playlist> = mutableStateListOf()

    // Personal replay heatmap: guid -> per-bucket replay counts across the episode.
    private val heatMap = mutableMapOf<String, IntArray>()

    // Listening stats (in-memory accumulators, flushed to disk periodically).
    private var statTotalMs = 0L
    private val statPerPodcast = mutableMapOf<String, Long>()
    private val statPerDay = mutableMapOf<String, Long>()
    private val statTitles = mutableMapOf<String, String>()

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        followedPodcasts.addAll(load(KEY_FOLLOWS, object : TypeToken<List<Podcast>>() {}))
        likedEpisodes.addAll(load(KEY_LIKES, object : TypeToken<List<PodcastEpisode>>() {}))
        moments.addAll(load(KEY_MOMENTS, object : TypeToken<List<FavoriteMoment>>() {}))
        downloads.addAll(load(KEY_DOWNLOADS, object : TypeToken<List<PodcastEpisode>>() {}))
        resumePoints.addAll(load(KEY_RESUME, object : TypeToken<List<ResumePoint>>() {}))
        recentPodcasts.addAll(load(KEY_RECENT, object : TypeToken<List<Podcast>>() {}))
        playlists.addAll(load(KEY_PLAYLISTS, object : TypeToken<List<Playlist>>() {}))

        val savedCategories = load(KEY_CATEGORIES, object : TypeToken<List<ExploreCategory>>() {})
        categories.addAll(savedCategories.ifEmpty { defaultCategories })

        prefs.getString(KEY_HEAT, null)?.let { json ->
            runCatching {
                val type = object : TypeToken<Map<String, List<Int>>>() {}.type
                gson.fromJson<Map<String, List<Int>>>(json, type)
            }.getOrNull()?.forEach { (guid, counts) ->
                heatMap[guid] = IntArray(HEAT_BUCKETS) { counts.getOrElse(it) { 0 } }
            }
        }

        prefs.getString(KEY_STATS, null)?.let { json ->
            runCatching { gson.fromJson(json, ListenStats::class.java) }.getOrNull()?.let { s ->
                statTotalMs = s.totalMs
                statPerPodcast.putAll(s.perPodcast)
                statPerDay.putAll(s.perDay)
                statTitles.putAll(s.titles)
            }
        }
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

    // ---- Personal replay heatmap -------------------------------------------

    /**
     * Record that the user jumped *backward* to re-listen to the [fromFraction]→[toFraction]
     * stretch of episode [guid] (both in 0..1). Every bucket the replayed region touches gets a
     * little hotter, so the spots you keep rewinding to rise to the top over time.
     */
    fun recordReplay(guid: String, fromFraction: Float, toFraction: Float) {
        if (guid.isBlank()) return
        val lo = minOf(fromFraction, toFraction).coerceIn(0f, 1f)
        val hi = maxOf(fromFraction, toFraction).coerceIn(0f, 1f)
        val counts = heatMap.getOrPut(guid) { IntArray(HEAT_BUCKETS) }
        val loBucket = (lo * (HEAT_BUCKETS - 1)).toInt().coerceIn(0, HEAT_BUCKETS - 1)
        val hiBucket = (hi * (HEAT_BUCKETS - 1)).toInt().coerceIn(0, HEAT_BUCKETS - 1)
        for (b in loBucket..hiBucket) {
            counts[b] = (counts[b] + 1).coerceAtMost(HEAT_MAX_COUNT)
        }
        persistHeat()
    }

    /** Normalised (0..1) per-bucket heat for [guid]; all-zero list when nothing's been replayed. */
    fun heatFor(guid: String): List<Float> {
        val counts = heatMap[guid] ?: return List(HEAT_BUCKETS) { 0f }
        val max = counts.maxOrNull()?.takeIf { it > 0 } ?: return List(HEAT_BUCKETS) { 0f }
        return counts.map { it.toFloat() / max }
    }

    private fun persistHeat() {
        if (!::prefs.isInitialized) return
        val serializable = heatMap.mapValues { it.value.toList() }
        prefs.edit().putString(KEY_HEAT, gson.toJson(serializable)).apply()
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

    // ---- Recently played history (persisted until the user clears it) -------

    fun getPlayHistory(): List<PodcastEpisode> =
        load(KEY_HISTORY, object : TypeToken<List<PodcastEpisode>>() {})

    fun savePlayHistory(history: List<PodcastEpisode>) = persist(KEY_HISTORY, history)

    fun clearPlayHistory() = persist(KEY_HISTORY, emptyList<PodcastEpisode>())

    // ---- New-episode tracking (for push notifications) ---------------------

    /** The latest episode GUID we've already notified the user about for [podcastId]. */
    fun getLastKnownEpisodeGuid(podcastId: String): String? {
        if (!::prefs.isInitialized) return null
        return prefs.getString("last_episode_$podcastId", null)
    }

    fun setLastKnownEpisodeGuid(podcastId: String, guid: String) {
        if (!::prefs.isInitialized) return
        prefs.edit().putString("last_episode_$podcastId", guid).apply()
    }

    /** Whether to post new-episode notifications for [podcastId] (default on). */
    fun isNotifyEnabled(podcastId: String): Boolean {
        if (!::prefs.isInitialized) return true
        return prefs.getBoolean("notify_$podcastId", true)
    }

    fun setNotifyEnabled(podcastId: String, enabled: Boolean) {
        if (!::prefs.isInitialized) return
        prefs.edit().putBoolean("notify_$podcastId", enabled).apply()
    }

    // ---- Custom playlists --------------------------------------------------

    fun createPlaylist(name: String): Playlist {
        val playlist = Playlist(
            id = java.util.UUID.randomUUID().toString(),
            name = name.trim().ifBlank { "Çalma listesi" },
            episodes = emptyList(),
            createdAt = System.currentTimeMillis(),
        )
        playlists.add(0, playlist)
        persist(KEY_PLAYLISTS, playlists)
        return playlist
    }

    fun renamePlaylist(id: String, name: String) {
        val idx = playlists.indexOfFirst { it.id == id }
        if (idx < 0) return
        playlists[idx] = playlists[idx].copy(name = name.trim().ifBlank { playlists[idx].name })
        persist(KEY_PLAYLISTS, playlists)
    }

    fun deletePlaylist(id: String) {
        if (playlists.removeIf { it.id == id }) persist(KEY_PLAYLISTS, playlists)
    }

    fun playlistById(id: String): Playlist? = playlists.firstOrNull { it.id == id }

    fun addToPlaylist(id: String, episode: PodcastEpisode) {
        val idx = playlists.indexOfFirst { it.id == id }
        if (idx < 0) return
        val current = playlists[idx]
        if (current.episodes.any { it.guid == episode.guid }) return // no duplicates
        playlists[idx] = current.copy(episodes = current.episodes + episode)
        persist(KEY_PLAYLISTS, playlists)
    }

    fun removeFromPlaylist(id: String, guid: String) {
        val idx = playlists.indexOfFirst { it.id == id }
        if (idx < 0) return
        val current = playlists[idx]
        playlists[idx] = current.copy(episodes = current.episodes.filterNot { it.guid == guid })
        persist(KEY_PLAYLISTS, playlists)
    }

    /** Move the episode at [from] to [to] within playlist [id] (for manual reordering). */
    fun movePlaylistEpisode(id: String, from: Int, to: Int) {
        val idx = playlists.indexOfFirst { it.id == id }
        if (idx < 0) return
        val list = playlists[idx].episodes.toMutableList()
        if (from !in list.indices || to !in list.indices) return
        list.add(to, list.removeAt(from))
        playlists[idx] = playlists[idx].copy(episodes = list)
        persist(KEY_PLAYLISTS, playlists)
    }

    // ---- Auto-download of new episodes -------------------------------------

    fun isAutoDownloadEnabled(podcastId: String): Boolean {
        if (!::prefs.isInitialized) return false
        return prefs.getBoolean("autodl_$podcastId", false)
    }

    fun setAutoDownloadEnabled(podcastId: String, enabled: Boolean) {
        if (!::prefs.isInitialized) return
        prefs.edit().putBoolean("autodl_$podcastId", enabled).apply()
    }

    // ---- Listening statistics ----------------------------------------------

    /** Accumulate [deltaMs] of listening time for [episode] (in memory; call [flushStats] to persist). */
    fun recordListen(deltaMs: Long, episode: PodcastEpisode) {
        if (deltaMs <= 0L || deltaMs > 5_000L) return // guard against jumps / seeks
        statTotalMs += deltaMs
        val id = episode.podcastId.ifBlank { episode.podcastTitle }
        statPerPodcast[id] = (statPerPodcast[id] ?: 0L) + deltaMs
        if (episode.podcastTitle.isNotBlank()) statTitles[id] = episode.podcastTitle
        val day = LocalDate.now().toString()
        statPerDay[day] = (statPerDay[day] ?: 0L) + deltaMs
    }

    fun flushStats() {
        if (!::prefs.isInitialized) return
        val snapshot = ListenStats(statTotalMs, statPerPodcast.toMap(), statPerDay.toMap(), statTitles.toMap())
        prefs.edit().putString(KEY_STATS, gson.toJson(snapshot)).apply()
    }

    fun getStats(): ListenStats =
        ListenStats(statTotalMs, statPerPodcast.toMap(), statPerDay.toMap(), statTitles.toMap())

    fun resetStats() {
        statTotalMs = 0L
        statPerPodcast.clear()
        statPerDay.clear()
        statTitles.clear()
        flushStats()
    }

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
            KEY_PLAYLISTS to playlists.toList(),
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
            playlists.replaceAllPersist(KEY_PLAYLISTS,
                section(KEY_PLAYLISTS, object : TypeToken<List<Playlist>>() {}))
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
