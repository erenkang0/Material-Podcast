@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.material.podcast.ui.components

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Downloading
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Forward30
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.material.podcast.EchoesApplication
import com.material.podcast.data.model.Podcast
import com.material.podcast.data.model.PodcastEpisode
import com.material.podcast.data.store.LibraryStore
import com.material.podcast.media.DownloadStatus
import com.material.podcast.ui.LocalPlayer

private val BounceSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMediumLow,
)

/** Shows a real network image; falls back to a Material icon if url is blank. */
@Composable
fun PodcastArtwork(
    imageUrl: String,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    fallbackIcon: ImageVector = Icons.Rounded.Podcasts,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    iconBg: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    // Always render the background so there's no layout shift / flash while Coil loads.
    Box(
        modifier = modifier
            .clip(shape)
            .background(iconBg),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl.isNotBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(250)
                    .memoryCacheKey(imageUrl)
                    .diskCacheKey(imageUrl)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(shape),
            )
        } else {
            Icon(fallbackIcon, contentDescription = null, tint = iconTint)
        }
    }
}

/** Legacy icon-only cover art used where we don't have a URL. */
@Composable
fun CoverArt(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    container: Color = MaterialTheme.colorScheme.surfaceVariant,
    content: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    iconFraction: Float = 0.42f,
) {
    BoxWithConstraints(
        modifier = modifier.clip(shape).background(container),
        contentAlignment = Alignment.Center,
    ) {
        val sz = (if (maxWidth < maxHeight) maxWidth else maxHeight) * iconFraction
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(sz))
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun PlayPauseIcon(
    isPlaying: Boolean,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = isPlaying,
        transitionSpec = {
            (scaleIn(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn()) togetherWith
                (scaleOut(spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut())
        },
        label = "playPause",
    ) { playing ->
        Icon(
            imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = contentDescription,
            modifier = modifier,
        )
    }
}

@Composable
fun EqualizerBars(active: Boolean, color: Color, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "eq")
    val a by t.animateFloat(0.35f, 1f, infiniteRepeatable(tween(420), RepeatMode.Reverse), "a")
    val b by t.animateFloat(1f, 0.4f, infiniteRepeatable(tween(560), RepeatMode.Reverse), "b")
    val c by t.animateFloat(0.5f, 1f, infiniteRepeatable(tween(340), RepeatMode.Reverse), "c")
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
        listOf(a, b, c).forEach { v ->
            val f = if (active) v else 0.3f
            Box(Modifier.width(3.dp).fillMaxHeight(f).clip(CircleShape).background(color))
        }
    }
}

@Composable
private fun rememberPressScale(src: MutableInteractionSource): Float {
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, BounceSpring, label = "press")
    return scale
}

