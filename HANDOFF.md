# Echoes — Developer Handoff

> **App**: Echoes  
> **Version**: 0.7-beta  
> **Stack**: Kotlin 2.0 · Compose BOM 2024.12.01 · Material 3 · Media3 ExoPlayer 1.5.1  
> **minSdk** 31 · **compileSdk / targetSdk** 36 · AGP 8.7.3 · Java 11  

---

## 1. Architecture overview

```
MainActivity
 └─ EchoesTheme  (5 palettes × light/dark, persisted to SharedPreferences)
     └─ PodcastApp  (Scaffold: NavigationBar · NowPlayingBar · FullPlayerSheet)
         └─ AppNavHost
             home · search · library · settings · setup
             show/{id} · author/{name} · category-editor
             queue · stats · transcript/{episodeGuid}
```

### Data layer

| Class | Purpose |
|---|---|
| `PodcastRepository` | iTunes Search API + RSS episode feeds (Retrofit / OkHttp). Parses `<podcast:transcript>` tags. |
| `LibraryStore` | **Single source of truth** for all user data: follows, likes, moments, downloads, history, resume points, stats, per-podcast notification state. Backed by SharedPreferences + Gson. |
| `SettingsStore` | Theme selection, language, default playback speed, first-run flag. |
| `DownloadManager` | Streams episode audio to internal storage; `localUriOrNull(guid)` lets the player prefer offline copies automatically. |

### Media layer

| Class | Purpose |
|---|---|
| `PlaybackService` | `MediaSessionService` wrapping an ExoPlayer. Handles `WAKE_LOCK`, headphone-disconnect pause, `onTaskRemoved` cleanup. |
| `SmartAudioCallback` | Inner `MediaSession.Callback` in `PlaybackService`. Handles `CMD_SKIP_SILENCE` and `CMD_VOICE_BOOST` custom session commands. |
| `EpisodeCheckWorker` | `CoroutineWorker` run every 2 h via WorkManager. Checks RSS for followed podcasts; fires per-podcast notifications on new episodes. |
| `SnipExporter` | Uses Media3 Transformer to clip a 30/60-second segment and share via `FileProvider`. |
| `NowPlayingWidget` | `AppWidgetProvider` showing the current episode; refreshed from `PlayerViewModel.maybeSaveProgress()`. |

### ViewModel layer

`PlayerViewModel` (single instance, `AndroidViewModel`) is the sole bridge between the UI and `PlaybackService`.

- Connects via `MediaController` (never creates its own ExoPlayer).
- `restoreFromController(ctrl)` — called when the process is recreated (e.g. notification tap). Resyncs queue, `nowPlaying`, position, and **sets `expandSheet = true`** so Now Playing surfaces automatically.
- `pendingPlay` — if the controller isn't ready yet, the pending action is stored and executed in the `controllerFuture` listener.
- Writes stats every 250 ms via `LibraryStore.recordListen(delta, episode)`.
- Saves resume points and refreshes the widget every 5 s during playback.

Provided to the Compose tree via `CompositionLocal`:

```kotlin
val LocalPlayer = staticCompositionLocalOf<PlayerViewModel> { error("No PlayerViewModel") }
```

---

## 2. Feature map

### Home / Keşfet
- Greeting + date in a collapsing top bar.
- **Senin İçin** recommended rail — derived from most-followed/listened genre, excludes already-followed shows.
- **Continue listening** resume card.
- One `LazyRow` per **user-editable category** (up to 10). Top-right edit opens `CategoryEditorScreen`.
- Single fade-in animation on first load (`animateFloatAsState`, `tween(420)` on root `Box` alpha). No per-row re-trigger on scroll.

### Search
- Native `SearchBar` with genre `FilterChip`s.
- **En son baktıkların** recently-viewed rail replaces the old "popular" placeholder.

### Show Details
- `LargeTopAppBar` with collapsing cover.
- **Animated follow button**: shows "Takip ediliyor" for 3 s, collapses to a compact check icon with notification + download buttons revealed via `AnimatedVisibility(expandHorizontally)`. Already-followed shows open directly in collapsed state (no replay).
- Working ⋮ menu: follow/unfollow, view producer, share, copy link, refresh.

