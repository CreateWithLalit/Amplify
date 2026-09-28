package com.lalit.amplify.core.network.jamendo

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Jamendo API Service
 * Base URL: https://api.jamendo.com/v3.0/
 * Documentation: https://developer.jamendo.com/v3.0
 */
interface JamendoApiService {

    /**
     * Search tracks by query, filter, or get trending
     */
    @GET("tracks")
    suspend fun searchTracks(
        @Query("client_id") clientId: String,
        @Query("search") query: String? = null,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("include") include: String = "licenses",  // Include license info
        @Query("audioformat") audioFormat: String = "mp32",
        @Query("audiodlformat") audioDownloadFormat: String = "mp32",
        @Query("imagesize") imagesize: Int = 100,  // Artwork size
        @Query("format") format: String = "json"  // Response format
    ): JamendoTracksResponse

    /**
     * Get popular/trending tracks
     */
    @GET("tracks")
    suspend fun getTrendingTracks(
        @Query("client_id") clientId: String,
        @Query("order") order: String = "popularity_month",  // Sort by monthly popularity
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("include") include: String = "licenses",
        @Query("audioformat") audioFormat: String = "mp32",
        @Query("audiodlformat") audioDownloadFormat: String = "mp32",
        @Query("imagesize") imagesize: Int = 100,
        @Query("format") format: String = "json"
    ): JamendoTracksResponse

    /** Tracks editorially featured by Jamendo, used for the Picks carousel. */
    @GET("tracks")
    suspend fun getFeaturedTracks(
        @Query("client_id") clientId: String,
        @Query("featured") featured: Boolean = true,
        @Query("order") order: String = "popularity_month",
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("include") include: String = "licenses",
        @Query("audioformat") audioFormat: String = "mp32",
        @Query("audiodlformat") audioDownloadFormat: String = "mp32",
        @Query("imagesize") imagesize: Int = 100,
        @Query("format") format: String = "json"
    ): JamendoTracksResponse

    /**
     * Get tracks by tag/genre
     */
    @GET("tracks")
    suspend fun getTracksByTag(
        @Query("client_id") clientId: String,
        @Query("tags") tags: String,  // e.g., "electronic" or multiple: "electronic,ambient"
        @Query("order") order: String = "popularity_month",
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("include") include: String = "licenses",
        @Query("audioformat") audioFormat: String = "mp32",
        @Query("audiodlformat") audioDownloadFormat: String = "mp32",
        @Query("imagesize") imagesize: Int = 100,
        @Query("format") format: String = "json"
    ): JamendoTracksResponse

    /**
     * Get albums by query
     */
    @GET("albums")
    suspend fun searchAlbums(
        @Query("client_id") clientId: String,
        @Query("search") query: String? = null,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("imagesize") imagesize: Int = 100,
        @Query("format") format: String = "json"
    ): JamendoAlbumsResponse

    /**
     * Get radios
     */
    @GET("radios")
    suspend fun getRadios(
        @Query("client_id") clientId: String,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("imagesize") imagesize: Int = 100,
        @Query("format") format: String = "json"
    ): JamendoRadiosResponse
}

