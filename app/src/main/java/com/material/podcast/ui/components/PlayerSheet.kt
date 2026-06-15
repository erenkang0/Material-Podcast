@file:OptIn(ExperimentalMaterial3Api::class)

package com.material.podcast.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.material.podcast.EchoesApplication
import com.material.podcast.data.store.LibraryStore
import com.material.podcast.media.DownloadStatus
import com.material.podcast.ui.LocalPlayer
import com.material.podcast.ui.theme.PlayerColors
import com.material.podcast.ui.theme.playerColorsFromSeed
import kotlinx.coroutines.launch
import kotlin.math.abs

private val BarSpring = spring<Float>(dampingRatio = 0.48f, stiffness = Spring.StiffnessMediumLow)
private val ArtSpring = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)

private enum class DragDir { Unknown, Horizontal, Vertical }

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
    val verticalThresholdPx = with(density) { 56.dp.toPx() }
    val offsetAnim = remember { Animatable(0f) }

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
                var direction = DragDir.Unknown
                var accX = 0f
                var accY = 0f
                detectDragGestures(
                    onDragStart = { _: Offset ->
                        direction = DragDir.Unknown
                        accX = 0f
                        accY = 0f
                    },
                    onDrag = { change: PointerInputChange, dragAmount: Offset ->
                        change.consume()
                        accX += dragAmount.x
                        accY += dragAmount.y
                        if (direction == DragDir.Unknown && (abs(accX) > 8f || abs(accY) > 8f)) {
                            direction = if (abs(accX) >= abs(accY)) DragDir.Horizontal else DragDir.Vertical
                        }
                        if (direction == DragDir.Horizontal) {
                            scope.launch {
                                val current = offsetAnim.value
                                val resistance = if (abs(current) < commitThresholdPx) 1f else 0.35f
                                offsetAnim.snapTo(current + dragAmount.x * resistance)
                            }
                        }
                    },
                    onDragEnd = {
                        when {
                            direction == DragDir.Vertical && accY < -verticalThresholdPx -> onExpand()
                            direction == DragDir.Horizontal -> {
                                val offset = offsetAnim.value
                                if (abs(offset) >= commitThresholdPx) {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (offset < 0) player.skipToNext() else player.skipToPrevious()
                                }
                            }
                        }
                        scope.launch { offsetAnim.animateTo(0f, BarSpring) }
                    },
                    onDragCancel = { scope.launch { offsetAnim.animateTo(0f, BarSpring) } },
                )
            },
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
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
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                } else {
                    EqualizerBars(
                        active = player.isPlaying,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .height(18.dp)
                            .width(20.dp),
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .height(3.dp)
                    .clip(CircleShape),
            )
        }
    }
}

@Composable
fun FullPlayerSheet(
    onDismiss: () -> Unit,
    onOpenShow: (String) -> Unit,
    onOpenAuthor: (String) -> Unit,
) {
    val player = LocalPlayer.current
    player.nowPlaying ?: return
    val haptics = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Dynamic surface derived from the cover, with a smooth wash when the track changes.
    val baseScheme = MaterialTheme.colorScheme
    val isDark = baseScheme.surface.luminance() < 0.5f
    val target: PlayerColors = if (player.artworkColorSeed != 0) {
        playerColorsFromSeed(player.artworkColorSeed, isDark)
    } else {
        PlayerColors(baseScheme.surfaceContainerLow, baseScheme.onSurface, baseScheme.primary)
    }
    val bg by animateColorAsState(target.background, tween(500), label = "playerBg")
    val accent by animateColorAsState(target.accent, tween(500), label = "playerAccent")
    val onBg = target.onBackground

    val tinted = baseScheme.copy(
        surface = bg,
        surfaceContainer = bg,
        surfaceContainerLow = bg,
        surfaceContainerHigh = bg,
        background = bg,
        onSurface = onBg,
        onBackground = onBg,
        onSurfaceVariant = onBg.copy(alpha = 0.7f),
        primary = accent,
        onPrimary = bg,
        primaryContainer = accent,
        onPrimaryContainer = bg,
        secondaryContainer = accent.copy(alpha = 0.20f),
        onSecondaryContainer = onBg,
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = bg,
        contentColor = onBg,
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(onBg.copy(alpha = 0.3f)),
                )
            }
        },
    ) {
        androidx.compose.material3.MaterialTheme(colorScheme = tinted) {
            FullPlayerContent(
                haptics = haptics,
                player = player,
                onOpenShow = onOpenShow,
                onOpenAuthor = onOpenAuthor,
            )
        }
    }
}

