package com.material.podcast.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.material.podcast.ui.viewmodel.PlayerViewModel

val LocalPlayer = staticCompositionLocalOf<PlayerViewModel> {
    error("PlayerViewModel not provided")
}
