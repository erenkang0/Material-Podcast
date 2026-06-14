package com.material.podcast.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Diversity3
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.SportsBasketball
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Structural placeholder data. Per the design brief there is intentionally no real podcast
 * content here — only neutral text and Material icons standing in for cover art, so the layout
 * and component structure can be evaluated on their own terms.
 */

data class Show(
    val id: Int,
    val title: String,
    val author: String,
    val category: String,
    val episodes: Int,
    val icon: ImageVector = Icons.Rounded.Podcasts,
)

data class Episode(
    val id: Int,
    val showTitle: String,
    val title: String,
    val duration: String,
    val date: String,
    val progress: Float = 0f,
    val icon: ImageVector = Icons.Rounded.Headphones,
)

data class Category(
    val name: String,
    val supporting: String,
    val icon: ImageVector,
)

object MockData {

    val featured: List<Show> = List(6) { i ->
        Show(
            id = i,
            title = "Featured Show ${i + 1}",
            author = "Creator name",
            category = categoryNames[i % categoryNames.size],
            episodes = 12 + i * 7,
            icon = showIcons[i % showIcons.size],
        )
    }

    val library: List<Show> = List(8) { i ->
        Show(
            id = 100 + i,
            title = "Saved Show ${i + 1}",
            author = "Creator name",
            category = categoryNames[i % categoryNames.size],
            episodes = 8 + i * 4,
            icon = showIcons[i % showIcons.size],
        )
    }

    val episodes: List<Episode> = List(10) { i ->
        Episode(
            id = i,
            showTitle = "Featured Show ${(i % 6) + 1}",
            title = "Episode title placeholder ${i + 1}",
            duration = "${24 + (i * 5) % 40} min",
            date = "Placeholder date",
            progress = when (i % 4) { 0 -> 0.7f; 1 -> 0.25f; 2 -> 0f; else -> 0.95f },
            icon = showIcons[i % showIcons.size],
        )
    }

    val categories: List<Category> = listOf(
        Category("Technology", "Placeholder description", Icons.Rounded.Bolt),
        Category("Science", "Placeholder description", Icons.Rounded.Science),
        Category("Culture", "Placeholder description", Icons.Rounded.Brush),
        Category("Society", "Placeholder description", Icons.Rounded.Diversity3),
        Category("Wellbeing", "Placeholder description", Icons.Rounded.SelfImprovement),
        Category("Stories", "Placeholder description", Icons.Rounded.MenuBook),
        Category("Sports", "Placeholder description", Icons.Rounded.SportsBasketball),
        Category("Interviews", "Placeholder description", Icons.Rounded.Mic),
    )

    /** Genre labels for the Explore filter chips. */
    val genres: List<String> = listOf(
        "Technology", "Science", "Culture", "Society", "Business", "History",
        "Wellbeing", "Stories", "Sports", "Comedy", "Music", "News",
    )
}

private val categoryNames = listOf(
    "Technology", "Science", "Culture", "Society", "Wellbeing", "Stories",
)

private val showIcons = listOf(
    Icons.Rounded.Podcasts,
    Icons.Rounded.Mic,
    Icons.Rounded.Headphones,
    Icons.Rounded.AutoAwesome,
    Icons.Rounded.Science,
    Icons.Rounded.MenuBook,
)
