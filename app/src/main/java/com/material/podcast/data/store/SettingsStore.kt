package com.material.podcast.data.store

import android.content.Context

object SettingsStore {
    private const val PREFS = "echoes_settings"
    private const val KEY_LANG = "language"
    private const val KEY_DEFAULT_SPEED = "default_speed"
    private const val KEY_ONBOARDED = "onboarded"

    fun isOnboarded(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ONBOARDED, false)

    fun setOnboarded(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ONBOARDED, value).apply()
    }

    fun getLanguage(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LANG, "tr") ?: "tr"

    fun setLanguage(context: Context, lang: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_LANG, lang).apply()
    }

    fun getDefaultSpeed(context: Context): Float =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getFloat(KEY_DEFAULT_SPEED, 1f)

    fun setDefaultSpeed(context: Context, speed: Float) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putFloat(KEY_DEFAULT_SPEED, speed).apply()
    }

    fun getEqPreset(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt("eq_preset", 0)

    fun setEqPreset(context: Context, preset: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt("eq_preset", preset).apply()
    }

    fun isWifiOnlyDownload(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean("wifi_only_download", false)

    fun setWifiOnlyDownload(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean("wifi_only_download", value).apply()
    }

    fun getTheme(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("theme", "Indigo|System") ?: "Indigo|System"

    fun setTheme(context: Context, value: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString("theme", value).apply()
    }

    fun getNowPlayingGuid(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("now_playing_guid", "") ?: ""

    fun setNowPlayingGuid(context: Context, guid: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString("now_playing_guid", guid).apply()
    }

    fun isEldenRingUnlocked(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean("elden_ring_unlocked", false)

    fun setEldenRingUnlocked(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean("elden_ring_unlocked", true).apply()
    }
}
