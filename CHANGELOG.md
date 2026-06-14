# Changelog

## v0.1-beta — 2026-06-14

First public **beta** of **Echoes**, a native Material 3 podcast UI for Android.

> ⚠️ Beta: this is a UI / interaction prototype. The layout, theming and motion
> are complete, but there is no audio engine or network layer yet — content is
> structural placeholder data.

### Highlights
- **5 screens** — Discover / Home, Show Details, Now Playing, Library, and
  Search & Explore — built entirely from native Material 3 components.
- **5 eye-friendly color themes** (Indigo, Emerald, Rose, Amber, Lavender), each
  with a matched **light and dark** scheme, switchable from an in-app theme sheet.
- **Animated theme switching** — every color role cross-fades when you change
  palette or light/dark mode.
- **Rich, tasteful motion** — fade-through tab transitions, a slide-up Now Playing
  screen with swipe-to-dismiss, springy press feedback, an animated mini-player
  equalizer, a morphing play/pause glyph, and a breathing album-art pulse.
- **New adaptive app icon** — a sound-wave emblem with a violet → indigo gradient
  and an Android 13+ themed (monochrome) variant.

### Tech
- Kotlin 2.0 · Jetpack Compose (BOM 2024.12.01) · Material 3
- `minSdk` 31 · `targetSdk` / `compileSdk` 36 · edge-to-edge

---

🤖 Developed with **Claude** (Anthropic). The design, architecture and Compose
implementation in this release were produced in collaboration with Claude.
