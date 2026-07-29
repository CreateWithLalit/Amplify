package com.lalit.amplify.core.data.repository

import com.lalit.amplify.core.database.entity.PlaylistEntity
import com.lalit.amplify.core.model.Song
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>
    suspend fun createPlaylist(name: String): Long
    suspend fun deletePlaylist(playlist: PlaylistEntity)
    suspend fun addSongToPlaylist(playlistId: Long, song: Song)
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)
    fun getSongsInPlaylist(playlistId: Long): Flow<List<Song>>
}
