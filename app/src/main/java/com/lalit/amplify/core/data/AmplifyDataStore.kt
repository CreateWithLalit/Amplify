package com.lalit.amplify.core.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import org.json.JSONArray
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "amplify_prefs")

class AmplifyDataStore(private val context: Context) {

    companion object {
        private val FAVORITES_KEY = stringSetPreferencesKey("favorites")
        private val RECENTLY_PLAYED_KEY = stringSetPreferencesKey("recently_played")
        private val JAMENDO_SEARCH_HISTORY_KEY = stringPreferencesKey("jamendo_search_history")
    }

    val favoriteIds: Flow<Set<Long>> = context.dataStore.data.map { prefs ->
        (prefs[FAVORITES_KEY] ?: emptySet()).mapNotNull { it.toLongOrNull() }.toSet()
    }

    val recentlyPlayedIds: Flow<List<Long>> = context.dataStore.data.map { prefs ->
        val set = prefs[RECENTLY_PLAYED_KEY] ?: emptySet()
        set.mapNotNull { it.toLongOrNull() }.toList()
    }

    val jamendoSearchHistory: Flow<List<String>> = context.dataStore.data.map { prefs ->
        val raw = prefs[JAMENDO_SEARCH_HISTORY_KEY] ?: "[]"
        try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val value = array.optString(i).trim()
                    if (value.isNotBlank()) add(value)
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun toggleFavorite(songId: Long) {
        context.dataStore.edit { prefs ->
            val current = prefs[FAVORITES_KEY] ?: emptySet()
            val idStr = songId.toString()
            if (current.contains(idStr)) {
                prefs[FAVORITES_KEY] = current - idStr
            } else {
                prefs[FAVORITES_KEY] = current + idStr
            }
        }
    }

    suspend fun addRecentlyPlayed(songId: Long) {
        context.dataStore.edit { prefs ->
            val current = (prefs[RECENTLY_PLAYED_KEY] ?: emptySet()).toMutableList()
            val idStr = songId.toString()
            current.remove(idStr)
            current.add(0, idStr)
            if (current.size > 20) {
                prefs[RECENTLY_PLAYED_KEY] = current.subList(0, 20).toSet()
            } else {
                prefs[RECENTLY_PLAYED_KEY] = current.toSet()
            }
        }
    }

    suspend fun addJamendoSearchQuery(query: String) {
        val normalized = query.trim()
        if (normalized.isBlank()) return

        context.dataStore.edit { prefs ->
            val current = try {
                val array = JSONArray(prefs[JAMENDO_SEARCH_HISTORY_KEY] ?: "[]")
                buildList {
                    for (i in 0 until array.length()) {
                        val value = array.optString(i).trim()
                        if (value.isNotBlank() && !value.equals(normalized, ignoreCase = true)) {
                            add(value)
                        }
                    }
                }
            } catch (_: Exception) {
                emptyList()
            }

            val updated = listOf(normalized) + current
            prefs[JAMENDO_SEARCH_HISTORY_KEY] = JSONArray(updated.take(20)).toString()
        }
    }

    suspend fun removeJamendoSearchQuery(query: String) {
        val normalized = query.trim()
        context.dataStore.edit { prefs ->
            val current = try {
                val array = JSONArray(prefs[JAMENDO_SEARCH_HISTORY_KEY] ?: "[]")
                buildList {
                    for (i in 0 until array.length()) {
                        val value = array.optString(i).trim()
                        if (value.isNotBlank() && !value.equals(normalized, ignoreCase = true)) {
                            add(value)
                        }
                    }
                }
            } catch (_: Exception) {
                emptyList()
            }
            prefs[JAMENDO_SEARCH_HISTORY_KEY] = JSONArray(current).toString()
        }
    }

    suspend fun clearJamendoSearchHistory() {
        context.dataStore.edit { prefs ->
            prefs[JAMENDO_SEARCH_HISTORY_KEY] = "[]"
        }
    }
}
