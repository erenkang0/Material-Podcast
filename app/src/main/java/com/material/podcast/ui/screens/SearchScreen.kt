@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.material.podcast.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.SportsBasketball
import androidx.compose.material.icons.rounded.TheaterComedy
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.material.podcast.data.model.Podcast
import com.material.podcast.data.store.LibraryStore
import com.material.podcast.data.store.SearchHistoryStore
import com.material.podcast.ui.components.PodcastArtwork
import com.material.podcast.ui.components.PodcastCard
import com.material.podcast.ui.components.SectionHeader
import com.material.podcast.ui.viewmodel.SearchViewModel

/** A discoverable category with its own tonal colour + icon. */
private data class DiscoverCategory(val label: String, val icon: ImageVector)

private val discoverCategories = listOf(
    DiscoverCategory("Teknoloji", Icons.Rounded.Computer),
    DiscoverCategory("Bilim", Icons.Rounded.Science),
    DiscoverCategory("Komedi", Icons.Rounded.TheaterComedy),
    DiscoverCategory("Haberler", Icons.Rounded.Newspaper),
    DiscoverCategory("Spor", Icons.Rounded.SportsBasketball),
    DiscoverCategory("Kültür", Icons.Rounded.Palette),
    DiscoverCategory("Müzik", Icons.Rounded.MusicNote),
    DiscoverCategory("Tarih", Icons.Rounded.HistoryEdu),
    DiscoverCategory("Toplum", Icons.Rounded.Groups),
)

@Composable
fun SearchScreen(onOpenShow: (String) -> Unit) {
    val vm: SearchViewModel = viewModel()
    val results by vm.results.collectAsStateWithLifecycle()
    val isSearching by vm.isSearching.collectAsStateWithLifecycle()
    val trending by vm.trendingPodcasts.collectAsStateWithLifecycle()
    val recent = LibraryStore.recentPodcasts
    // Touch the store once so it initialises and exposes its reactive list.
    remember { SearchHistoryStore.get() }
    val history = SearchHistoryStore.queries

    var query by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
    ) {
        Text(
            "Keşfet",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 2.dp),
        )
        Text(
            "Yeni bir şeyler dinlemeye ne dersin?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 20.dp, bottom = 6.dp),
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
                    placeholder = { Text("Podcast, yapımcı veya konu ara") },
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
            LazyColumn(
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // When nothing is typed yet, surface recent searches.
                if (query.isBlank() && history.isNotEmpty()) {
                    item(key = "history_header") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 8.dp, top = 4.dp),
                        ) {
                            Icon(
                                Icons.Rounded.History, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(
                                "Son aramalar",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { SearchHistoryStore.clear() }) {
                                Text("Tümünü temizle")
                            }
                        }
                    }
                    item(key = "history_chips") {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            history.forEach { item ->
                                AssistChip(
                                    onClick = { query = item; vm.search(item) },
                                    label = { Text(item) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Rounded.History, null,
                                            modifier = Modifier.size(AssistChipDefaults.IconSize),
                                        )
                                    },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Rounded.Close,
                                            contentDescription = "\"$item\" aramasını kaldır",
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clickable { SearchHistoryStore.remove(item) },
                                        )
                                    },
                                )
                            }
                        }
                    }
                }

                itemsIndexed(results, key = { _, it -> it.id }) { index, podcast ->
                    SearchResultRow(
                        podcast = podcast,
                        index = index,
                        onClick = { active = false; onOpenShow(podcast.id) },
                    )
                }

                if (isSearching && results.isEmpty()) {
                    items(4, key = { "shimmer_$it" }) { ShimmerResultRow() }
                }

                if (results.isEmpty() && query.isNotBlank() && !isSearching) {
                    item(key = "no_results") {
                        Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.size(72.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Rounded.SearchOff, null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(36.dp),
                                        )
                                    }
                                }
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "\"$query\" için sonuç yok",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Farklı bir kelimeyle aramayı dene.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }

        // ---- Idle / discovery surface ----
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Recent searches as inviting chips on the idle surface too.
            if (history.isNotEmpty()) {
                item(key = "idle_history_header") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp),
                    ) {
                        Icon(
                            Icons.Rounded.History, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            "Son aramalar",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { SearchHistoryStore.clear() }) {
                            Text("Temizle")
                        }
                    }
                }
                item(key = "idle_history_chips") {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        history.forEach { item ->
                            AssistChip(
                                onClick = { query = item; vm.search(item); active = true },
                                label = { Text(item) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.History, null,
                                        modifier = Modifier.size(AssistChipDefaults.IconSize),
                                    )
                                },
                                trailingIcon = {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = "\"$item\" aramasını kaldır",
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clickable { SearchHistoryStore.remove(item) },
                                    )
                                },
                            )
                        }
                    }
                }
                item(key = "idle_history_space") { Spacer(Modifier.height(8.dp)) }
            }

            item(key = "discover_header") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 4.dp),
                ) {
                    Icon(
                        Icons.Rounded.AutoAwesome, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        "Keşfet",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            item(key = "discover_grid") {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    discoverCategories.forEachIndexed { i, cat ->
                        CategoryPill(
                            category = cat,
                            tone = i % 3,
                            onClick = {
                                query = cat.label
                                vm.search(cat.label)
                                active = true
                            },
                        )
                    }
                }
            }

            item(key = "discover_space") { Spacer(Modifier.height(12.dp)) }

            // Recently viewed.
            if (recent.isNotEmpty()) {
                item(key = "recent_header") {
                    SectionHeader("En son baktıkların", modifier = Modifier.padding(top = 4.dp))
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
                    SectionHeader("Popüler başlangıçlar", modifier = Modifier.padding(top = 4.dp))
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

/** A colourful, tonal category pill with an icon used on the discovery surface. */
@Composable
private fun CategoryPill(
    category: DiscoverCategory,
    tone: Int,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val (container, content) = when (tone) {
        0 -> scheme.primaryContainer to scheme.onPrimaryContainer
        1 -> scheme.secondaryContainer to scheme.onSecondaryContainer
        else -> scheme.tertiaryContainer to scheme.onTertiaryContainer
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = container,
        contentColor = content,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(content.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(category.icon, null, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.size(10.dp))
            Text(
                category.label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/** A rich, animated search result row with artwork, title/author and a chevron. */
@Composable
private fun SearchResultRow(
    podcast: Podcast,
    index: Int,
    onClick: () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(podcast.id) {
        kotlinx.coroutines.delay(index * 40L)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 3 },
    ) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(10.dp),
            ) {
                PodcastArtwork(
                    imageUrl = podcast.artworkUrl,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.size(60.dp),
                )
                Spacer(Modifier.size(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        podcast.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.size(2.dp))
                    Text(
                        "${podcast.author} · ${podcast.genre}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.size(8.dp))
                Icon(
                    Icons.Rounded.ChevronRight, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** A shimmering placeholder row shown while a search is in flight. */
@Composable
private fun ShimmerResultRow() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shimmerAlpha",
    )
    val base = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha * 0.25f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(base),
        )
        Spacer(Modifier.size(14.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(base),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.45f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(base),
            )
        }
    }
}
