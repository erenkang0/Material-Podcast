package com.material.podcast.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.material.podcast.EchoesApplication
import com.material.podcast.data.model.Podcast
import com.material.podcast.data.model.PodcastEpisode
import com.material.podcast.data.store.LibraryStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ShowDetailsUiState {
    data object Loading : ShowDetailsUiState()
    data class Success(
        val podcast: Podcast,
        val episodes: List<PodcastEpisode>,
    ) : ShowDetailsUiState()
    data class Error(val message: String) : ShowDetailsUiState()
}

class ShowDetailsViewModel(private val podcastId: String) : ViewModel() {

    private val repo get() = EchoesApplication.instance.repository

    private val _uiState = MutableStateFlow<ShowDetailsUiState>(ShowDetailsUiState.Loading)
    val uiState: StateFlow<ShowDetailsUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = ShowDetailsUiState.Loading
            try {
                val podcast = repo.getPodcast(podcastId)
                    ?: throw IllegalStateException("Podcast not found")
                LibraryStore.recordView(podcast)
                val episodes = repo.getEpisodes(podcast)
                _uiState.value = ShowDetailsUiState.Success(podcast, episodes)
            } catch (e: Exception) {
                _uiState.value = ShowDetailsUiState.Error(e.message ?: "Yüklenemedi")
            }
        }
    }

    class Factory(private val podcastId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ShowDetailsViewModel(podcastId) as T
    }
}
