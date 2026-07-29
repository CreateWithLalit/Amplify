package com.lalit.amplify.core.data.repository

import com.lalit.amplify.core.database.dao.DownloadDao
import com.lalit.amplify.core.database.dao.SongDao
import com.lalit.amplify.core.database.entity.DownloadHistoryEntity
import com.lalit.amplify.core.database.mapper.toEntity
import com.lalit.amplify.core.database.mapper.toSong
import com.lalit.amplify.core.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DownloadHistoryRepositoryImpl @Inject constructor(
    private val downloadDao: DownloadDao,
    private val songDao: SongDao
) : DownloadHistoryRepository {

    override fun getDownloadedSongs(): Flow<List<Song>> {
        return downloadDao.getDownloadedSongs().map { entities ->
            entities.map { it.toSong() }
        }
    }

    override suspend fun addDownload(
        song: Song,
        downloadUrl: String,
        quality: String,
        filePath: String?
    ) {
        songDao.insertSong(song.toEntity())
        downloadDao.insertDownload(
            DownloadHistoryEntity(
                songId = song.id,
                downloadUrl = downloadUrl,
                quality = quality,
                filePath = filePath
            )
        )
    }

    override suspend fun removeDownload(songId: Long) {
        downloadDao.deleteDownload(songId)
    }
}
