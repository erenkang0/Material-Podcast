package com.material.podcast.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.material.podcast.data.MockData
import com.material.podcast.ui.theme.*

@Composable
private fun CategoryPill(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

private val categoryColors = listOf(
    listOf(Color(0xFF7C4DFF), Color(0xFFE040FB)),
    listOf(Color(0xFFE040FB), Color(0xFFFF6B6B)),
    listOf(Color(0xFF00BCD4), Color(0xFF7C4DFF)),
    listOf(Color(0xFFFFD740), Color(0xFFFF6B6B)),
    listOf(Color(0xFF69F0AE), Color(0xFF00BCD4)),
    listOf(Color(0xFFFF6B6B), Color(0xFFFFD740)),
    listOf(Color(0xFF7C4DFF), Color(0xFF00BCD4)),
    listOf(Color(0xFFE040FB), Color(0xFF69F0AE)),
    listOf(Color(0xFFFFD740), Color(0xFF7C4DFF)),
    listOf(Color(0xFF00BCD4), Color(0xFFFF6B6B)),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen() {
    var query by remember { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val searchWidth by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0.95f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "searchWidth"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(bottom = 100.dp)
    ) {
        Spacer(Modifier.height(20.dp))

        // Header
        Text(
            "Discover",
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
            color = OnSurface,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Text(
            "Find your next obsession",
            style = MaterialTheme.typography.bodyLarge,
            color = OnSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(Modifier.height(20.dp))

        // Search bar
        Box(
            modifier = Modifier
                .fillMaxWidth(searchWidth)
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 24.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isFocused = it.isFocused },
                placeholder = { Text("Podcasts, episodes, creators...", color = OnSurfaceVariant) },
                leadingIcon = {
                    Icon(Icons.Rounded.Search, null, tint = if (isFocused) Violet60 else OnSurfaceVariant)
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Rounded.Close, null, tint = OnSurfaceVariant)
                        }
                    } else {
                        Icon(Icons.Rounded.Tune, null, tint = OnSurfaceVariant)
                    }
                },
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Violet60,
                    unfocusedBorderColor = GlassStroke,
                    focusedContainerColor = SurfaceContainerHigh,
                    unfocusedContainerColor = SurfaceContainer,
                    focusedTextColor = OnSurface,
                    unfocusedTextColor = OnSurface,
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )
        }

        Spacer(Modifier.height(24.dp))

        // Categories
        Text(
            "Browse Categories",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = OnSurface,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(Modifier.height(12.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(MockData.categories.zip(categoryColors)) { (category, colors) ->
                val isSelected = selectedCategory == category
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.08f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                    label = "chipScale"
                )
                Box(
                    modifier = Modifier
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) Brush.linearGradient(colors)
                            else Brush.linearGradient(listOf(SurfaceContainer, SurfaceContainerHigh))
                        )
                        .border(
                            width = if (isSelected) 0.dp else 1.dp,
                            color = if (isSelected) Color.Transparent else GlassStroke,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { selectedCategory = if (isSelected) null else category }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        category,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) Color.White else OnSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        // Trending Now
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Whatshot, null, tint = Color(0xFFFF6B6B), modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(6.dp))
                Text("Trending Now", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = OnSurface)
            }
            TextButton(onClick = {}) {
                Text("See all", color = Violet60, style = MaterialTheme.typography.labelLarge)
            }
        }

        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MockData.trendingPodcasts.forEach { podcast ->
                val color = Color(podcast.colorHex)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(SurfaceContainer)
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(listOf(color.copy(0.4f), Color.Transparent)),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .clickable {}
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.linearGradient(listOf(color.copy(0.9f), color.copy(0.4f)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Podcasts, null, tint = Color.White, modifier = Modifier.size(26.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(podcast.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = OnSurface)
                        Text(podcast.author, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CategoryPill(podcast.category, color)
                            Text("${podcast.episodeCount} eps", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                        }
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Rounded.Add, null, tint = color, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // New Releases Feature Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(160.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.linearGradient(listOf(Color(0xFF7C4DFF), Color(0xFFE040FB), Color(0xFFFF6B6B)))
                )
                .clickable {}
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0x55000000))))
            )
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                Text("New Releases", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(0.8f))
                Text("Fresh episodes\nthis week", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black), color = Color.White)
            }
            Icon(
                Icons.Rounded.NewReleases,
                null,
                tint = Color.White.copy(0.15f),
                modifier = Modifier.size(120.dp).align(Alignment.TopEnd).offset(x = 20.dp, y = (-10).dp)
            )
        }
    }
}
