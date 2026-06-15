# Changelog

## v0.5-beta — 2026-06-15

### Playback fixes
- **Now Playing awareness** — opening the app from the notification or mini-bar after the
  process was recreated now restores the playing episode and queue (the UI no longer loses
  track of what's playing).
- **Real next / previous** — playing from a show loads the show's episodes as the queue, so
  the transport's next/previous walk actual episodes.
- **Queue panel** — now shows **Sıradaki** (upcoming episodes of the current show) and
  **Önceki dinlediklerim** (history); every row is tappable.

### Now Playing UI
- **Wavy progress** — Material-3-expressive style animated wavy seek bar (drag or tap to
  scrub); the static slider and the cover-art breathing animation are gone.
- **Sleep timer** — horizontally scrollable; first option is **Bölüm bitince** (stop when the
  episode ends), then 5–90 minutes.
- Haptics added across the new controls.

### First-run setup
- A welcome / onboarding flow walks through notifications and **disabling battery
  optimization** so playback isn't killed in the background. Re-runnable from Settings.

### Settings
- Redesigned with cards; a **Background** section (battery, re-run setup) and a **Backup**
  section to **export / import the whole library as JSON**.

### Performance
- Theme switching is now applied instantly instead of animating ~25 color roles at the root
  every frame, removing the jank when changing palette / light-dark.

---

## v0.4-beta — 2026-06-15

### On-device library (never deleted)
- **LibraryStore** — all of the below persist to disk (SharedPreferences + Gson) and
  survive restarts: followed podcasts, liked episodes, favorite moments, downloads,
  Explore categories, resume points and recently-viewed podcasts.

### Likes, notes & downloads
- **Beğeniler** — the Now Playing heart now saves the episode to a Likes list, shown in
  a new Library tab. Liked episodes that have notes get a "Notlu" badge.
- **Favori Anlar** — bookmark the current timestamp with an optional note from Now Playing;
  browse them in the Library "Notlar" tab and tap to replay from that exact second.
- **İndirilenler** — download episode audio for offline playback (internal storage, live
  progress). Playback automatically prefers the offline copy. Manage in the Downloads tab.

### Now Playing
- **Dynamic background** — the surface is tinted from the cover art: deep colour + white
  text in dark theme; a soft, low-saturation tint + black text in light theme.
- **Producer & show navigation** — tap the show title to open its page; tap the producer
  name to open a new Author profile listing all of their podcasts.

### Explore (Home)
- **Continue where you left off** — a card at the top of Home (and Library, and each show
  page) resumes the last episode from the saved position.
- **Editable categories** — the rows on Home are now user-defined. The top-right edit
  button opens an editor where each category has a name and a background search query
  (up to 10 categories), with reset-to-default.

### Search & Library
- **Recently viewed** — Search's "popular" rail is replaced by "En son baktıkların".
- **Library** — redesigned with five tabs (Saved · Likes · Notes · Downloads · Recent).

### Podcast page
- **Working ⋮ menu** — follow/unfollow, view producer, share, copy link, refresh.

---

## v0.3-beta — 2026-06-14

### Background playback fix (critical)
- **MediaController architecture** — `PlayerViewModel` now connects to `PlaybackService` via
  `MediaController` instead of creating its own local `ExoPlayer`. Media3 automatically starts
  and maintains a foreground service with a media notification, preventing Android from killing
  the process when backgrounded.
- **WAKE_LOCK + noisy handling** — `PlaybackService` enables `WAKE_MODE_NETWORK` so the CPU
  stays awake during network playback, and pauses automatically when headphones disconnect.

### Permissions
- `POST_NOTIFICATIONS` — declared in manifest + runtime request on Android 13+.
- `WAKE_LOCK` — required for background audio with wake mode enabled.

### Performance
- **120 fps hint** — `window.attributes.preferredRefreshRate = 120f` set in `MainActivity.onCreate`
  so high-refresh-rate devices render at full speed.

### Now Playing — expanded controls
- **Speed slider** — inline `Slider` (0.5× → 2×, 7 snap steps) always visible above the button
  row, replacing the old upward-opening `DropdownMenu`.
- **Sleep timer** — tap the timer icon to expand a chip panel (10 / 15 / 30 / 45 / 60 min).
  While active, shows live countdown + cancel button. Icon turns primary-colored when running.
- **Queue panel** — tap the queue icon to reveal playback history above the controls.
  Current episode is highlighted with a queue icon.
- **Swipe up on mini player** — vertical swipe up on the `NowPlayingBar` now expands the
  full-screen player sheet (bidirectional drag replaces horizontal-only gesture detection).

---

## v0.2-beta — 2026-06-14

### Settings screen (new)
- **Ayarlar / Settings** — 4th nav tab with full settings screen.
- **Language toggle** — English / Türkçe. Tap to switch; app restarts with
  the new locale applied system-wide via `attachBaseContext`.
- **Color theme** shortcut — opens the existing theme picker sheet.
- **Default playback speed** — choose from 0.75× to 2×, persisted across sessions.
- **About** section with version number and open-source attribution.

### Now Playing — physics-grade redesign
- **NowPlayingBar** — mini player strip with horizontal swipe-to-skip.
  Swipe left → next, swipe right → previous.  
  Spring snap-back with elastic resistance past the commit threshold.
  Haptic pulse on play/pause, strong haptic on swipe commit.
- **FullPlayerSheet** — replaces the old full-screen player with a
  `ModalBottomSheet` that uses Material 3's built-in `AnchoredDraggable`
  spring physics. Swipe down to dismiss.
- **Breathing artwork** — artwork gently scales 1.0 → 1.025 in a slow
  sine wave while playing; springs to 0.96× when paused.
- **Seek haptic ticks** — light haptic every 5% of seek bar (20 ticks
  across the full duration) while scrubbing.
- **Speed picker** with haptic confirmation for each selection.

### Bug fixes
- Fixed `Unresolved reference 'animateFloat'` compile errors in
  `AppComponents.kt` and `PlayerScreen.kt` that blocked the v0.1 build.

---

## v0.1-beta — 2026-06-14

First public **beta** of **Echoes**, a native Material 3 podcast app for Android.

### Highlights
- Real podcast data via iTunes Search API (no key required) + RSS episode feeds.
- ExoPlayer (Media3) audio engine with background playback service.
- 5 screens: Home, Search, Show Details, Library, Now Playing.
- 5 switchable color themes (Indigo, Emerald, Rose, Amber, Lavender),
  each with light and dark mode.
- Zero-jank scrolling with `@Immutable` models and `key = { id }` in all lazy lists.
- Adaptive app icon with Android 13+ monochrome variant.

### Tech
- Kotlin 2.0 · Compose BOM 2024.12.01 · Material 3 · Media3 ExoPlayer 1.5.1
- Retrofit 2.11.0 + OkHttp 4.12.0 · Coil 2.7.0
- `minSdk` 31 · `targetSdk` / `compileSdk` 36 · edge-to-edge

---

🤖 Developed with **Claude** (Anthropic).
