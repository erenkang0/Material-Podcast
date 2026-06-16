@file:OptIn(ExperimentalMaterial3Api::class)

package com.material.podcast.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.material.podcast.media.MomentCardRenderer
import com.material.podcast.nfc.MomentShareController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Moment Card Studio: turn the current moment (and optional transcript quote) into a beautiful,
 * shareable square card. The live preview is the *actual* rendered bitmap, and "Paylaş" attaches
 * a playable `echoes://moment?…` deep link so a tap opens the app right at this second.
 */
@Composable
fun MomentCardSheet(
    guid: String,
    title: String,
    podcastTitle: String,
    artworkUrl: String,
    audioUrl: String,
    startMs: Long,
    seedColor: Int,
    initialQuote: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var quote by remember { mutableStateOf(initialQuote) }
    var preview by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    fun payload(q: String) = MomentShareController.MomentPayload(
        guid = guid,
        title = title,
        podcastTitle = podcastTitle,
        artworkUrl = artworkUrl,
        audioUrl = audioUrl,
        startMs = startMs,
        quote = q,
    )

    // Re-render the preview shortly after the quote stops changing (cheap debounce).
    LaunchedEffect(quote) {
        delay(180)
        val bmp = withContext(Dispatchers.Default) {
            MomentCardRenderer.renderBitmap(payload(quote), seedColor)
        }
        preview = bmp?.asImageBitmap()
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Anı Kartı",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Bu anı şık bir kartla paylaş — dokununca tam bu saniyeden açılır",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )

            Column(
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .fillMaxWidth(0.78f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(20.dp)),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val p = preview
                if (p != null) {
                    Image(
                        bitmap = p,
                        contentDescription = "Anı kartı önizlemesi",
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    CircularProgressIndicator()
                }
            }

            OutlinedTextField(
                value = quote,
                onValueChange = { quote = it.take(220) },
                label = { Text("Alıntı") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
            )

            Button(
                onClick = {
                    val bmp = MomentCardRenderer.renderBitmap(payload(quote), seedColor) ?: return@Button
                    val file = MomentCardRenderer.save(context, bmp)
                    bmp.recycle()
                    if (file != null) MomentCardRenderer.share(context, file, payload(quote))
                    onDismiss()
                },
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth(),
            ) {
                Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("  Paylaş", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
