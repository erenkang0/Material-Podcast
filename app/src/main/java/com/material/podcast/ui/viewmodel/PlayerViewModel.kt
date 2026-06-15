package com.material.podcast.ui.viewmodel

import android.app.Application
import android.content.ComponentName
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.material.podcast.EchoesApplication
import com.material.podcast.data.model.FavoriteMoment
import com.material.podcast.data.model.PodcastEpisode
import com.material.podcast.data.model.ResumePoint
import com.material.podcast.data.store.LibraryStore
import com.material.podcast.media.PlaybackService
import com.material.podcast.ui.theme.extractArtworkColor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    private val sessionToken = SessionToken(app, ComponentName(app, PlaybackService::class.java))
    private val controllerFuture = MediaController.Builder(app, sessionToken).buildAsync()
    private var controller: MediaController? = null

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
    var sleepTimerMs by mutableLongStateOf(0L)
        private set

    /** Dominant ARGB color pulled from the current cover (0 = none/not ready). */
    var artworkColorSeed by mutableIntStateOf(0)
        private set
    /** Whether the current episode is in the user's Beğeniler list. */
    var isLiked by mutableStateOf(false)
        private set

    val history = mutableStateListOf<PodcastEpisode>()
    private var pendingEpisode: PodcastEpisode? = null
    private var pendingStartMs: Long = 0L
    private var sleepJob: Job? = null
    private var lastSavedAt: Long = 0L

    private val downloadManager get() = EchoesApplication.instance.downloadManager

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
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
        controllerFuture.addListener({
            try {
                controller = controllerFuture.get().also { ctrl ->
                    ctrl.addListener(listener)
                    pendingEpisode?.let { ep ->
                        performPlay(ep, ctrl, pendingStartMs)
                        pendingEpisode = null
                        pendingStartMs = 0L
                    }
                }
            } catch (_: Exception) {}
        }, ContextCompat.getMainExecutor(app))

        viewModelScope.launch {
            while (true) {
                val ctrl = controller
                if (ctrl != null && ctrl.isPlaying) {
                    updatePosition()
                    maybeSaveProgress()
                }
                delay(250L)
            }
        }
    }

    /** Persist the resume point at most every 5s while playing. */
    private fun maybeSaveProgress() {
        val ep = nowPlaying ?: return
        val now = System.currentTimeMillis()
        if (now - lastSavedAt < 5_000L) return
        lastSavedAt = now
        LibraryStore.saveProgress(ep, positionMs, durationMs)
    }

    private fun updatePosition() {
        val ctrl = controller ?: return
        val pos = ctrl.currentPosition.coerceAtLeast(0L)
        val dur = ctrl.duration.takeIf { it > 0 } ?: 0L
        positionMs = pos
        durationMs = dur
        progress = if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f
    }

    fun play(episode: PodcastEpisode, startPositionMs: Long = 0L) {
        nowPlaying = episode
        isLiked = LibraryStore.isLiked(episode.guid)
        artworkColorSeed = 0
        lastSavedAt = System.currentTimeMillis()
        history.removeIf { it.guid == episode.guid }
        history.add(0, episode)
        if (history.size > 50) history.removeLastOrNull()

        viewModelScope.launch {
            val seed = extractArtworkColor(getApplication<Application>(), episode.artworkUrl)
            if (seed != null && nowPlaying?.guid == episode.guid) artworkColorSeed = seed
        }

        val ctrl = controller
        if (ctrl != null) {
            performPlay(episode, ctrl, startPositionMs)
        } else {
            pendingEpisode = episode
            pendingStartMs = startPositionMs
        }
    }

    /** Resume a saved "continue listening" point from its stored position. */
    fun resume(point: ResumePoint) = play(point.episode, point.positionMs)

    private fun performPlay(episode: PodcastEpisode, ctrl: MediaController, startPositionMs: Long) {
        val metadata = MediaMetadata.Builder()
            .setTitle(episode.title)
            .setArtist(episode.podcastTitle)
            .setArtworkUri(android.net.Uri.parse(episode.artworkUrl))
            .build()
        // Prefer an offline copy when the episode has been downloaded.
        val uri = downloadManager.localUriOrNull(episode.guid) ?: episode.audioUrl
        val item = MediaItem.Builder()
            .setUri(uri)
            .setMediaMetadata(metadata)
            .build()
        if (startPositionMs > 0L) ctrl.setMediaItem(item, startPositionMs) else ctrl.setMediaItem(item)
        ctrl.prepare()
        ctrl.play()
    }

    fun togglePlayPause() {
        val ctrl = controller ?: return
        if (ctrl.isPlaying) {
            ctrl.pause()
            nowPlaying?.let { LibraryStore.saveProgress(it, positionMs, durationMs) }
        } else {
            ctrl.play()
        }
    }

    fun toggleLike() {
        val ep = nowPlaying ?: return
        LibraryStore.toggleLike(ep)
        isLiked = LibraryStore.isLiked(ep.guid)
    }

    /** Bookmark the current position as a favorite moment with an optional note. */
    fun addMoment(note: String) {
        val ep = nowPlaying ?: return
        LibraryStore.addMoment(
            FavoriteMoment(
                id = UUID.randomUUID().toString(),
                episodeGuid = ep.guid,
                episodeTitle = ep.title,
                podcastTitle = ep.podcastTitle,
                podcastId = ep.podcastId,
                artworkUrl = ep.artworkUrl,
                audioUrl = ep.audioUrl,
                positionMs = positionMs,
                note = note.trim(),
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    fun seekTo(fraction: Float) {
        val ctrl = controller ?: return
        val dur = ctrl.duration.takeIf { it > 0 } ?: return
        ctrl.seekTo((dur * fraction.coerceIn(0f, 1f)).toLong())
        updatePosition()
    }

    fun seekBy(deltaMs: Long) {
        val ctrl = controller ?: return
        val newPos = (ctrl.currentPosition + deltaMs).coerceAtLeast(0L)
        ctrl.seekTo(newPos)
        updatePosition()
    }

    fun skipToNext() {
        val ctrl = controller ?: return
        ctrl.seekTo(ctrl.duration.coerceAtLeast(0L))
    }

    fun skipToPrevious() {
        val ctrl = controller ?: return
        if (ctrl.currentPosition > 3000) ctrl.seekTo(0L) else ctrl.seekToPreviousMediaItem()
    }

    fun setSpeed(speed: Float) {
        playbackSpeed = speed
        controller?.setPlaybackSpeed(speed)
    }

    fun setSleepTimer(minutes: Int) {
        sleepJob?.cancel()
        sleepTimerMs = 0L
        if (minutes == 0) return
        sleepJob = viewModelScope.launch {
            val endAt = System.currentTimeMillis() + minutes * 60_000L
            while (true) {
                val remaining = endAt - System.currentTimeMillis()
                if (remaining <= 0) { controller?.pause(); sleepTimerMs = 0L; break }
                sleepTimerMs = remaining
                delay(500L)
            }
        }
    }

    fun cancelSleepTimer() {
        sleepJob?.cancel()
        sleepJob = null
        sleepTimerMs = 0L
    }

    override fun onCleared() {
        controller?.removeListener(listener)
        sleepJob?.cancel()
        MediaController.releaseFuture(controllerFuture)
    }
}
