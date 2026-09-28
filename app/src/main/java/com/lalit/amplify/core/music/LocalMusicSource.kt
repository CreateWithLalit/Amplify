package com.lalit.amplify.core.music

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.core.net.toUri
import com.lalit.amplify.core.model.Song
import com.lalit.amplify.core.model.SongSource
import com.lalit.amplify.feature.downloader.data.DownloadedSongRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow

/**
 * Local music source: combines device MediaStore audio + downloaded tracks.
 * This preserves all existing local playback logic without modification.
 */
class LocalMusicSource(
    private val context: Context
) : MusicSource {

    override val sourceId: String = "local"
    override val displayName: String = "My Library"
    override val requiresInternet: Boolean = false

    private val downloadedSongRepository = DownloadedSongRepository.getInstance(context)

    override suspend fun search(query: String, limit: Int, offset: Int): List<Song> {
        return getLocalSongsFromMediaStore().filter { song ->
            song.title.contains(query, ignoreCase = true) ||
            song.artist.contains(query, ignoreCase = true) ||
            song.album.contains(query, ignoreCase = true)
        }.drop(offset).take(limit)
    }

    override suspend fun getTrending(limit: Int, offset: Int): List<Song> {
        // Local source doesn't have "trending" - return recently played or most played
        return emptyList() // Will be handled by Dashboard recommendations
    }

    override suspend fun getByGenre(genre: String, limit: Int, offset: Int): List<Song> {
        // Local files don't typically have genre metadata without additional parsing
        return emptyList()
    }

    override suspend fun getRecommendations(limit: Int): List<Song> {
        // Return first songs from library as default recommendations
        return getLocalSongsFromMediaStore().take(limit)
    }

    override fun getSongsFlow(): Flow<List<Song>> {
        // Combine local MediaStore songs with downloaded songs
        return combine(
            getLocalSongsFlowFromMediaStore(),
            downloadedSongRepository.downloadedSongs
        ) { local, downloaded ->
            val downloadedUris = downloaded.mapTo(mutableSetOf()) { it.uri.toString() }
            // Prefer downloaded-song record (carries source + art), prevent duplicates
            (downloaded + local.filterNot { it.uri.toString() in downloadedUris })
                .sortedBy { it.title.lowercase() }
        }
    }

    override suspend fun isAvailable(): Boolean {
        return true // Local storage is always available
    }

    /**
     * Fetch all local audio from device MediaStore.
     * Handles different API levels (Q+ vs legacy).
     */
    private suspend fun getLocalSongsFromMediaStore(): List<Song> {
        val songs = mutableListOf<Song>()
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        context.contentResolver.query(
            uri,
            arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM_ID
            ),
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
            val albumIdColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "Unknown"
                val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                val album = cursor.getString(albumColumn) ?: "Unknown Album"
                val duration = cursor.getLong(durationColumn)
                val albumId = cursor.getLong(albumIdColumn)

                val contentUri = ContentUris.withAppendedId(uri, id)
                val albumArtUri = ContentUris.withAppendedId(
                    "content://media/external/audio/albumart".toUri(),
                    albumId
                )

                songs.add(
                    Song(
                        id = id,
                        title = title,
                        artist = artist,
                        album = album,
                        duration = duration,
                        uri = contentUri,
                        albumArtUri = albumArtUri,
                        source = SongSource.LOCAL
                    )
                )
            }
        }

        return songs
    }

    /**
     * Get MediaStore songs as a Flow for reactive updates.
     */
    private fun getLocalSongsFlowFromMediaStore(): Flow<List<Song>> {
        return kotlinx.coroutines.flow.flow {
            val songs = getLocalSongsFromMediaStore()
            emit(songs)
        }
    }
}

