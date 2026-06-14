@file:OptIn(ExperimentalMaterial3Api::class)

package com.material.podcast.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.material.podcast.data.model.Podcast
import com.material.podcast.data.model.PodcastEpisode
import com.material.podcast.data.store.FavoritesStore
import com.material.podcast.ui.LocalPlayer
import com.material.podcast.ui.components.EpisodeListItem
import com.material.podcast.ui.components.PodcastArtwork
import com.material.podcast.ui.components.SectionHeader
import com.material.podcast.ui.viewmodel.ShowDetailsUiState
import com.material.podcast.ui.viewmodel.ShowDetailsViewModel

@Composable
fun ShowDetailsScreen(
    podcastId: String,
    onBack: () -> Unit,
) {
    val vm: ShowDetailsViewModel = viewModel(
        key = podcastId,
        factory = ShowDetailsViewModel.Factory(podcastId),
    )
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyListState()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    val podcast = (uiState as? ShowDetailsUiState.Success)?.podcast
                    Text(
                        text = podcast?.title ?: "",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Rounded.MoreVert, "More")
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        when (val state = uiState) {
            is ShowDetailsUiState.Loading -> LoadingContent(innerPadding)
            is ShowDetailsUiState.Error -> ErrorContent(
                message = state.message,
                onRetry = vm::load,
                contentPadding = innerPadding,
            )
            is ShowDetailsUiState.Success -> SuccessContent(
                podcast = state.podcast,
                episodes = state.episodes,
                listState = listState,
                contentPadding = innerPadding,
            )
        }
    }
}

@Composable
private fun LoadingContent(contentPadding: PaddingValues) {
    Box(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text("Bölümler yükleniyor…", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    contentPadding: PaddingValues,
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(contentPadding).padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Yüklenemedi", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(message, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))
            OutlinedButton(onClick = onRetry) {
                Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Tekrar dene")
            }
        }
    }
}

@Composable
private fun SuccessContent(
    podcast: Podcast,
    episodes: List<PodcastEpisode>,
    listState: LazyListState,
    contentPadding: PaddingValues,
) {
    val player = LocalPlayer.current
    var following by remember(podcast.id) { mutableStateOf(FavoritesStore.isFavorite(podcast.id)) }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        // Header: artwork + meta
        item(key = "header") {
            PodcastHeader(podcast = podcast)
        }

        // Action buttons
        item(key = "actions") {
            ActionRow(
                podcast = podcast,
                following = following,
                onPlay = {
                    val first = episodes.firstOrNull() ?: return@ActionRow
                    player.play(first)
                    player.expandSheet = true
                },
                onFollowToggle = {
                    following = !following
                    FavoritesStore.toggle(podcast)
                },
            )
        }

        // Description
        if (podcast.description.isNotBlank()) {
            item(key = "desc") {
                ExpandableDescription(text = podcast.description)
            }
        }

        // Episode count header
        item(key = "ep_header") {
            SectionHeader(
                title = "Bölümler",
                actionLabel = "${episodes.size} bölüm",
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        item(key = "divider_top") {
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }

        // Episodes list
        items(episodes, key = { it.guid }) { episode ->
            EpisodeListItem(
                episode = episode,
                onPlay = {
                    player.play(episode)
                    player.expandSheet = true
                },
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )
        }

        if (episodes.isEmpty()) {
            item(key = "empty") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Henüz bölüm yok",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun PodcastHeader(podcast: Podcast) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        PodcastArtwork(
            imageUrl = podcast.artworkUrl,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.size(120.dp),
        )
        Spacer(Modifier.width(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                podcast.title,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                podcast.author,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        podcast.genre,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                if (podcast.episodeCount > 0) {
                    Text(
                        "${podcast.episodeCount} bölüm",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionRow(
    podcast: Podcast,
    following: Boolean,
    onPlay: () -> Unit,
    onFollowToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalButton(onClick = onPlay, modifier = Modifier.weight(1f)) {
            Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text("Oynat")
        }
        OutlinedButton(onClick = onFollowToggle, modifier = Modifier.weight(1f)) {
            AnimatedContent(following, label = "follow") { isFollowing ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isFollowing) Icons.Rounded.Check else Icons.Rounded.Add,
                        null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (isFollowing) "Takip ediliyor" else "Takip et")
                }
            }
        }
        OutlinedIconButton(onClick = { }) {
            Icon(Icons.Rounded.IosShare, "Paylaş")
        }
    }
}

@Composable
private fun ExpandableDescription(text: String) {
    var expanded by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (expanded) 1f else 1f, spring(Spring.DampingRatioMediumBouncy), label = "desc"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .graphicsLayer { scaleX = scale },
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (expanded) Int.MAX_VALUE else 3,
            overflow = if (expanded) TextOverflow.Visible else TextOverflow.Ellipsis,
        )
        AnimatedVisibility(visible = text.length > 200) {
            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.padding(top = 2.dp),
            ) {
                Text(if (expanded) "Daha az" else "Devamını gör")
            }
        }
    }
}