@Composable
private fun FullPlayerContent(
    haptics: androidx.compose.ui.hapticfeedback.HapticFeedback,
    player: com.material.podcast.ui.viewmodel.PlayerViewModel,
    onOpenShow: (String) -> Unit,
    onOpenAuthor: (String) -> Unit,
) {
    val episode = player.nowPlaying ?: return

    var dragging by remember { mutableStateOf(false) }
    var scrubValue by remember { mutableFloatStateOf(0f) }
    val sliderValue = if (dragging) scrubValue else player.progress

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

    var showSleepTimer by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var showAddMoment by remember { mutableStateOf(false) }

    if (showAddMoment) {
        AddMomentDialog(
            positionLabel = formatTimeSec((player.positionMs / 1000).toInt()),
            onConfirm = { note ->
                player.addMoment(note)
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                showAddMoment = false
            },
            onDismiss = { showAddMoment = false },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PodcastArtwork(
            imageUrl = episode.artworkUrl,
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .aspectRatio(1f),
        )

        Spacer(Modifier.height(20.dp))

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
                    modifier = Modifier.clickable { onOpenShow(episode.podcastId) },
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = episode.podcastTitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clickable { onOpenShow(episode.podcastId) },
                    )
                    if (episode.podcastAuthor.isNotBlank() && episode.podcastAuthor != episode.podcastTitle) {
                        Text(
                            text = "  •  ${episode.podcastAuthor}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable { onOpenAuthor(episode.podcastAuthor) },
                        )
                    }
                }
            }
            IconButton(onClick = {
                player.toggleLike()
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }) {
                Icon(
                    if (player.isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    "Beğen",
                    tint = if (player.isLiked) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        WavySeekBar(
            fraction = sliderValue,
            playing = player.isPlaying,
            onScrubStart = { dragging = true },
            onScrub = { scrubValue = it; dragging = true },
            onScrubFinished = {
                player.seekTo(it)
                dragging = false
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            },
            activeColor = MaterialTheme.colorScheme.primary,
            inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
            thumbColor = MaterialTheme.colorScheme.primary,
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

        Spacer(Modifier.height(8.dp))

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

        Spacer(Modifier.height(4.dp))

        // Speed slider — always visible above the button row
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "0.5×",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Hız: ${"%.2f".format(player.playbackSpeed)}×",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "2×",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Slider(
                value = player.playbackSpeed,
                onValueChange = { player.setSpeed(it) },
                onValueChangeFinished = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
                valueRange = 0.5f..2.0f,
                steps = 5,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Queue panel — "Up next" from the current podcast + "Previously played"
        AnimatedVisibility(
            visible = showQueue,
            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
        ) {
            val upNext = player.upNext
            val history = player.history
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Sıra", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = { showQueue = false }) { Text("Kapat") }
                }
                LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                    item(key = "up_header") {
                        QueueSubHeader("Sıradaki")
                    }
                    if (upNext.isEmpty()) {
                        item(key = "up_empty") {
                            Text(
                                "Bu bölümden sonrası yok",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 6.dp),
                            )
                        }
                    } else {
                        items(upNext, key = { "up_${it.guid}" }) { ep ->
                            QueueRow(ep, isCurrent = false) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                player.playQueueIndex(player.queue.indexOfFirst { q -> q.guid == ep.guid })
                            }
                        }
                    }
                    if (history.isNotEmpty()) {
                        item(key = "hist_header") {
                            QueueSubHeader("Önceki dinlediklerim")
                        }
                        items(history, key = { "hist_${it.guid}" }) { ep ->
                            QueueRow(ep, isCurrent = ep.guid == player.nowPlaying?.guid) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                player.play(ep)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }

        // Sleep timer panel — expands above secondary controls
        AnimatedVisibility(
            visible = showSleepTimer,
            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                when {
                    player.sleepTimerMs > 0 -> Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Kalan: ${formatTimeSec((player.sleepTimerMs / 1000).toInt())}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        TextButton(onClick = {
                            player.cancelSleepTimer()
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showSleepTimer = false
                        }) { Text("İptal") }
                    }
                    player.sleepAtEnd -> Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Bölüm bitince duracak",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        TextButton(onClick = {
                            player.cancelSleepTimer()
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showSleepTimer = false
                        }) { Text("İptal") }
                    }
                    else -> {
                        Text(
                            "Uyku zamanlayıcı",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item(key = "end") {
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        player.setSleepAtEpisodeEnd()
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        showSleepTimer = false
                                    },
                                    label = { Text("Bölüm bitince") },
                                )
                            }
                            items(listOf(5, 10, 15, 30, 45, 60, 90), key = { it }) { minutes ->
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        player.setSleepTimer(minutes)
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        showSleepTimer = false
                                    },
                                    label = { Text("$minutes dk") },
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }

        // Secondary controls row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = {
                showSleepTimer = !showSleepTimer
                if (showSleepTimer) showQueue = false
            }) {
                Icon(
                    Icons.Rounded.Timer,
                    "Uyku zamanlayıcı",
                    tint = if (player.sleepTimerMs > 0 || player.sleepAtEnd || showSleepTimer)
                        MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = {
                showAddMoment = true
            }) {
                Icon(
                    Icons.Rounded.BookmarkAdd,
                    "Favori an ekle",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DownloadButton(episode = episode)
            IconButton(onClick = {
                showQueue = !showQueue
                if (showQueue) showSleepTimer = false
            }) {
                Icon(
                    Icons.Rounded.QueueMusic,
                    "Sıra",
                    tint = if (showQueue) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun QueueSubHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
    )
}

@Composable
private fun QueueRow(
    episode: com.material.podcast.data.model.PodcastEpisode,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PodcastArtwork(
            imageUrl = episode.artworkUrl,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.size(38.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                episode.title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                episode.podcastTitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (isCurrent) {
            Icon(
                Icons.Rounded.QueueMusic, "Şu an çalıyor",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun DownloadButton(episode: com.material.podcast.data.model.PodcastEpisode) {
    val dm = EchoesApplication.instance.downloadManager
    val haptics = LocalHapticFeedback.current
    val downloaded = LibraryStore.isDownloaded(episode.guid)
    val state = dm.states[episode.guid]
    IconButton(onClick = {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        when {
            downloaded -> dm.delete(episode.guid)
            state?.status == DownloadStatus.Downloading -> {} // already running
            else -> dm.download(episode)
        }
    }) {
        when {
            downloaded -> Icon(
                Icons.Rounded.DownloadDone, "İndirildi (kaldırmak için dokun)",
                tint = MaterialTheme.colorScheme.primary,
            )
            state?.status == DownloadStatus.Downloading -> CircularProgressIndicator(
                progress = { state.progress.coerceIn(0f, 1f) },
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
            )
            else -> Icon(
                Icons.Rounded.Download, "İndir",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AddMomentDialog(
    positionLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Favori an") },
        text = {
            Column {
                Text(
                    "Bu an $positionLabel konumunda kaydedilecek.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text("Not (isteğe bağlı)") },
                    singleLine = false,
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(note) }) { Text("Kaydet") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } },
    )
}

private fun formatTimeSec(totalSec: Int): String {
    val s = totalSec.coerceAtLeast(0)
    val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}
