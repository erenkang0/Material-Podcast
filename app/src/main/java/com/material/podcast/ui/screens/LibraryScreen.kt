@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.material.podcast.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.material.podcast.EchoesApplication
import com.material.podcast.data.model.FavoriteMoment
import com.material.podcast.data.model.PodcastEpisode
import com.material.podcast.data.store.LibraryStore
import com.material.podcast.media.DownloadStatus
import com.material.podcast.ui.LocalPlayer
import com.material.podcast.ui.components.ContinueListeningCard
import com.material.podcast.ui.components.EpisodeListItem
import com.material.podcast.ui.components.LibraryPodcastCard
import com.material.podcast.ui.components.PodcastArtwork
import kotlinx.coroutines.launch

private val tabs = listOf("Kaydedilenler", "Beğeniler", "Notlar", "İndirilenler", "Son Dinlenenler")

@Composable
fun LibraryScreen(
    onOpenShow: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenThemes: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenStats: () -> Unit = {},
    onOpenPlaylists: () -> Unit = {},
) {
    val pagerState = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()
    val player = LocalPlayer.current
    val resume = LibraryStore.lastResume()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Kitaplığım",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenPlaylists) {
                        Icon(Icons.AutoMirrored.Rounded.QueueMusic, "Çalma listeleri")
                    }
                    IconButton(onClick = onOpenStats) {
                        Icon(Icons.Rounded.BarChart, "İstatistikler")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Rounded.Settings, "Ayarlar")
                    }
                    IconButton(onClick = onOpenThemes) {
                        Icon(Icons.Rounded.Palette, "Tema")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
        ) {
            if (resume != null) {
                ContinueListeningCard(
                    point = resume,
                    onResume = { player.resume(resume); player.expandSheet = true },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                edgePadding = 16.dp,
                divider = {},
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = {
                            Text(
                                title,
                                fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) { page ->
                when (page) {
                    0 -> SavedPodcastsTab(onOpenShow = onOpenShow)
                    1 -> LikedEpisodesTab()
                    2 -> MomentsTab()
                    3 -> DownloadsTab()
                    else -> RecentEpisodesTab()
                }
            }
        }
    }
}

// ─── Saved ──────────────────────────────────────────────────────────────────

@Composable
private fun SavedPodcastsTab(onOpenShow: (String) -> Unit) {
    val saved = LibraryStore.followedPodcasts
    if (saved.isEmpty()) {
        EmptyTabContent(
            icon = Icons.Rounded.Explore,
            title = "Henüz takip edilen podcast yok",
            subtitle = "Keşfet'e giderek ilginizi çeken podcastleri takip edin.",
        )
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }, key = "header") {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${saved.size} podcast takip ediliyor",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(saved, key = { it.id }) { podcast ->
                LibraryPodcastCard(podcast = podcast, onClick = { onOpenShow(podcast.id) })
            }
        }
    }
}

// ─── Liked ──────────────────────────────────────────────────────────────────

