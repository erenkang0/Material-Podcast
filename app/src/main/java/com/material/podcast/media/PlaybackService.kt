package com.material.podcast.media

import android.app.PendingIntent
import android.content.Intent
import android.media.audiofx.LoudnessEnhancer
import android.os.Bundle
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.material.podcast.MainActivity

class PlaybackService : MediaSessionService() {

    private var session: MediaSession? = null
    private var player: ExoPlayer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    override fun onCreate() {
        super.onCreate()
        val exo = ExoPlayer.Builder(this)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setHandleAudioBecomingNoisy(true)
            .build()
        player = exo
        val activityIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        session = MediaSession.Builder(this, exo)
            .setSessionActivity(activityIntent)
            .setCallback(SmartAudioCallback())
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    /** Grants our custom commands on top of the defaults, and routes them to the ExoPlayer. */
    private inner class SmartAudioCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(SessionCommand(CMD_SKIP_SILENCE, Bundle.EMPTY))
                .add(SessionCommand(CMD_VOICE_BOOST, Bundle.EMPTY))
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                CMD_SKIP_SILENCE -> player?.skipSilenceEnabled = args.getBoolean(EXTRA_ENABLED)
                CMD_VOICE_BOOST -> setVoiceBoost(args.getBoolean(EXTRA_ENABLED))
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    private fun setVoiceBoost(enabled: Boolean) {
        val exo = player ?: return
        if (enabled) {
            try {
                val sessionId = exo.audioSessionId
                if (sessionId != C.AUDIO_SESSION_ID_UNSET) {
                    if (loudnessEnhancer == null) loudnessEnhancer = LoudnessEnhancer(sessionId)
                    loudnessEnhancer?.setTargetGain(900) // +9 dB, lifts quiet speech
                    loudnessEnhancer?.enabled = true
                }
            } catch (_: Exception) {
                loudnessEnhancer = null
            }
        } else {
            try { loudnessEnhancer?.enabled = false } catch (_: Exception) {}
        }
    }

    /**
     * When the user swipes the app away from recents and nothing is actively playing,
     * tear the service down so audio doesn't linger. If something is still playing we keep
     * going (that's the whole point of background playback).
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = session?.player
        if (p == null || !p.playWhenReady || p.playbackState == Player.STATE_IDLE) {
            p?.stop()
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        try { loudnessEnhancer?.release() } catch (_: Exception) {}
        loudnessEnhancer = null
        session?.run { player.release(); release() }
        session = null
        player = null
        super.onDestroy()
    }

    companion object {
        const val CMD_SKIP_SILENCE = "com.material.podcast.SKIP_SILENCE"
        const val CMD_VOICE_BOOST = "com.material.podcast.VOICE_BOOST"
        const val EXTRA_ENABLED = "enabled"
    }
}
