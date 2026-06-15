@file:OptIn(ExperimentalMaterial3Api::class)

package com.material.podcast.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.material.podcast.data.model.PodcastEpisode
import com.material.podcast.ui.LocalPlayer
import com.material.podcast.ui.components.PodcastArtwork

/**
 * A dedicated "Sıradakiler" screen: upcoming episodes of the current show followed by
 * previously played history. Replaces the in-sheet queue panel.
 */
@Composable
fun QueueScreen(onBack: () -> Unit) {
    val player = LocalPlayer.current
    val haptics = LocalHapticFeedback.current
    val upNext = player.upNext
    val history = player.history

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sıradakiler", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Geri")
                    }
                },
            )
        },
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 16.dp),
        ) {
            player.nowPlaying?.let { current ->
                item(key = "now_header") { SectionHeader("Şu an çalıyor") }
                item(key = "now_${current.guid}") {
                    QueueListRow(current, isCurrent = true) {}
                }
            }

            item(key = "up_header") { SectionHeader("Sıradaki") }
            if (upNext.isEmpty()) {
                item(key = "up_empty") {
                    Text(
                        "Bu bölümden sonrası yok",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            } else {
                items(upNext, key = { "up_${it.guid}" }) { ep ->
                    QueueListRow(ep, isCurrent = false) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        player.playQueueIndex(player.queue.indexOfFirst { q -> q.guid == ep.guid })
                    }
                }
            }

            if (history.isNotEmpty()) {
                item(key = "hist_header") { SectionHeader("Önceki dinlediklerim") }
                items(history, key = { "hist_${it.guid}" }) { ep ->
                    QueueListRow(ep, isCurrent = ep.guid == player.nowPlaying?.guid) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        player.play(ep)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp),
    )
}

@Composable
private fun QueueListRow(
    episode: PodcastEpisode,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        PodcastArtwork(
            imageUrl = episode.artworkUrl,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                episode.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                episode.podcastTitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (isCurrent) {
            Icon(
                Icons.Rounded.GraphicEq, "Şu an çalıyor",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
