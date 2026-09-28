 package com.lalit.amplify.feature.dashboard

import com.lalit.amplify.core.model.Song
import com.lalit.amplify.core.model.SongSource
import com.lalit.amplify.core.music.MusicSourceManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Provides curated sections for the Dashboard from multiple music sources.
 * Handles loading, caching, and organizing content by sections:
 * - Trending
 * - Recommendations
 * - Genre-based
 */
data class CuratedSection(
    val title: String,
    val sourceId: String,
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class CuratedContentProvider(private val musicSourceManager: MusicSourceManager) {

    private val _curatedSections = MutableStateFlow<List<CuratedSection>>(emptyList())
    val curatedSections: Flow<List<CuratedSection>> = _curatedSections.asStateFlow()

    private val _trendingSongs = MutableStateFlow<Map<String, List<Song>>>(emptyMap())
    val trendingSongs: Flow<Map<String, List<Song>>> = _trendingSongs.asStateFlow()

    private val _recommendations = MutableStateFlow<Map<String, List<Song>>>(emptyMap())
    val recommendations: Flow<Map<String, List<Song>>> = _recommendations.asStateFlow()

    /**
     * Load all curated sections for Dashboard.
     * This includes trending, recommendations, and featured content.
     */
    suspend fun loadCuratedContent(limit: Int = 50) {
        _curatedSections.value = listOf(
            CuratedSection("Trending on Jamendo", "jamendo-trending", isLoading = true),
            CuratedSection("Jamendo Picks", "jamendo-picks", isLoading = true)
        )
        try {
            // Load trending from all sources
            val trendingBySource = musicSourceManager.getTrendingAll(limit)
            _trendingSongs.update { trendingBySource }

            // Load recommendations from all sources
            val recommendationsBySource = musicSourceManager.getRecommendationsAll(limit)
            _recommendations.update { recommendationsBySource }

            // Build curated sections list
            val sections = mutableListOf<CuratedSection>()

            // Add trending sections
            trendingBySource.forEach { (sourceId, songs) ->
                if (songs.isNotEmpty()) {
                    val source = musicSourceManager.getSource(sourceId)
                    sections.add(
                        CuratedSection(
                            title = "Trending on ${source?.displayName ?: sourceId}",
                            sourceId = sourceId,
                            songs = songs.take(10)
                        )
                    )
                }
            }

            // Add recommendation sections
            recommendationsBySource.forEach { (sourceId, songs) ->
                if (songs.isNotEmpty()) {
                    val source = musicSourceManager.getSource(sourceId)
                    sections.add(
                        CuratedSection(
                            title = "${source?.displayName ?: sourceId} Picks",
                            sourceId = sourceId,
                            songs = songs.take(10)
                        )
                    )
                }
            }

            _curatedSections.value = sections.ifEmpty {
                listOf(
                    CuratedSection(
                        title = "Trending on Jamendo",
                        sourceId = "jamendo-trending",
                        errorMessage = "No Jamendo tracks are available right now."
                    )
                )
            }

        } catch (e: Exception) {
            _curatedSections.value = listOf(
                    CuratedSection(
                        title = "Trending on Jamendo",
                        sourceId = "jamendo-trending",
                        errorMessage = e.message ?: "Unknown error"
                    )
                )
        }
    }

    /**
     * Search across all sources with curated display.
     */
    suspend fun searchCurated(query: String, limit: Int = 50): Map<String, List<Song>> {
        return try {
            musicSourceManager.searchAll(query, limit)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /**
     * Get songs by genre from all sources.
     */
    suspend fun getCuratedByGenre(genre: String, limit: Int = 50): Map<String, List<Song>> {
        return try {
            musicSourceManager.getByGenreAll(genre, limit)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /**
     * Get a specific section of curated content.
     */
    fun getSectionSongs(sourceId: String): List<Song> {
        return _curatedSections.value.find { it.sourceId == sourceId }?.songs ?: emptyList()
    }

    companion object {
        private var instance: CuratedContentProvider? = null

        fun getInstance(musicSourceManager: MusicSourceManager): CuratedContentProvider {
            return instance ?: CuratedContentProvider(musicSourceManager).also { instance = it }
        }
    }
}