@Composable
private fun LikedEpisodesTab() {
    val player = LocalPlayer.current
    val liked = LibraryStore.likedEpisodes
    if (liked.isEmpty()) {
        EmptyTabContent(
            icon = Icons.Rounded.MusicNote,
            title = "Henüz beğenilen bölüm yok",
            subtitle = "Now Playing ekranında kalp simgesine dokunarak beğenin.",
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp),
        ) {
            items(liked, key = { it.guid }) { episode ->
                Column {
                    EpisodeListItem(
                        episode = episode,
                        onPlay = { player.play(episode); player.expandSheet = true },
                    )
                    if (LibraryStore.hasNotes(episode.guid)) {
                        AssistChip(
                            onClick = { },
                            label = { Text("Notlu") },
                            leadingIcon = {
                                Icon(Icons.Rounded.Bookmark, null, Modifier.size(AssistChipDefaults.IconSize))
                            },
                            modifier = Modifier.padding(start = 16.dp, bottom = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

// ─── Moments ────────────────────────────────────────────────────────────────

@Composable
private fun MomentsTab() {
    val player = LocalPlayer.current
    val moments = LibraryStore.moments
    if (moments.isEmpty()) {
        EmptyTabContent(
            icon = Icons.Rounded.Bookmark,
            title = "Henüz favori an yok",
            subtitle = "Now Playing'de yer imi simgesiyle anları kaydedin.",
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(moments, key = { it.id }) { moment ->
                MomentRow(
                    moment = moment,
                    onPlay = {
                        player.play(moment.toEpisode(), startPositionMs = moment.positionMs)
                        player.expandSheet = true
                    },
                    onDelete = { LibraryStore.removeMoment(moment.id) },
                )
            }
        }
    }
}

@Composable
private fun MomentRow(
    moment: FavoriteMoment,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PodcastArtwork(
                imageUrl = moment.artworkUrl,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.size(56.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    moment.episodeTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    moment.podcastTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "⏱ ${formatClock(moment.positionMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (moment.note.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        moment.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(4.dp))
            FilledTonalIconButton(onClick = onPlay, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Rounded.PlayArrow, "Bu andan oynat", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Rounded.DeleteOutline, "Sil", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ─── Downloads — Apple Music style ──────────────────────────────────────────

@Composable
private fun DownloadsTab() {
    val player = LocalPlayer.current
    val downloads = LibraryStore.downloads
    val dm = EchoesApplication.instance.downloadManager

    // Group downloaded episodes by podcast
    val grouped = remember(downloads.size) {
        downloads.groupBy { it.podcastId.ifBlank { it.podcastTitle } }
    }

    if (grouped.isEmpty()) {
        EmptyTabContent(
            icon = Icons.Rounded.CheckCircle,
            title = "Henüz indirilmiş bölüm yok",
            subtitle = "Now Playing'de indirme simgesine dokunarak bölümleri çevrimdışı kaydedin.",
        )
    } else {
        // Track which podcast sections are expanded
        val expanded = remember { mutableStateMapOf<String, Boolean>().also { map ->
            grouped.keys.firstOrNull()?.let { map[it] = true } // open first section
        } }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            grouped.forEach { (podcastKey, episodes) ->
                val firstEp = episodes.first()
                val isExpanded = expanded[podcastKey] == true

                item(key = "podcast_$podcastKey") {
                    PodcastDownloadSection(
                        podcastTitle = firstEp.podcastTitle,
                        artworkUrl = firstEp.artworkUrl,
                        episodeCount = episodes.size,
                        isExpanded = isExpanded,
                        onToggle = { expanded[podcastKey] = !isExpanded },
                    )
                }

                if (isExpanded) {
                    items(episodes, key = { "dl_${it.guid}" }) { episode ->
                        DownloadEpisodeRow(
                            episode = episode,
                            onPlay = { player.play(episode, episodes); player.expandSheet = true },
                            onDelete = { dm.delete(episode.guid) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PodcastDownloadSection(
    podcastTitle: String,
    artworkUrl: String,
    episodeCount: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit,
) {
    Surface(
        onClick = onToggle,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PodcastArtwork(
                imageUrl = artworkUrl,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.size(52.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    podcastTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "$episodeCount bölüm indirildi",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Icon(
                if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DownloadEpisodeRow(
    episode: PodcastEpisode,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
) {
    val player = LocalPlayer.current
    val isCurrentEpisode = player.nowPlaying?.guid == episode.guid

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (isCurrentEpisode) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onPlay)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Download tick indicator
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Check,
                    "İndirildi",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    episode.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isCurrentEpisode) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isCurrentEpisode) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    episode.publishedDate + if (episode.durationLabel.isNotBlank()) " · ${episode.durationLabel}" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledIconButton(
                onClick = onPlay,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(Icons.Rounded.PlayArrow, "Oynat", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Rounded.DeleteOutline, "Sil",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

// ─── Recent ─────────────────────────────────────────────────────────────────

@Composable
private fun RecentEpisodesTab() {
    val player = LocalPlayer.current
    val history = player.history
    if (history.isEmpty()) {
        EmptyTabContent(
            icon = Icons.Rounded.PlayArrow,
            title = "Henüz dinlenen bölüm yok",
            subtitle = "Bir şeyler çalmaya başladığınızda burada görünür.",
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp),
        ) {
            items(history, key = { it.guid }) { episode ->
                EpisodeListItem(
                    episode = episode,
                    onPlay = { player.play(episode); player.expandSheet = true },
                )
            }
        }
    }
}

// ─── Shared helpers ──────────────────────────────────────────────────────────

@Composable
private fun EmptyTabContent(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon, null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(36.dp),
                )
            }
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun formatClock(ms: Long): String {
    val totalSec = (ms / 1000).toInt().coerceAtLeast(0)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
