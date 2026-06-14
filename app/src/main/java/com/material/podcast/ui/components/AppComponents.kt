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
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.material.podcast.data.Category
import com.material.podcast.data.Episode
import com.material.podcast.data.Show
import com.material.podcast.ui.state.PlayerState

/** A gentle spring used for press / selection feedback across the app. */
private val ExpressiveSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMediumLow,
)

/**
 * Cover-art stand-in: a rounded [androidx.compose.foundation.layout.Box] filled with a tonal
 * surface and a centered Material icon. No images are ever used — this represents artwork
 * structurally while staying fully on-theme.
 */
@Composable
fun CoverArt(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = MaterialTheme.shapes.medium,
    container: Color = MaterialTheme.colorScheme.surfaceVariant,
    content: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    iconFraction: Float = 0.42f,
) {
    BoxWithConstraints(
        modifier = modifier
            .clip(shape)
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        val iconSize = (if (maxWidth < maxHeight) maxWidth else maxHeight) * iconFraction
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(iconSize),
        )
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
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f),
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

/** Smoothly morphs between play and pause glyphs. */
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

/** Three little bars that dance while something is playing. */
@Composable
fun EqualizerBars(
    active: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "equalizer")
    val a by transition.animateFloat(
        0.35f, 1f, infiniteRepeatable(tween(420), RepeatMode.Reverse), label = "a"
    )
    val b by transition.animateFloat(
        1f, 0.4f, infiniteRepeatable(tween(560), RepeatMode.Reverse), label = "b"
    )
    val c by transition.animateFloat(
        0.5f, 1f, infiniteRepeatable(tween(340), RepeatMode.Reverse), label = "c"
    )
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        listOf(a, b, c).forEach { value ->
            val factor = if (active) value else 0.3f
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(factor)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

@Composable
private fun rememberPressScale(interaction: MutableInteractionSource): Float {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = ExpressiveSpring,
        label = "pressScale",
    )
    return scale
}

@Composable
fun FeaturedCard(
    show: Show,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction)
    ElevatedCard(
        onClick = onClick,
        interactionSource = interaction,
        shape = MaterialTheme.shapes.large,
        modifier = modifier
            .width(184.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale },
    ) {
        Column(Modifier.padding(12.dp)) {
            CoverArt(
                icon = show.icon,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                container = MaterialTheme.colorScheme.primaryContainer,
                content = MaterialTheme.colorScheme.onPrimaryContainer,
                iconFraction = 0.46f,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = show.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = show.author,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun CategoryListItem(
    category: Category,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = {
            Text(category.name, style = MaterialTheme.typography.titleMedium)
        },
        supportingContent = { Text(category.supporting) },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp),
                )
            }
        },
        trailingContent = {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
    )
}

@Composable
fun EpisodeListItem(
    episode: Episode,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        ListItem(
            headlineContent = {
                Text(
                    episode.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            supportingContent = { Text("${episode.date} · ${episode.duration}") },
            leadingContent = {
                CoverArt(
                    icon = episode.icon,
                    modifier = Modifier.size(52.dp),
                    shape = MaterialTheme.shapes.small,
                )
            },
            trailingContent = {
                FilledTonalIconButton(onClick = onPlay) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = "Play episode")
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
        if (episode.progress > 0f) {
            LinearProgressIndicator(
                progress = { episode.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(4.dp)
                    .clip(CircleShape),
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
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.05f else 1f,
        animationSpec = ExpressiveSpring,
        label = "chipScale",
    )
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (selected) {
            { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
        } else null,
        shape = MaterialTheme.shapes.small,
        colors = FilterChipDefaults.filterChipColors(),
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale },
    )
}

@Composable
fun LibraryShowCard(
    show: Show,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction)
    ElevatedCard(
        onClick = onClick,
        interactionSource = interaction,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale },
    ) {
        Column(Modifier.padding(12.dp)) {
            CoverArt(
                icon = show.icon,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                container = MaterialTheme.colorScheme.tertiaryContainer,
                content = MaterialTheme.colorScheme.onTertiaryContainer,
                iconFraction = 0.44f,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = show.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${show.episodes} episodes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Compact, tappable now-playing bar that floats above the navigation bar. */
@Composable
fun MiniPlayer(
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onExpand,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CoverArt(
                    icon = Icons.Rounded.Headphones,
                    modifier = Modifier.size(44.dp),
                    shape = MaterialTheme.shapes.small,
                    container = MaterialTheme.colorScheme.primary,
                    content = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = PlayerState.episodeTitle,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = PlayerState.showTitle,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                EqualizerBars(
                    active = PlayerState.isPlaying,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .height(18.dp)
                        .width(20.dp),
                )
                FilledIconButton(onClick = { PlayerState.togglePlayPause() }) {
                    PlayPauseIcon(
                        isPlaying = PlayerState.isPlaying,
                        contentDescription = if (PlayerState.isPlaying) "Pause" else "Play",
                    )
                }
            }
            LinearProgressIndicator(
                progress = { PlayerState.progress },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .height(3.dp)
                    .clip(CircleShape),
            )
        }
    }
}

/** The transport row on the Now Playing screen, with a large springy play/pause button. */
@Composable
fun PlaybackControls(
    isPlaying: Boolean,
    onToggle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = ExpressiveSpring,
        label = "playScale",
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onSeekBack) {
            Icon(
                Icons.Rounded.Replay10,
                contentDescription = "Rewind 10 seconds",
                modifier = Modifier.size(28.dp),
            )
        }
        IconButton(onClick = onPrevious) {
            Icon(
                Icons.Rounded.SkipPrevious,
                contentDescription = "Previous",
                modifier = Modifier.size(36.dp),
            )
        }
        FilledIconButton(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggle()
            },
            interactionSource = interaction,
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier
                .size(84.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale },
        ) {
            PlayPauseIcon(
                isPlaying = isPlaying,
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.size(40.dp),
            )
        }
        IconButton(onClick = onNext) {
            Icon(
                Icons.Rounded.SkipNext,
                contentDescription = "Next",
                modifier = Modifier.size(36.dp),
            )
        }
        IconButton(onClick = onSeekForward) {
            Icon(
                Icons.Rounded.Forward10,
                contentDescription = "Forward 10 seconds",
                modifier = Modifier.size(28.dp),
            )
        }
    }
}
