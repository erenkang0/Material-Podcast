@file:OptIn(ExperimentalMaterial3Api::class)

package com.material.podcast.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.material.podcast.data.model.Chapter
import com.material.podcast.ui.LocalPlayer
import com.material.podcast.ui.components.PodcastArtwork

@Composable
fun ChaptersScreen(onBack: () -> Unit) {
    val player = LocalPlayer.current
    val chapters = player.chapters
    val current = player.currentChapterIndex
    val positionMs = player.positionMs
    val listState = rememberLazyListState()

    // Auto-scroll to the current chapter when it changes.
    LaunchedEffect(current) {
        if (current >= 0 && current < chapters.size) {
            listState.animateScrollToItem(current)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bölümler", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Geri")
                    }
                },
            )
        },
    ) { inner ->
        if (chapters.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Bu bölüm için işaret bulunamadı.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            ) {
                items(chapters.size) { i ->
                    val chapter = chapters[i]
                    val nextStartMs = chapters.getOrNull(i + 1)?.startMs ?: player.durationMs
                    val chapterDurationMs = (nextStartMs - chapter.startMs).coerceAtLeast(1L)
                    val chapterProgress = if (i == current && positionMs >= chapter.startMs) {
                        ((positionMs - chapter.startMs).toFloat() / chapterDurationMs).coerceIn(0f, 1f)
                    } else if (i < current) 1f else 0f

                    ChapterRow(
                        index = i + 1,
                        chapter = chapter,
                        isCurrent = i == current,
                        progress = chapterProgress,
                        onClick = { player.seekToChapter(i) },
                    )
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun ChapterRow(
    index: Int,
    chapter: Chapter,
    isCurrent: Boolean,
    progress: Float,
    onClick: () -> Unit,
) {
    val containerColor by animateColorAsState(
        if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        else MaterialTheme.colorScheme.surface,
        animationSpec = tween(400),
        label = "chapterBg",
    )

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(containerColor),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (chapter.imageUrl.isNotBlank()) {
                    PodcastArtwork(
                        imageUrl = chapter.imageUrl,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.size(44.dp),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(
                                if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isCurrent) {
                            Icon(
                                Icons.Rounded.GraphicEq, null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp),
                            )
                        } else {
                            Text(
                                "$index",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        chapter.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        formatChapterStamp(chapter.startMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (isCurrent) {
                    Icon(
                        Icons.Rounded.PlayArrow, "Şu an çalıyor",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            // Within-chapter progress bar — only show for current or already-played chapters.
            if (progress > 0f) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = if (isCurrent) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    trackColor = Color.Transparent,
                )
            }
        }
    }
}

private fun formatChapterStamp(ms: Long): String {
    val totalSec = (ms / 1000).toInt().coerceAtLeast(0)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
