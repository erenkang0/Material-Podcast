@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.material.podcast.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.unit.dp
import com.material.podcast.data.MockData
import com.material.podcast.ui.components.FeaturedCard
import com.material.podcast.ui.components.GenreChip
import com.material.podcast.ui.components.SectionHeader

@Composable
fun SearchScreen(
    onOpenShow: (Int) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(false) }
    var selectedGenre by remember { mutableStateOf<String?>(null) }

    val suggestions = remember(query) {
        val base = listOf(
            "Placeholder search one",
            "Placeholder search two",
            "Placeholder search three",
            "Placeholder topic",
        )
        if (query.isBlank()) base else base.filter { it.contains(query, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        SearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query = query,
                    onQueryChange = { query = it },
                    onSearch = { active = false },
                    expanded = active,
                    onExpandedChange = { active = it },
                    placeholder = { Text("Search shows, episodes, topics") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (active) {
                            IconButton(onClick = {
                                if (query.isNotEmpty()) query = "" else active = false
                            }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Close")
                            }
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
            // Expanded suggestion list
            Column {
                suggestions.forEach { suggestion ->
                    ListItem(
                        headlineContent = { Text(suggestion) },
                        leadingContent = {
                            Icon(Icons.Rounded.History, contentDescription = null)
                        },
                        modifier = Modifier.clickable {
                            query = suggestion
                            active = false
                        },
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                SectionHeader("Browse by genre")
            }
            item {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MockData.genres.forEach { genre ->
                        GenreChip(
                            label = genre,
                            selected = selectedGenre == genre,
                            onClick = {
                                selectedGenre = if (selectedGenre == genre) null else genre
                            },
                        )
                    }
                }
            }
            item {
                SectionHeader(
                    title = "Popular now",
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(MockData.featured, key = { it.id }) { show ->
                        FeaturedCard(show = show, onClick = { onOpenShow(show.id) })
                    }
                }
            }
        }
    }
}
