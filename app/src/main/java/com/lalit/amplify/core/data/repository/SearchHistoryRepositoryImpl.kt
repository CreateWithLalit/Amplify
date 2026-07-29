package com.lalit.amplify.core.data.repository

import com.lalit.amplify.core.database.dao.SearchHistoryDao
import com.lalit.amplify.core.database.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchHistoryRepositoryImpl @Inject constructor(
    private val searchHistoryDao: SearchHistoryDao
) : SearchHistoryRepository {

    override fun getSearchHistory(): Flow<List<SearchHistoryEntity>> {
        return searchHistoryDao.getSearchHistory()
    }

    override suspend fun addSearch(query: String) {
        searchHistoryDao.insertSearch(SearchHistoryEntity(query = query))
    }

    override suspend fun deleteSearch(query: String) {
        searchHistoryDao.deleteSearch(query)
    }

    override suspend fun clearHistory() {
        searchHistoryDao.clearSearchHistory()
    }
}
