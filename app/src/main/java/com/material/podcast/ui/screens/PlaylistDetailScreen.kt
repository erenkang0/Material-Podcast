@file:OptIn(ExperimentalMaterial3Api::class)

package com.material.podcast.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.material.podcast.data.store.LibraryStore
import com.material.podcast.ui.LocalPlayer
import com.material.podcast.ui.components.PodcastArtwork

@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    onBack: () -> Unit,
) {
    val player = LocalPlayer.current
    val playlist = LibraryStore.playlists.firstOrNull { it.id == playlistId }
    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    if (playlist == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Çalma listesi") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Geri")
                        }
                    },
                )
            },
        ) { inner ->
            Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                Text("Liste bulunamadı", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    if (showRename) {
        CreatePlaylistDialog(
            initial = playlist.name,
            title = "Listeyi yeniden adlandır",
            onConfirm = { name -> LibraryStore.renamePlaylist(playlist.id, name); showRename = false },
            onDismiss = { showRename = false },
        )
    }
    if (showDelete) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Listeyi sil") },
            text = { Text("\"${playlist.name}\" silinsin mi? Bölümler silinmez.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    LibraryStore.deletePlaylist(playlist.id)
                    showDelete = false
                    onBack()
                }) { Text("Sil") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDelete = false }) { Text("İptal") }
            },
        )
    }

    val episodes = playlist.episodes

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(playlist.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { showRename = true }) {
                        Icon(Icons.Rounded.DriveFileRenameOutline, "Yeniden adlandır")
                    }
                    IconButton(onClick = { showDelete = true }) {
                        Icon(Icons.Rounded.DeleteOutline, "Listeyi sil")
                    }
                },
            )
        },
    ) { inner ->
        LazyColumn(
            Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item(key = "play_all") {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Button(
                        onClick = {
                            val first = episodes.firstOrNull() ?: return@Button
                            player.play(first, episodes)
                            player.expandSheet = true
                        },
                        enabled = episodes.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Tümünü oynat")
                    }
                }
            }

            if (episodes.isEmpty()) {
                item(key = "empty") {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "Bu liste boş. Bölümlerden \"Çalma listesine ekle\" ile ekleyebilirsin.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                itemsIndexed(episodes, key = { _, ep -> ep.guid }) { index, episode ->
                    PlaylistEpisodeRow(
                        title = episode.title,
                        subtitle = episode.podcastTitle,
                        artworkUrl = episode.artworkUrl,
                        canMoveUp = index > 0,
                        canMoveDown = index < episodes.lastIndex,
                        onPlay = { player.play(episode, episodes); player.expandSheet = true },
                        onMoveUp = { LibraryStore.movePlaylistEpisode(playlist.id, index, index - 1) },
                        onMoveDown = { LibraryStore.movePlaylistEpisode(playlist.id, index, index + 1) },
                        onRemove = { LibraryStore.removeFromPlaylist(playlist.id, episode.guid) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistEpisodeRow(
    title: String,
    subtitle: String,
    artworkUrl: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onPlay: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PodcastArtwork(
            imageUrl = artworkUrl,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(
            Modifier.weight(1f).clickable(onClick = onPlay),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column {
            IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Rounded.ArrowUpward, "Yukarı taşı", modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Rounded.ArrowDownward, "Aşağı taşı", modifier = Modifier.size(18.dp))
            }
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Rounded.DeleteOutline, "Listeden çıkar",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
