@file:OptIn(ExperimentalMaterial3Api::class)

package com.material.podcast.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.material.podcast.EchoesApplication
import com.material.podcast.data.model.FavoriteMoment
import com.material.podcast.data.store.LibraryStore
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
) {
    val pagerState = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()
    val player = LocalPlayer.current
    val resume = LibraryStore.lastResume()

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = {
                    Text("Kitaplığım", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Rounded.Settings, "Ayarlar")
                    }
                    IconButton(onClick = onOpenThemes) {
                        Icon(Icons.Rounded.Palette, "Tema")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenSearch,
                icon = { Icon(Icons.Rounded.Explore, null) },
                text = { Text("Keşfet") },
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
                edgePadding = 12.dp,
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(title) },
                    )
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) { page ->
                when (page) {
                    0 -> SavedPodcastsGrid(onOpenShow = onOpenShow)
                    1 -> LikedEpisodesTab()
                    2 -> MomentsTab()
                    3 -> DownloadsTab()
                    else -> RecentEpisodesTab()
                }
            }
        }
    }
}

@Composable
private fun SavedPodcastsGrid(onOpenShow: (String) -> Unit) {
    val saved = LibraryStore.followedPodcasts
    if (saved.isEmpty()) {
        EmptyTabContent("Henüz kaydettiğiniz podcast yok.\nKeşfet'e tıklayarak başlayın.")
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(saved, key = { it.id }) { podcast ->
                LibraryPodcastCard(podcast = podcast, onClick = { onOpenShow(podcast.id) })
            }
        }
    }
}

@Composable
private fun LikedEpisodesTab() {
    val player = LocalPlayer.current
    val liked = LibraryStore.likedEpisodes
    if (liked.isEmpty()) {
        EmptyTabContent("Henüz beğendiğiniz bölüm yok.\nNow Playing'de kalp simgesine dokunun.")
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

@Composable
private fun MomentsTab() {
    val player = LocalPlayer.current
    val moments = LibraryStore.moments
    if (moments.isEmpty()) {
        EmptyTabContent("Henüz favori anınız yok.\nNow Playing'de yer imi simgesiyle ekleyin.")
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
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PodcastArtwork(
            imageUrl = moment.artworkUrl,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.size(52.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                moment.episodeTitle,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${moment.podcastTitle} · ${formatClock(moment.positionMs)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (moment.note.isNotBlank()) {
                Text(
                    moment.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        FilledTonalIconButton(onClick = onPlay) {
            Icon(Icons.Rounded.PlayArrow, "Bu andan oynat")
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Rounded.DeleteOutline, "Sil", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DownloadsTab() {
    val player = LocalPlayer.current
    val downloads = LibraryStore.downloads
    val dm = EchoesApplication.instance.downloadManager
    if (downloads.isEmpty()) {
        EmptyTabContent("Henüz indirdiğiniz bölüm yok.\nNow Playing'de indir simgesine dokunun.")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp),
        ) {
            items(downloads, key = { it.guid }) { episode ->
                Column {
                    EpisodeListItem(
                        episode = episode,
                        onPlay = { player.play(episode); player.expandSheet = true },
                    )
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        IconButton(onClick = { dm.delete(episode.guid) }) {
                            Icon(
                                Icons.Rounded.DeleteOutline, "İndirmeyi kaldır",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentEpisodesTab() {
    val player = LocalPlayer.current
    val history = player.history
    if (history.isEmpty()) {
        EmptyTabContent("Henüz dinlediğiniz bölüm yok.\nBir şeyler çalmaya başlayın.")
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

@Composable
private fun EmptyTabContent(message: String) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(48.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyLarge,
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
