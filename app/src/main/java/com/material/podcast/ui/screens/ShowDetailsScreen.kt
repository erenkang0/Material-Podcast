@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.material.podcast.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.delay
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.material.podcast.data.model.Podcast
import com.material.podcast.data.model.PodcastEpisode
import com.material.podcast.data.store.LibraryStore
import com.material.podcast.ui.LocalPlayer
import com.material.podcast.ui.components.EpisodeListItem
import com.material.podcast.ui.components.PodcastArtwork
import com.material.podcast.ui.components.ResumeHint
import com.material.podcast.ui.components.SectionHeader
import com.material.podcast.ui.theme.extractArtworkColor
import com.material.podcast.ui.viewmodel.ShowDetailsUiState
import com.material.podcast.ui.viewmodel.ShowDetailsViewModel

@Composable
fun ShowDetailsScreen(
    podcastId: String,
    onBack: () -> Unit,
    onOpenAuthor: (String) -> Unit,
) {
    val vm: ShowDetailsViewModel = viewModel(
        key = podcastId,
        factory = ShowDetailsViewModel.Factory(podcastId),
    )
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }

    val podcast = (uiState as? ShowDetailsUiState.Success)?.podcast

    var artworkColorSeed by remember(podcast?.id) { mutableStateOf(0) }
    LaunchedEffect(podcast?.artworkUrl) {
        val url = podcast?.artworkUrl
        if (url.isNullOrBlank()) return@LaunchedEffect
        val seed = extractArtworkColor(context, url)
        if (seed != null) artworkColorSeed = seed
    }

    // Track whether content has appeared for the artwork entrance animation
    var artworkVisible by remember { mutableStateOf(false) }
    LaunchedEffect(uiState) {
        if (uiState is ShowDetailsUiState.Success) {
            artworkVisible = true
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = podcast?.title ?: "",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Geri")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Rounded.MoreVert, "Daha fazla")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            if (podcast != null) {
                                val following = LibraryStore.isFollowed(podcast.id)
                                DropdownMenuItem(
                                    text = { Text(if (following) "Takipten çık" else "Takip et") },
                                    leadingIcon = {
                                        Icon(if (following) Icons.Rounded.Check else Icons.Rounded.Add, null)
                                    },
                                    onClick = { LibraryStore.toggleFollow(podcast); menuOpen = false },
                                )
                                DropdownMenuItem(
                                    text = { Text("Yapımcıyı gör") },
                                    leadingIcon = { Icon(Icons.Rounded.Person, null) },
                                    onClick = { menuOpen = false; onOpenAuthor(podcast.author) },
                                )
                                DropdownMenuItem(
                                    text = { Text("Paylaş") },
                                    leadingIcon = { Icon(Icons.Rounded.Share, null) },
                                    onClick = { menuOpen = false; sharePodcast(context, podcast) },
                                )
                                DropdownMenuItem(
                                    text = { Text("Bağlantıyı kopyala") },
                                    leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) },
                                    onClick = { menuOpen = false; copyLink(context, podcast) },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Yenile") },
                                leadingIcon = { Icon(Icons.Rounded.Refresh, null) },
                                onClick = { menuOpen = false; vm.load() },
                            )
                        }
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
                artworkColorSeed = artworkColorSeed,
                artworkVisible = artworkVisible,
                onOpenAuthor = onOpenAuthor,
            )
        }
    }
}

private fun sharePodcast(context: Context, podcast: Podcast) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "${podcast.title} — ${podcast.author}\n${podcast.feedUrl}")
    }
    context.startActivity(Intent.createChooser(sendIntent, "Paylaş"))
}

