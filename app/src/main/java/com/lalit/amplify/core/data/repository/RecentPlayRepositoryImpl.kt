package com.lalit.amplify.core.data.repository

import com.lalit.amplify.core.database.dao.RecentDao
import com.lalit.amplify.core.database.dao.SongDao
import com.lalit.amplify.core.database.entity.RecentPlayEntity
import com.lalit.amplify.core.database.mapper.toEntity
import com.lalit.amplify.core.database.mapper.toSong
import com.lalit.amplify.core.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RecentPlayRepositoryImpl @Inject constructor(
    private val recentDao: RecentDao,
    private val songDao: SongDao
) : RecentPlayRepository {

    override fun getRecentPlays(limit: Int): Flow<List<Song>> {
        return recentDao.getRecentPlays(limit).map { entities ->
            entities.map { it.toSong() }
        }
    }

    override suspend fun addRecentPlay(song: Song) {
        songDao.insertSong(song.toEntity())
        recentDao.insertRecentPlay(RecentPlayEntity(songId = song.id))
        recentDao.trimRecentPlays(20) // Keep only last 20
    }
}
