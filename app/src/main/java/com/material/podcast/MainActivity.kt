package com.material.podcast

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Contactless
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import com.material.podcast.nfc.MomentShareController
import com.material.podcast.nfc.NfcShareController
import com.material.podcast.ui.components.NfcShareSheet
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

    private val nfcAdapter: NfcAdapter? by lazy { NfcAdapter.getDefaultAdapter(this) }

    /**
     * Reader-mode callback: while this phone is *receiving* (not actively sending a share), it
     * polls for NFC tags. Another Echoes phone in HCE mode looks like a Type-4 NDEF tag carrying
     * our `echoes://…` URI record — we read it directly here so a share lands even without relying
     * on the system's tag-dispatch intent.
     */
    private val readerCallback = NfcAdapter.ReaderCallback { tag: Tag ->
        readEchoesUri(tag)?.let { uri ->
            runOnUiThread { NfcShareController.onReceived(uri) }
        }
    }

    private fun readEchoesUri(tag: Tag): String? {
        val ndef = Ndef.get(tag) ?: return null
        return try {
            ndef.connect()
            val message = ndef.ndefMessage ?: ndef.cachedNdefMessage
            message?.records?.firstNotNullOfOrNull { record ->
                runCatching { record.toUri()?.toString() }.getOrNull()
                    ?.takeIf { it.startsWith("echoes://") }
            }
        } catch (_: Exception) {
            null
        } finally {
            runCatching { ndef.close() }
        }
    }

    /**
     * Reader mode and host-card-emulation can't run at once on the same NFC controller, so we only
     * poll for incoming shares while we aren't broadcasting one ([NfcShareController.activeShare]
     * is null). Called from [onResume] and whenever the active-share state flips.
     */
    private fun updateNfcReaderMode() {
        val adapter = nfcAdapter ?: return
        if (NfcShareController.activeShare == null) {
            val flags = NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS
            adapter.enableReaderMode(this, readerCallback, flags, null)
        } else {
            adapter.disableReaderMode(this)
        }
    }

    override fun onResume() {
        super.onResume()
        updateNfcReaderMode()
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }

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
        handleShareIntent(intent)
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
                    // Toggle NFC reader mode whenever we start/stop broadcasting a share, so the
                    // controller flips between host-card-emulation (sending) and reader (receiving).
                    LaunchedEffect(NfcShareController.activeShare) { updateNfcReaderMode() }
                    if (NfcShareController.activeShare != null) {
                        NfcShareSheet(onDismiss = { NfcShareController.stopShare() })
                    }
                    if (showOpening) {
                        OpeningScreen(onDone = { showOpening = false })
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // singleTask delivers re-launch intents here (e.g. a received NFC/deep-link share).
        setIntent(intent)
        handleShareIntent(intent)
    }

    /** Route an incoming `echoes://…` deep link (NFC NDEF dispatch or browser) to a controller. */
    private fun handleShareIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme != "echoes") return
        when (data.host) {
            "share" -> NfcShareController.onReceived(data.toString())
            "moment" -> MomentShareController.onReceived(data.toString())
        }
    }
}

private data class NavItem(val route: String, val label: String, val icon: ImageVector)

@Composable
private fun PodcastApp(themeController: ThemeController) {
    val player = LocalPlayer.current

    // Material You: when a podcast is playing, blend the primary color toward the cover art seed.
    val seed = player.artworkColorSeed
    val baseScheme = MaterialTheme.colorScheme
    val isDark = baseScheme.surface.luminance() < 0.5f
    val blendFraction = if (isDark) 0.30f else 0.22f
    val targetPrimary = if (seed != 0) lerp(baseScheme.primary, Color(seed), blendFraction) else baseScheme.primary
    val targetContainer = if (seed != 0) lerp(baseScheme.primaryContainer, Color(seed).copy(alpha = 0.4f), 0.35f) else baseScheme.primaryContainer
    val dynamicPrimary by animateColorAsState(targetPrimary, tween(700), label = "dynPrimary")
    val dynamicContainer by animateColorAsState(targetContainer, tween(700), label = "dynContainer")
    val dynamicScheme = if (seed != 0) baseScheme.copy(primary = dynamicPrimary, primaryContainer = dynamicContainer) else baseScheme

    MaterialTheme(colorScheme = dynamicScheme) {

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
    // Show the mini player on the main tabs AND on the show-details (episodes) screen.
    val showMiniPlayer = showBottomBar || currentRoute == Screen.ShowDetails.route

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
                visible = showMiniPlayer,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                // When the bottom nav is visible it supplies its own navigation-bar inset; on
                // screens without it (e.g. ShowDetails) the mini bar needs that inset itself so it
                // doesn't sit flush against the system gesture area.
                Column(
                    modifier = if (showBottomBar) Modifier else Modifier.navigationBarsPadding(),
                ) {
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
                    AnimatedVisibility(
                        visible = showBottomBar,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut(),
                    ) {
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

    // A playable moment link was received: jump straight into playback at the shared second.
    val pendingMoment = MomentShareController.pendingMoment
    LaunchedEffect(pendingMoment) {
        if (pendingMoment != null) {
            val episode = MomentShareController.toEpisode(pendingMoment)
            player.play(episode, startPositionMs = pendingMoment.startMs)
            player.expandSheet = true
            MomentShareController.pendingMoment = null
        }
    }

    // A share was received: show a brief "açılıyor" overlay, then navigate to the show.
    val pendingOpenId = NfcShareController.pendingOpenId
    if (pendingOpenId != null) {
        LaunchedEffect(pendingOpenId) {
            delay(600)
            navController.navigate(Screen.ShowDetails.create(pendingOpenId))
            NfcShareController.pendingOpenId = null
        }
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            ) {
                CircularProgressIndicator()
                androidx.compose.foundation.layout.Spacer(Modifier.padding(8.dp))
                Text(
                    text = "Podcast açılıyor…",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }

    } // end MaterialTheme(colorScheme = dynamicScheme)
}
