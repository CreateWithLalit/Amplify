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
    private var client: OkHttpClient = com.lalit.amplify.feature.downloader.engine.NetworkClients.client
    private var backendBaseUrl = DownloadPreferences.DEFAULT_BACKEND_URL

    constructor(client: OkHttpClient) : this() {
        this.client = client
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

        Log.d(TAG, "Resolving videoId=${videoId ?: "unknown"} via backend: $backendBaseUrl stage=RESOLVE_START")

        try {
            val requestJson = JSONObject().put("url", canonicalUrl).toString()
            val request = Request.Builder()
                .url("$backendBaseUrl/resolve")
                .header("Accept", "application/json")
                .post(requestJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = try { response.body?.string().orEmpty() } catch (_: Exception) { "" }

                if (!response.isSuccessful) {
                    val message = parseErrorMessage(response.code, responseBody)
                    Log.e(TAG, "Resolver HTTP error code=${response.code} stage=RESOLVE_FAILED msg=$message")

                    // If it's a 401/403/404/500, attempt zero-auth public oEmbed fallback for metadata
                    if (videoId != null && (response.code == 401 || response.code == 403 || response.code == 404 || response.code >= 500)) {
                        Log.i(TAG, "Attempting public oEmbed metadata fallback for videoId: $videoId")
                        val oEmbedResult = fetchOEmbedMetadata(videoId, canonicalUrl)
                        if (oEmbedResult != null) {
                            return@withContext Result.success(oEmbedResult)
                        }
                    }

                    return@withContext Result.failure(IOException(message))
                }

                if (responseBody.isBlank()) {
                    Log.e(TAG, "Resolver returned empty body stage=RESOLVE_EMPTY")
                    return@withContext Result.failure(IOException("Empty response from resolver backend"))
                }

                val json = JSONObject(responseBody)
                val title = json.optString("title", "Unknown title")
                val artist = json.optString("artist", "Unknown artist")
                val durationMillis = json.optLong("duration", json.optLong("durationSeconds", 0L)) * 1000
                val thumbnail = json.optString("thumbnail", json.optString("thumbnailUrl", "")).ifBlank { null }
                val streamUrl = json.optString("streamUrl", "$backendBaseUrl/download")

                Log.d(TAG, "Resolver success title='$title' artist='$artist' stage=RESOLVE_SUCCESS")

                Result.success(
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
        } catch (e: Exception) {
            Log.e(TAG, "Backend connection failed: ${e.message}. Attempting public oEmbed fallback...")
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

    companion object {
        private const val TAG = "YouTubeResolver"
        private val YOUTUBE_ID_PATTERN = Pattern.compile(
            "(?:https?://)?(?:www\\.|m\\.|music\\.)?(?:youtube\\.com/(?:watch\\?v=|embed/|v/|shorts/)|youtu\\.be/)([a-zA-Z0-9_-]{11})"
        )

        fun parseErrorMessage(code: Int, errorBody: String?): String {
            val serverMessage = try {
                if (!errorBody.isNullOrBlank()) {
                    val regex = Regex("\"error\"\\s*:\\s*\"([^\"]+)\"")
                    regex.find(errorBody)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
                        ?: JSONObject(errorBody).optString("error").takeIf { it.isNotBlank() }
                } else null
            } catch (_: Exception) {
                null
            }

            if (!serverMessage.isNullOrBlank()) {
                return serverMessage
            }

            return when (code) {
                401 -> "YouTube or provider access denied (401 Unauthorized). Provider authentication or bot verification required."
                403 -> "Access forbidden by provider (403 Forbidden). Content restricted or region-locked."
                404 -> "Track or link not found (404 Not Found)."
                429 -> "Rate limit exceeded (429). Please wait a moment and try again."
                502 -> "Could not resolve YouTube link (502 Bad Gateway). Service unable to fetch track metadata."
                in 500..599 -> "Resolver backend server error (HTTP $code)."
                else -> "Resolver returned HTTP $code"
            }
        }
    }
}
