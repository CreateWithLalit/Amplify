package com.lalit.amplify.core.model

import android.net.Uri
import androidx.media3.common.MediaMetadata

enum class SongSource {
    LOCAL,
    DOWNLOADED
}

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String = "Unknown Album",
    val duration: Long = 0L,
    val uri: Uri,
    val albumArtUri: Uri? = null,
    val source: SongSource = SongSource.LOCAL
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


