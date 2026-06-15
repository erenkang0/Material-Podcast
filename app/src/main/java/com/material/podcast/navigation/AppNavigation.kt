package com.material.podcast.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.material.podcast.ui.screens.AuthorScreen
import com.material.podcast.ui.screens.CategoryEditorScreen
import com.material.podcast.ui.screens.HomeScreen
import com.material.podcast.ui.screens.LibraryScreen
import com.material.podcast.ui.screens.QueueScreen
import com.material.podcast.ui.screens.SearchScreen
import com.material.podcast.ui.screens.SettingsScreen
import com.material.podcast.ui.screens.ShowDetailsScreen
import com.material.podcast.ui.screens.StatsScreen
import java.net.URLDecoder
import java.net.URLEncoder

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Search : Screen("search")
    data object Library : Screen("library")
    data object Settings : Screen("settings")
    data object CategoryEditor : Screen("category_editor")
    data object Queue : Screen("queue")
    data object Stats : Screen("stats")
    data object ShowDetails : Screen("show/{podcastId}") {
        fun create(id: String) = "show/${id}"
    }
    data object Author : Screen("author/{name}") {
        fun create(name: String) = "author/${URLEncoder.encode(name, "UTF-8")}"
    }
}

private val SlideSpring = spring<IntOffset>(dampingRatio = 0.88f, stiffness = Spring.StiffnessMediumLow)

@Composable
fun AppNavHost(
    navController: NavHostController,
    onOpenThemes: () -> Unit,
    onSelectTab: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val openShow: (String) -> Unit = { navController.navigate(Screen.ShowDetails.create(it)) }
    val openAuthor: (String) -> Unit = { navController.navigate(Screen.Author.create(it)) }

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier,
        enterTransition = {
            fadeIn(tween(220, 80)) + scaleIn(initialScale = 0.94f, animationSpec = tween(220, 80))
        },
        exitTransition = { fadeOut(tween(110)) },
        popEnterTransition = {
            fadeIn(tween(220, 80)) + scaleIn(initialScale = 0.94f, animationSpec = tween(220, 80))
        },
        popExitTransition = { fadeOut(tween(110)) },
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onOpenShow = openShow,
                onOpenThemes = onOpenThemes,
                onEditCategories = { navController.navigate(Screen.CategoryEditor.route) },
            )
        }

        composable(Screen.Search.route) {
            SearchScreen(onOpenShow = openShow)
        }

        composable(Screen.Library.route) {
            LibraryScreen(
                onOpenShow = openShow,
                onOpenSearch = { onSelectTab(Screen.Search.route) },
                onOpenThemes = onOpenThemes,
                onOpenSettings = { navController.navigate(Screen.Settings.route) },
                onOpenStats = { navController.navigate(Screen.Stats.route) },
            )
        }

        composable(Screen.Queue.route) {
            QueueScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Stats.route) {
            StatsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onOpenThemes = onOpenThemes)
        }

        composable(Screen.CategoryEditor.route) {
            CategoryEditorScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.ShowDetails.route,
            arguments = listOf(navArgument("podcastId") { type = NavType.StringType }),
            enterTransition = { slideInHorizontally(SlideSpring) { it } + fadeIn(tween(250)) },
            popExitTransition = { slideOutHorizontally(SlideSpring) { it } + fadeOut(tween(250)) },
        ) { entry ->
            val podcastId = entry.arguments?.getString("podcastId") ?: return@composable
            ShowDetailsScreen(
                podcastId = podcastId,
                onBack = { navController.popBackStack() },
                onOpenAuthor = openAuthor,
            )
        }

        composable(
            route = Screen.Author.route,
            arguments = listOf(navArgument("name") { type = NavType.StringType }),
            enterTransition = { slideInHorizontally(SlideSpring) { it } + fadeIn(tween(250)) },
            popExitTransition = { slideOutHorizontally(SlideSpring) { it } + fadeOut(tween(250)) },
        ) { entry ->
            val encoded = entry.arguments?.getString("name") ?: return@composable
            val name = runCatching { URLDecoder.decode(encoded, "UTF-8") }.getOrDefault(encoded)
            AuthorScreen(
                authorName = name,
                onBack = { navController.popBackStack() },
                onOpenShow = openShow,
            )
        }
    }
}
