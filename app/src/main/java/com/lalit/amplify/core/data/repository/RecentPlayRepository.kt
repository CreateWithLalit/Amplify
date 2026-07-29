package com.lalit.amplify.core.data.repository

import com.lalit.amplify.core.model.Song
import kotlinx.coroutines.flow.Flow

interface RecentPlayRepository {
    fun getRecentPlays(limit: Int = 20): Flow<List<Song>>
    suspend fun addRecentPlay(song: Song)
}
