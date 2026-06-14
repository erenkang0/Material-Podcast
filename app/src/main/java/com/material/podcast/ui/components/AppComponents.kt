@file:OptIn(ExperimentalMaterial3Api::class)

package com.material.podcast.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Forward30
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.material.podcast.data.model.Podcast
import com.material.podcast.data.model.PodcastEpisode
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
    if (imageUrl.isNotBlank()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(shape),
        )
    } else {
        Box(
            modifier = modifier
                .clip(shape)
                .background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
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

@Composable
fun EpisodeListItem(
    episode: PodcastEpisode,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
    showProgress: Boolean = false,
    progressFraction: Float = 0f,
) {
    val player = LocalPlayer.current
    val isCurrentEpisode = player.nowPlaying?.guid == episode.guid
    Column(modifier) {
        ListItem(
            headlineContent = {
                Text(
                    episode.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isCurrentEpisode) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                )
            },
            supportingContent = {
                Column {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${episode.publishedDate} · ${episode.durationLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (episode.description.isNotBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            episode.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                }
            },
            trailingContent = {
                FilledTonalIconButton(onClick = onPlay) {
                    PlayPauseIcon(
                        isPlaying = isCurrentEpisode && player.isPlaying,
                        contentDescription = "Play",
                    )
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
        if (showProgress && progressFraction > 0f) {
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
