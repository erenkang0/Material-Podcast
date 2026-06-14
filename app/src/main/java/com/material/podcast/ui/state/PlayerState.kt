package com.material.podcast.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Lightweight, in-memory "now playing" state shared between the mini-player and the full
 * Now Playing screen. This is a layout/interaction prototype, so there is no real audio engine —
 * just enough observable state to make the controls feel alive and consistent everywhere.
 */
object PlayerState {
    var isPlaying by mutableStateOf(false)
        private set

    /** Playback progress in 0f..1f. */
    var progress by mutableFloatStateOf(0.32f)

    var showTitle by mutableStateOf("Featured Show")
        private set

    var episodeTitle by mutableStateOf("Episode title placeholder")
        private set

    /** Total length, in seconds, of the placeholder episode (used for the time readouts). */
    var durationSeconds by mutableStateOf(2730)
        private set

    /** True once the user has started something, so the mini-player can reveal itself. */
    var hasStarted by mutableStateOf(false)
        private set

    fun play(show: String, episode: String) {
        showTitle = show
        episodeTitle = episode
        hasStarted = true
        isPlaying = true
    }

    fun togglePlayPause() {
        hasStarted = true
        isPlaying = !isPlaying
    }

    fun seekTo(fraction: Float) {
        progress = fraction.coerceIn(0f, 1f)
    }
}
