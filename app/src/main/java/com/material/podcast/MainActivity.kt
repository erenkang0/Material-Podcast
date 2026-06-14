package com.material.podcast

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.material.podcast.navigation.AppNavHost
import com.material.podcast.navigation.Screen
import com.material.podcast.ui.LocalPlayer
import com.material.podcast.ui.components.MiniPlayer
import com.material.podcast.ui.components.ThemePickerSheet
import com.material.podcast.ui.theme.EchoesTheme
import com.material.podcast.ui.theme.ThemeController
import com.material.podcast.ui.theme.rememberThemeController
import com.material.podcast.ui.viewmodel.PlayerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeController = rememberThemeController()
            val playerVm: PlayerViewModel = viewModel()
            EchoesTheme(controller = themeController) {
                CompositionLocalProvider(LocalPlayer provides playerVm) {
                    PodcastApp(
                        themeController = themeController,
                    )
                }
            }
        }
    }
}

private data class NavItem(val route: String, val label: String, val icon: ImageVector)

@Composable
private fun PodcastApp(themeController: ThemeController) {
    val player = LocalPlayer.current
    val navController = rememberNavController()
    val backEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backEntry?.destination?.route

    var showThemeSheet by remember { mutableStateOf(false) }

    val navItems = listOf(
        NavItem(Screen.Home.route, "Ana Sayfa", Icons.Rounded.Home),
        NavItem(Screen.Search.route, "Keşfet", Icons.Rounded.Search),
        NavItem(Screen.Library.route, "Kitaplık", Icons.Rounded.LibraryMusic),
    )
    val bottomBarRoutes = navItems.map { it.route }.toSet()
    val showBottomBar = currentRoute in bottomBarRoutes

    val selectTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                Column {
                    AnimatedVisibility(
                        visible = player.nowPlaying != null,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut(),
                    ) {
                        MiniPlayer(
                            onExpand = { navController.navigate(Screen.Player.route) },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        )
                    }
                    NavigationBar {
                        navItems.forEach { item ->
                            val selected = backEntry?.destination?.hierarchy
                                ?.any { it.route == item.route } == true
                            val scale by animateFloatAsState(
                                targetValue = if (selected) 1.12f else 1f,
                                animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
                                label = "navScale",
                            )
                            NavigationBarItem(
                                selected = selected,
                                onClick = { selectTab(item.route) },
                                icon = {
                                    Icon(
                                        item.icon, item.label,
                                        modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale },
                                    )
                                },
                                label = { Text(item.label) },
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            onOpenThemes = { showThemeSheet = true },
            onSelectTab = selectTab,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
        )
    }

    if (showThemeSheet) {
        ThemePickerSheet(
            controller = themeController,
            onDismiss = { showThemeSheet = false },
        )
    }
}
