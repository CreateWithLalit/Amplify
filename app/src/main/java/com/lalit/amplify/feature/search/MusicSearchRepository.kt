package com.lalit.amplify.feature.search

/**
 * Repository contract for internet music search.
 */
interface MusicSearchRepository {
    suspend fun search(query: String): Result<List<MusicSearchResult>>

    /**
     * Resolve a search result into a downloadable track with direct stream URL.
     * This is the "Get Download Link" step.
     */
    suspend fun resolveDownloadLink(result: MusicSearchResult): Result<DownloadableTrack>
}


