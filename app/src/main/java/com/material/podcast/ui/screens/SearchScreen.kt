@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.material.podcast.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.material.podcast.data.store.LibraryStore
import com.material.podcast.ui.components.GenreChip
import com.material.podcast.ui.components.PodcastCard
import com.material.podcast.ui.components.PodcastListItem
import com.material.podcast.ui.components.SectionHeader
import com.material.podcast.ui.viewmodel.SearchViewModel

private val genres = listOf(
    "Teknoloji", "Bilim", "Kültür", "Toplum", "İş Dünyası", "Tarih",
    "Sağlık", "Hikayeler", "Spor", "Komedi", "Müzik", "Haberler",
)

@Composable
fun SearchScreen(onOpenShow: (String) -> Unit) {
    val vm: SearchViewModel = viewModel()
    val results by vm.results.collectAsStateWithLifecycle()
    val isSearching by vm.isSearching.collectAsStateWithLifecycle()
    val trending by vm.trendingPodcasts.collectAsStateWithLifecycle()
    val recent = LibraryStore.recentPodcasts

    var query by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(false) }
    var selectedGenre by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
    ) {
        Text(
            "Ara",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 4.dp),
        )
        SearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query = query,
                    onQueryChange = { q ->
                        query = q
                        vm.search(q)
                    },
                    onSearch = { active = false },
                    expanded = active,
                    onExpandedChange = { active = it },
                    placeholder = { Text("Podcast, bölüm, konu ara…") },
                    leadingIcon = {
                        if (isSearching) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                        } else {
                            Icon(Icons.Rounded.Search, null)
                        }
                    },
                    trailingIcon = {
                        if (active) {
                            IconButton(onClick = {
                                if (query.isNotEmpty()) { query = ""; vm.search("") } else active = false
                            }) { Icon(Icons.Rounded.Close, "Kapat") }
                        }
                    },
                )
            },
            expanded = active,
            onExpandedChange = { active = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (active) 0.dp else 16.dp),
        ) {
            // Search results inside the expanded bar
            LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                items(results, key = { it.id }) { podcast ->
                    PodcastListItem(
                        podcast = podcast,
                        onClick = { active = false; onOpenShow(podcast.id) },
                    )
                }
                if (results.isEmpty() && query.isNotBlank() && !isSearching) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                            Text(
                                "\"$query\" için sonuç bulunamadı",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item(key = "genre_header") { SectionHeader("Türe göre keşfet") }
            item(key = "genres") {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    genres.forEach { genre ->
                        GenreChip(
                            label = genre,
                            selected = selectedGenre == genre,
                            onClick = {
                                selectedGenre = if (selectedGenre == genre) null else genre
                                if (selectedGenre != null) {
                                    query = selectedGenre!!
                                    vm.search(selectedGenre!!)
                                    active = true
                                }
                            },
                        )
                    }
                }
            }

            // Recently viewed — replaces the old "popular" rail.
            if (recent.isNotEmpty()) {
                item(key = "recent_header") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp, start = 20.dp, end = 8.dp),
                    ) {
                        Icon(
                            Icons.Rounded.History, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            "En son baktıkların",
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                }
                item(key = "recent_row") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(recent, key = { "recent_${it.id}" }) { podcast ->
                            PodcastCard(podcast = podcast, onClick = { onOpenShow(podcast.id) })
                        }
                    }
                }
                item(key = "space") { Spacer(Modifier.height(8.dp)) }
            } else if (trending.isNotEmpty()) {
                // New users haven't viewed anything yet — show some popular starters.
                item(key = "trend_header") {
                    SectionHeader("Popüler başlangıçlar", modifier = Modifier.padding(top = 8.dp))
                }
                item(key = "trend_row") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(trending, key = { "trend_${it.id}" }) { podcast ->
                            PodcastCard(podcast = podcast, onClick = { onOpenShow(podcast.id) })
                        }
                    }
                }
                item(key = "space") { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}
