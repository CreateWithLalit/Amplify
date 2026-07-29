package com.lalit.amplify.core.data.repository

import com.lalit.amplify.core.database.dao.PlaylistDao
import com.lalit.amplify.core.database.dao.SongDao
import com.lalit.amplify.core.database.entity.PlaylistEntity
import com.lalit.amplify.core.database.entity.PlaylistSongCrossRef
import com.lalit.amplify.core.database.mapper.toEntity
import com.lalit.amplify.core.database.mapper.toSong
import com.lalit.amplify.core.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PlaylistRepositoryImpl @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val songDao: SongDao
) : PlaylistRepository {

    override fun getAllPlaylists(): Flow<List<PlaylistEntity>> {
        return playlistDao.getAllPlaylists()
    }

    override suspend fun createPlaylist(name: String): Long {
        return playlistDao.insertPlaylist(PlaylistEntity(name = name))
    }

    override suspend fun deletePlaylist(playlist: PlaylistEntity) {
        playlistDao.deletePlaylist(playlist)
    }

    override suspend fun addSongToPlaylist(playlistId: Long, song: Song) {
        songDao.insertSong(song.toEntity())
        playlistDao.insertPlaylistSongCrossRef(
            PlaylistSongCrossRef(playlistId = playlistId, songId = song.id)
        )
    }

    override suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        playlistDao.deletePlaylistSongCrossRef(
            PlaylistSongCrossRef(playlistId = playlistId, songId = songId)
        )
    }

    override fun getSongsInPlaylist(playlistId: Long): Flow<List<Song>> {
        return playlistDao.getSongsInPlaylist(playlistId).map { entities ->
            entities.map { it.toSong() }
        }
    }
}
