package com.material.podcast.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.material.podcast.ui.LocalPlayer
import com.material.podcast.ui.components.PlaybackControls
import com.material.podcast.ui.components.PodcastArtwork

@Composable
fun PlayerScreen(onBack: () -> Unit) {
    val player = LocalPlayer.current
    val episode = player.nowPlaying

    // Artwork pulse while playing
    val infinite = rememberInfiniteTransition(label = "art")
    val pulse by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(tween(2400), RepeatMode.Reverse),
        label = "pulse",
    )
    val artScale by animateFloatAsState(
        targetValue = if (player.isPlaying) pulse else 0.96f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
        label = "artScale",
    )

    // Scrubbing state
    var dragging by remember { mutableStateOf(false) }
    var scrubValue by remember { mutableFloatStateOf(0f) }
    val sliderValue = if (dragging) scrubValue else player.progress

    var liked by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .pointerInput(Unit) {
                var acc = 0f
                detectVerticalDragGestures(
                    onDragStart = { acc = 0f },
                    onDragEnd = { if (acc > 160f) onBack() },
                ) { _, dy -> acc += dy }
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp),
        ) {
            // ── Top bar ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.KeyboardArrowDown, "Kapat", modifier = Modifier.size(32.dp))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "ŞİMDİ OYNUYOR",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        episode?.podcastTitle ?: "",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = { }) {
                    Icon(Icons.Rounded.MoreVert, "Daha fazla")
                }
            }

            Spacer(Modifier.weight(1f))

            // ── Artwork ───────────────────────────────────────────────────────
            PodcastArtwork(
                imageUrl = episode?.artworkUrl ?: "",
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .graphicsLayer { scaleX = artScale; scaleY = artScale },
            )

            Spacer(Modifier.weight(0.6f))

            // ── Episode info ──────────────────────────────────────────────────
            Text(
                text = episode?.title ?: "",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = episode?.podcastTitle ?: "",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.weight(0.4f))

            // ── Slider ────────────────────────────────────────────────────────
            Slider(
                value = sliderValue,
                onValueChange = { dragging = true; scrubValue = it },
                onValueChangeFinished = { player.seekTo(scrubValue); dragging = false },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val pos = if (dragging) {
                    (scrubValue * player.durationMs / 1000).toLong().toInt()
                } else {
                    (player.positionMs / 1000).toInt()
                }
                Text(
                    formatTime(pos),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    formatTime((player.durationMs / 1000).toInt()),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Playback controls ─────────────────────────────────────────────
            PlaybackControls(
                isPlaying = player.isPlaying,
                isBuffering = player.isBuffering,
                onToggle = { player.togglePlayPause() },
                onPrevious = { player.skipToPrevious() },
                onNext = { player.skipToNext() },
                onSeekBack = { player.seekBy(-10_000L) },
                onSeekForward = { player.seekBy(30_000L) },
            )

            Spacer(Modifier.height(16.dp))

            // ── Secondary controls ────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { liked = !liked }) {
                    Icon(
                        if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        "Beğen",
                        tint = if (liked) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box {
                    IconButton(onClick = { showSpeedMenu = true }) {
                        Icon(
                            Icons.Rounded.Speed, "Oynatma hızı",
                            tint = if (player.playbackSpeed != 1f) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DropdownMenu(expanded = showSpeedMenu, onDismissRequest = { showSpeedMenu = false }) {
                        listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).forEach { speed ->
                            DropdownMenuItem(
                                text = { Text("${speed}×", fontWeight = if (speed == player.playbackSpeed) FontWeight.Bold else FontWeight.Normal) },
                                onClick = { player.setSpeed(speed); showSpeedMenu = false },
                            )
                        }
                    }
                }
                IconButton(onClick = { }) {
                    Icon(Icons.Rounded.Timer, "Uyku zamanlayıcı",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { }) {
                    Icon(Icons.Rounded.QueueMusic, "Sıra",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun formatTime(totalSec: Int): String {
    val s = totalSec.coerceAtLeast(0)
    val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}
