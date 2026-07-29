package com.lalit.amplify.core.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "amplify_prefs")

class AmplifyDataStore(private val context: Context) {

    companion object {
        private val FAVORITES_KEY = stringSetPreferencesKey("favorites")
        private val RECENTLY_PLAYED_KEY = stringSetPreferencesKey("recently_played")
    }

    val favoriteIds: Flow<Set<Long>> = context.dataStore.data.map { prefs ->
        (prefs[FAVORITES_KEY] ?: emptySet()).mapNotNull { it.toLongOrNull() }.toSet()
    }

    val recentlyPlayedIds: Flow<List<Long>> = context.dataStore.data.map { prefs ->
        val set = prefs[RECENTLY_PLAYED_KEY] ?: emptySet()
        set.mapNotNull { it.toLongOrNull() }.toList()
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
}
