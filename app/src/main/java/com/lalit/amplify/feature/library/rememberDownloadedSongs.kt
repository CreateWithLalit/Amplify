@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.lalit.amplify.feature.library

import android.content.ContentUris
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.lalit.amplify.core.model.Song
import com.lalit.amplify.core.model.SongSource
import com.lalit.amplify.feature.downloader.DownloadViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.absoluteValue

/**
 * Safe ID offset for downloaded songs. MediaStore IDs are typically small (< 1M).
 * This guarantees zero collision with local songs.
 */
private const val DOWNLOAD_ID_OFFSET = 10_000_000L

@Composable
fun rememberDownloadedSongs(downloadViewModel: DownloadViewModel): List<Song> {
    val destinationUri by downloadViewModel.destinationUri.collectAsState()
    val downloadState by downloadViewModel.downloadState.collectAsState()

    val songs: State<List<Song>> = produceState(
        initialValue = emptyList(),
        key1 = destinationUri,
        key2 = downloadState
    ) {
        value = withContext(Dispatchers.IO) {
            scanDownloadedSongs(downloadViewModel)
        }
    }

    return songs.value
}

private suspend fun scanDownloadedSongs(downloadViewModel: DownloadViewModel): List<Song> {
    val context = downloadViewModel.getApplication<android.app.Application>()
    val destUri = downloadViewModel.destinationUri.value
    val songs = mutableListOf<Song>()
    val seenUris = mutableSetOf<String>()

    // 1. Scan SAF folder if set
    if (destUri != null) {
        try {
            val parent = DocumentFile.fromTreeUri(context, destUri)
            parent?.listFiles()?.forEach { doc ->
                if (doc.isFile && isAudioFile(doc.name)) {
                    createSongFromDocument(context, doc)?.let { song ->
                        if (seenUris.add(song.uri.toString())) {
                            songs.add(song)
                        }
                    }
                }
            }
        } catch (_: Exception) { /* SAF access may be revoked */ }
    }

    // 2. Scan internal fallback folder
    val internalDir = context.getExternalFilesDir(null)?.let {
        File(it, "Music/Amplify")
    }
    internalDir?.listFiles()?.forEach { file ->
        if (isAudioFile(file.name)) {
            createSongFromFile(context, file)?.let { song ->
                if (seenUris.add(song.uri.toString())) {
                    songs.add(song)
                }
            }
        }
    }

    // 3. Scan MediaStore for Amplify-downloaded files
    scanMediaStoreDownloads(context)?.let { mediaStoreSongs ->
        mediaStoreSongs.forEach { song ->
            if (seenUris.add(song.uri.toString())) {
                songs.add(song)
            }
        }
    }

    return songs
}

private fun isAudioFile(name: String?): Boolean {
    if (name == null) return false
    return name.endsWith(".mp3", ignoreCase = true) ||
           name.endsWith(".m4a", ignoreCase = true) ||
           name.endsWith(".wav", ignoreCase = true) ||
           name.endsWith(".flac", ignoreCase = true) ||
           name.endsWith(".ogg", ignoreCase = true) ||
           name.endsWith(".aac", ignoreCase = true)
}

/**
 * Extract real metadata using MediaMetadataRetriever.
 * Falls back to filename parsing if metadata is missing.
 */
private fun createSongFromDocument(context: android.content.Context, doc: DocumentFile): Song? {
    val name = doc.name ?: return null
    val uri = doc.uri

    val retriever = MediaMetadataRetriever()
    var title: String? = null
    var artist: String? = null
    var album: String? = null
    var duration: Long = 0L

    try {
        retriever.setDataSource(context, uri)
        title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
        artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
        album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
        duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
    } catch (_: Exception) {
        // File may be corrupted or unsupported
    } finally {
        retriever.release()
    }

    // Parse from filename as fallback
    val fallbackArtist = name.substringBefore(" - ", "").takeIf { it.isNotBlank() }
    val fallbackTitle = if (name.contains(" - ")) {
        name.substringAfter(" - ", "").substringBeforeLast(".")
    } else {
        name.substringBeforeLast(".")
    }

    // Generate safe ID: offset + hash of URI (absolute value to ensure positive)
    val safeId = DOWNLOAD_ID_OFFSET + uri.toString().hashCode().absoluteValue

    return Song(
        id = safeId,
        title = title?.takeIf { it.isNotBlank() } ?: fallbackTitle,
        artist = artist?.takeIf { it.isNotBlank() } ?: fallbackArtist ?: "Unknown Artist",
        album = album?.takeIf { it.isNotBlank() } ?: "Downloaded",
        duration = duration,
        uri = uri,
        albumArtUri = null,
        source = SongSource.DOWNLOADED
    )
}

private fun createSongFromFile(context: android.content.Context, file: File): Song? {
    val name = file.name
    val uri = Uri.fromFile(file)

    val retriever = MediaMetadataRetriever()
    var title: String? = null
    var artist: String? = null
    var album: String? = null
    var duration: Long = 0L

    try {
        retriever.setDataSource(file.absolutePath)
        title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
        artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
        album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
        duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
    } catch (_: Exception) {
        // File may be corrupted or unsupported
    } finally {
        retriever.release()
    }

    val fallbackArtist = name.substringBefore(" - ", "").takeIf { it.isNotBlank() }
    val fallbackTitle = if (name.contains(" - ")) {
        name.substringAfter(" - ", "").substringBeforeLast(".")
    } else {
        name.substringBeforeLast(".")
    }

    val safeId = DOWNLOAD_ID_OFFSET + uri.toString().hashCode().absoluteValue

    return Song(
        id = safeId,
        title = title?.takeIf { it.isNotBlank() } ?: fallbackTitle,
        artist = artist?.takeIf { it.isNotBlank() } ?: fallbackArtist ?: "Unknown Artist",
        album = album?.takeIf { it.isNotBlank() } ?: "Downloaded",
        duration = duration,
        uri = uri,
        albumArtUri = null,
        source = SongSource.DOWNLOADED
    )
}

private fun scanMediaStoreDownloads(context: android.content.Context): List<Song>? {
    val songs = mutableListOf<Song>()
    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    }

    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.ALBUM_ID,
        MediaStore.Audio.Media.DATA
    )

    val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

    context.contentResolver.query(collection, projection, selection, null, null)
        ?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (cursor.moveToNext()) {
                val data = cursor.getString(dataCol) ?: continue
                // Only include files from Amplify directories
                if (!data.contains("Amplify") && !data.contains("amplify")) continue

                val mediaStoreId = cursor.getLong(idCol)
                val title = cursor.getString(titleCol) ?: "Unknown"
                val artist = cursor.getString(artistCol)?.takeIf { it.isNotBlank() } ?: "Unknown Artist"
                val album = cursor.getString(albumCol) ?: "Downloaded"
                val duration = cursor.getLong(durationCol)
                val albumId = cursor.getLong(albumIdCol)

                val uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaStoreId
                )
                val albumArtUri = "content://media/external/audio/albumart/$albumId".toUri()

                // Offset MediaStore IDs from Amplify downloads too
                val safeId = DOWNLOAD_ID_OFFSET + mediaStoreId

                songs.add(
                    Song(
                        id = safeId,
                        title = title,
                        artist = artist,
                        album = album,
                        duration = duration,
                        uri = uri,
                        albumArtUri = albumArtUri,
                        source = SongSource.DOWNLOADED
                    )
                )
            }
        }

    return songs.takeIf { it.isNotEmpty() }
}
