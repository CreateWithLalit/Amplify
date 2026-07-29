package com.lalit.amplify.core.database.dao

import androidx.room.*
import com.lalit.amplify.core.database.entity.DownloadHistoryEntity
import com.lalit.amplify.core.database.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadHistoryEntity)

    @Transaction
    @Query("""
        SELECT songs.* FROM songs 
        INNER JOIN download_history ON songs.id = download_history.songId 
        ORDER BY download_history.downloadedAt DESC
    """)
    fun getDownloadedSongs(): Flow<List<SongEntity>>

    @Query("DELETE FROM download_history WHERE songId = :songId")
    suspend fun deleteDownload(songId: Long)
}
