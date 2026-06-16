package com.material.podcast.ui.components

import android.content.Intent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Contactless
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.material.podcast.nfc.NfcShareController

/**
 * Full-screen overlay shown to the SENDER while a podcast is being offered over NFC.
 *
 * Shows the podcast it's sharing, an animated "bekleme animasyonu" (concentric expanding
 * rings + a gently pulsing Contactless icon), an instruction to tap phones together, and a
 * graceful fallback to the system share sheet. [onDismiss] should stop the active share.
 */
@Composable
fun NfcShareSheet(onDismiss: () -> Unit) {
    val payload = NfcShareController.activeShare ?: return
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Box(Modifier.fillMaxSize()) {
                // Close affordance, top-end.
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Kapat")
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    PodcastArtwork(
                        imageUrl = payload.artworkUrl,
                        modifier = Modifier.size(96.dp),
                        shape = RoundedCornerShape(20.dp),
                    )

                    Spacer(Modifier.height(20.dp))

                    Text(
                        text = payload.title,
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (payload.author.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = payload.author,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Spacer(Modifier.height(48.dp))

                    RadarPulse()

                    Spacer(Modifier.height(48.dp))

                    Text(
                        text = "Telefonun arkasını diğer telefona yaklaştır",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "NFC'nin açık olduğundan emin ol",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(Modifier.height(40.dp))

                    FilledTonalButton(
                        onClick = {
                            val link = NfcShareController.buildUri(payload)
                            val body = buildString {
                                append(payload.title)
                                if (payload.feedUrl.isNotBlank()) {
                                    append("\n")
                                    append(payload.feedUrl)
                                }
                                append("\n")
                                append(link)
                            }
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, body)
                                putExtra(Intent.EXTRA_SUBJECT, payload.title)
                            }
                            context.startActivity(
                                Intent.createChooser(send, "Paylaş")
                            )
                        },
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Paylaş menüsüyle gönder")
                    }
                }
            }
        }
    }
}

/**
 * The "bekleme animasyonu": three concentric rings continuously expanding and fading out
 * from behind a gently pulsing Contactless icon.
 */
@Composable
private fun RadarPulse() {
    val transition = rememberInfiniteTransition(label = "radar")

    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    val ringCount = 3
    val rings = (0 until ringCount).map { i ->
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
                initialStartOffset = androidx.compose.animation.core.StartOffset(i * 600),
            ),
            label = "ring$i",
        )
    }

    val ringColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier.size(220.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val maxRadius = size.minDimension / 2f
                    rings.forEach { ring ->
                        val p = ring.value
                        val radius = maxRadius * (0.35f + 0.65f * p)
                        val alpha = (1f - p) * 0.45f
                        drawCircle(
                            color = ringColor,
                            radius = radius,
                            alpha = alpha,
                        )
                    }
                },
        )
        Box(
            modifier = Modifier
                .size(96.dp)
                .graphicsLayer { scaleX = pulse; scaleY = pulse }
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = androidx.compose.foundation.shape.CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Contactless,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(48.dp),
            )
        }
    }
}