private fun copyLink(context: Context, podcast: Podcast) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(podcast.title, podcast.feedUrl))
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
    artworkColorSeed: Int = 0,
    artworkVisible: Boolean = true,
    onOpenAuthor: (String) -> Unit,
    onSearchGenre: (String) -> Unit = {},
) {
    val player = LocalPlayer.current
    var following by remember(podcast.id) { mutableStateOf(LibraryStore.isFollowed(podcast.id)) }
    val resume = LibraryStore.resumeForPodcast(podcast.id)

    // Episode search state
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var searchExpanded by rememberSaveable { mutableStateOf(false) }

    val filteredEpisodes = remember(searchQuery, episodes) {
        if (searchQuery.isBlank()) episodes
        else episodes.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.description.contains(searchQuery, ignoreCase = true)
        }
    }

    // Group episodes by year for sticky headers
    val episodesByYear = remember(filteredEpisodes) {
        filteredEpisodes.groupBy { ep ->
            ep.publishedDate.take(4).toIntOrNull() ?: 0
        }.entries.sortedByDescending { it.key }
    }

    // Listening stats: total ms listened for this podcast
    val listenStats = remember { LibraryStore.getStats() }
    val listenedMs = listenStats.perPodcast[podcast.id] ?: 0L
    val listenedHours = listenedMs / 3_600_000L

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        item(key = "header") {
            PodcastHeader(
                podcast = podcast,
                artworkColorSeed = artworkColorSeed,
                artworkVisible = artworkVisible,
                onOpenAuthor = onOpenAuthor,
                onSearchGenre = onSearchGenre,
            )
        }

        // Stats bar: latest date, episode count, rating
        item(key = "stats_bar") {
            StatsBar(
                latestDate = episodes.firstOrNull()?.publishedDate.orEmpty(),
                episodeCount = episodes.size,
            )
        }

        item(key = "actions") {
            ActionRow(
                podcast = podcast,
                latestEpisode = episodes.firstOrNull(),
                following = following,
                onPlay = {
                    val first = episodes.firstOrNull() ?: return@ActionRow
                    player.play(first, episodes)
                    player.expandSheet = true
                },
                onFollowToggle = {
                    following = !following
                    LibraryStore.toggleFollow(podcast)
                },
            )
        }

        if (resume != null) {
            item(key = "resume") {
                ResumeHint(
                    point = resume,
                    onResume = { player.resume(resume); player.expandSheet = true },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                )
            }
        }

        if (podcast.description.isNotBlank()) {
            item(key = "desc") {
                ExpandableDescription(text = podcast.description)
            }
        }

        // Listening stats row
        if (listenedMs > 0L) {
            item(key = "listen_stats") {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("🎧", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = if (listenedHours > 0)
                                "Bu podcastten $listenedHours saat dinledin"
                            else
                                "Bu podcastten az dinledin",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }
        }

        item(key = "ep_header") {
            SectionHeader(
                title = "Bölümler",
                actionLabel = "${episodes.size} bölüm",
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        // Collapsible search bar
        item(key = "ep_search") {
            EpisodeSearchBar(
                query = searchQuery,
                expanded = searchExpanded,
                onQueryChange = { searchQuery = it },
                onExpandedChange = { searchExpanded = it },
                onClear = { searchQuery = ""; searchExpanded = false },
            )
        }

        item(key = "divider_top") {
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }

        if (filteredEpisodes.isEmpty() && searchQuery.isNotBlank()) {
            item(key = "no_results") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "\"$searchQuery\" için sonuç bulunamadı",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else if (episodesByYear.isEmpty()) {
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
        } else {
            episodesByYear.forEach { (year, yearEpisodes) ->
                if (year > 0) {
                    stickyHeader(key = "year_$year") {
                        YearSectionHeader(year = year)
                    }
                }
                items(yearEpisodes, key = { it.guid }) { episode ->
                    EpisodeListItem(
                        episode = episode,
                        onPlay = {
                            player.play(episode, episodes)
                            player.expandSheet = true
                        },
                        isDownloaded = LibraryStore.isDownloaded(episode.guid),
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsBar(
    latestDate: String,
    episodeCount: Int,
) {
    val dividerColor = MaterialTheme.colorScheme.outlineVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Latest episode date
        StatCell(
            label = "Son bölüm",
            value = if (latestDate.isNotBlank()) latestDate.take(10) else "—",
            emoji = "📅",
            modifier = Modifier.weight(1f),
        )

        Box(
            Modifier
                .width(1.dp)
                .height(32.dp)
                .background(dividerColor)
        )

        // Episode count
        StatCell(
            label = "Bölüm sayısı",
            value = "$episodeCount bölüm",
            emoji = "🎙️",
            modifier = Modifier.weight(1f),
        )

        Box(
            Modifier
                .width(1.dp)
                .height(32.dp)
                .background(dividerColor)
        )

        // Rating placeholder
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "⭐ 4.8",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    "Puan",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
private fun StatCell(
    label: String,
    value: String,
    emoji: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(emoji, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun YearSectionHeader(year: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = year.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(8.dp))
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )
        }
    }
}

@Composable
private fun EpisodeSearchBar(
    query: String,
    expanded: Boolean,
    onQueryChange: (String) -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(200)) + expandHorizontally(),
            exit = fadeOut(tween(150)) + shrinkHorizontally(),
            modifier = Modifier.weight(1f),
        ) {
            SearchBar(
                inputField = {
                    SearchBarDefaults.InputField(
                        query = query,
                        onQueryChange = onQueryChange,
                        onSearch = {},
                        expanded = false,
                        onExpandedChange = {},
                        placeholder = { Text("Bölüm ara…") },
                        leadingIcon = { Icon(Icons.Rounded.Search, null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { onQueryChange("") }) {
                                    Icon(Icons.Rounded.Close, "Temizle", modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                    )
                },
                expanded = false,
                onExpandedChange = {},
                modifier = Modifier.fillMaxWidth(),
            ) {}
        }

        AnimatedVisibility(
            visible = !expanded,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(100)),
        ) {
            IconButton(onClick = { onExpandedChange(true) }) {
                Icon(Icons.Rounded.Search, "Bölüm ara", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(150)),
        ) {
            TextButton(onClick = onClear) {
                Text("İptal")
            }
        }
    }
}

@Composable
private fun PodcastHeader(
    podcast: Podcast,
    artworkColorSeed: Int,
    artworkVisible: Boolean,
    onOpenAuthor: (String) -> Unit,
    onSearchGenre: (String) -> Unit = {},
) {
    val baseScheme = MaterialTheme.colorScheme
    val isDark = baseScheme.surface.luminance() < 0.5f
    val tintTarget = if (artworkColorSeed != 0) {
        Color(artworkColorSeed).copy(alpha = if (isDark) 0.38f else 0.20f)
    } else {
        Color.Transparent
    }
    val animatedTint by animateColorAsState(tintTarget, tween(600), label = "headerTint")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(animatedTint, Color.Transparent),
                    endY = 420f,
                ),
            ),
    ) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // Animated artwork entrance
        AnimatedVisibility(
            visible = artworkVisible,
            enter = scaleIn(
                initialScale = 0.85f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            ) + fadeIn(animationSpec = tween(300)),
        ) {
            PodcastArtwork(
                imageUrl = podcast.artworkUrl,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.size(120.dp),
            )
        }
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
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onOpenAuthor(podcast.author) },
            )
            Spacer(Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Clickable genre chip
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .clickable { onSearchGenre(podcast.genre) }
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
}

@Composable
private fun ActionRow(
    podcast: Podcast,
    latestEpisode: PodcastEpisode?,
    following: Boolean,
    onPlay: () -> Unit,
    onFollowToggle: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current

    // After actively following, show "Takip ediliyor" for 3 seconds, then collapse to a compact
    // icon. Already-followed shows open straight in the collapsed state (no replay).
    var justFollowed by remember(podcast.id) { mutableStateOf(false) }
    var firstComposition by remember(podcast.id) { mutableStateOf(true) }
    LaunchedEffect(following) {
        if (firstComposition) {
            firstComposition = false
            return@LaunchedEffect
        }
        if (following) {
            justFollowed = true
            delay(3000L)
            justFollowed = false
        } else {
            justFollowed = false
        }
    }
    val collapsed = following && !justFollowed

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Play button — full width, scrollable with content
        FilledTonalButton(onClick = onPlay, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text("Oynat")
        }

        // Follow row — animated collapse + notification/autodl icons
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedContent(
                targetState = collapsed,
                transitionSpec = {
                    (fadeIn(spring(stiffness = Spring.StiffnessMedium)) + scaleIn(initialScale = 0.7f))
                        .togetherWith(fadeOut(spring(stiffness = Spring.StiffnessMedium)) + scaleOut(targetScale = 0.7f))
                },
                label = "followCollapse",
                modifier = Modifier.weight(1f),
            ) { isCollapsed ->
                if (isCollapsed) {
                    OutlinedIconButton(onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onFollowToggle()
                    }) {
                        Icon(Icons.Rounded.Check, "Takip ediliyor")
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onFollowToggle()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            if (following) Icons.Rounded.Check else Icons.Rounded.Add,
                            null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (following) "Takip ediliyor" else "Takip et")
                    }
                }
            }

            // Notification + autodl + download — visible only after following
            AnimatedVisibility(
                visible = collapsed,
                enter = fadeIn(spring(stiffness = Spring.StiffnessMedium)) + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    var notify by remember(podcast.id) { mutableStateOf(LibraryStore.isNotifyEnabled(podcast.id)) }
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Yeni bölüm bildirimi") } },
                        state = rememberTooltipState(),
                    ) {
                        OutlinedIconButton(onClick = {
                            notify = !notify
                            LibraryStore.setNotifyEnabled(podcast.id, notify)
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }) {
                            Icon(
                                if (notify) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff,
                                "Yeni bölüm bildirimi",
                                tint = if (notify) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    var autoDl by remember(podcast.id) { mutableStateOf(LibraryStore.isAutoDownloadEnabled(podcast.id)) }
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Yeni bölümleri otomatik indir") } },
                        state = rememberTooltipState(),
                    ) {
                        OutlinedIconButton(onClick = {
                            autoDl = !autoDl
                            LibraryStore.setAutoDownloadEnabled(podcast.id, autoDl)
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }) {
                            Icon(
                                if (autoDl) Icons.Rounded.DownloadForOffline else Icons.Rounded.Download,
                                "Yeni bölümleri otomatik indir",
                                tint = if (autoDl) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    if (latestEpisode != null) {
                        ShowDownloadButton(episode = latestEpisode)
                    }
                }
            }
        }
    }
}

@Composable
private fun ShowDownloadButton(episode: PodcastEpisode) {
    val dm = com.material.podcast.EchoesApplication.instance.downloadManager
    val haptics = LocalHapticFeedback.current
    val downloaded = LibraryStore.isDownloaded(episode.guid)
    val state = dm.states[episode.guid]
    OutlinedIconButton(onClick = {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        when {
            downloaded -> dm.delete(episode.guid)
            state?.status == com.material.podcast.media.DownloadStatus.Downloading ||
            state?.status == com.material.podcast.media.DownloadStatus.Queued -> {}
            else -> dm.download(episode)
        }
    }) {
        when {
            downloaded -> Icon(
                Icons.Rounded.DownloadDone, "İndirildi",
                tint = MaterialTheme.colorScheme.primary,
            )
            state?.status == com.material.podcast.media.DownloadStatus.Downloading ||
            state?.status == com.material.podcast.media.DownloadStatus.Queued ->
                CircularProgressIndicator(
                    progress = { state.progress.coerceIn(0f, 1f) },
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            else -> Icon(Icons.Rounded.Download, "Son bölümü indir")
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
