@file:OptIn(ExperimentalMaterial3Api::class)

package com.material.podcast.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocationSearching
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.material.podcast.EchoesApplication
import com.material.podcast.data.model.TranscriptCue
import com.material.podcast.ui.LocalPlayer
import com.material.podcast.ui.theme.playerColorsFromSeed
import androidx.compose.foundation.isSystemInDarkTheme

@Composable
fun TranscriptScreen(onBack: () -> Unit) {
    val player = LocalPlayer.current
    val episode = player.nowPlaying

    var cues by remember { mutableStateOf<List<TranscriptCue>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Immersive colors derived from the now-playing artwork seed.
    val dark = isSystemInDarkTheme()
    val seed = player.artworkColorSeed
    val colors = remember(seed, dark) {
        if (seed != 0) playerColorsFromSeed(seed, dark) else null
    }
    val bg = colors?.background ?: MaterialTheme.colorScheme.background
    val onBg = colors?.onBackground ?: MaterialTheme.colorScheme.onBackground
    val accent = colors?.accent ?: MaterialTheme.colorScheme.primary

    // A calm vertical gradient: tinted at the top, fading toward a darker/lighter base.
    val gradient = remember(bg, dark) {
        Brush.verticalGradient(
            listOf(
                bg,
                if (dark) bg.copy(alpha = 0.92f) else bg,
                if (dark) Color(0xFF050505) else bg.copy(alpha = 0.85f),
            ),
        )
    }

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

    // Centering offset: half the viewport height (in px) so the active line sits mid-screen.
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val centerOffsetPx = remember(configuration.screenHeightDp, density) {
        with(density) { -(configuration.screenHeightDp.dp.toPx() / 2.4f).toInt() }
    }

    // Follow the playhead: keep the active line vertically centered while not searching.
    var autoFollow by remember { mutableStateOf(true) }
    LaunchedEffect(currentIndex, searching, autoFollow) {
        if (autoFollow && !searching && currentIndex >= 0) {
            listState.animateScrollToItem(currentIndex.coerceAtLeast(0), centerOffsetPx)
        }
    }
    // Manual scrolling pauses auto-follow until the user taps "follow" again.
    val userScrolling by remember {
        derivedStateOf { listState.isScrollInProgress }
    }
    LaunchedEffect(userScrolling) {
        if (userScrolling) autoFollow = false
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(gradient),
    ) {
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accent)
            }

            cues.isEmpty() -> EmptyTranscript(onBg = onBg, accent = accent, onBack = onBack)

            else -> {
                if (searching) {
                    SearchableList(
                        filtered = filtered,
                        query = query,
                        onBg = onBg,
                        accent = accent,
                        listState = listState,
                        onSeek = { player.seekToMs(it) },
                    )
                } else {
                    LyricsFlow(
                        cues = cues,
                        currentIndex = currentIndex,
                        onBg = onBg,
                        accent = accent,
                        listState = listState,
                        onSeek = { player.seekToMs(it) },
                    )
                }

                // Re-engage auto-follow if the user scrolled away while audio is playing.
                AnimatedVisibility(
                    visible = !autoFollow && !searching && currentIndex >= 0,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp),
                ) {
                    FilledTonalButton(
                        onClick = { autoFollow = true },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = accent.copy(alpha = 0.9f),
                            contentColor = onBg,
                        ),
                    ) {
                        Icon(Icons.Rounded.LocationSearching, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Çalan satıra dön")
                    }
                }
            }
        }

        // Transparent top bar blended into the immersive background.
        ImmersiveTopBar(
            onBg = onBg,
            accent = accent,
            showSearch = showSearch,
            query = query,
            resultCount = if (searching) filtered.size else null,
            onQueryChange = { query = it },
            onToggleSearch = {
                showSearch = !showSearch
                if (!showSearch) query = ""
            },
            onBack = onBack,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Composable
private fun ImmersiveTopBar(
    onBg: Color,
    accent: Color,
    showSearch: Boolean,
    query: String,
    resultCount: Int?,
    onQueryChange: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(onBg.copy(alpha = 0.0f), Color.Transparent),
                ),
            ),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Geri", tint = onBg)
            }
            Text(
                "Transkript",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = onBg,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onToggleSearch) {
                Icon(
                    if (showSearch) Icons.Rounded.Close else Icons.Rounded.Search,
                    if (showSearch) "Aramayı kapat" else "Ara",
                    tint = onBg,
                )
            }
        }

        AnimatedVisibility(
            visible = showSearch,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    placeholder = { Text("Transkriptte ara", color = onBg.copy(alpha = 0.6f)) },
                    leadingIcon = { Icon(Icons.Rounded.Search, null, tint = onBg.copy(alpha = 0.7f)) },
                    trailingIcon = {
                        if (query.isNotBlank()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(Icons.Rounded.Clear, "Temizle", tint = onBg.copy(alpha = 0.7f))
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = onBg,
                        unfocusedTextColor = onBg,
                        cursorColor = accent,
                        focusedBorderColor = accent,
                        unfocusedBorderColor = onBg.copy(alpha = 0.4f),
                    ),
                )
                if (resultCount != null) {
                    Text(
                        text = "$resultCount sonuç",
                        style = MaterialTheme.typography.labelMedium,
                        color = onBg.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

/** Apple-Music-style flowing karaoke transcript. */
@Composable
private fun LyricsFlow(
    cues: List<TranscriptCue>,
    currentIndex: Int,
    onBg: Color,
    accent: Color,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onSeek: (Long) -> Unit,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        // Generous top/bottom padding lets the first/last lines reach screen center.
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 280.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(
            count = cues.size,
            key = { i -> "${cues[i].startMs}-$i" },
        ) { i ->
            val cue = cues[i]
            val isCurrent = i == currentIndex
            val isPast = currentIndex >= 0 && i < currentIndex
            LyricLine(
                cue = cue,
                isCurrent = isCurrent,
                isPast = isPast,
                onBg = onBg,
                accent = accent,
                seekable = cue.startMs >= 0,
                onClick = { if (cue.startMs >= 0) onSeek(cue.startMs) },
            )
        }
    }
}

@Composable
private fun LyricLine(
    cue: TranscriptCue,
    isCurrent: Boolean,
    isPast: Boolean,
    onBg: Color,
    accent: Color,
    seekable: Boolean,
    onClick: () -> Unit,
) {
    val targetAlpha = when {
        isCurrent -> 1f
        isPast -> 0.3f
        else -> 0.38f
    }
    val targetSize = if (isCurrent) 30f else 22f

    val alpha by animateFloatAsState(targetAlpha, tween(350), label = "lyricAlpha")
    val fontSize by animateFloatAsState(targetSize, tween(350), label = "lyricSize")
    val color by animateColorAsState(
        if (isCurrent) onBg else onBg.copy(alpha = 0.9f),
        tween(350),
        label = "lyricColor",
    )

    Text(
        text = cue.text,
        fontSize = fontSize.sp,
        lineHeight = (fontSize * 1.25f).sp,
        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
        color = color,
        textAlign = TextAlign.Start,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                enabled = seekable,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 10.dp),
    )
}

/** Compact searchable list shown while the user is searching. */
@Composable
private fun SearchableList(
    filtered: List<TranscriptCue>,
    query: String,
    onBg: Color,
    accent: Color,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onSeek: (Long) -> Unit,
) {
    if (filtered.isEmpty()) {
        Box(
            Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "\"$query\" için sonuç yok.",
                style = MaterialTheme.typography.bodyMedium,
                color = onBg.copy(alpha = 0.7f),
            )
        }
        return
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 140.dp, bottom = 24.dp),
    ) {
        items(
            count = filtered.size,
            key = { i -> "${filtered[i].startMs}-$i" },
        ) { i ->
            val cue = filtered[i]
            val seekable = cue.startMs >= 0
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = seekable) { if (seekable) onSeek(cue.startMs) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                if (seekable) {
                    Text(
                        formatStamp(cue.startMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    text = highlight(cue.text, query, accent),
                    style = MaterialTheme.typography.bodyLarge,
                    color = onBg.copy(alpha = 0.92f),
                )
            }
        }
    }
}

@Composable
private fun EmptyTranscript(onBg: Color, accent: Color, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                "Bu bölüm için transkript bulunamadı.",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = onBg,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Geri dönüp dinlemeye devam edebilirsin.",
                style = MaterialTheme.typography.bodyMedium,
                color = onBg.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            FilledTonalButton(
                onClick = onBack,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = accent.copy(alpha = 0.9f),
                    contentColor = onBg,
                ),
            ) {
                Icon(Icons.Rounded.Close, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Kapat")
            }
        }
    }
}

/** Build an annotated string emphasising every case-insensitive match of [query]. */
private fun highlight(text: String, query: String, color: Color) =
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
