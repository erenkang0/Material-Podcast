@file:OptIn(ExperimentalMaterial3Api::class)

package com.material.podcast.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.LocationSearching
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.material.podcast.EchoesApplication
import com.material.podcast.data.model.TranscriptCue
import com.material.podcast.ui.LocalPlayer

@Composable
fun TranscriptScreen(onBack: () -> Unit) {
    val player = LocalPlayer.current
    val episode = player.nowPlaying

    var cues by remember { mutableStateOf<List<TranscriptCue>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(episode?.transcriptUrl) {
        loading = true
        cues = episode?.transcriptUrl
            ?.takeIf { it.isNotBlank() }
            ?.let { EchoesApplication.instance.repository.fetchTranscript(it) }
            ?: emptyList()
        loading = false
    }

    val searching = query.isNotBlank()
    val filtered = if (!searching) cues
    else cues.filter { it.text.contains(query, ignoreCase = true) }

    val hasTimestamps = remember(cues) { cues.any { it.startMs >= 0 } }

    // Index (within the unfiltered list) of the cue currently being spoken.
    val currentIndex = remember(player.positionMs, cues) {
        if (!hasTimestamps) -1
        else cues.indexOfLast { it.startMs in 0..player.positionMs }
    }

    // Follow the playhead: keep the active line comfortably in view while not searching.
    var autoFollow by remember { mutableStateOf(true) }
    LaunchedEffect(currentIndex, searching, autoFollow) {
        if (autoFollow && !searching && currentIndex >= 0) {
            listState.animateScrollToItem(currentIndex.coerceAtLeast(0))
        }
    }
    // Manual scrolling pauses auto-follow until the user taps "follow" again.
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) autoFollow = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transkript", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Geri")
                    }
                },
            )
        },
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Transkriptte ara") },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                trailingIcon = {
                    if (searching) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Rounded.Clear, "Temizle")
                        }
                    }
                },
                singleLine = true,
            )

            if (searching) {
                Text(
                    text = "${filtered.size} sonuç",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
            }

            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                cues.isEmpty() -> Box(
                    Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Bu bölüm için transkript bulunamadı.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                searching && filtered.isEmpty() -> Box(
                    Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "\"$query\" için sonuç yok.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> Box(Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 16.dp, vertical = 8.dp,
                        ),
                    ) {
                        items(
                            count = filtered.size,
                            key = { i -> "${filtered[i].startMs}-$i" },
                        ) { i ->
                            val cue = filtered[i]
                            val isCurrent = !searching &&
                                cue.startMs >= 0 &&
                                cue === cues.getOrNull(currentIndex)
                            CueRow(
                                cue = cue,
                                isCurrent = isCurrent,
                                query = query,
                                seekable = cue.startMs >= 0,
                                onClick = { if (cue.startMs >= 0) player.seekToMs(cue.startMs) },
                            )
                        }
                    }

                    // Re-engage auto-follow if the user scrolled away while audio is playing.
                    if (!autoFollow && !searching && currentIndex >= 0) {
                        FilledTonalButton(
                            onClick = { autoFollow = true },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp),
                        ) {
                            Icon(Icons.Rounded.LocationSearching, null, Modifier.width(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Çalan satıra dön")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CueRow(
    cue: TranscriptCue,
    isCurrent: Boolean,
    query: String,
    seekable: Boolean,
    onClick: () -> Unit,
) {
    val bg by animateColorAsState(
        if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        else androidx.compose.ui.graphics.Color.Transparent,
        label = "cueBg",
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(enabled = seekable, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        if (seekable) {
            Text(
                formatStamp(cue.startMs),
                style = MaterialTheme.typography.labelSmall,
                color = if (isCurrent) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            text = highlight(cue.text, query, MaterialTheme.colorScheme.tertiary),
            style = MaterialTheme.typography.bodyLarge,
            color = if (isCurrent) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/** Build an annotated string emphasising every case-insensitive match of [query]. */
private fun highlight(text: String, query: String, color: androidx.compose.ui.graphics.Color) =
    buildAnnotatedString {
        if (query.isBlank()) {
            append(text)
            return@buildAnnotatedString
        }
        var start = 0
        while (true) {
            val idx = text.indexOf(query, start, ignoreCase = true)
            if (idx < 0) {
                append(text.substring(start))
                break
            }
            append(text.substring(start, idx))
            withStyle(SpanStyle(color = color, fontWeight = FontWeight.Bold)) {
                append(text.substring(idx, idx + query.length))
            }
            start = idx + query.length
        }
    }

private fun formatStamp(ms: Long): String {
    val totalSec = (ms / 1000).toInt()
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}
