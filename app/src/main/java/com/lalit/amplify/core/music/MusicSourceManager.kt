package com.lalit.amplify.core.music

import android.content.Context
import com.lalit.amplify.core.data.jamendo.JamendoRepository
import com.lalit.amplify.core.model.Song
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.async

/**
 * Manages multiple music sources and provides unified access.
 * Can be extended with JamendoSource, AudiusSource, etc. as they're implemented.
 */
class MusicSourceManager(private val context: Context) : MusicSourceProvider {
    
    private val sources = mutableMapOf<String, MusicSource>()
    
    init {
        // Register local source by default
        registerSource(LocalMusicSource(context))

        // Register Bollywood & Global discovery catalog
        registerSource(BollywoodDiscoveryMusicSource())
        
        // Register Jamendo source (streaming)
        registerSource(JamendoMusicSource { JamendoRepository.getInstance(context).getQualityPreferenceSync() })
    }

    /**
     * Register a new music source.
     * Can be called for Jamendo, Audius, YouTube Music, etc.
     */
    fun registerSource(source: MusicSource) {
        sources[source.sourceId] = source
    }

    /**
     * Remove a source (e.g., if API becomes unavailable).
     */
    fun unregisterSource(sourceId: String) {
        sources.remove(sourceId)
    }

    override fun getSource(sourceId: String): MusicSource? {
        return sources[sourceId]
    }

    override fun getAllSources(): List<MusicSource> {
        return sources.values.toList()
    }

    /**
     * Search across all available sources.
     * Returns results grouped by source ID.
     */
    override suspend fun searchAll(query: String, limit: Int): Map<String, List<Song>> {
        return supervisorScope {
            sources.map { (sourceId, source) ->
                async {
                    sourceId to try {
                        source.search(query, limit)
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
            }.associate { it.await() }
        }
    }

    /**
     * Get trending songs from all available sources.
     * Returns results grouped by source ID.
     */
    override suspend fun getTrendingAll(limit: Int): Map<String, List<Song>> {
        return supervisorScope {
            sources.map { (sourceId, source) ->
                async {
                    sourceId to try {
                        source.getTrending(limit)
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
            }.associate { it.await() }
        }
    }

    /**
     * Get recommendations from all sources.
     * Useful for dashboard curated sections.
     */
    suspend fun getRecommendationsAll(limit: Int = 50): Map<String, List<Song>> {
        return supervisorScope {
            sources.map { (sourceId, source) ->
                async {
                    sourceId to try {
                        source.getRecommendations(limit)
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
            }.associate { it.await() }
        }
    }

    /**
     * Get songs by genre from all sources that support it.
     */
    suspend fun getByGenreAll(genre: String, limit: Int = 50): Map<String, List<Song>> {
        return supervisorScope {
            sources.map { (sourceId, source) ->
                async {
                    sourceId to try {
                        source.getByGenre(genre, limit)
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
            }.associate { it.await() }
        }
    }

    companion object {
        private var instance: MusicSourceManager? = null

        fun getInstance(context: Context): MusicSourceManager {
            return instance ?: MusicSourceManager(context).also { instance = it }
        }
    }
}

