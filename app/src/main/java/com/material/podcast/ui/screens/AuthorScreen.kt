@file:OptIn(ExperimentalMaterial3Api::class)

package com.material.podcast.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.material.podcast.ui.components.LibraryPodcastCard
import com.material.podcast.ui.viewmodel.AuthorUiState
import com.material.podcast.ui.viewmodel.AuthorViewModel

@Composable
fun AuthorScreen(
    authorName: String,
    onBack: () -> Unit,
    onOpenShow: (String) -> Unit,
) {
    val vm: AuthorViewModel = viewModel(
        key = authorName,
        factory = AuthorViewModel.Factory(authorName),
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(authorName, maxLines = 2, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Geri")
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        when (val s = state) {
            is AuthorUiState.Loading -> Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            is AuthorUiState.Error -> Box(
                Modifier.fillMaxSize().padding(innerPadding).padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                OutlinedButton(onClick = vm::load) {
                    Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Tekrar dene")
                }
            }

            is AuthorUiState.Success -> {
                if (s.podcasts.isEmpty()) {
                    Box(
                        Modifier.fillMaxSize().padding(innerPadding).padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Bu yapımcı için podcast bulunamadı.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp, end = 16.dp,
                            top = innerPadding.calculateTopPadding() + 8.dp,
                            bottom = innerPadding.calculateBottomPadding() + 24.dp,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(s.podcasts, key = { it.id }) { podcast ->
                            LibraryPodcastCard(podcast = podcast, onClick = { onOpenShow(podcast.id) })
                        }
                    }
                }
            }
        }
    }
}