### Now Playing — NowPlayingBar
- Restyled mini-player: artwork, title, prev/play-pause/next controls, flush 2.5 dp progress bar.
- Horizontal swipe to skip (spring snap-back, haptic on commit).
- Vertical swipe-up or tap → opens `FullPlayerSheet`.

### Now Playing — FullPlayerSheet
- Cover-tinted dynamic surface (AndroidX Palette, animates on track change).
- `StaticSeekBar` (formerly `WavySeekBar`) with moment markers as dots above the line.
- Speed slider (0.5–2×, 6 steps).
- Smart audio `FilterChip`s: **Sessizlikleri atla** (skip silence) + **Ses yükselt** (voice boost) — routed via `SessionCommand` to `PlaybackService`.
- Sleep timer panel: **Bölüm bitince** + 5–90 min options.
- Secondary controls: Transcript · Sleep timer · Snip · Bookmark moment · Download · Queue.
- **Full-screen mode**: swipe up on the drag handle → opens a `Dialog` with no chrome (covers status bar). Swipe down anywhere → returns to sheet.

### Queue — QueueScreen
- Separate full screen (route: `queue`).
- Shows current episode, up-next list, history. Tapping any row plays from that position.

### Transcript — TranscriptScreen
- Route: `transcript/{episodeGuid}`.
- Fetches the `<podcast:transcript>` URL (WebVTT or SRT) via repository.
- `OutlinedTextField` for live search across cue text.
- Tapping a cue calls `player.seekToMs(cue.startMs)`.
- Current cue is highlighted by comparing `player.positionMs`.

### Library
Five tabs: **Saved · Likes · Notes · Downloads · Recent** plus a resume card.

### Stats — StatsScreen
- Route: `stats`, reached from Library top bar.
- Total listening time card, 7-day bar chart (Turkish weekday labels), top-shows progress bars.
- Reset button clears `LibraryStore.resetStats()`.

### Settings
- Theme (5 palettes × light/dark), language (EN/TR, app restarts on change), default speed.
- Background section: battery optimization request, re-run onboarding.
- Backup section: export/import full library as JSON.

### Setup / Onboarding
- Welcome flow: notification permission, battery optimization disable.
- Re-runnable from Settings.

### Opening animation
- `OpeningScreen` composable: 5 `Animatable` bars animate with spring stagger (equalizer motif), "Echoes" wordmark fades in, then the overlay fades out after ~1150 ms.
- Layered over the main Scaffold in `MainActivity` using `AnimatedVisibility`.

---

## 3. Key files quick-reference

| File | What's notable |
|---|---|
| `MainActivity.kt` | `installSplashScreen()`, WorkManager enqueue, `OpeningScreen` overlay, wires `FullPlayerSheet` callbacks |
| `PlayerViewModel.kt` | All playback state; `restoreFromController` for notification tap; `pendingPlay` for async connect |
| `PlaybackService.kt` | `SmartAudioCallback`, custom session commands, `LoudnessEnhancer`, cleanup hooks |
| `PlayerSheet.kt` | `NowPlayingBar` + `FullPlayerSheet` (with full-screen Dialog mode) + `FullPlayerContent` |
| `WavySeekBar.kt` | Static seek bar with moment markers (dots above the line) |
| `LibraryStore.kt` | All persistence; key constants start with `KEY_` |
| `SettingsStore.kt` | `getTheme` / `setTheme` (format: `"PaletteName|DarkMode"`, e.g. `"Indigo|System"`) |
| `Theme.kt` | `rememberThemeController()` reads initial theme from `SettingsStore`; `LaunchedEffect` persists changes |
| `Models.kt` | `PodcastEpisode` (includes `transcriptUrl`), `TranscriptCue`, `ListenStats`, `FavoriteMoment` |
| `RssParser.kt` | Captures `<podcast:transcript>` → `PodcastEpisode.transcriptUrl` |
| `TranscriptParser.kt` | Regex VTT/SRT parser; `parseTimeMs` handles both `HH:MM:SS.mmm` and `MM:SS.mmm` |
| `SnipExporter.kt` | Media3 Transformer clip + `FileProvider` share |
| `NowPlayingWidget.kt` | `AppWidgetProvider`; companion `refresh(context)` called from ViewModel |
| `EpisodeCheckWorker.kt` | WorkManager 2-hour RSS check → per-podcast notifications |
| `AppNavigation.kt` | All routes: `home`, `search`, `library`, `settings`, `setup`, `show/{id}`, `author/{name}`, `category-editor`, `queue`, `stats`, `transcript/{episodeGuid}` |

