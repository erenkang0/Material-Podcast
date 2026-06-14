package com.material.podcast.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.material.podcast.EchoesApplication
import com.material.podcast.data.model.Podcast
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Success(
        val featured: List<Podcast>,
        val technology: List<Podcast>,
        val science: List<Podcast>,
        val culture: List<Podcast>,
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

class HomeViewModel : ViewModel() {

    private val repo get() = EchoesApplication.instance.repository

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                val featuredDeferred = async { repo.getFeaturedPodcasts() }
                val techDeferred = async { repo.searchByGenre("technology podcast") }
                val sciDeferred = async { repo.searchByGenre("science education podcast") }
                val cultureDeferred = async { repo.searchByGenre("society culture podcast") }

                _uiState.value = HomeUiState.Success(
                    featured = featuredDeferred.await(),
                    technology = techDeferred.await(),
                    science = sciDeferred.await(),
                    culture = cultureDeferred.await(),
                )
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
