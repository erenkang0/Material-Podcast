@file:OptIn(ExperimentalMaterial3Api::class)

package com.material.podcast.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.material.podcast.data.model.Playlist
import com.material.podcast.data.store.LibraryStore

private enum class PlaylistSort { Alphabetical, CreationDate, EpisodeCount }

@Composable
fun PlaylistsScreen(
    onBack: () -> Unit,
    onOpenPlaylist: (String) -> Unit,
) {
    val playlists = LibraryStore.playlists
    var showCreate by remember { mutableStateOf(false) }
    var sortMode by remember { mutableStateOf(PlaylistSort.CreationDate) }
    var showSortMenu by remember { mutableStateOf(false) }

    if (showCreate) {
        CreatePlaylistDialog(
            onConfirm = { name ->
                val pl = LibraryStore.createPlaylist(name)
                showCreate = false
                onOpenPlaylist(pl.id)
            },
            onDismiss = { showCreate = false },
        )
    }

    val sortedPlaylists = remember(playlists.toList(), sortMode) {
        when (sortMode) {
            PlaylistSort.Alphabetical -> playlists.sortedBy { it.name.lowercase() }
            PlaylistSort.CreationDate -> playlists.sortedByDescending { it.createdAt }
            PlaylistSort.EpisodeCount -> playlists.sortedByDescending { it.episodes.size }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Çalma Listeleri", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Geri")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Rounded.Sort, contentDescription = "Sırala")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Ad") },
                                onClick = { sortMode = PlaylistSort.Alphabetical; showSortMenu = false },
                                leadingIcon = if (sortMode == PlaylistSort.Alphabetical) {
                                    { Icon(Icons.Rounded.Sort, null, modifier = Modifier.size(18.dp)) }
                                } else null,
                            )
                            DropdownMenuItem(
                                text = { Text("Oluşturma tarihi") },
                                onClick = { sortMode = PlaylistSort.CreationDate; showSortMenu = false },
                                leadingIcon = if (sortMode == PlaylistSort.CreationDate) {
                                    { Icon(Icons.Rounded.Sort, null, modifier = Modifier.size(18.dp)) }
                                } else null,
                            )
                            DropdownMenuItem(
                                text = { Text("Bölüm sayısı") },
                                onClick = { sortMode = PlaylistSort.EpisodeCount; showSortMenu = false },
                                leadingIcon = if (sortMode == PlaylistSort.EpisodeCount) {
                                    { Icon(Icons.Rounded.Sort, null, modifier = Modifier.size(18.dp)) }
                                } else null,
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreate = true },
                icon = { Icon(Icons.Rounded.Add, null) },
                text = { Text("Yeni liste") },
            )
        },
    ) { inner ->
        if (playlists.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(inner).padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(36.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.QueueMusic, null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Henüz çalma listen yok",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Farklı podcastlerden bölümleri tek listede topla.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(inner),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(sortedPlaylists, key = { it.id }) { playlist ->
                    ElevatedCard(
                        onClick = { onOpenPlaylist(playlist.id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PlaylistArtworkCollage(
                                playlist = playlist,
                                modifier = Modifier.size(52.dp),
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    playlist.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    "${playlist.episodes.size} bölüm",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Icon(
                                Icons.Rounded.ChevronRight, null,
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
private fun PlaylistArtworkCollage(
    playlist: Playlist,
    modifier: Modifier = Modifier,
) {
    val artworks = playlist.episodes.take(4).map { it.artworkUrl }
    Box(modifier = modifier.clip(MaterialTheme.shapes.medium)) {
        if (artworks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.QueueMusic, null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        } else if (artworks.size == 1) {
            AsyncImage(
                model = artworks[0],
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            // 2x2 grid collage
            val quadrantSize = Modifier.fillMaxSize(0.5f)
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    AsyncImage(
                        model = artworks[0],
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.weight(1f).fillMaxSize(),
                    )
                    if (artworks.size >= 2) {
                        AsyncImage(
                            model = artworks[1],
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).fillMaxSize(),
                        )
                    } else {
                        Box(
                            Modifier.weight(1f).fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        )
                    }
                }
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    if (artworks.size >= 3) {
                        AsyncImage(
                            model = artworks[2],
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).fillMaxSize(),
                        )
                    } else {
                        Box(
                            Modifier.weight(1f).fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        )
                    }
                    if (artworks.size >= 4) {
                        AsyncImage(
                            model = artworks[3],
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).fillMaxSize(),
                        )
                    } else {
                        Box(
                            Modifier.weight(1f).fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CreatePlaylistDialog(
    initial: String = "",
    title: String = "Yeni çalma listesi",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Liste adı") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
            ) { Text("Kaydet") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } },
    )
}
