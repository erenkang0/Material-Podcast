package com.material.podcast.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.material.podcast.EchoesApplication
import com.material.podcast.data.model.ExploreCategory
import com.material.podcast.data.model.Podcast
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
                val sectionDeferreds = categories.map { category ->
                    category to async {
                        runCatching { repo.searchByGenre(category.query) }.getOrDefault(emptyList())
                    }
                }
                val sections = sectionDeferreds
                    .map { (category, deferred) -> CategorySection(category, deferred.await()) }
                    .filter { it.podcasts.isNotEmpty() }
                _uiState.value = HomeUiState.Success(featuredDeferred.await(), sections)
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
