package com.lalit.amplify.core.database.dao

import androidx.room.*
import com.lalit.amplify.core.database.entity.RecentPlayEntity
import com.lalit.amplify.core.database.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentPlay(recentPlay: RecentPlayEntity)

    @Transaction
    @Query("""
        SELECT songs.* FROM songs 
        INNER JOIN recent_plays ON songs.id = recent_plays.songId 
        ORDER BY recent_plays.playedAt DESC 
        LIMIT :limit
    """)
    fun getRecentPlays(limit: Int): Flow<List<SongEntity>>

    @Query("DELETE FROM recent_plays WHERE songId NOT IN (SELECT songId FROM recent_plays ORDER BY playedAt DESC LIMIT :limit)")
    suspend fun trimRecentPlays(limit: Int)
}
