package com.material.podcast.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.material.podcast.EchoesApplication
import com.material.podcast.data.model.Podcast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthorUiState {
    data object Loading : AuthorUiState()
    data class Success(val podcasts: List<Podcast>) : AuthorUiState()
    data class Error(val message: String) : AuthorUiState()
}

class AuthorViewModel(private val authorName: String) : ViewModel() {

    private val repo get() = EchoesApplication.instance.repository

    private val _state = MutableStateFlow<AuthorUiState>(AuthorUiState.Loading)
    val state: StateFlow<AuthorUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = AuthorUiState.Loading
            try {
                val results = repo.searchPodcasts(authorName)
                // Surface the producer's own shows first, then related results.
                val sorted = results.sortedByDescending {
                    it.author.equals(authorName, ignoreCase = true)
                }
                _state.value = AuthorUiState.Success(sorted)
            } catch (e: Exception) {
                _state.value = AuthorUiState.Error(e.message ?: "Yüklenemedi")
            }
        }
    }

    class Factory(private val authorName: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AuthorViewModel(authorName) as T
    }
}
