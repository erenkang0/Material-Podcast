package com.material.podcast.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.material.podcast.data.model.ResumePoint

/**
 * "Continue where you left off" card: cover, episode, where you stopped, and a resume button.
 * Used at the top of Home and inside Library.
 */
@Composable
fun ContinueListeningCard(
    point: ResumePoint,
    onResume: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ep = point.episode
    ElevatedCard(
        onClick = onResume,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                "Kaldığınız yerden devam edin",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PodcastArtwork(
                    imageUrl = ep.artworkUrl,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.size(60.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        ep.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${ep.podcastTitle} · ${formatClock(point.positionMs)} konumunda kaldınız",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(8.dp))
                FilledIconButton(onClick = onResume) {
                    Icon(Icons.Rounded.PlayArrow, "Devam et")
                }
            }
            if (point.fraction > 0f) {
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { point.fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                )
            }
        }
    }
}

/** A compact one-line resume hint, e.g. for the podcast details page. */
@Composable
fun ResumeHint(point: ResumePoint, onResume: () -> Unit, modifier: Modifier = Modifier) {
    ElevatedCard(onClick = onResume, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .padding(0.dp),
                contentAlignment = Alignment.Center,
            ) {
                FilledIconButton(onClick = onResume, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Rounded.PlayArrow, "Devam et")
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "Devam et",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "${point.episode.title} · ${formatClock(point.positionMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun formatClock(ms: Long): String {
    val totalSec = (ms / 1000).toInt().coerceAtLeast(0)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
