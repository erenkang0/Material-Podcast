package com.material.podcast.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.material.podcast.data.Episode
import com.material.podcast.data.MockData
import com.material.podcast.data.Podcast
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(onNavigateToPlayer: () -> Unit) {
    var animateIn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        animateIn = true
    }

    val headerAlpha by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "headerAlpha"
    )
    val headerTranslation by animateFloatAsState(
        targetValue = if (animateIn) 0f else 40f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "headerTranslation"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // Greeting header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .graphicsLayer {
                        alpha = headerAlpha
                        translationY = headerTranslation
                    }
            ) {
                Text(
                    text = "Good evening, Eren 👋",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = Color(0xFFF3EEFF)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "What are you listening to today?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFCBC2DC)
                )
            }
        }

        // Now Playing mini banner
        item {
            var bannerVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { delay(200); bannerVisible = true }
            val bannerAlpha by animateFloatAsState(
                targetValue = if (bannerVisible) 1f else 0f,
                animationSpec = tween(600),
                label = "bannerAlpha"
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .graphicsLayer { alpha = bannerAlpha }
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onNavigateToPlayer() },
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Transparent,
                    tonalElevation = 0.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF7C4DFF).copy(alpha = 0.4f),
                                        Color(0xFFE040FB).copy(alpha = 0.3f)
                                    )
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF7C4DFF), Color(0xFFE040FB))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MusicNote,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "The Fabric of Time",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "Cosmos Unfolded",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFCBC2DC)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x40FFFFFF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.GraphicEq,
                                    contentDescription = "Playing",
                                    tint = Color(0xFF7C4DFF),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Featured section header
        item {
            SectionHeader(title = "Featured", onSeeAll = {})
        }

        // Featured podcasts horizontal scroll
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(MockData.featuredPodcasts) { podcast ->
                    FeaturedPodcastCard(podcast = podcast, onClick = onNavigateToPlayer)
                }
            }
            Spacer(modifier = Modifier.height(28.dp))
        }

        // Recently Played section header
        item {
            SectionHeader(title = "Recently Played", onSeeAll = {})
        }

        // Recent episodes horizontal scroll
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(MockData.recentEpisodes) { episode ->
                    RecentEpisodeCard(episode = episode, onClick = onNavigateToPlayer)
                }
            }
            Spacer(modifier = Modifier.height(28.dp))
        }

        // Trending section header
        item {
            SectionHeader(title = "Trending", onSeeAll = {})
        }

        // Trending podcast items
        items(MockData.trendingPodcasts) { podcast ->
            TrendingPodcastItem(podcast = podcast, onClick = onNavigateToPlayer)
        }
    }
}

@Composable
private fun SectionHeader(title: String, onSeeAll: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = Color(0xFFF3EEFF)
        )
        TextButton(onClick = onSeeAll) {
            Text(
                "See all",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF9C77FF)
            )
        }
    }
}

@Composable
private fun FeaturedPodcastCard(podcast: Podcast, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "cardScale"
    )

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(podcast.id) {
        delay(50L * podcast.id.hashCode().coerceIn(0, 4))
        visible = true
    }
    val cardAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "cardAlpha"
    )
    val cardTranslation by animateFloatAsState(
        targetValue = if (visible) 0f else 60f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "cardTranslation"
    )

    val podcastColor = Color(podcast.colorHex)

    Box(
        modifier = Modifier
            .size(width = 280.dp, height = 360.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = cardAlpha
                translationY = cardTranslation
            }
            .clip(
                RoundedCornerShape(
                    topStart = 28.dp,
                    topEnd = 12.dp,
                    bottomStart = 12.dp,
                    bottomEnd = 28.dp
                )
            )
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D0714),
                        podcastColor.copy(alpha = 0.6f),
                        podcastColor.copy(alpha = 0.9f)
                    )
                )
            )
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
    ) {
        // Art area — top 60%
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(216.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            podcastColor.copy(alpha = 0.7f),
                            Color(0xFF0D0714).copy(alpha = 0.5f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(80.dp)
            )
        }

        // Bottom info area
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF0D0714).copy(alpha = 0.95f)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            // Category chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(podcastColor.copy(alpha = 0.3f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = podcast.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = podcastColor
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = podcast.title,
                style = MaterialTheme.typography.displaySmall.copy(fontSize = MaterialTheme.typography.headlineMedium.fontSize),
                color = Color.White,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = podcast.author,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFCBC2DC)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${podcast.episodeCount} episodes",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFCBC2DC)
                )
                // Duration badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x40FFFFFF))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.AccessTime,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = podcast.duration,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentEpisodeCard(episode: Episode, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "episodeCardScale"
    )

    val episodeColor = Color(episode.colorHex)

    Box(
        modifier = Modifier
            .size(width = 160.dp, height = 200.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(
                RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 8.dp,
                    bottomStart = 8.dp,
                    bottomEnd = 20.dp
                )
            )
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        episodeColor.copy(alpha = 0.4f),
                        Color(0xFF1C1528)
                    )
                )
            )
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Art placeholder
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(episodeColor, episodeColor.copy(alpha = 0.5f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column {
                Text(
                    text = episode.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = episode.podcastTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFCBC2DC),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(Color(0x40FFFFFF))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(episode.progress)
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(episodeColor, Color(0xFFE040FB))
                                )
                            )
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = episode.duration,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFCBC2DC)
                    )
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(episodeColor.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Play",
                            tint = episodeColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendingPodcastItem(podcast: Podcast, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "trendingScale"
    )

    val podcastColor = Color(podcast.colorHex)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1C1528))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Colored avatar
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(podcastColor, podcastColor.copy(alpha = 0.5f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = podcast.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFFF3EEFF)
                )
                Text(
                    text = podcast.author,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCBC2DC)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(podcastColor.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = podcast.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = podcastColor
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x30FFFFFF))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = podcast.duration,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFCBC2DC)
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${podcast.episodeCount}",
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize),
                    color = podcastColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "eps",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFCBC2DC)
                )
            }
        }
    }
}
