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
import com.material.podcast.ui.screens.HomeScreen
import com.material.podcast.ui.screens.LibraryScreen
import com.material.podcast.ui.screens.SearchScreen
import com.material.podcast.ui.screens.SettingsScreen
import com.material.podcast.ui.screens.ShowDetailsScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Search : Screen("search")
    data object Library : Screen("library")
    data object Settings : Screen("settings")
    data object ShowDetails : Screen("show/{podcastId}") {
        fun create(id: String) = "show/${id}"
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
                onOpenShow = { navController.navigate(Screen.ShowDetails.create(it)) },
                onOpenThemes = onOpenThemes,
            )
        }

        composable(Screen.Search.route) {
            SearchScreen(
                onOpenShow = { navController.navigate(Screen.ShowDetails.create(it)) },
            )
        }

        composable(Screen.Library.route) {
            LibraryScreen(
                onOpenShow = { navController.navigate(Screen.ShowDetails.create(it)) },
                onOpenSearch = { onSelectTab(Screen.Search.route) },
                onOpenThemes = onOpenThemes,
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onOpenThemes = onOpenThemes)
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
            )
        }
    }
}
