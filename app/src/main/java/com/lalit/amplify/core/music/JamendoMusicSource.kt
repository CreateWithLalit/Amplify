package com.lalit.amplify.core.music

import com.lalit.amplify.BuildConfig
import com.lalit.amplify.core.model.Song
import com.lalit.amplify.core.network.jamendo.JamendoApiService
import com.lalit.amplify.core.network.jamendo.JamendoNetworkMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.HttpException
import java.io.IOException

/**
 * Jamendo music source implementation
 * Provides streaming access to Jamendo's catalog of independent music
 */
class JamendoMusicSource(
    private val qualityProvider: () -> String = { "mp32" }
) : MusicSource {

    override val sourceId: String = "jamendo"
    override val displayName: String = "Jamendo"
    override val requiresInternet: Boolean = true

    private val apiService: JamendoApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.jamendo.com/v3.0/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(JamendoApiService::class.java)
    }

    private val clientId: String
        get() = BuildConfig.JAMENDO_CLIENT_ID

    override suspend fun search(query: String, limit: Int, offset: Int): List<Song> {
        return requestTracks {
            apiService.searchTracks(
                clientId = clientId,
                query = query,
                limit = limit.coerceAtMost(50),  // Jamendo limit is 50 per request
                offset = offset,
                include = "licenses",
                audioFormat = qualityProvider(),
                audioDownloadFormat = qualityProvider()
            )
        }
    }

    override suspend fun getTrending(limit: Int, offset: Int): List<Song> {
        return requestTracks {
            apiService.getTrendingTracks(
                clientId = clientId,
                order = "popularity_month",
                limit = limit.coerceAtMost(50),
                offset = offset,
                include = "licenses",
                audioFormat = qualityProvider(),
                audioDownloadFormat = qualityProvider()
            )
        }
    }

    override suspend fun getByGenre(genre: String, limit: Int, offset: Int): List<Song> {
        return requestTracks {
            apiService.getTracksByTag(
                clientId = clientId,
                tags = genre,
                order = "popularity_month",
                limit = limit.coerceAtMost(50),
                offset = offset,
                include = "licenses",
                audioFormat = qualityProvider(),
                audioDownloadFormat = qualityProvider()
            )
        }
    }

    override suspend fun getRecommendations(limit: Int): List<Song> {
        return requestTracks {
            apiService.getFeaturedTracks(
                clientId = clientId,
                limit = limit.coerceAtMost(50),
                include = "licenses",
                audioFormat = qualityProvider(),
                audioDownloadFormat = qualityProvider()
            )
        }
    }

    override fun getSongsFlow(): Flow<List<Song>> {
        // Jamendo is a streaming service, not a full library sync
        // Return empty for now - actual data comes from search/trending
        return flow {
            emit(emptyList())
        }
    }

    override suspend fun isAvailable(): Boolean {
        return try {
            // Try fetching one trending track to verify API is available
            apiService.getTrendingTracks(
                clientId = clientId,
                limit = 1
            ).results.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun requestTracks(
        request: suspend () -> com.lalit.amplify.core.network.jamendo.JamendoTracksResponse
    ): List<Song> {
        check(clientId.isNotBlank()) {
            "Jamendo is not configured. Add JAMENDO_CLIENT_ID to local.properties."
        }
        var lastError: Exception? = null
        repeat(3) { attempt ->
            try {
                val response = request()
                if (response.headers?.code?.let { it != 0 } == true) {
                    throw IllegalStateException(response.headers?.errorMessage ?: "Jamendo returned an API error")
                }
                return JamendoNetworkMapper.toSongs(response.results, qualityProvider())
            } catch (error: Exception) {
                lastError = error
                val retryable = error is IOException ||
                    (error is HttpException && (error.code() == 429 || error.code() >= 500))
                if (!retryable || attempt == 2) throw error
                delay(500L * (1 shl attempt))
            }
        }
        throw lastError ?: IllegalStateException("Jamendo request failed")
    }

    companion object {
        // Common Jamendo genres/tags
        val JAMENDO_GENRES = listOf(
            "electronic",
            "piano",
            "rock",
            "pop",
            "jazz",
            "classical",
            "ambient",
            "hip-hop",
            "metal",
            "indie",
            "folk",
            "acoustic",
            "dance",
            "blues",
            "soul",
            "world"
        )
    }
}

