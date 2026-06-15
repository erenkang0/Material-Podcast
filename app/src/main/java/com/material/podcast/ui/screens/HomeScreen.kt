@file:OptIn(ExperimentalMaterial3Api::class)

package com.material.podcast.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.material.podcast.data.model.ExploreCategory
import com.material.podcast.data.model.Podcast
import com.material.podcast.data.model.ResumePoint
import com.material.podcast.data.store.LibraryStore
import com.material.podcast.ui.LocalPlayer
import com.material.podcast.ui.components.PodcastCard
import com.material.podcast.ui.components.SectionHeader
import com.material.podcast.ui.viewmodel.HomeUiState
import com.material.podcast.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.delay
import java.util.Calendar

@Composable
fun HomeScreen(
    onOpenShow: (String) -> Unit,
    onOpenThemes: () -> Unit,
    onEditCategories: () -> Unit,
) {
    val vm: HomeViewModel = viewModel()
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val player = LocalPlayer.current

    val categories = LibraryStore.categories
    val categoriesKey = categories.joinToString("|") { "${it.id}:${it.query}" }
    LaunchedEffect(categoriesKey) { vm.load(categories.toList()) }

    val resume = LibraryStore.lastResume()

    // One calm, one-shot fade for the whole feed.
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val contentAlpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(420),
        label = "homeFade",
    )

    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greetingText = remember(hour) {
        when (hour) {
            in 0..11 -> "Günaydın ☀️"
            in 12..17 -> "İyi günler 🎧"
            else -> "İyi akşamlar 🌙"
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = greetingText,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onEditCategories) {
                        Icon(Icons.Rounded.Tune, "Kategorileri düzenle")
                    }
                    IconButton(onClick = onOpenThemes) {
                        Icon(Icons.Rounded.Palette, "Tema")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            MaterialTheme.colorScheme.background,
                        ),
                        endY = 520f,
                    ),
                ),
        ) {
            when (val state = uiState) {
                is HomeUiState.Loading -> Box(
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }

                is HomeUiState.Error -> Box(
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Bağlantı hatası", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            state.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(20.dp))
                        OutlinedButton(onClick = { vm.load(categories.toList(), force = true) }) {
                            Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(18.dp))
                            Text(" Tekrar dene")
                        }
                    }
                }

                is HomeUiState.Success -> LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = contentAlpha },
                    contentPadding = PaddingValues(
                        top = innerPadding.calculateTopPadding(),
                        bottom = 28.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (resume != null) {
                        item(key = "continue") {
                            HomeResumeCard(
                                point = resume,
                                onResume = { player.resume(resume); player.expandSheet = true },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                    }

                    // Hero card for the first featured podcast.
                    if (state.featured.isNotEmpty()) {
                        item(key = "hero") {
                            HeroFeaturedCard(
                                podcast = state.featured.first(),
                                onClick = { onOpenShow(state.featured.first().id) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                        }
                        if (state.featured.size > 1) {
                            item(key = "featured_header") {
                                SectionHeader(title = "Öne Çıkanlar")
                            }
                            item(key = "featured_row") {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    items(state.featured.drop(1), key = { it.id }) { podcast ->
                                        PodcastCard(podcast = podcast, onClick = { onOpenShow(podcast.id) })
                                    }
                                }
                            }
                        }
                    }

                    if (state.recommended.isNotEmpty()) {
                        item(key = "recommended_header") {
                            CategoryPillHeader(
                                category = ExploreCategory(id = "rec", name = "Senin İçin", query = ""),
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                        item(key = "recommended_row") {
                            StaggeredRow(index = 0) {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    items(state.recommended, key = { "rec_${it.id}" }) { podcast ->
                                        ForYouPodcastCard(
                                            podcast = podcast,
                                            onClick = { onOpenShow(podcast.id) },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    state.sections.forEachIndexed { index, section ->
                        item(key = "header_${section.category.id}") {
                            CategoryPillHeader(
                                category = section.category,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                        item(key = "row_${section.category.id}") {
                            if (section.podcasts.isEmpty()) {
                                EmptyStateShimmer(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                )
                            } else {
                                StaggeredRow(index = index + 1) {
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        items(section.podcasts, key = { "${section.category.id}_${it.id}" }) { podcast ->
                                            PodcastCard(podcast = podcast, onClick = { onOpenShow(podcast.id) })
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Category pill header with colored initial-letter circle
// ---------------------------------------------------------------------------

@Composable
private fun CategoryPillHeader(
    category: ExploreCategory,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Colored circle with first letter
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = category.name.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = category.name,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f),
        )
    }
}

// ---------------------------------------------------------------------------
// "For You" podcast card with ✨ Senin için badge overlay
// ---------------------------------------------------------------------------

@Composable
private fun ForYouPodcastCard(
    podcast: Podcast,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        PodcastCard(podcast = podcast, onClick = onClick)
        // Badge overlay in the top-left corner of the artwork inside the card
        Box(
            modifier = Modifier
                .padding(start = 20.dp, top = 20.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f))
                .padding(horizontal = 6.dp, vertical = 3.dp),
        ) {
            Text(
                text = "✨ Senin için",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Staggered entrance wrapper: fade + slide-up with per-index delay
// ---------------------------------------------------------------------------

@Composable
private fun StaggeredRow(
    index: Int,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(index) {
        delay(index * 80L)
        visible = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "rowAlpha_$index",
    )
    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 32f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "rowOffset_$index",
    )
    Box(
        modifier = Modifier.graphicsLayer {
            this.alpha = alpha
            translationY = offsetY
        },
    ) {
        content()
    }
}

// ---------------------------------------------------------------------------
// Empty state with shimmer bars
// ---------------------------------------------------------------------------

@Composable
private fun EmptyStateShimmer(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shimmerAlpha",
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(0.75f, 0.55f, 0.40f).forEach { fraction ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(14.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = shimmerAlpha * 0.15f),
                    ),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Hero card with animated gradient pulse
// ---------------------------------------------------------------------------

@Composable
private fun HeroFeaturedCard(
    podcast: Podcast,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val infiniteTransition = rememberInfiniteTransition(label = "heroPulse")
    val gradientAlpha by infiniteTransition.animateFloat(
        initialValue = 0.40f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "heroGradientAlpha",
    )

    ElevatedCard(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(podcast.artworkUrl)
                    .crossfade(300)
                    .memoryCacheKey(podcast.artworkUrl)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // Animated gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = gradientAlpha * 0.4f),
                                Color.Black.copy(alpha = gradientAlpha),
                            ),
                        ),
                    ),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(18.dp),
            ) {
                Box(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        "Öne Çıkan",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    podcast.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    podcast.author,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Redesigned "Continue Listening" card (Home-specific, full ElevatedCard layout)
// ---------------------------------------------------------------------------

@Composable
private fun HomeResumeCard(
    point: ResumePoint,
    onResume: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ep = point.episode
    val progressPercent = (point.fraction * 100).toInt()

    ElevatedCard(
        onClick = onResume,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                modifier = Modifier.padding(start = 14.dp, top = 14.dp, end = 14.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Left: 64dp artwork with rounded corners
                coil.compose.AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(ep.artworkUrl)
                        .crossfade(250)
                        .memoryCacheKey(ep.artworkUrl)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(MaterialTheme.shapes.medium),
                )
                Spacer(Modifier.width(12.dp))
                // Middle: episode info
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Kaldığın yerden devam et",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = ep.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${ep.podcastTitle} · %d%%".format(progressPercent),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(8.dp))
                // Right: play button
                FilledIconButton(onClick = onResume) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = "Devam et")
                }
            }
            // Bottom: full-width progress indicator
            LinearProgressIndicator(
                progress = { point.fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
            )
        }
    }
}