---

## 4. Navigation routes

| Route constant | Destination |
|---|---|
| `"home"` | `HomeScreen` |
| `"search"` | `SearchScreen` |
| `"library"` | `LibraryScreen` |
| `"settings"` | `SettingsScreen` |
| `"setup"` | `SetupScreen` |
| `"show/{podcastId}"` | `ShowDetailsScreen` |
| `"author/{authorName}"` | `AuthorScreen` |
| `"category-editor"` | `CategoryEditorScreen` |
| `"queue"` | `QueueScreen` |
| `"stats"` | `StatsScreen` |
| `"transcript/{episodeGuid}"` | `TranscriptScreen` |

---

## 5. Permissions

| Permission | Why |
|---|---|
| `INTERNET` | API catalogue + RSS + audio streaming |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Background audio with media notification |
| `POST_NOTIFICATIONS` | Playback notification (Android 13+) + new-episode alerts |
| `WAKE_LOCK` | CPU stays awake during network playback |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Optional; offered in setup flow |
| `RECEIVE_BOOT_COMPLETED` | Re-register WorkManager on device reboot |

---

## 6. Known gaps / things to verify on device

| Area | Notes |
|---|---|
| **Snip export** | Media3 Transformer runs on-device; the output is an `.mp4` audio container in `context.cacheDir/snips/`. Verified to compile; real clip length and audio fidelity need a physical device test. |
| **Transcript sync** | VTT/SRT regex parser works for standard formats; edge cases (multi-line cues, overlapping times, non-standard time formats) may need fixing after testing with real feeds. |
| **Widget layout** | `NowPlayingWidget` uses `RemoteViews`; the layout XML (`R.layout.widget_now_playing`) must match what's in `res/layout/`. Verify it renders correctly on different launcher sizes. |
| **WorkManager on battery-optimized devices** | On Xiaomi/Samsung, WorkManager constraints may defer the 2-hour check significantly. The battery optimization setup screen helps, but can't guarantee timeliness. |
| **LoudnessEnhancer** | `AudioEffect` availability is hardware-dependent. The service catches exceptions at construction, but test on real devices to ensure voice boost fails gracefully when unsupported. |

---

## 7. Build & release

```bash
# Debug APK
./gradlew :app:assembleDebug

# Release APK (minify disabled — enable and configure ProGuard before production)
./gradlew :app:assembleRelease
```

Every push to `release` triggers the GitHub Actions workflow (`.github/workflows/build.yml`) which builds a debug APK and publishes it as a Release artifact.

Current version: **versionCode = 7**, **versionName = "0.7-beta"**.

---

## 8. Commit history (summary)

| Tag | What shipped |
|---|---|
| v0.1-beta | Foundation: iTunes API, ExoPlayer, 5 screens, 5 themes |
| v0.2-beta | Settings screen, NowPlayingBar physics, FullPlayerSheet |
| v0.3-beta | MediaController architecture (real background audio), speed slider, sleep timer |
| v0.4-beta | LibraryStore persistence, Likes/Notes/Downloads, dynamic player bg, Explore editor |
| v0.5-beta | Session restore, real queue (next/previous), wavy seek bar |
| v0.6-beta | Static seek bar with moment markers, follow-button animation, new-episode notifications, navigation cleanup, theme persistence, opening animation, home animation fix |
| v0.7-beta | Smart audio (skip silence, voice boost), Senin İçin rail, Transcripts, Stats + widget, Snip export, calmer home animation, logo opening |
| post-v0.7 | Fix: Now Playing restores on notification tap; Full-screen player mode on second swipe-up |

---

*Built by Erenkang0 & Claude (Anthropic).*
