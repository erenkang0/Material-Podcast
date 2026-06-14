@file:OptIn(ExperimentalMaterial3Api::class)

package com.material.podcast.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.material.podcast.data.MockData
import com.material.podcast.data.Show
import com.material.podcast.ui.components.CoverArt
import com.material.podcast.ui.components.EpisodeListItem
import com.material.podcast.ui.components.SectionHeader
import com.material.podcast.ui.state.PlayerState

@Composable
fun ShowDetailsScreen(
    showId: Int,
    onBack: () -> Unit,
    onOpenPlayer: () -> Unit,
) {
    val show = remember(showId) { resolveShow(showId) }
    var following by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(show.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "More")
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CoverArt(
                        icon = show.icon,
                        modifier = Modifier.size(116.dp),
                        shape = MaterialTheme.shapes.large,
                        container = MaterialTheme.colorScheme.primaryContainer,
                        content = MaterialTheme.colorScheme.onPrimaryContainer,
                        iconFraction = 0.46f,
                    )
                    Spacer(Modifier.width(18.dp))
                    Column {
                        Text(
                            text = show.title,
                            style = MaterialTheme.typography.headlineSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = show.author,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "${show.category} · ${show.episodes} episodes",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilledTonalButton(
                        onClick = {
                            val first = MockData.episodes.first()
                            PlayerState.play(show.title, first.title)
                            onOpenPlayer()
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Play")
                    }
                    OutlinedButton(
                        onClick = { following = !following },
                        modifier = Modifier.weight(1f),
                    ) {
                        AnimatedContent(targetState = following, label = "follow") { isFollowing ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (isFollowing) Icons.Rounded.Check else Icons.Rounded.Add,
                                    contentDescription = null,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(if (isFollowing) "Following" else "Follow")
                            }
                        }
                    }
                    OutlinedIconButton(onClick = { }) {
                        Icon(Icons.Rounded.IosShare, contentDescription = "Share")
                    }
                }
            }

            item {
                Text(
                    text = "This is placeholder description text representing the show's summary. " +
                        "It spans a couple of lines to show how supporting copy sits beneath the " +
                        "header and action row within a clean, native Material 3 layout.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }

            item {
                SectionHeader(
                    title = "Episodes",
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            items(MockData.episodes, key = { it.id }) { episode ->
                EpisodeListItem(
                    episode = episode,
                    onPlay = {
                        PlayerState.play(show.title, episode.title)
                        onOpenPlayer()
                    },
                )
            }
        }
    }
}

private fun resolveShow(showId: Int): Show =
    MockData.featured.firstOrNull { it.id == showId }
        ?: MockData.library.firstOrNull { it.id == showId }
        ?: MockData.featured.first()
