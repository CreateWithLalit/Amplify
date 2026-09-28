package com.lalit.amplify.feature.downloader.data

import android.util.Log
import com.lalit.amplify.feature.search.DownloadableTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Talks to the Amplify backend to resolve track metadata and audio stream endpoints.
 * Includes defensive URL sanitization, rich error diagnostics, and a public oEmbed fallback.
 */
@Singleton
class YouTubeResolverRepository @Inject constructor() {
    private val client = com.lalit.amplify.feature.downloader.engine.NetworkClients.client
    private var backendBaseUrl = DownloadPreferences.DEFAULT_BACKEND_URL

    companion object {
        private const val TAG = "YouTubeResolver"
        private val YOUTUBE_ID_PATTERN = Pattern.compile(
            "(?:https?://)?(?:www\\.|m\\.|music\\.)?(?:youtube\\.com/(?:watch\\?v=|embed/|v/|shorts/)|youtu\\.be/)([a-zA-Z0-9_-]{11})"
        )
    }

    fun setBackendUrl(url: String) {
        val clean = url.trim().removeSuffix("/")
        if (clean.isNotBlank()) {
            backendBaseUrl = clean
            Log.d(TAG, "Backend base URL updated to: $clean")
        }
    }

    fun getBackendUrl(): String = backendBaseUrl

    /**
     * Sanitizes user-entered URL and extracts the canonical YouTube video URL.
     * Strips tracking parameters, playlist contexts, and referral tokens.
     */
    fun sanitizeYouTubeUrl(input: String): String {
        val trimmed = input.trim()
        val matcher = YOUTUBE_ID_PATTERN.matcher(trimmed)
        return if (matcher.find()) {
            val videoId = matcher.group(1) ?: return trimmed
            "https://www.youtube.com/watch?v=$videoId"
        } else {
            trimmed
        }
    }

    fun extractVideoId(input: String): String? {
        val matcher = YOUTUBE_ID_PATTERN.matcher(input.trim())
        return if (matcher.find()) matcher.group(1) else null
    }

    suspend fun resolveYouTubeUrl(youtubeUrl: String): Result<DownloadableTrack> = withContext(Dispatchers.IO) {
        val canonicalUrl = sanitizeYouTubeUrl(youtubeUrl)
        val videoId = extractVideoId(canonicalUrl)

        Log.d(TAG, "Resolving videoId=${videoId ?: "unknown"} via backend: $backendBaseUrl")

        // Try primary backend resolver
        try {
            val requestJson = JSONObject().put("url", canonicalUrl).toString()
            val request = Request.Builder()
                .url("$backendBaseUrl/resolve")
                .header("Accept", "application/json")
                .post(requestJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val json = JSONObject(responseBody)
                    val title = json.optString("title", "Unknown title")
                    val artist = json.optString("artist", "Unknown artist")
                    val durationMillis = json.optLong("duration", json.optLong("durationSeconds", 0L)) * 1000
                    val thumbnail = json.optString("thumbnail", json.optString("thumbnailUrl", "")).ifBlank { null }
                    val streamUrl = json.optString("streamUrl", "$backendBaseUrl/download")

                    Log.d(TAG, "Backend resolve succeeded for: $title by $artist")

                    return@withContext Result.success(
                        DownloadableTrack(
                            id = videoId ?: canonicalUrl.hashCode().toString(),
                            title = title,
                            artist = artist,
                            duration = durationMillis,
                            thumbnailUrl = thumbnail,
                            sourceLabel = "YouTube",
                            webUrl = canonicalUrl,
                            streamUrl = streamUrl,
                            audioQuality = "MP3",
                            fileExtension = "mp3",
                            contentType = "audio/mpeg"
                        )
                    )
                }

                // If backend returned error, parse error message
                val errorMessage = extractErrorMessage(response.code, responseBody)
                Log.w(TAG, "Backend resolve returned HTTP ${response.code}: $errorMessage")

                // If it's a 401 Unauthorized, try oEmbed fallback for metadata so user has context
                if (videoId != null && (response.code == 401 || response.code == 403 || response.code == 404 || response.code >= 500)) {
                    Log.i(TAG, "Attempting public oEmbed metadata fallback for videoId: $videoId")
                    val oEmbedResult = fetchOEmbedMetadata(videoId, canonicalUrl)
                    if (oEmbedResult != null) {
                        return@withContext Result.success(oEmbedResult)
                    }
                }

                return@withContext Result.failure(IOException(errorMessage))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Backend connection failed: ${e.message}. Attempting public oEmbed fallback...")
            if (videoId != null) {
                val oEmbedResult = fetchOEmbedMetadata(videoId, canonicalUrl)
                if (oEmbedResult != null) {
                    return@withContext Result.success(oEmbedResult)
                }
            }
            Result.failure(e)
        }
    }

    /**
     * Resolves metadata using YouTube's public zero-auth oEmbed service.
     * Extremely reliable, requires no authentication, and handles videos where backend extractors are challenged.
     */
    private fun fetchOEmbedMetadata(videoId: String, canonicalUrl: String): DownloadableTrack? {
        return try {
            val oEmbedUrl = "https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=$videoId&format=json"
            val request = Request.Builder()
                .url(oEmbedUrl)
                .header("Accept", "application/json")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val json = JSONObject(body)

                val rawTitle = json.optString("title", "YouTube Audio")
                val author = json.optString("author_name", "YouTube")
                val thumbnail = json.optString("thumbnail_url").ifBlank {
                    "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                }

                // Parse artist and title if formatted as "Artist - Title"
                val (parsedArtist, parsedTitle) = if (rawTitle.contains(" - ")) {
                    val parts = rawTitle.split(" - ", limit = 2)
                    parts[0].trim() to parts[1].trim()
                } else {
                    author to rawTitle
                }

                Log.d(TAG, "oEmbed successfully resolved metadata: $parsedTitle by $parsedArtist")

                DownloadableTrack(
                    id = videoId,
                    title = parsedTitle,
                    artist = parsedArtist,
                    duration = 0L,
                    thumbnailUrl = thumbnail,
                    sourceLabel = "YouTube",
                    webUrl = canonicalUrl,
                    streamUrl = "$backendBaseUrl/download",
                    audioQuality = "MP3",
                    fileExtension = "mp3",
                    contentType = "audio/mpeg"
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "oEmbed resolution failed: ${e.message}")
            null
        }
    }

    private fun extractErrorMessage(statusCode: Int, responseBody: String): String {
        val serverMsg = try {
            val json = JSONObject(responseBody)
            json.optString("error", json.optString("message", ""))
        } catch (_: Exception) {
            ""
        }

        return when (statusCode) {
            401 -> {
                if (serverMsg.isNotBlank()) "Unauthorized (401): $serverMsg"
                else "Unauthorized (401): Access restricted or authentication required by provider."
            }
            403 -> {
                if (serverMsg.isNotBlank()) "Forbidden (403): $serverMsg"
                else "Forbidden (403): The requested video is restricted or unavailable."
            }
            404 -> {
                if (serverMsg.isNotBlank()) "Not Found (404): $serverMsg"
                else "Resolver service endpoint or video not found (404)."
            }
            429 -> "Rate limit reached (429): Please wait a moment before trying again."
            in 500..599 -> {
                if (serverMsg.isNotBlank()) "Server Error ($statusCode): $serverMsg"
                else "Resolver server error ($statusCode). Please try again shortly."
            }
            else -> {
                if (serverMsg.isNotBlank()) "Error ($statusCode): $serverMsg"
                else "Resolver returned HTTP $statusCode"
            }
        }
    }
}
