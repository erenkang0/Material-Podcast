@file:OptIn(ExperimentalMaterial3Api::class)

package com.material.podcast.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.nestedscroll.nestedScroll
import kotlinx.coroutines.delay
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.material.podcast.data.store.LibraryStore
import com.material.podcast.ui.LocalPlayer
import com.material.podcast.ui.components.ContinueListeningCard
import com.material.podcast.ui.components.PodcastCard
import com.material.podcast.ui.components.SectionHeader
import com.material.podcast.ui.viewmodel.HomeUiState
import com.material.podcast.ui.viewmodel.HomeViewModel
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

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = greeting(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text("Keşfet", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
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
            )
        },
    ) { innerPadding ->
        when (val state = uiState) {
            is HomeUiState.Loading -> Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            is HomeUiState.Error -> Box(
                Modifier.fillMaxSize().padding(innerPadding).padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Bağlantı hatası", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(state.message, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(20.dp))
                    OutlinedButton(onClick = { vm.load(categories.toList(), force = true) }) {
                        Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(18.dp))
                        Text(" Tekrar dene")
                    }
                }
            }

            is HomeUiState.Success -> Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.05f),
                                MaterialTheme.colorScheme.background.copy(alpha = 0f),
                            ),
                            endY = 700f,
                        ),
                    ),
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(
                        top = innerPadding.calculateTopPadding(),
                        bottom = 28.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (resume != null) {
                        item(key = "continue") {
                            Reveal(0) {
                                ContinueListeningCard(
                                    point = resume,
                                    onResume = { player.resume(resume); player.expandSheet = true },
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                                )
                            }
                        }
                    }

                    if (state.featured.isNotEmpty()) {
                        item(key = "featured_header") {
                            Reveal(60) { SectionHeader(title = "Öne Çıkanlar") }
                        }
                        item(key = "featured_row") {
                            Reveal(100) {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                ) {
                                    items(state.featured, key = { it.id }) { podcast ->
                                        PodcastCard(podcast = podcast, onClick = { onOpenShow(podcast.id) })
                                    }
                                }
                            }
                        }
                    }

                    state.sections.forEachIndexed { index, section ->
                        val delay = 140 + index * 80
                        item(key = "header_${section.category.id}") {
                            Reveal(delay) {
                                SectionHeader(
                                    title = section.category.name,
                                    modifier = Modifier.padding(top = 12.dp),
                                )
                            }
                        }
                        item(key = "row_${section.category.id}") {
                            Reveal(delay + 40) {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
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

/** Fades + slides its content in once, after [delayMillis], for a staggered reveal. */
@Composable
private fun Reveal(delayMillis: Int, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(420)) + slideInVertically(tween(420)) { it / 5 },
    ) {
        content()
    }
}

private fun greeting(): String {
    return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Günaydın"
        in 12..16 -> "İyi öğleden sonralar"
        in 17..21 -> "İyi akşamlar"
        else -> "İyi geceler"
    }
}
