package com.material.podcast.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.material.podcast.EchoesApplication
import com.material.podcast.data.model.Podcast
import com.material.podcast.data.store.SearchHistoryStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {

    private val repo get() = EchoesApplication.instance.repository

    private val _results = MutableStateFlow<List<Podcast>>(emptyList())
    val results: StateFlow<List<Podcast>> = _results.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _trendingPodcasts = MutableStateFlow<List<Podcast>>(emptyList())
    val trendingPodcasts: StateFlow<List<Podcast>> = _trendingPodcasts.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            try {
                _trendingPodcasts.value = repo.getFeaturedPodcasts().take(8)
            } catch (_: Exception) {}
        }
    }

    fun search(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _results.value = emptyList()
            _isSearching.value = false
            return
        }
        searchJob = viewModelScope.launch {
            delay(350) // debounce
            _isSearching.value = true
            try {
                _results.value = repo.searchPodcasts(query)
                // Only record once the debounced search actually executes.
                SearchHistoryStore.add(query)
            } catch (_: Exception) {
                _results.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }
}
