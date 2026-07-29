package com.lalit.amplify.core.data.repository

import com.lalit.amplify.core.database.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

interface SearchHistoryRepository {
    fun getSearchHistory(): Flow<List<SearchHistoryEntity>>
    suspend fun addSearch(query: String)
    suspend fun deleteSearch(query: String)
    suspend fun clearHistory()
}
