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
    var sleepAtEnd by mutableStateOf(false)
        private set
    var artworkColorSeed by mutableIntStateOf(0)
        private set
    var isLiked by mutableStateOf(false)
        private set

    /** The active playlist (current podcast context); native next/previous walk this. */
    val queue = mutableStateListOf<PodcastEpisode>()
    var currentQueueIndex by mutableIntStateOf(0)
        private set
    /** Episodes that come after the current one in [queue]. */
    val upNext: List<PodcastEpisode>
        get() = queue.drop(currentQueueIndex + 1)

    /** Previously played episodes (most recent first). */
    val history = mutableStateListOf<PodcastEpisode>()

    private var pendingPlay: (() -> Unit)? = null
    private var sleepJob: Job? = null
    private var lastSavedAt: Long = 0L

    private val downloadManager get() = EchoesApplication.instance.downloadManager

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
        override fun onPlaybackStateChanged(state: Int) {
            isBuffering = state == Player.STATE_BUFFERING
            updatePosition()
        }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            syncToCurrentItem()
            if (sleepAtEnd) {
                // Stop instead of rolling into the next episode.
                controller?.pause()
                sleepAtEnd = false
            }
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
                    playbackSpeed = ctrl.playbackParameters.speed
                    val pending = pendingPlay
                    if (pending != null) {
                        pending()
                        pendingPlay = null
                    } else if (ctrl.currentMediaItem != null && ctrl.playbackState != Player.STATE_IDLE) {
                        // Reconnecting to a session that is already playing (e.g. opened from the
                        // notification after the UI process was recreated) — restore the UI state.
                        restoreFromController(ctrl)
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

    private fun restoreFromController(ctrl: MediaController) {
        if (queue.isEmpty()) {
            val saved = LibraryStore.getCurrentQueue()
            if (saved.isNotEmpty()) { queue.clear(); queue.addAll(saved) }
        }
        isPlaying = ctrl.isPlaying
        syncToCurrentItem()
        updatePosition()
    }

    /** Point [nowPlaying] (and derived state) at the controller's current item. */
    private fun syncToCurrentItem() {
        val ctrl = controller ?: return
        val index = ctrl.currentMediaItemIndex
        currentQueueIndex = index
        val episode = queue.getOrNull(index)
            ?: ctrl.currentMediaItem?.let { mediaItemToEpisode(it) }
            ?: return
        if (queue.getOrNull(index)?.guid != episode.guid && queue.none { it.guid == episode.guid }) {
            // queue lost; keep a single-item context so the UI still works
            if (queue.isEmpty()) queue.add(episode)
        }
        if (nowPlaying?.guid != episode.guid) {
            nowPlaying = episode
            isLiked = LibraryStore.isLiked(episode.guid)
            updateArtworkColor(episode)
            pushHistory(episode)
        }
    }

    private fun mediaItemToEpisode(item: MediaItem): PodcastEpisode {
        val md = item.mediaMetadata
        return PodcastEpisode(
            guid = item.mediaId,
            title = md.title?.toString() ?: "",
            description = "",
            audioUrl = item.localConfiguration?.uri?.toString() ?: "",
            artworkUrl = md.artworkUri?.toString() ?: "",
            publishedDate = "",
            durationSeconds = 0,
            podcastTitle = md.artist?.toString() ?: "",
            podcastId = "",
        )
    }

    private fun updatePosition() {
        val ctrl = controller ?: return
        val pos = ctrl.currentPosition.coerceAtLeast(0L)
        val dur = ctrl.duration.takeIf { it > 0 } ?: 0L
        positionMs = pos
        durationMs = dur
        progress = if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f
    }

    private fun maybeSaveProgress() {
        val ep = nowPlaying ?: return
        val now = System.currentTimeMillis()
        if (now - lastSavedAt < 5_000L) return
        lastSavedAt = now
        LibraryStore.saveProgress(ep, positionMs, durationMs)
    }

    private fun pushHistory(episode: PodcastEpisode) {
        history.removeIf { it.guid == episode.guid }
        history.add(0, episode)
        if (history.size > 50) history.removeAt(history.lastIndex)
    }

    private fun updateArtworkColor(episode: PodcastEpisode) {
        artworkColorSeed = 0
        viewModelScope.launch {
            val seed = extractArtworkColor(getApplication<Application>(), episode.artworkUrl)
            if (seed != null && nowPlaying?.guid == episode.guid) artworkColorSeed = seed
        }
    }

    /**
     * Play [episode] within an optional [context] playlist so next/previous walk the
     * surrounding episodes. Defaults to a single-item context.
     */
    fun play(
        episode: PodcastEpisode,
        context: List<PodcastEpisode> = listOf(episode),
        startPositionMs: Long = 0L,
    ) {
        val list = context.ifEmpty { listOf(episode) }
        val startIndex = list.indexOfFirst { it.guid == episode.guid }.coerceAtLeast(0)

        nowPlaying = episode
        isLiked = LibraryStore.isLiked(episode.guid)
        lastSavedAt = System.currentTimeMillis()
        queue.clear(); queue.addAll(list)
        currentQueueIndex = startIndex
        pushHistory(episode)
        updateArtworkColor(episode)
        LibraryStore.saveCurrentQueue(list)

        val action = {
            controller?.let { ctrl ->
                val items = list.map(::toMediaItem)
                ctrl.setMediaItems(items, startIndex, startPositionMs)
                ctrl.prepare()
                ctrl.play()
            }
        }
        if (controller != null) action() else pendingPlay = action
    }

    fun resume(point: ResumePoint) = play(point.episode, startPositionMs = point.positionMs)

    fun playQueueIndex(index: Int) {
        val ctrl = controller ?: return
        if (index in queue.indices) {
            ctrl.seekToDefaultPosition(index)
            ctrl.play()
        }
    }

    private fun toMediaItem(episode: PodcastEpisode): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(episode.title)
            .setArtist(episode.podcastTitle)
            .setArtworkUri(android.net.Uri.parse(episode.artworkUrl))
            .build()
        val uri = downloadManager.localUriOrNull(episode.guid) ?: episode.audioUrl
        return MediaItem.Builder()
            .setMediaId(episode.guid)
            .setUri(uri)
            .setMediaMetadata(metadata)
            .build()
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
        if (ctrl.hasNextMediaItem()) ctrl.seekToNextMediaItem() else ctrl.seekTo(ctrl.duration.coerceAtLeast(0L))
    }

    fun skipToPrevious() {
        val ctrl = controller ?: return
        if (ctrl.currentPosition > 3000 || !ctrl.hasPreviousMediaItem()) ctrl.seekTo(0L)
        else ctrl.seekToPreviousMediaItem()
    }

    fun setSpeed(speed: Float) {
        playbackSpeed = speed
        controller?.setPlaybackSpeed(speed)
    }

    fun setSleepTimer(minutes: Int) {
        sleepJob?.cancel()
        sleepTimerMs = 0L
        sleepAtEnd = false
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

    /** Stop playback when the current episode finishes. */
    fun setSleepAtEpisodeEnd() {
        sleepJob?.cancel()
        sleepTimerMs = 0L
        sleepAtEnd = true
    }

    fun cancelSleepTimer() {
        sleepJob?.cancel()
        sleepJob = null
        sleepTimerMs = 0L
        sleepAtEnd = false
    }

    override fun onCleared() {
        controller?.removeListener(listener)
        sleepJob?.cancel()
        MediaController.releaseFuture(controllerFuture)
    }
}
