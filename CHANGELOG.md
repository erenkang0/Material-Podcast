# Changelog

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
