package com.lalit.amplify.feature.downloader.data

import com.lalit.amplify.feature.search.DownloadableTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Talks to the Amplify backend; it resolves metadata and supplies the MP3 stream URL. */
@Singleton
class YouTubeResolverRepository @Inject constructor() {
    private val client = OkHttpClient()
    private var backendBaseUrl = "https://amplify-production-0b58.up.railway.app"

    fun setBackendUrl(url: String) {
        backendBaseUrl = url.removeSuffix("/")
    }

    suspend fun resolveYouTubeUrl(youtubeUrl: String): Result<DownloadableTrack> = withContext(Dispatchers.IO) {
        try {
            val requestJson = JSONObject().put("url", youtubeUrl).toString()
            val request = Request.Builder()
                .url("$backendBaseUrl/resolve")
                .post(requestJson.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("Resolver returned HTTP ${response.code}"))
                }
                val json = JSONObject(response.body?.string() ?: "")
                val title = json.optString("title", "Unknown title")
                val artist = json.optString("artist", "Unknown artist")
                val durationMillis = json.optLong("duration", json.optLong("durationSeconds", 0L)) * 1000
                val thumbnail = json.optString("thumbnail", json.optString("thumbnailUrl", "")).ifBlank { null }

                Result.success(
                    DownloadableTrack(
                        id = youtubeUrl.hashCode().toString(),
                        title = title,
                        artist = artist,
                        duration = durationMillis,
                        thumbnailUrl = thumbnail,
                        sourceLabel = "YouTube",
                        webUrl = youtubeUrl,
                        streamUrl = "$backendBaseUrl/download",
                        audioQuality = "MP3",
                        fileExtension = "mp3",
                        contentType = "audio/mpeg"
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}