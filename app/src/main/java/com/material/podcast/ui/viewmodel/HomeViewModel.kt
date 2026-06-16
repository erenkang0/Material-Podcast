package com.material.podcast.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.material.podcast.EchoesApplication
import com.material.podcast.data.model.ExploreCategory
import com.material.podcast.data.model.Podcast
import com.material.podcast.data.store.LibraryStore
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CategorySection(
    val category: ExploreCategory,
    val podcasts: List<Podcast>,
)

sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Success(
        val featured: List<Podcast>,
        val sections: List<CategorySection>,
        val recommended: List<Podcast> = emptyList(),
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

class HomeViewModel : ViewModel() {

    private val repo get() = EchoesApplication.instance.repository

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var lastKey: String = ""

    /** Load featured + one row per editable category. No-op if [categories] are unchanged. */
    fun load(categories: List<ExploreCategory>, force: Boolean = false) {
        val key = categories.joinToString("|") { "${it.id}:${it.query}" }
        if (!force && key == lastKey && _uiState.value is HomeUiState.Success) return
        lastKey = key
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                val featuredDeferred = async { repo.getFeaturedPodcasts() }
                val recommendedDeferred = async { loadRecommendations() }
                val sectionDeferreds = categories.map { category ->
                    category to async {
                        runCatching { repo.searchByGenre(category.query) }.getOrDefault(emptyList())
                    }
                }
                val sections = sectionDeferreds
                    .map { (category, deferred) -> CategorySection(category, deferred.await()) }
                    .filter { it.podcasts.isNotEmpty() }
                _uiState.value = HomeUiState.Success(
                    featured = personalizeFeatured(featuredDeferred.await()),
                    sections = sections,
                    recommended = recommendedDeferred.await(),
                )
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Weighted genre affinity from the user's behaviour. Followed shows count most, then how much
     * they've actually *listened* per podcast (from [LibraryStore] stats), then recent views
     * (decaying with position). Returns genre → score, highest first.
     */
    private fun genreAffinity(): List<Pair<String, Double>> {
        val weights = mutableMapOf<String, Double>()
        fun add(genre: String?, w: Double) {
            val g = genre?.trim().orEmpty()
            if (g.isNotEmpty()) weights[g] = (weights[g] ?: 0.0) + w
        }

        LibraryStore.followedPodcasts.forEach { add(it.genre, 3.0) }
        // Recent views decay so the latest interests dominate.
        LibraryStore.recentPodcasts.forEachIndexed { i, p -> add(p.genre, 1.5 / (1 + i * 0.5)) }
        // Reinforce by real listening time: map per-podcast minutes back onto its genre.
        val stats = runCatching { LibraryStore.getStats() }.getOrNull()
        if (stats != null) {
            val byId = (LibraryStore.followedPodcasts + LibraryStore.recentPodcasts).associateBy { it.id }
            stats.perPodcast.forEach { (podcastId, ms) ->
                val genre = byId[podcastId]?.genre
                add(genre, (ms / 600_000.0)) // ~1 point per 10 listened minutes
            }
        }
        return weights.entries.sortedByDescending { it.value }.map { it.key to it.value }
    }

    /**
     * "Senin İçin" — pull fresh shows from the user's *top few* affinity genres (not just one),
     * score by genre weight, interleave for variety, and exclude anything they already follow,
     * recently viewed, or that's already in the featured rail.
     */
    private suspend fun loadRecommendations(): List<Podcast> {
        val affinity = genreAffinity()
        if (affinity.isEmpty()) return emptyList()

        val topGenres = affinity.take(3)
        val seedIds = (LibraryStore.followedPodcasts + LibraryStore.recentPodcasts).map { it.id }.toSet()
        val featuredIds = runCatching { repo.getFeaturedPodcasts() }.getOrDefault(emptyList()).map { it.id }.toSet()
        val excluded = seedIds + featuredIds

        // Fetch candidates per genre concurrently, keeping them grouped so we can interleave.
        val perGenre = topGenres.map { (genre, weight) ->
            viewModelScope.async {
                weight to runCatching { repo.searchByGenre(genre) }.getOrDefault(emptyList())
                    .filter { it.id !in excluded }
            }
        }.map { it.await() }

        // Round-robin across genres (best genre first) so the rail stays diverse, de-duping by id.
        val seen = HashSet<String>()
        val result = ArrayList<Podcast>()
        val queues = perGenre.sortedByDescending { it.first }.map { it.second.toMutableList() }
        var added = true
        while (added && result.size < 12) {
            added = false
            for (q in queues) {
                val next = q.removeFirstOrNull() ?: continue
                if (seen.add(next.id)) {
                    result.add(next)
                    added = true
                    if (result.size >= 12) break
                }
            }
        }
        return result
    }

    /**
     * Reorder the curated featured rail so shows in the user's favourite genres float toward the
     * front, with a gentle daily rotation so the page still feels fresh day to day.
     */
    private fun personalizeFeatured(featured: List<Podcast>): List<Podcast> {
        if (featured.isEmpty()) return featured
        val affinity = genreAffinity().toMap()
        val daySeed = (System.currentTimeMillis() / 86_400_000L)
        return featured
            .map { podcast ->
                val genreScore = affinity[podcast.genre.trim()] ?: 0.0
                // Deterministic per-day jitter keeps order stable within a day but rotates daily.
                val jitter = ((podcast.id.hashCode().toLong() xor daySeed) % 100) / 100.0
                podcast to (genreScore * 2.0 + jitter)
            }
            .sortedByDescending { it.second }
            .map { it.first }
    }
}