@Composable
fun PodcastCard(
    podcast: Podcast,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val src = remember { MutableInteractionSource() }
    val scale = rememberPressScale(src)
    ElevatedCard(
        onClick = onClick,
        interactionSource = src,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.width(184.dp).graphicsLayer { scaleX = scale; scaleY = scale },
    ) {
        Column(Modifier.padding(12.dp)) {
            PodcastArtwork(
                imageUrl = podcast.artworkUrl,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                podcast.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                podcast.author,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun PodcastListItem(
    podcast: Podcast,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = {
            Text(podcast.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text("${podcast.author} · ${podcast.genre}", maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        leadingContent = {
            PodcastArtwork(
                imageUrl = podcast.artworkUrl,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.size(56.dp),
            )
        },
        trailingContent = {
            Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = modifier.clip(MaterialTheme.shapes.large).clickable(onClick = onClick),
    )
}

/**
 * Parses the "MMM d, yyyy" date string we store in [PodcastEpisode.publishedDate] and returns a
 * concise Turkish relative label ("3 gün önce", "Bugün") for recent items, falling back to an
 * absolute "d MMM yyyy" Turkish date for older ones. Returns the raw string if it can't be parsed.
 */
private fun relativeTurkishDate(raw: String): String {
    if (raw.isBlank()) return ""
    val parsed = runCatching {
        java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.ENGLISH).parse(raw)
    }.getOrNull() ?: return raw
    val now = System.currentTimeMillis()
    val days = ((now - parsed.time) / 86_400_000L).toInt()
    return when {
        days < 0 -> formatTurkishDate(parsed)
        days == 0 -> "Bugün"
        days == 1 -> "Dün"
        days < 7 -> "$days gün önce"
        days < 14 -> "Geçen hafta"
        days < 30 -> "${days / 7} hafta önce"
        days < 60 -> "Geçen ay"
        days < 365 -> "${days / 30} ay önce"
        else -> formatTurkishDate(parsed)
    }
}

private val TR_MONTHS = arrayOf(
    "Oca", "Şub", "Mar", "Nis", "May", "Haz",
    "Tem", "Ağu", "Eyl", "Eki", "Kas", "Ara",
)

private fun formatTurkishDate(date: java.util.Date): String {
    val cal = java.util.Calendar.getInstance().apply { time = date }
    val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
    val month = TR_MONTHS[cal.get(java.util.Calendar.MONTH)]
    val year = cal.get(java.util.Calendar.YEAR)
    return "$day $month $year"
}

/** True if the episode was published within the last [withinDays] days. */
private fun isRecentEpisode(raw: String, withinDays: Int = 3): Boolean {
    if (raw.isBlank()) return false
    val parsed = runCatching {
        java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.ENGLISH).parse(raw)
    }.getOrNull() ?: return false
    val days = (System.currentTimeMillis() - parsed.time) / 86_400_000L
    return days in 0 until withinDays.toLong()
}

/** Small uppercase "YENİ" pill for very recent episodes. */
@Composable
private fun NewBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) {
        Text(
            "YENİ",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

/** Subtle separator dot between metadata fields. */
@Composable
private fun MetaDot() {
    Box(
        Modifier
            .size(3.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)),
    )
}

@Composable
fun EpisodeListItem(
    episode: PodcastEpisode,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
    showProgress: Boolean = false,
    progressFraction: Float = 0f,
    isDownloaded: Boolean = false,
) {
    val player = LocalPlayer.current
    val haptics = LocalHapticFeedback.current
    val isCurrentEpisode = player.nowPlaying?.guid == episode.guid
    var showActionsSheet by remember { mutableStateOf(false) }

    // Per-episode resume state: in-progress fraction + "played" detection. Observing the
    // SnapshotStateList keeps this row live as the user listens elsewhere.
    val resumePoint = LibraryStore.resumePoints.firstOrNull { it.episode.guid == episode.guid }
    val savedFraction = resumePoint?.fraction ?: 0f
    val inProgress = !isCurrentEpisode && savedFraction > 0.02f && savedFraction < 0.98f
    // Download lifecycle for queued/downloading/downloaded badges.
    val dlState = EchoesApplication.instance.downloadManager.states[episode.guid]
    val isQueued = dlState?.status == DownloadStatus.Queued
    val isDownloading = dlState?.status == DownloadStatus.Downloading
    val isNew = remember(episode.guid) { isRecentEpisode(episode.publishedDate) }
    val relativeDate = remember(episode.publishedDate) { relativeTurkishDate(episode.publishedDate) }
    val isPlayed = !isCurrentEpisode && savedFraction >= 0.98f
    val remainingLabel = remember(episode.durationSeconds, savedFraction) {
        if (inProgress) {
            val remainingSec = (episode.durationSeconds * (1f - savedFraction)).toInt().coerceAtLeast(0)
            val m = remainingSec / 60
            if (m >= 1) "$m dk kaldı" else "Az kaldı"
        } else null
    }

    if (showActionsSheet) {
        EpisodeActionsSheet(
            episode = episode,
            onPlay = {
                onPlay()
                showActionsSheet = false
            },
            onDismiss = { showActionsSheet = false },
        )
    }

    // Swipe-to-play: end-to-start swipe triggers onPlay then resets
    val swipeState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onPlay()
                false // don't dismiss — snap back
            } else {
                false
            }
        },
    )

    // If it got stuck at EndToStart, snap it back after a short delay
    LaunchedEffect(swipeState.currentValue) {
        if (swipeState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            delay(300L)
            swipeState.reset()
        }
    }

    SwipeToDismissBox(
        state = swipeState,
        backgroundContent = {
            val progress = swipeState.progress
            val triggered = swipeState.targetValue == SwipeToDismissBoxValue.EndToStart
            val bgAlpha = (progress * 3f).coerceIn(0f, 1f)
            val playGreen = Color(0xFF2E7D32)
            val surfaceColor = MaterialTheme.colorScheme.surface
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(lerp(surfaceColor, playGreen, bgAlpha))
                    .padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = "Oynat",
                        tint = Color.White.copy(alpha = bgAlpha),
                        modifier = Modifier.size(28.dp),
                    )
                    if (triggered) {
                        Text(
                            "Oynat",
                            color = Color.White.copy(alpha = bgAlpha),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .combinedClickable(
                    onClick = {},
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        showActionsSheet = true
                    },
                ),
        ) {
            ListItem(
                headlineContent = {
                    Row(verticalAlignment = Alignment.Top) {
                        if (isNew) {
                            NewBadge(modifier = Modifier.padding(end = 6.dp, top = 1.dp))
                        }
                        Text(
                            episode.title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = when {
                                isCurrentEpisode -> MaterialTheme.colorScheme.primary
                                isPlayed -> MaterialTheme.colorScheme.onSurfaceVariant
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                },
                supportingContent = {
                    Column {
                        Spacer(Modifier.height(3.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            if (isPlayed) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = "Dinlendi",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                            // Primary meta: remaining time while in progress, else relative date.
                            Text(
                                text = remainingLabel ?: relativeDate.ifBlank { episode.publishedDate },
                                style = MaterialTheme.typography.labelMedium,
                                color = if (inProgress) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            MetaDot()
                            Text(
                                text = episode.durationLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (isQueued || isDownloading) {
                                MetaDot()
                                Text(
                                    text = if (isDownloading) "İndiriliyor" else "Sırada",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (episode.description.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                episode.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                leadingContent = {
                    Box {
                        PodcastArtwork(
                            imageUrl = episode.artworkUrl,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.size(56.dp),
                        )
                        if (isCurrentEpisode && player.isPlaying) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                EqualizerBars(
                                    active = true,
                                    color = Color.White,
                                    modifier = Modifier.height(20.dp).width(22.dp),
                                )
                            }
                        }
                        if (isDownloaded && !isCurrentEpisode) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .align(Alignment.BottomEnd)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Rounded.CheckCircle,
                                    "İndirildi",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(12.dp),
                                )
                            }
                        }
                    }
                },
                trailingContent = {
                    FilledTonalIconButton(
                        onClick = onPlay,
                        modifier = Modifier.size(48.dp),
                    ) {
                        if (inProgress) {
                            // Resume affordance: a ring showing saved progress around the play icon.
                            Box(contentAlignment = Alignment.Center) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    progress = { savedFraction },
                                    modifier = Modifier.size(36.dp),
                                    strokeWidth = 2.5.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                )
                                Icon(
                                    Icons.Rounded.PlayArrow,
                                    contentDescription = "Devam et",
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        } else {
                            PlayPauseIcon(
                                isPlaying = isCurrentEpisode && player.isPlaying,
                                contentDescription = if (isPlayed) "Yeniden oynat" else "Oynat",
                            )
                        }
                    }
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
            // In-progress resume bar (when this row isn't the active episode).
            if (inProgress) {
                LinearProgressIndicator(
                    progress = { savedFraction },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(3.dp).clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                )
            }
            if (showProgress && progressFraction > 0f && !isCurrentEpisode && !inProgress) {
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(3.dp).clip(CircleShape),
                )
            }
            if (isCurrentEpisode) {
                LinearProgressIndicator(
                    progress = { player.progress },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(3.dp).clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer,
                )
            }
        }
    }
}

@Composable
private fun EpisodeActionsSheet(
    episode: PodcastEpisode,
    onPlay: () -> Unit,
    onDismiss: () -> Unit,
) {
    val player = LocalPlayer.current
    val dm = EchoesApplication.instance.downloadManager
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var isLiked by remember { mutableStateOf(LibraryStore.isLiked(episode.guid)) }
    val downloaded = LibraryStore.isDownloaded(episode.guid)
    val dlState = dm.states[episode.guid]
    var showPlaylistPicker by remember { mutableStateOf(false) }

    if (showPlaylistPicker) {
        AddToPlaylistDialog(
            onPick = { playlistId ->
                LibraryStore.addToPlaylist(playlistId, episode)
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                showPlaylistPicker = false
                onDismiss()
            },
            onCreate = { name ->
                val pl = LibraryStore.createPlaylist(name)
                LibraryStore.addToPlaylist(pl.id, episode)
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                showPlaylistPicker = false
                onDismiss()
            },
            onDismiss = { showPlaylistPicker = false },
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(Modifier.navigationBarsPadding()) {
            // Episode header
            ListItem(
                headlineContent = {
                    Text(episode.title, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium)
                },
                supportingContent = {
                    Text(episode.podcastTitle, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                leadingContent = {
                    PodcastArtwork(imageUrl = episode.artworkUrl,
                        shape = MaterialTheme.shapes.small, modifier = Modifier.size(48.dp))
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))

            // Play
            ListItem(
                headlineContent = { Text("Oynat") },
                leadingContent = { Icon(Icons.Rounded.PlayArrow, null, tint = MaterialTheme.colorScheme.onSurface) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onPlay()
                },
            )
            // Like
            ListItem(
                headlineContent = { Text(if (isLiked) "Beğenildi" else "Beğen") },
                leadingContent = {
                    Icon(if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        null,
                        tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    isLiked = !isLiked
                    LibraryStore.toggleLike(episode)
                },
            )
            // Add to playlist
            ListItem(
                headlineContent = { Text("Çalma listesine ekle") },
                leadingContent = { Icon(Icons.Rounded.PlaylistAdd, null, tint = MaterialTheme.colorScheme.onSurface) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showPlaylistPicker = true
                },
            )
            // Download / delete
            val isDownloading = dlState?.status == DownloadStatus.Downloading
            ListItem(
                headlineContent = {
                    Text(when {
                        downloaded -> "İndirmeyi sil"
                        isDownloading -> "İndiriliyor…"
                        else -> "İndir"
                    })
                },
                leadingContent = {
                    Icon(
                        if (downloaded) Icons.Rounded.DownloadDone else Icons.Rounded.Download,
                        null,
                        tint = if (downloaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    when {
                        downloaded -> dm.delete(episode.guid)
                        !isDownloading -> dm.download(episode)
                    }
                    onDismiss()
                },
            )
            // Share
            ListItem(
                headlineContent = { Text("Paylaş") },
                leadingContent = { Icon(Icons.Rounded.Share, null, tint = MaterialTheme.colorScheme.onSurface) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "${episode.title}\n${episode.audioUrl}")
                    }
                    context.startActivity(Intent.createChooser(intent, "Paylaş"))
                    onDismiss()
                },
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun LibraryPodcastCard(
    podcast: Podcast,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val src = remember { MutableInteractionSource() }
    val scale = rememberPressScale(src)
    ElevatedCard(
        onClick = onClick,
        interactionSource = src,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale },
    ) {
        Column(Modifier.padding(12.dp)) {
            PodcastArtwork(
                imageUrl = podcast.artworkUrl,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            )
            Spacer(Modifier.height(10.dp))
            Text(podcast.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                podcast.author,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun GenreChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(if (selected) 1.05f else 1f, BounceSpring, label = "chip")
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (selected) {
            { Icon(Icons.Rounded.Check, null, modifier = Modifier.size(18.dp)) }
        } else null,
        shape = MaterialTheme.shapes.small,
        colors = FilterChipDefaults.filterChipColors(),
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale },
    )
}

@Composable
fun MiniPlayer(
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val player = LocalPlayer.current
    val episode = player.nowPlaying ?: return
    Surface(
        onClick = onExpand,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
        modifier = modifier.fillMaxWidth(),
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
                    FilledIconButton(onClick = { player.togglePlayPause() }) {
                        PlayPauseIcon(player.isPlaying, if (player.isPlaying) "Pause" else "Play")
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

@Composable
fun PlaybackControls(
    isPlaying: Boolean,
    isBuffering: Boolean,
    onToggle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.88f else 1f, BounceSpring, label = "playBtn")

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Rounded.SkipPrevious, "Previous", modifier = Modifier.size(34.dp))
        }
        IconButton(onClick = onSeekBack) {
            Icon(Icons.Rounded.Replay10, "Rewind 10s", modifier = Modifier.size(32.dp))
        }
        FilledIconButton(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggle()
            },
            interactionSource = src,
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.size(80.dp).graphicsLayer { scaleX = scale; scaleY = scale },
        ) {
            if (isBuffering) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 3.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                PlayPauseIcon(isPlaying, if (isPlaying) "Pause" else "Play", Modifier.size(38.dp))
            }
        }
        IconButton(onClick = onSeekForward) {
            Icon(Icons.Rounded.Forward30, "Forward 30s", modifier = Modifier.size(32.dp))
        }
        IconButton(onClick = onNext) {
            Icon(Icons.Rounded.SkipNext, "Next", modifier = Modifier.size(34.dp))
        }
    }
}
