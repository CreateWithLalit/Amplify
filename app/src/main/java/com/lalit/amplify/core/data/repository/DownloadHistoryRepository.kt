package com.lalit.amplify.core.data.repository

import com.lalit.amplify.core.model.Song
import kotlinx.coroutines.flow.Flow

interface DownloadHistoryRepository {
    fun getDownloadedSongs(): Flow<List<Song>>
    suspend fun addDownload(song: Song, downloadUrl: String, quality: String, filePath: String?)
    suspend fun removeDownload(songId: Long)
}
