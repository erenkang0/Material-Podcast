@file:OptIn(ExperimentalMaterial3Api::class)

package com.material.podcast.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.material.podcast.ui.components.PodcastCard
import com.material.podcast.ui.components.SectionHeader
import com.material.podcast.ui.viewmodel.HomeUiState
import com.material.podcast.ui.viewmodel.HomeViewModel
import java.util.Calendar

@Composable
fun HomeScreen(
    onOpenShow: (String) -> Unit,
    onOpenThemes: () -> Unit,
) {
    val vm: HomeViewModel = viewModel()
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

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
                    IconButton(onClick = onOpenThemes) {
                        Icon(Icons.Rounded.Palette, "Tema")
                    }
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Person, "Profil",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(22.dp),
                        )
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
                    OutlinedButton(onClick = vm::load) {
                        Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(18.dp))
                        Text(" Tekrar dene")
                    }
                }
            }

            is HomeUiState.Success -> LazyColumn(
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = 28.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (state.featured.isNotEmpty()) {
                    item(key = "featured_header") {
                        SectionHeader(title = "Öne Çıkanlar", actionLabel = "Tümünü gör")
                    }
                    item(key = "featured_row") {
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

                if (state.technology.isNotEmpty()) {
                    item(key = "tech_header") {
                        SectionHeader(
                            title = "Teknoloji",
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                    item(key = "tech_row") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            items(state.technology, key = { "tech_${it.id}" }) { podcast ->
                                PodcastCard(podcast = podcast, onClick = { onOpenShow(podcast.id) })
                            }
                        }
                    }
                }

                if (state.science.isNotEmpty()) {
                    item(key = "sci_header") {
                        SectionHeader(
                            title = "Bilim & Eğitim",
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                    item(key = "sci_row") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            items(state.science, key = { "sci_${it.id}" }) { podcast ->
                                PodcastCard(podcast = podcast, onClick = { onOpenShow(podcast.id) })
                            }
                        }
                    }
                }

                if (state.culture.isNotEmpty()) {
                    item(key = "cult_header") {
                        SectionHeader(
                            title = "Toplum & Kültür",
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                    item(key = "cult_row") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            items(state.culture, key = { "cult_${it.id}" }) { podcast ->
                                PodcastCard(podcast = podcast, onClick = { onOpenShow(podcast.id) })
                            }
                        }
                    }
                }
            }
        }
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
