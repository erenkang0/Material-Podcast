# Echoes — a native Material 3 podcast UI

A clean, layout-first podcast app built entirely with **native Jetpack Compose
Material 3** components. Every surface, color and type style comes from
`MaterialTheme.*` tokens — no images, no external assets, no custom containers
hacked together from scratch. The result reads as a genuinely native Android app
that feels calm and spacious in both light and dark.

## Highlights

- **5 screens** — Discover / Home, Show Details, Now Playing, Library, and
  Search & Explore.
- **5 eye-friendly palettes** — Indigo, Emerald, Rose, Amber and Lavender, each
  with a hand-tuned light **and** dark scheme. Pick one from the in-app
  **theme sheet** (the palette icon, top-right).
- **Smooth theme switching** — every color role cross-fades, so changing palette
  or light/dark washes across the whole app instead of cutting hard.
- **Considered motion** — fade-through transitions between tabs, a slide-up
  Now Playing screen, a swipe-down to dismiss it, springy press feedback on
  cards and chips, an animated equalizer in the mini-player, a morphing
  play/pause glyph, and a breathing album-art pulse while playing.
- **All native components** — `Scaffold`, `TopAppBar` / `LargeTopAppBar`,
  `NavigationBar`, `ElevatedCard`, `ListItem`, `FilterChip`, `Slider`,
  `SearchBar`, `PrimaryTabRow` + `HorizontalPager`, `SegmentedButton`,
  `ExtendedFloatingActionButton`, `ModalBottomSheet`.

## Screens

| Screen | What's inside |
| --- | --- |
| **Discover / Home** | Collapsing `TopAppBar` greeting, a `LazyRow` of featured `ElevatedCard`s, and a `LazyColumn` of category `ListItem`s. |
| **Show Details** | `LargeTopAppBar` that collapses on scroll, a header with cover + meta, a `FilledTonalButton` / `OutlinedButton` action row, and an episode list with trailing play buttons. |
| **Now Playing** | Full-screen layout: large `extraLarge`-rounded artwork, `Slider` scrubber with time read-outs, big springy transport controls (with haptics), and a secondary action row. Swipe down to dismiss. |
| **Library** | `PrimaryTabRow` (Saved / Downloaded / History) wired to a `HorizontalPager`; Saved is a 2-column `LazyVerticalGrid`; an `ExtendedFloatingActionButton` to discover more. |
| **Search & Explore** | Native `SearchBar` with suggestions, plus a `FlowRow` of genre `FilterChip`s and a popular row. |

A compact **mini-player** floats above the navigation bar once playback starts
and expands into the full Now Playing screen on tap.

## Architecture

```
MainActivity
 └─ EchoesTheme (palette + light/dark, animated color scheme, edge-to-edge bars)
     └─ PodcastApp  (Scaffold: NavigationBar + MiniPlayer + theme sheet)
         └─ AppNavHost  (home · search · library · show/{id} · player)
             └─ screens/  ──uses──▶  ui/components/  (CoverArt, cards, chips, controls…)
                                     ui/state/PlayerState   (shared now-playing state)
                                     data/MockData          (structural placeholders)
                                     ui/theme/              (Color · Type · Shape · Theme)
```

This is a **UI / interaction prototype**: there is no audio engine or network
layer. `PlayerState` is a small in-memory holder that keeps the mini-player and
the Now Playing screen in sync so the controls feel real, and `MockData`
provides neutral placeholder content (icons stand in for artwork).

## Build

Open the project in **Android Studio (Ladybug or newer)** and run the `app`
configuration. Android Studio provisions the matching Gradle and SDK
automatically.

- `minSdk` 31 · `targetSdk` / `compileSdk` 36
- Kotlin 2.0 · Jetpack Compose (BOM 2024.12.01) · Material 3
- Dark/light follows the system by default, or can be forced from the theme
  sheet.

## Theming

All five palettes are defined in
[`ui/theme/Theme.kt`](app/src/main/java/com/material/podcast/ui/theme/Theme.kt)
as matched `lightColorScheme` / `darkColorScheme` pairs. To add another, add an
entry to the `AppThemeColor` enum with a swatch and its two schemes — it shows
up in the theme sheet automatically and retints the entire app through the
standard color roles.
