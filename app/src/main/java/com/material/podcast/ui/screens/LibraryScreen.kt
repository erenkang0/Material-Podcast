package com.material.podcast.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.material.podcast.data.MockData
import com.material.podcast.data.Podcast
import com.material.podcast.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(onNavigateToPlayer: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Podcasts", "Episodes", "Downloads")

    val tabOffsetX by animateFloatAsState(
        targetValue = selectedTab.toFloat(),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "tabOffset"
    )

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .padding(bottom = 80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(Violet40, Pink60)))
                    .clickable {}
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Explore, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Discover More", style = MaterialTheme.typography.labelLarge, color = Color.White)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Your Library",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
                    color = OnSurface
                )
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceContainer)
                        .clickable {},
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Add, null, tint = Violet60, modifier = Modifier.size(22.dp))
                }
            }

            Spacer(Modifier.height(20.dp))

            // Custom tab row
            Row(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceContainer)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                tabs.forEachIndexed { index, label ->
                    val isSelected = selectedTab == index
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1f else 0.95f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                        label = "tabScale$index"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .graphicsLayer { scaleX = scale; scaleY = scale }
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) Brush.linearGradient(listOf(Violet40, Pink60))
                                else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                            )
                            .clickable { selectedTab = index }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) Color.White else OnSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            when (selectedTab) {
                0 -> PodcastsTab(onNavigateToPlayer)
                1 -> EpisodesTab(onNavigateToPlayer)
                2 -> DownloadsTab()
            }
        }
    }
}

@Composable
fun PodcastsTab(onNavigateToPlayer: () -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(bottom = 100.dp)
    ) {
        items(MockData.libraryPodcasts) { podcast ->
            LibraryPodcastItem(podcast = podcast, onPlay = onNavigateToPlayer)
        }
    }
}

@Composable
fun LibraryPodcastItem(podcast: Podcast, onPlay: () -> Unit) {
    var subscribed by remember { mutableStateOf(podcast.isSubscribed) }
    val color = Color(podcast.colorHex)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceContainer)
            .clickable { onPlay() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(listOf(color, color.copy(0.5f)))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Podcasts, null, tint = Color.White, modifier = Modifier.size(32.dp))
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(podcast.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = OnSurface)
            Text(podcast.author, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.MicNone, null, tint = color, modifier = Modifier.size(12.dp))
                Text("${podcast.episodeCount} episodes", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
            }
        }

        val subScale by animateFloatAsState(
            targetValue = if (subscribed) 1.1f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium),
            label = "subScale"
        )
        Box(
            modifier = Modifier
                .graphicsLayer { scaleX = subScale; scaleY = subScale }
                .clip(RoundedCornerShape(12.dp))
                .background(if (subscribed) Brush.linearGradient(listOf(Violet40, Pink60)) else Brush.linearGradient(listOf(SurfaceContainerHigh, SurfaceContainerHigh)))
                .clickable { subscribed = !subscribed }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                if (subscribed) "Following" else "Follow",
                style = MaterialTheme.typography.labelMedium,
                color = if (subscribed) Color.White else OnSurfaceVariant
            )
        }
    }
}

@Composable
fun EpisodesTab(onNavigateToPlayer: () -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(bottom = 100.dp)
    ) {
        items(MockData.recentEpisodes) { episode ->
            val color = Color(episode.colorHex)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceContainer)
                    .clickable { onNavigateToPlayer() }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(color, color.copy(0.4f)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Headphones, null, tint = Color.White, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(episode.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = OnSurface, maxLines = 1)
                    Text(episode.podcastTitle, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { episode.progress },
                        modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                        color = color,
                        trackColor = SurfaceContainerHigh
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(episode.duration, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { onNavigateToPlayer() }) {
                    Icon(Icons.Rounded.PlayCircle, null, tint = color, modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}

@Composable
fun DownloadsTab() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Rounded.DownloadDone,
                null,
                tint = Violet60,
                modifier = Modifier.size(72.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "No downloads yet",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = OnSurface
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Downloaded episodes appear here\nfor offline listening",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant
            )
        }
    }
}
