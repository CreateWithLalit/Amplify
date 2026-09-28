package com.lalit.amplify.core.music

import com.lalit.amplify.core.model.Song
import kotlinx.coroutines.flow.Flow

/**
 * Defines a music source providing songs from various platforms.
 * Implementations: LocalMusicSource, JamendoSource, AudiusSource, etc.
 */
interface MusicSource {

    /** Unique identifier for this source (e.g., "local", "jamendo", "audius") */
    val sourceId: String

    /** Display name for the source (e.g., "My Library", "Jamendo", "Audius") */
    val displayName: String

    /** Whether this source requires internet */
    val requiresInternet: Boolean

    /** Search songs by query string */
    suspend fun search(query: String, limit: Int = 50, offset: Int = 0): List<Song>

    /** Get trending/popular songs */
    suspend fun getTrending(limit: Int = 50, offset: Int = 0): List<Song>

    /** Get songs by category/genre */
    suspend fun getByGenre(genre: String, limit: Int = 50, offset: Int = 0): List<Song>

    /** Get curated recommendations (could be based on recently played, favorites, etc.) */
    suspend fun getRecommendations(limit: Int = 50): List<Song>

    /** Get all available songs from this source (for local source) or empty for streaming */
    fun getSongsFlow(): Flow<List<Song>>

    /** Health check: is this source available/working? */
    suspend fun isAvailable(): Boolean
}

/**
 * Parent class: manages multiple music sources and provides unified access.
 */
interface MusicSourceProvider {

    /** Get a specific source by ID */
    fun getSource(sourceId: String): MusicSource?

    /** Get all available sources */
    fun getAllSources(): List<MusicSource>

    /** Search across multiple sources */
    suspend fun searchAll(query: String, limit: Int = 50): Map<String, List<Song>>

    /** Get trending from all available sources */
    suspend fun getTrendingAll(limit: Int = 50): Map<String, List<Song>>
}

