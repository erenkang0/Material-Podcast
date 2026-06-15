@file:OptIn(ExperimentalMaterial3Api::class)

package com.material.podcast.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.material.podcast.data.model.ExploreCategory
import com.material.podcast.data.store.LibraryStore
import java.util.UUID

@Composable
fun CategoryEditorScreen(onBack: () -> Unit) {
    val draft = remember { mutableStateListOf<ExploreCategory>().apply { addAll(LibraryStore.categories) } }

    fun save() {
        LibraryStore.setCategories(
            draft.map { it.copy(name = it.name.trim(), query = it.query.trim()) }
                .filter { it.name.isNotBlank() && it.query.isNotBlank() },
        )
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kategorileri düzenle") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        draft.clear()
                        draft.addAll(LibraryStore.defaultCategories)
                    }) {
                        Icon(Icons.Rounded.Restore, "Varsayılana sıfırla")
                    }
                    IconButton(onClick = { save() }) {
                        Icon(Icons.Rounded.Check, "Kaydet")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "hint") {
                Text(
                    "Her kategori için görünen adı ve arka planda aranacak ifadeyi yazın. " +
                        "En fazla ${LibraryStore.MAX_CATEGORIES} kategori ekleyebilirsiniz.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            itemsIndexed(draft, key = { _, c -> c.id }) { index, category ->
                ElevatedCard {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Kategori ${index + 1}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { draft.removeAt(index) }) {
                                Icon(
                                    Icons.Rounded.DeleteOutline, "Sil",
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                        OutlinedTextField(
                            value = category.name,
                            onValueChange = { draft[index] = category.copy(name = it) },
                            label = { Text("Kategori adı") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = category.query,
                            onValueChange = { draft[index] = category.copy(query = it) },
                            label = { Text("Arama ifadesi (ör. technology podcast)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            item(key = "add") {
                TextButton(
                    onClick = {
                        if (draft.size < LibraryStore.MAX_CATEGORIES) {
                            draft.add(ExploreCategory(UUID.randomUUID().toString(), "", ""))
                        }
                    },
                    enabled = draft.size < LibraryStore.MAX_CATEGORIES,
                ) {
                    Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (draft.size < LibraryStore.MAX_CATEGORIES) "Kategori ekle"
                        else "En fazla ${LibraryStore.MAX_CATEGORIES} kategori",
                    )
                }
            }
        }
    }
}
