package com.lalit.amplify.feature.downloader.data

import com.lalit.amplify.feature.search.DownloadableTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YouTubeResolverRepository @Inject constructor() {

    private val client = OkHttpClient()
    
    // This should be configurable, for now using a placeholder
    private var backendBaseUrl = "https://amplify-production-0b58.up.railway.app"

    fun setBackendUrl(url: String) {
        backendBaseUrl = url.removeSuffix("/")
    }

    suspend fun resolveYouTubeUrl(youtubeUrl: String): Result<DownloadableTrack> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$backendBaseUrl/resolve?url=$youtubeUrl")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("Unexpected code $response"))
                }

                val body = response.body?.string() ?: return@withContext Result.failure(IOException("Empty body"))
                val json = JSONObject(body)

                val track = DownloadableTrack(
                    id = youtubeUrl.hashCode().toString(), // Simple ID for now
                    title = json.getString("title"),
                    artist = json.getString("artist"),
                    duration = json.getLong("durationSeconds") * 1000,
                    thumbnailUrl = json.getString("thumbnailUrl"),
                    sourceLabel = "YouTube",
                    webUrl = youtubeUrl,
                    streamUrl = "$backendBaseUrl/stream?url=$youtubeUrl", // Mode A: Proxying via backend
                    audioQuality = "High",
                    fileExtension = "m4a",
                    contentType = "audio/mp4"
                )

                Result.success(track)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
