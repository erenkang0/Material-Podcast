package com.material.podcast.data.store

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.material.podcast.EchoesApplication

/**
 * Recent search queries the user has run, most-recent-first and de-duplicated.
 *
 * Backed by SharedPreferences + Gson and mirrored into a [SnapshotStateList] so the
 * search screen recomposes whenever the history changes. Initialises lazily from the
 * application context, so callers don't need to wire anything into [EchoesApplication].
 */
object SearchHistoryStore {

    private const val PREFS = "echoes_search_history"
    private const val KEY_QUERIES = "queries"
    private const val MAX_QUERIES = 8

    private val gson = Gson()
    private var prefs: SharedPreferences? = null

    val queries: SnapshotStateList<String> = mutableStateListOf()

    private fun ensureInit() {
        if (prefs != null) return
        val context: Context = EchoesApplication.instance.applicationContext
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        queries.clear()
        queries.addAll(load())
    }

    private fun load(): List<String> {
        val json = prefs?.getString(KEY_QUERIES, null) ?: return emptyList()
        return try {
            gson.fromJson(json, object : TypeToken<List<String>>() {}.type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun persist() {
        prefs?.edit()?.putString(KEY_QUERIES, gson.toJson(queries.toList()))?.apply()
    }

    /** Read the current history (also guarantees the store is initialised). */
    fun get(): List<String> {
        ensureInit()
        return queries.toList()
    }

    fun add(query: String) {
        ensureInit()
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        // De-dupe case-insensitively, keeping the latest spelling at the top.
        queries.removeAll { it.equals(trimmed, ignoreCase = true) }
        queries.add(0, trimmed)
        while (queries.size > MAX_QUERIES) queries.removeAt(queries.lastIndex)
        persist()
    }

    fun remove(query: String) {
        ensureInit()
        if (queries.removeAll { it.equals(query, ignoreCase = true) }) persist()
    }

    fun clear() {
        ensureInit()
        if (queries.isEmpty()) return
        queries.clear()
        persist()
    }
}
