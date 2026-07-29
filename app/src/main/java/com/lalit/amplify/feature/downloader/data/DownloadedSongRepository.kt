@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.lalit.amplify.feature.downloader.data

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.lalit.amplify.core.model.Song
import com.lalit.amplify.core.model.SongSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.absoluteValue

/**
 * Safe ID offset for downloaded songs. MediaStore IDs are typically small (< 1M).
 * This guarantees zero collision with local songs.
 */
private const val DOWNLOAD_ID_OFFSET = 10_000_000L

/**
 * Repository that scans for downloaded audio files and returns them as proper Song objects.
 * Uses MediaMetadataRetriever for real metadata extraction.
 */
class DownloadedSongRepository private constructor(private val context: Context) {

    private val _downloadedSongs = MutableStateFlow<List<Song>>(emptyList())
    val downloadedSongs: Flow<List<Song>> = _downloadedSongs.asStateFlow()

    /**
     * Scan all known download locations for audio files.
     * Call this on app startup and after each successful download.
     */
    suspend fun scanDownloadedSongs(folderUri: Uri? = null) = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()

        // 1. Scan SAF folder if set
        folderUri?.let { uri ->
            try {
                val parent = DocumentFile.fromTreeUri(context, uri)
                parent?.listFiles()?.forEach { doc ->
                    if (doc.isFile && isAudioFile(doc.name)) {
                        createSongFromDocument(doc)?.let { songs.add(it) }
                    }
                }
            } catch (_: Exception) {
                // SAF permission may be revoked.
            }
        }

        // 2. Scan internal fallback folder
        val internalDir = context.getExternalFilesDir(null)?.let {
            File(it, "Music/Amplify")
        }
        internalDir?.listFiles()?.forEach { file ->
            if (isAudioFile(file.name)) {
                createSongFromFile(file)?.let { songs.add(it) }
            }
        }

        // 3. Scan MediaStore for our downloaded files
        val mediaStoreSongs = scanMediaStoreDownloads()
        songs.addAll(mediaStoreSongs)

        // Deduplicate by URI
        val unique = songs.distinctBy { it.uri.toString() }
        _downloadedSongs.value = unique
    }

    /**
     * Add a single newly downloaded song without full rescan.
     */
    fun addDownloadedSong(song: Song) {
        val current = _downloadedSongs.value.toMutableList()
        current.removeAll { it.uri.toString() == song.uri.toString() }
        current.add(0, song)
        _downloadedSongs.value = current
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

    private fun createSongFromDocument(doc: DocumentFile): Song? {
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

        val parsedArtist = name.substringBefore(" - ", "").takeIf { it.isNotBlank() }
        val parsedTitle = if (name.contains(" - ")) {
            name.substringAfter(" - ", "").substringBeforeLast(".")
        } else {
            name.substringBeforeLast(".")
        }

        // Generate safe ID: offset + hash of URI (absolute value to ensure positive)
        val safeId = DOWNLOAD_ID_OFFSET + uri.toString().hashCode().absoluteValue

        return Song(
            id = safeId,
            title = title?.takeIf { it.isNotBlank() } ?: parsedTitle,
            artist = artist?.takeIf { it.isNotBlank() } ?: parsedArtist ?: "Unknown Artist",
            album = album?.takeIf { it.isNotBlank() } ?: "Downloaded",
            duration = duration,
            uri = uri,
            albumArtUri = null,
            source = SongSource.DOWNLOADED
        )
    }

    private fun createSongFromFile(file: File): Song? {
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

        val parsedArtist = name.substringBefore(" - ", "").takeIf { it.isNotBlank() }
        val parsedTitle = if (name.contains(" - ")) {
            name.substringAfter(" - ", "").substringBeforeLast(".")
        } else {
            name.substringBeforeLast(".")
        }

        val safeId = DOWNLOAD_ID_OFFSET + uri.toString().hashCode().absoluteValue

        return Song(
            id = safeId,
            title = title?.takeIf { it.isNotBlank() } ?: parsedTitle,
            artist = artist?.takeIf { it.isNotBlank() } ?: parsedArtist ?: "Unknown Artist",
            album = album?.takeIf { it.isNotBlank() } ?: "Downloaded",
            duration = duration,
            uri = uri,
            albumArtUri = null,
            source = SongSource.DOWNLOADED
        )
    }

    private fun scanMediaStoreDownloads(): List<Song> {
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

                    // Offset MediaStore IDs from Amplify downloads
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
        return songs
    }


    companion object {
        @Volatile
        private var instance: DownloadedSongRepository? = null

        fun getInstance(context: Context): DownloadedSongRepository {
            return instance ?: synchronized(this) {
                instance ?: DownloadedSongRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}

