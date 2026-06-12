package com.material.podcast.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.material.podcast.ui.screens.HomeScreen
import com.material.podcast.ui.screens.LibraryScreen
import com.material.podcast.ui.screens.PlayerScreen
import com.material.podcast.ui.screens.SearchScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Player : Screen("player")
    object Search : Screen("search")
    object Library : Screen("library")
}

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it / 3 },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(spring(stiffness = Spring.StiffnessMediumLow))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it / 3 },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeOut(spring(stiffness = Spring.StiffnessMediumLow))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it / 3 },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(spring(stiffness = Spring.StiffnessMediumLow))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it / 3 },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeOut(spring(stiffness = Spring.StiffnessMediumLow))
        }
    ) {
        composable(Screen.Home.route) {
            HomeScreen(onNavigateToPlayer = { navController.navigate(Screen.Player.route) })
        }
        composable(Screen.Player.route) {
            PlayerScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.Search.route) {
            SearchScreen()
        }
        composable(Screen.Library.route) {
            LibraryScreen(onNavigateToPlayer = { navController.navigate(Screen.Player.route) })
        }
    }
}
