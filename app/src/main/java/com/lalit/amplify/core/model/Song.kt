package com.lalit.amplify.core.model

import android.net.Uri
import androidx.media3.common.MediaMetadata

enum class SongSource {
    LOCAL,           // Device local audio files via MediaStore
    DOWNLOADED,      // Downloaded/cached audio files
    JAMENDO,         // Jamendo streaming API
    AUDIUS,          // Audius streaming API
    STREAMING        // Generic streaming (YouTube Music, etc., future)
}

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String = "Unknown Album",
    val duration: Long = 0L,
    val uri: Uri,
    val albumArtUri: Uri? = null,
    val source: SongSource = SongSource.LOCAL,
    val streamUrl: String? = null,           // For streaming sources (optional)
    val downloadUrl: String? = null,         // For remote sources that support download
    val sourceUrl: String? = null,           // Source/track page URL for attribution
    val artistUrl: String? = null,           // Artist page/profile URL
    val licenseInfo: String? = null,        // Attribution/license info for streaming
    val isExplicit: Boolean = false,        // Content rating
    val genres: List<String> = emptyList()  // Genre tags from API
) {
    fun toMediaMetadata(): MediaMetadata {
        return MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(albumArtUri)
            .build()
    }
}


