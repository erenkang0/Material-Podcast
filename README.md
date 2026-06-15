<div align="center">

# 🎧 Echoes

### A calm, fully-native **Material 3** podcast app for Android

*Real podcasts, real audio, thoughtful motion — built end-to-end in Jetpack Compose.*

<br/>

![Version](https://img.shields.io/badge/version-0.9--beta-6750A4?style=for-the-badge)
![Android](https://img.shields.io/badge/Android-7%2B%20·%20API%2031-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Compose-Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)

![CI](https://img.shields.io/badge/CI-GitHub%20Actions-2088FF?style=flat-square&logo=githubactions&logoColor=white)
![Media3](https://img.shields.io/badge/Audio-Media3%20ExoPlayer-FF6F00?style=flat-square)
![Offline](https://img.shields.io/badge/Playback-Offline%20downloads-00897B?style=flat-square)
![Themes](https://img.shields.io/badge/Themes-5%20·%20light%20%26%20dark-EC407A?style=flat-square)
![i18n](https://img.shields.io/badge/Languages-EN%20·%20TR-455A64?style=flat-square)

</div>

---

> **Echoes** streams real shows from the iTunes catalogue, plays them with a proper
> background audio engine, and remembers everything you care about **on-device** —
> likes, timestamped notes, downloads and where you left off. Every pixel comes from
> `MaterialTheme.*` tokens, so the whole app retints instantly across five palettes
> and light/dark.

<br/>

## ✨ Highlights

<table>
  <tr>
    <td width="50%" valign="top">

### 🔎 Discover
- Real catalogue via the **iTunes Search API** + live **RSS** episode feeds — no API key.
- A home feed of **editable categories** — name each row and the query it searches (up to 10).
- **Continue where you left off** card, front and centre.

</td>
    <td width="50%" valign="top">

### ▶️ Playback
- **Media3 ExoPlayer** with a real `MediaSessionService` — controls in the notification, survives backgrounding.
- A true **queue**: next/previous walk the show's episodes; *Up next* + *Previously played*.
- **Wavy**, Material-3-expressive seek bar; variable speed; smart **sleep timer** (incl. *stop at episode end*).

</td>
  </tr>
  <tr>
    <td width="50%" valign="top">

### 📚 Your library — kept forever
- **Likes**, **timestamped notes**, **downloads**, follows, history & resume points.
- All persisted locally (SharedPreferences + Gson) — **nothing is silently deleted**.
- One-tap **JSON backup & restore** from Settings.

</td>
    <td width="50%" valign="top">

### 🎨 Crafted experience
- **Dynamic Now Playing** surface tinted from the cover art.
- Tap a producer → a full **Author profile** of their shows.
- **5 palettes** (light + dark), springy motion, and haptics throughout.

</td>
  </tr>
</table>

<br/>

## 📱 Screens

| Screen | What's inside |
| :-- | :-- |
| **Home / Keşfet** | Greeting app bar, a *Continue listening* card, a featured rail, and one `LazyRow` per **user-editable category**. The top-right button opens the category editor. |
| **Search** | Native `SearchBar` (with its expand animation), genre `FilterChip`s, and an **“En son baktıkların”** recently-viewed rail. |
| **Show Details** | Collapsing `LargeTopAppBar`, cover + meta, play/follow actions, a working **⋮ menu** (follow · view producer · share · copy link · refresh), and the full episode list. |
| **Author** | Every podcast by a producer, fetched on demand. |
| **Now Playing** | Cover-tinted bottom sheet: wavy scrubber, transport, speed slider, sleep timer, queue, **like**, **bookmark a moment**, and **download** — swipe-up from the mini-bar to open it. |
| **Library** | Five tabs — **Saved · Likes · Notes · Downloads · Recent** — plus the resume card. |
| **Settings** | Theme, language (EN/TR), default speed, a **Background** section (battery optimization, re-run setup) and **Backup** (export/import JSON). |
| **Setup** | A first-run welcome that walks through notifications and disabling battery optimization. |

A compact **mini-player** floats above the navigation bar once playback starts; swipe it
left/right to skip, or up to expand the full player.

<br/>

## 🧩 Under the hood

```
MainActivity ─ first run? ─▶ SetupScreen (welcome · notifications · battery)
     │
     └─ EchoesTheme (5 palettes · light/dark · edge-to-edge)
         └─ PodcastApp (Scaffold: NavigationBar · mini-player · Now Playing sheet)
             └─ AppNavHost  home · search · library · settings
                          · show/{id} · author/{name} · category-editor
                                │
   data/ ──────────────────────┤  PodcastRepository → iTunes API + RSS parser (OkHttp/Retrofit)
   media/ ─────────────────────┤  PlaybackService (MediaSessionService) · DownloadManager
   store/ ─────────────────────┤  LibraryStore (persisted) · SettingsStore
   ui/viewmodel/ ──────────────┘  PlayerViewModel ⇄ MediaController  (single source of truth)
```

- **`PlayerViewModel`** talks to the service through a **`MediaController`**, so the UI and the
  background session never drift — and it restores *what's playing* even after the process is recreated.
- **`LibraryStore`** is the on-device source of truth for everything you keep; **`DownloadManager`**
  streams audio to internal storage and playback prefers the offline copy automatically.

<br/>

## 🔒 Permissions

| Permission | Why |
| :-- | :-- |
| `INTERNET` | Fetch catalogue, feeds and audio. |
| `FOREGROUND_SERVICE` · `…_MEDIA_PLAYBACK` | Keep playing in the background with a media notification. |
| `POST_NOTIFICATIONS` | Show the playback notification (Android 13+). |
| `WAKE_LOCK` | Uninterrupted playback while the screen is off. |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Optional — offered in setup so the OS doesn't kill background playback. |

<br/>

## 🛠️ Tech stack

**Kotlin 2.0** · **Jetpack Compose** (BOM 2024.12.01) · **Material 3** · **Media3 ExoPlayer 1.5**
· Retrofit 2.11 + OkHttp 4.12 · Gson · Coil 2.7 · AndroidX Palette · Navigation-Compose
· AGP 8.7 · `minSdk 31` · `target / compileSdk 36` · Java 11

<br/>

## 🚀 Build & run

```bash
git clone https://github.com/erenkang0/Material-Podcast.git
cd Material-Podcast
./gradlew :app:assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk
```

Or just open the project in **Android Studio (Ladybug or newer)** and run the `app`
configuration — it provisions the matching Gradle and SDK automatically. Every push to the
release branch also builds a debug APK and publishes it via **GitHub Actions** (see the
[Releases](https://github.com/erenkang0/Material-Podcast/releases) page).

<br/>

## 🗺️ Changelog

Release notes live in [`CHANGELOG.md`](CHANGELOG.md) — from the first UI beta through real
audio, downloads, the editable home, and the cover-tinted player.

<br/>

---

<div align="center">

### 🙌 Credits

Built by **[Erenkang0](https://github.com/erenkang0)** &amp; **Claude** (Anthropic).

<sub>Made with Kotlin, Jetpack Compose and Material 3.</sub>

</div>
