package com.material.podcast

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.material.podcast.data.store.SettingsStore
import com.material.podcast.media.EpisodeCheckWorker
import com.material.podcast.ui.screens.SetupScreen
import java.util.concurrent.TimeUnit
import java.util.Locale
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.res.stringResource
import com.material.podcast.R
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
import com.material.podcast.ui.components.FullPlayerSheet
import com.material.podcast.ui.components.NowPlayingBar
import com.material.podcast.ui.components.OpeningScreen
import com.material.podcast.ui.components.ThemePickerSheet
import com.material.podcast.ui.theme.EchoesTheme
import com.material.podcast.ui.theme.ThemeController
import com.material.podcast.ui.theme.rememberThemeController
import com.material.podcast.ui.viewmodel.PlayerViewModel

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val lang = SettingsStore.getLanguage(newBase)
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "episode_check",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<EpisodeCheckWorker>(2, TimeUnit.HOURS).build()
        )
        enableEdgeToEdge()
        window.attributes.preferredRefreshRate = 120f
        setContent {
            val themeController = rememberThemeController()
            val playerVm: PlayerViewModel = viewModel()
            var onboarded by remember { mutableStateOf(SettingsStore.isOnboarded(this)) }
            var showOpening by remember { mutableStateOf(true) }
            EchoesTheme(controller = themeController) {
                Box(Modifier.fillMaxSize()) {
                    if (!onboarded) {
                        SetupScreen(onFinish = {
                            SettingsStore.setOnboarded(this@MainActivity, true)
                            onboarded = true
                        })
                    } else {
                        CompositionLocalProvider(LocalPlayer provides playerVm) {
                            PodcastApp(
                                themeController = themeController,
                            )
                        }
                    }
                    if (showOpening) {
                        OpeningScreen(onDone = { showOpening = false })
                    }
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
        NavItem(Screen.Home.route, stringResource(R.string.nav_home), Icons.Rounded.Home),
        NavItem(Screen.Search.route, stringResource(R.string.nav_search), Icons.Rounded.Search),
        NavItem(Screen.Library.route, stringResource(R.string.nav_library), Icons.Rounded.LibraryMusic),
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
                        NowPlayingBar(
                            onExpand = { player.expandSheet = true },
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

    if (player.expandSheet) {
        FullPlayerSheet(
            onDismiss = { player.expandSheet = false },
            onOpenShow = { id ->
                player.expandSheet = false
                navController.navigate(Screen.ShowDetails.create(id))
            },
            onOpenAuthor = { name ->
                player.expandSheet = false
                navController.navigate(Screen.Author.create(name))
            },
            onOpenQueue = {
                player.expandSheet = false
                navController.navigate(Screen.Queue.route)
            },
            onOpenTranscript = {
                player.expandSheet = false
                navController.navigate(Screen.Transcript.route)
            },
            onOpenChapters = {
                player.expandSheet = false
                navController.navigate(Screen.Chapters.route)
            },
        )
    }
}
