@file:OptIn(ExperimentalMaterial3Api::class)

package com.material.podcast.ui.components

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Contactless
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Subject
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
import androidx.compose.runtime.mutableLongStateOf
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
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

    // Dynamic color from the artwork, blended toward the M3 primaryContainer so it stays tasteful.
    val baseScheme = MaterialTheme.colorScheme
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    val seed = player.artworkColorSeed
    val targetContainer: androidx.compose.ui.graphics.Color
    val targetOnContainer: androidx.compose.ui.graphics.Color
    val targetAccent: androidx.compose.ui.graphics.Color
    if (seed != 0) {
        val pc = playerColorsFromSeed(seed, dark)
        // Blend cover background with the theme container so it never looks garish.
        targetContainer = androidx.compose.ui.graphics.lerp(baseScheme.primaryContainer, pc.background, 0.85f)
        targetOnContainer = pc.onBackground
        targetAccent = pc.accent
    } else {
        targetContainer = baseScheme.primaryContainer
        targetOnContainer = baseScheme.onPrimaryContainer
        targetAccent = baseScheme.primary
    }
    val barColor by animateColorAsState(targetContainer, tween(500), label = "barColor")
    val barOnColor by animateColorAsState(targetOnContainer, tween(500), label = "barOnColor")
    val barAccent by animateColorAsState(targetAccent, tween(500), label = "barAccent")

    val expandWithHaptic: () -> Unit = {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        onExpand()
    }

    Surface(
        onClick = expandWithHaptic,
        shape = MaterialTheme.shapes.large,
        color = barColor,
        contentColor = barOnColor,
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
                            direction == DragDir.Vertical && accY < -verticalThresholdPx -> expandWithHaptic()
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
                    .padding(start = 8.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    // Make the artwork participate in the same tap + swipe-up-to-expand
                    // gesture as the rest of the bar, instead of swallowing the touch.
                    modifier = Modifier
                        .clickable(
                            indication = null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        ) { expandWithHaptic() }
                        .pointerInput(Unit) {
                            var accY = 0f
                            var accX = 0f
                            detectDragGestures(
                                onDragStart = { accY = 0f; accX = 0f },
                                onDrag = { change, drag ->
                                    change.consume()
                                    accY += drag.y
                                    accX += drag.x
                                },
                                onDragEnd = {
                                    if (accY < -verticalThresholdPx && abs(accY) > abs(accX)) expandWithHaptic()
                                    accY = 0f; accX = 0f
                                },
                                onDragCancel = { accY = 0f; accX = 0f },
                            )
                        },
                ) {
                    // Pulsing glow behind artwork when playing
                    val infiniteGlow = rememberInfiniteTransition(label = "glowPulse")
                    val glowAlpha by infiniteGlow.animateFloat(
                        initialValue = 0.0f,
                        targetValue = if (player.isPlaying) 0.55f else 0.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(900),
                            repeatMode = RepeatMode.Reverse,
                        ),
                        label = "glowAlpha",
                    )
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .graphicsLayer {
                                shadowElevation = if (player.isPlaying) 24f * glowAlpha else 0f
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                                clip = false
                                alpha = glowAlpha
                            }
                            .background(
                                barAccent.copy(alpha = glowAlpha * 0.6f),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            ),
                    )
                    PodcastArtwork(
                        imageUrl = episode.artworkUrl,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.size(48.dp),
                    )
                    if (player.isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = barOnColor,
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        episode.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        episode.podcastTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = barOnColor.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        player.skipToPrevious()
                    },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        Icons.Rounded.SkipPrevious, "Önceki",
                        tint = barOnColor,
                    )
                }
                FilledIconButton(onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    player.togglePlayPause()
                }) {
                    PlayPauseIcon(player.isPlaying, if (player.isPlaying) "Duraklat" else "Oynat")
                }
                IconButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        player.skipToNext()
                    },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        Icons.Rounded.SkipNext, "Sonraki",
                        tint = barOnColor,
                    )
                }
            }
            LinearProgressIndicator(
                progress = { player.progress },
                color = barAccent,
                trackColor = barOnColor.copy(alpha = 0.15f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
            )
        }
    }
}

