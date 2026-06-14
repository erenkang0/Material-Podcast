@file:OptIn(ExperimentalMaterial3Api::class)

package com.material.podcast.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Forward30
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.material.podcast.ui.LocalPlayer
import kotlinx.coroutines.launch
import kotlin.math.abs

private val BarSpring = spring<Float>(dampingRatio = 0.48f, stiffness = Spring.StiffnessMediumLow)
private val ArtSpring = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)

/**
 * Mini player strip with horizontal swipe-to-skip physics and spring snap-back.
 * Swipe left → next · Swipe right → previous. Haptic fires on commit.
 */
@Composable
fun NowPlayingBar(
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val player = LocalPlayer.current
    val episode = player.nowPlaying ?: return
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val commitThresholdPx = with(density) { 96.dp.toPx() }
    val offsetAnim = remember { Animatable(0f) }

    // Subtle scale-down on press (handled via inner Surface click)
    val pressedScale by animateFloatAsState(
        targetValue = if (abs(offsetAnim.value) > 8f) 0.975f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "barScale",
    )

    Surface(
        onClick = onExpand,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationX = offsetAnim.value
                scaleX = pressedScale
                scaleY = pressedScale
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        val offset = offsetAnim.value
                        if (abs(offset) >= commitThresholdPx) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (offset < 0) player.skipToNext() else player.skipToPrevious()
                        }
                        scope.launch { offsetAnim.animateTo(0f, BarSpring) }
                    },
                    onDragCancel = {
                        scope.launch { offsetAnim.animateTo(0f, BarSpring) }
                    },
                    onHorizontalDrag = { _, delta ->
                        scope.launch {
                            // Elastic resistance: full speed up to threshold, dampened beyond
                            val current = offsetAnim.value
                            val resistance = if (abs(current) < commitThresholdPx) 1f else 0.35f
                            offsetAnim.snapTo(current + delta * resistance)
                        }
                    },
                )
            },
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PodcastArtwork(
                    imageUrl = episode.artworkUrl,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.size(46.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        episode.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        episode.podcastTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (player.isBuffering) {
                    Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                } else {
                    EqualizerBars(
                        active = player.isPlaying,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp).height(18.dp).width(20.dp),
                    )
                    FilledIconButton(onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        player.togglePlayPause()
                    }) {
                        PlayPauseIcon(player.isPlaying, if (player.isPlaying) "Duraklat" else "Oynat")
                    }
                }
            }
            LinearProgressIndicator(
                progress = { player.progress },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).height(3.dp).clip(CircleShape),
            )
        }
    }
}

/**
 * Full-screen player as a ModalBottomSheet.
 * Material 3's sheet uses AnchoredDraggable internally — spring physics included.
 * Features: breathing artwork, haptic seek ticks, speed picker.
 */
@Composable
fun FullPlayerSheet(onDismiss: () -> Unit) {
    val player = LocalPlayer.current
    val episode = player.nowPlaying ?: return
    val haptics = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp).height(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)),
                )
            }
        },
    ) {
        FullPlayerContent(episode = episode, haptics = haptics, player = player)
    }
}

@Composable
private fun FullPlayerContent(
    episode: com.material.podcast.data.model.PodcastEpisode,
    haptics: androidx.compose.ui.hapticfeedback.HapticFeedback,
    player: com.material.podcast.ui.viewmodel.PlayerViewModel,
) {
    // Breathing artwork animation
    val infinite = rememberInfiniteTransition(label = "artBreath")
    val breathScale by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(tween(2400), RepeatMode.Reverse),
        label = "breath",
    )
    val artScale by animateFloatAsState(
        targetValue = if (player.isPlaying) breathScale else 0.96f,
        animationSpec = ArtSpring,
        label = "artScale",
    )

    // Seek scrubbing
    var dragging by remember { mutableStateOf(false) }
    var scrubValue by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    val sliderValue = if (dragging) scrubValue else player.progress

    // Haptic ticks on seek: fires every ~5% of duration
    var lastTick by remember { mutableIntStateOf(-1) }
    LaunchedEffect(sliderValue, dragging) {
        if (dragging) {
            val tick = (sliderValue * 20).toInt()
            if (tick != lastTick) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                lastTick = tick
            }
        } else {
            lastTick = -1
        }
    }

    var liked by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Artwork with breathing scale
        PodcastArtwork(
            imageUrl = episode.artworkUrl,
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .aspectRatio(1f)
                .graphicsLayer { scaleX = artScale; scaleY = artScale },
        )

        Spacer(Modifier.height(28.dp))

        // Episode info
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = episode.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = episode.podcastTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = {
                liked = !liked
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }) {
                Icon(
                    if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    "Beğen",
                    tint = if (liked) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Seek bar
        Slider(
            value = sliderValue,
            onValueChange = { dragging = true; scrubValue = it },
            onValueChangeFinished = { player.seekTo(scrubValue); dragging = false },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val posSec = if (dragging) (scrubValue * player.durationMs / 1000).toLong().toInt()
                         else (player.positionMs / 1000).toInt()
            Text(
                formatTimeSec(posSec),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                formatTimeSec((player.durationMs / 1000).toInt()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(16.dp))

        // Main playback controls
        PlaybackControls(
            isPlaying = player.isPlaying,
            isBuffering = player.isBuffering,
            onToggle = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                player.togglePlayPause()
            },
            onPrevious = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                player.skipToPrevious()
            },
            onNext = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                player.skipToNext()
            },
            onSeekBack = { player.seekBy(-10_000L) },
            onSeekForward = { player.seekBy(30_000L) },
        )

        Spacer(Modifier.height(16.dp))

        // Secondary controls row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                IconButton(onClick = { showSpeedMenu = true }) {
                    Icon(
                        Icons.Rounded.Speed, "Hız",
                        tint = if (player.playbackSpeed != 1f) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DropdownMenu(expanded = showSpeedMenu, onDismissRequest = { showSpeedMenu = false }) {
                    listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).forEach { speed ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "${speed}×",
                                    fontWeight = if (speed == player.playbackSpeed) FontWeight.Bold else FontWeight.Normal,
                                    color = if (speed == player.playbackSpeed) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface,
                                )
                            },
                            onClick = {
                                player.setSpeed(speed)
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showSpeedMenu = false
                            },
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

        Spacer(Modifier.height(12.dp))
    }
}

private fun formatTimeSec(totalSec: Int): String {
    val s = totalSec.coerceAtLeast(0)
    val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}
