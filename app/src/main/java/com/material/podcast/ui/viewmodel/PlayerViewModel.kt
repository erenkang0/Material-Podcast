package com.material.podcast.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.material.podcast.data.model.PodcastEpisode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    val player: ExoPlayer = ExoPlayer.Builder(app).build().apply {
        playWhenReady = false
    }

    var nowPlaying by mutableStateOf<PodcastEpisode?>(null)
        private set
    var expandSheet by mutableStateOf(false)
    var isPlaying by mutableStateOf(false)
        private set
    var isBuffering by mutableStateOf(false)
        private set
    var progress by mutableFloatStateOf(0f)
    var positionMs by mutableLongStateOf(0L)
        private set
    var durationMs by mutableLongStateOf(0L)
        private set
    var playbackSpeed by mutableFloatStateOf(1f)
        private set

    val history = mutableStateListOf<PodcastEpisode>()

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            isPlaying = playing
        }
        override fun onPlaybackStateChanged(state: Int) {
            isBuffering = state == Player.STATE_BUFFERING
            updatePosition()
        }
        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) { updatePosition() }
    }

    init {
        player.addListener(listener)
        viewModelScope.launch {
            while (true) {
                if (player.isPlaying) updatePosition()
                delay(250)
            }
        }
    }

    private fun updatePosition() {
        val pos = player.currentPosition.coerceAtLeast(0L)
        val dur = player.duration.takeIf { it > 0 } ?: 0L
        positionMs = pos
        durationMs = dur
        progress = if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f
    }

    fun play(episode: PodcastEpisode) {
        nowPlaying = episode
        history.removeIf { it.guid == episode.guid }
        history.add(0, episode)
        if (history.size > 50) history.removeLastOrNull()

        val metadata = MediaMetadata.Builder()
            .setTitle(episode.title)
            .setArtist(episode.podcastTitle)
            .build()
        val item = MediaItem.Builder()
            .setUri(episode.audioUrl)
            .setMediaMetadata(metadata)
            .build()
        player.setMediaItem(item)
        player.prepare()
        player.play()
    }

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekTo(fraction: Float) {
        val dur = player.duration.takeIf { it > 0 } ?: return
        player.seekTo((dur * fraction.coerceIn(0f, 1f)).toLong())
        updatePosition()
    }

    fun seekBy(deltaMs: Long) {
        val newPos = (player.currentPosition + deltaMs).coerceAtLeast(0L)
        player.seekTo(newPos)
        updatePosition()
    }

    fun skipToNext() {
        // Seek to end to trigger "next" behavior
        player.seekTo(player.duration.coerceAtLeast(0L))
    }

    fun skipToPrevious() {
        if (player.currentPosition > 3000) player.seekTo(0L) else player.seekToPreviousMediaItem()
    }

    fun setSpeed(speed: Float) {
        playbackSpeed = speed
        player.setPlaybackSpeed(speed)
    }

    override fun onCleared() {
        player.removeListener(listener)
        player.release()
    }
}