@Composable
fun FullPlayerSheet(
    onDismiss: () -> Unit,
    onOpenShow: (String) -> Unit,
    onOpenAuthor: (String) -> Unit,
    onOpenQueue: () -> Unit,
    onOpenTranscript: () -> Unit,
    onOpenChapters: () -> Unit = {},
) {
    val player = LocalPlayer.current
    player.nowPlaying ?: return
    val haptics = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Track fullscreen state via a stable MutableState reference so pointerInput lambdas
    // can read/write it without stale captures.
    val fullscreenState = remember { mutableStateOf(false) }
    var isFullscreen by fullscreenState

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

    if (isFullscreen) {
        // True full-screen: no sheet chrome, no drag handle, covers status bar.
        Dialog(
            onDismissRequest = { isFullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(fullscreenState) {
                        // Swipe down anywhere to exit full-screen back to the sheet.
                        var accY = 0f
                        detectDragGestures(
                            onDragStart = { accY = 0f },
                            onDrag = { change, drag ->
                                change.consume()
                                accY += drag.y
                            },
                            onDragEnd = {
                                if (accY > 80f) fullscreenState.value = false
                                accY = 0f
                            },
                            onDragCancel = { accY = 0f },
                        )
                    },
                color = bg,
                contentColor = onBg,
            ) {
                androidx.compose.material3.MaterialTheme(colorScheme = tinted) {
                    // Immersive fullscreen: blurred/dimmed artwork fills the background
                    Box(modifier = Modifier.fillMaxSize()) {
                        val fsEpisode = player.nowPlaying
                        if (fsEpisode != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                    .data(fsEpisode.artworkUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { alpha = 0.15f },
                            )
                        }
                        Column(modifier = Modifier.statusBarsPadding()) {
                        // Thin dismiss hint bar instead of a drag handle
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(onBg.copy(alpha = 0.15f)),
                            )
                        }
                        FullPlayerContent(
                            haptics = haptics,
                            player = player,
                            onOpenShow = onOpenShow,
                            onOpenAuthor = onOpenAuthor,
                            onOpenQueue = onOpenQueue,
                            onOpenTranscript = onOpenTranscript,
                            onOpenChapters = onOpenChapters,
                        )
                        } // end Column inside Box
                    } // end Box (immersive bg)
                }
            }
        }
    } else {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = bg,
            contentColor = onBg,
            dragHandle = {
                // Swipe up on the handle area to enter full-screen mode.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .pointerInput(fullscreenState) {
                            var accY = 0f
                            detectDragGestures(
                                onDragStart = { accY = 0f },
                                onDrag = { change, drag ->
                                    change.consume()
                                    accY += drag.y
                                },
                                onDragEnd = {
                                    if (accY < -24f) fullscreenState.value = true
                                    accY = 0f
                                },
                                onDragCancel = { accY = 0f },
                            )
                        },
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
                    onOpenQueue = onOpenQueue,
                    onOpenTranscript = onOpenTranscript,
                    onOpenChapters = onOpenChapters,
                )
            }
        }
    }
}

@Composable
private fun FullPlayerContent(
    haptics: androidx.compose.ui.hapticfeedback.HapticFeedback,
    player: com.material.podcast.ui.viewmodel.PlayerViewModel,
    onOpenShow: (String) -> Unit,
    onOpenAuthor: (String) -> Unit,
    onOpenQueue: () -> Unit,
    onOpenTranscript: () -> Unit,
    onOpenChapters: () -> Unit = {},
) {
    val episode = player.nowPlaying ?: return

    var dragging by remember { mutableStateOf(false) }
    var scrubValue by remember { mutableFloatStateOf(0f) }
    val sliderValue = if (dragging) scrubValue else player.progress
    // Read LibraryStore.moments directly (it is a SnapshotStateList) so the seek bar
    // markers update live as moments are added — without needing to toggle the sheet.
    val momentFractions = LibraryStore.moments
        .filter { it.episodeGuid == episode.guid }
        .map { it.positionMs.toFloat() / player.durationMs.coerceAtLeast(1L) }

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
    // Track the total sleep timer duration so we can show a countdown ring
    var sleepTimerTotalMs by remember { mutableLongStateOf(0L) }
    var showAddMoment by remember { mutableStateOf(false) }
    var showSnip by remember { mutableStateOf(false) }
    var snipExporting by remember { mutableStateOf(false) }
    var showAddToPlaylist by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    if (showQueueSheet) {
        QueueBottomSheet(
            player = player,
            haptics = haptics,
            onDismiss = { showQueueSheet = false },
        )
    }

    if (showAddToPlaylist) {
        AddToPlaylistDialog(
            onPick = { playlistId ->
                LibraryStore.addToPlaylist(playlistId, episode)
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                showAddToPlaylist = false
                android.widget.Toast.makeText(context, "Çalma listesine eklendi", android.widget.Toast.LENGTH_SHORT).show()
            },
            onCreate = { name ->
                val pl = LibraryStore.createPlaylist(name)
                LibraryStore.addToPlaylist(pl.id, episode)
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                showAddToPlaylist = false
                android.widget.Toast.makeText(context, "\"$name\" listesine eklendi", android.widget.Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showAddToPlaylist = false },
        )
    }

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

    val startSnip: (Long, Long) -> Unit = { startMs, endMs ->
        showSnip = false
        snipExporting = true
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        com.material.podcast.media.SnipExporter.export(context, episode, startMs, endMs) { file ->
            snipExporting = false
            if (file != null) {
                com.material.podcast.media.SnipExporter.share(context, file, episode)
            } else {
                android.widget.Toast.makeText(context, "Snip oluşturulamadı", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (showSnip) {
        val durationSec = (player.durationMs / 1000f).coerceAtLeast(1f)
        val curSec = (player.positionMs / 1000f).coerceIn(0f, durationSec)
        // Default selection: ~30s ending at the current position.
        var snipRange by remember {
            mutableStateOf((curSec - 30f).coerceAtLeast(0f)..curSec)
        }
        val startSel = snipRange.start
        val endSel = snipRange.endInclusive
        val lengthSel = (endSel - startSel).toInt()

        AlertDialog(
            onDismissRequest = { showSnip = false },
            title = { Text("Snip oluştur") },
            text = {
                Column {
                    Text(
                        "Paylaşılacak bölümü seç, dilersen önce dinle.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            formatTimeSec(startSel.toInt()),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            "$lengthSel sn",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            formatTimeSec(endSel.toInt()),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    androidx.compose.material3.RangeSlider(
                        value = snipRange,
                        onValueChange = { range ->
                            var s = range.start
                            var e = range.endInclusive
                            // Constrain selected length to 5..90 seconds.
                            val len = e - s
                            if (len < 5f) {
                                if (s == snipRange.start) e = (s + 5f).coerceAtMost(durationSec)
                                else s = (e - 5f).coerceAtLeast(0f)
                            } else if (len > 90f) {
                                if (s == snipRange.start) e = s + 90f
                                else s = e - 90f
                            }
                            snipRange = s..e
                        },
                        valueRange = 0f..durationSec,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            player.seekToMs((startSel * 1000L).toLong())
                            if (!player.isPlaying) player.togglePlayPause()
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Icon(Icons.Rounded.PlayArrow, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Önizle")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    startSnip((startSel * 1000L).toLong(), (endSel * 1000L).toLong())
                }) { Text("Oluştur") }
            },
            dismissButton = { TextButton(onClick = { showSnip = false }) { Text("İptal") } },
        )
    }

    if (snipExporting) {
        AlertDialog(
            onDismissRequest = { },
            confirmButton = { },
            title = { Text("Snip hazırlanıyor…") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Lütfen bekleyin")
                }
            },
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

        Spacer(Modifier.height(16.dp))

        // Current chapter chip with AnimatedContent cross-fade on chapter changes.
        if (player.chapters.isNotEmpty()) {
            val chapterIdx = player.currentChapterIndex
            val chapterTitle = player.chapters.getOrNull(chapterIdx)?.title
            AnimatedContent(
                targetState = chapterTitle,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                label = "chapterChip",
            ) { title ->
                if (title != null) {
                    Surface(
                        onClick = onOpenChapters,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(bottom = 8.dp),
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.FormatListBulleted, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                title,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                } else {
                    // Empty placeholder to keep layout stable
                    Box(Modifier.height(0.dp))
                }
            }
        }

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
            // NFC ile bu podcast'i paylaş
            IconButton(onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                com.material.podcast.nfc.NfcShareController.startShare(
                    com.material.podcast.nfc.NfcShareController.SharePayload(
                        id = episode.podcastId.ifBlank { episode.guid },
                        title = episode.podcastTitle.ifBlank { episode.title },
                        author = episode.podcastAuthor,
                        artworkUrl = episode.artworkUrl,
                        feedUrl = "",
                    ),
                )
            }) {
                Icon(
                    Icons.Rounded.Contactless,
                    "NFC ile paylaş",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
            momentFractions = momentFractions,
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

        // Speed control — just the current value above a fine-tune slider.
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Hız: ${"%.2f".format(player.playbackSpeed)}×",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Slider(
                value = player.playbackSpeed,
                onValueChange = { player.setSpeed(it) },
                onValueChangeFinished = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
                valueRange = 0.5f..2.0f,
                steps = 5,
                // Slightly shorter than the default to reduce its footprint.
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
            )
        }

        // Mini stats row: duration · publish date · genre
        val podcastGenre = remember(episode.podcastId) {
            LibraryStore.followedPodcasts.find { it.id == episode.podcastId }?.genre.orEmpty()
        }
        val statParts = buildList {
            if (episode.durationLabel.isNotBlank() && episode.durationSeconds > 0) add(episode.durationLabel)
            if (episode.publishedDate.isNotBlank()) add(episode.publishedDate)
            if (podcastGenre.isNotBlank()) add(podcastGenre)
        }
        if (statParts.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = statParts.joinToString("  ·  "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(8.dp))

        // Smart audio — skip silences + voice boost (handled in the playback service)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            FilterChip(
                selected = player.skipSilence,
                onClick = {
                    player.toggleSkipSilence()
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
                label = { Text("Sessizlikleri atla") },
                leadingIcon = if (player.skipSilence) {
                    { Icon(Icons.Rounded.Check, null, Modifier.size(16.dp)) }
                } else null,
            )
            FilterChip(
                selected = player.voiceBoost,
                onClick = {
                    player.toggleVoiceBoost()
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
                label = { Text("Ses yükselt") },
                leadingIcon = if (player.voiceBoost) {
                    { Icon(Icons.Rounded.Check, null, Modifier.size(16.dp)) }
                } else null,
            )
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
                                        sleepTimerTotalMs = minutes * 60_000L
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
            if (player.chapters.isNotEmpty()) {
                IconButton(onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onOpenChapters()
                }) {
                    Icon(
                        Icons.Rounded.FormatListBulleted,
                        "Bölümler",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            // Always available — the transcript screen shows a clear "not found" state with a
            // close button when the episode has no transcript.
            IconButton(onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onOpenTranscript()
            }) {
                Icon(
                    Icons.Rounded.Subject,
                    "Transkript",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = {
                showAddToPlaylist = true
            }) {
                Icon(
                    Icons.Rounded.PlaylistAdd,
                    "Çalma listesine ekle",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = {
                showSleepTimer = !showSleepTimer
            }) {
                val timerActive = player.sleepTimerMs > 0 || player.sleepAtEnd || showSleepTimer
                Box(contentAlignment = Alignment.Center) {
                    // Animated countdown ring when sleep timer is running
                    if (player.sleepTimerMs > 0 && sleepTimerTotalMs > 0) {
                        val timerProgress = (player.sleepTimerMs.toFloat() / sleepTimerTotalMs.toFloat()).coerceIn(0f, 1f)
                        CircularProgressIndicator(
                            progress = { timerProgress },
                            modifier = Modifier.size(34.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        )
                    }
                    Icon(
                        Icons.Rounded.Timer,
                        "Uyku zamanlayıcı",
                        tint = if (timerActive) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = {
                showSnip = true
            }) {
                Icon(
                    Icons.Rounded.ContentCut,
                    "Snip oluştur",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                showQueueSheet = true
            }) {
                Icon(
                    Icons.Rounded.QueueMusic,
                    "Sıradakiler",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun QueueBottomSheet(
    player: com.material.podcast.ui.viewmodel.PlayerViewModel,
    haptics: androidx.compose.ui.hapticfeedback.HapticFeedback,
    onDismiss: () -> Unit,
) {
    val queueSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val upNext = player.upNext
    val history = player.history

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = queueSheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            Text(
                "Sıra Listesi",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp),
            ) {
                player.nowPlaying?.let { current ->
                    item(key = "q_now_header") { QueueSubHeader("Şu an çalıyor") }
                    item(key = "q_now_${current.guid}") {
                        QueueRow(current, isCurrent = true) {}
                    }
                }

                item(key = "q_up_header") { QueueSubHeader("Sıradakiler") }
                if (upNext.isEmpty()) {
                    item(key = "q_up_empty") {
                        Text(
                            "Bu bölümden sonrası yok",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                } else {
                    items(upNext, key = { "q_up_${it.guid}" }) { ep ->
                        QueueRow(ep, isCurrent = false) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            player.playQueueIndex(player.queue.indexOfFirst { q -> q.guid == ep.guid })
                        }
                    }
                }

                if (history.isNotEmpty()) {
                    item(key = "q_hist_header") { QueueSubHeader("Önceki dinlediklerim") }
                    items(history, key = { "q_hist_${it.guid}" }) { ep ->
                        QueueRow(ep, isCurrent = ep.guid == player.nowPlaying?.guid) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            player.play(ep)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
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
            state?.status == DownloadStatus.Downloading || state?.status == DownloadStatus.Queued -> {} // already running
            else -> dm.download(episode)
        }
    }) {
        when {
            downloaded -> Icon(
                Icons.Rounded.DownloadDone, "İndirildi (kaldırmak için dokun)",
                tint = MaterialTheme.colorScheme.primary,
            )
            state?.status == DownloadStatus.Downloading || state?.status == DownloadStatus.Queued -> {
                val dlProgress = if (state.status == DownloadStatus.Downloading) state.progress.coerceIn(0f, 1f) else 0f
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(36.dp)) {
                    // Filled background circle
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                    )
                    // Progress ring on top
                    CircularProgressIndicator(
                        progress = { dlProgress },
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.secondaryContainer,
                    )
                    // Percentage text centered inside
                    Text(
                        "${(dlProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = androidx.compose.ui.unit.TextUnit(8f, androidx.compose.ui.unit.TextUnitType.Sp)
                        ),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
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

@Composable
internal fun AddToPlaylistDialog(
    onPick: (String) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val playlists = LibraryStore.playlists
    var creating by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Çalma listesine ekle") },
        text = {
            Column {
                if (creating) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        placeholder = { Text("Liste adı") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    if (playlists.isEmpty()) {
                        Text(
                            "Henüz çalma listen yok. Yeni bir tane oluştur.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                            items(playlists, key = { it.id }) { pl ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onPick(pl.id) }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Icons.Rounded.QueueMusic, null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(pl.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(
                                            "${pl.episodes.size} bölüm",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (creating) {
                TextButton(onClick = { onCreate(newName) }, enabled = newName.isNotBlank()) { Text("Oluştur ve ekle") }
            } else {
                TextButton(onClick = { creating = true }) { Text("Yeni liste") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } },
    )
}

private fun formatTimeSec(totalSec: Int): String {
    val s = totalSec.coerceAtLeast(0)
    val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}
