package com.lalit.amplify.feature.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lalit.amplify.core.data.AmplifyDataStore
import com.lalit.amplify.core.data.jamendo.JamendoRepository
import com.lalit.amplify.core.music.BollywoodDiscoveryMusicSource
import com.lalit.amplify.core.music.JamendoMusicSource
import com.lalit.amplify.core.model.Song
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SearchTab { ALL, BOLLYWOOD_GLOBAL, JAMENDO, LOCAL }

sealed class DownloadLinkState {
    object Idle : DownloadLinkState()
    object Resolving : DownloadLinkState()
    data class Resolved(val track: DownloadableTrack) : DownloadLinkState()
    data class Error(val message: String) : DownloadLinkState()
}

data class SearchUiState(
    val query: String = "",
    val selectedTab: SearchTab = SearchTab.ALL,
    val selectedGenre: String? = null,
    val bollywoodResults: List<Song> = emptyList(),
    val jamendoResults: List<Song> = emptyList(),
    val jamendoLoading: Boolean = false,
    val jamendoError: String? = null,
    val jamendoHasMore: Boolean = false,
    val jamendoOffset: Int = 0
)

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStore = AmplifyDataStore(application)
    private val jamendoRepository = JamendoRepository.getInstance(application)
    private val bollywoodSource = BollywoodDiscoveryMusicSource()
    private val pageSize = 20

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    val recentSearches: StateFlow<List<String>> = dataStore.jamendoSearchHistory.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val genres = BollywoodDiscoveryMusicSource.GENRES + JamendoMusicSource.JAMENDO_GENRES

    private var searchJob: Job? = null
    private var paginationJob: Job? = null

    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        if (_uiState.value.selectedTab == SearchTab.LOCAL) return
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(
                bollywoodResults = emptyList(),
                jamendoResults = emptyList(),
                jamendoLoading = false,
                jamendoError = null,
                jamendoHasMore = false,
                jamendoOffset = 0,
                selectedGenre = null
            )
            return
        }

        searchJob = viewModelScope.launch {
            delay(400)
            val bResults = bollywoodSource.search(query.trim(), 20, 0)
            _uiState.value = _uiState.value.copy(bollywoodResults = bResults)
            performJamendoSearch(query.trim(), offset = 0, append = false)
            dataStore.addJamendoSearchQuery(query)
        }
    }

    fun onTabSelected(tab: SearchTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
        if (tab == SearchTab.LOCAL) {
            return
        }
        val query = _uiState.value.query.trim()
        val genre = _uiState.value.selectedGenre
        if (genre != null) {
            searchGenre(genre)
        } else if (query.isNotBlank()) {
            searchJob?.cancel()
            searchJob = viewModelScope.launch {
                val bResults = bollywoodSource.search(query, 20, 0)
                _uiState.value = _uiState.value.copy(bollywoodResults = bResults)
                performJamendoSearch(query, offset = 0, append = false)
            }
        }
    }

    fun searchNow() {
        val query = _uiState.value.query.trim()
        if (_uiState.value.selectedGenre != null) {
            searchGenre(_uiState.value.selectedGenre!!)
            return
        }
        if (query.isBlank()) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val bResults = bollywoodSource.search(query, 20, 0)
            _uiState.value = _uiState.value.copy(bollywoodResults = bResults)
            performJamendoSearch(query, offset = 0, append = false)
        }
    }

    fun searchGenre(genre: String) {
        _uiState.value = _uiState.value.copy(selectedGenre = genre, query = "")
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val bResults = bollywoodSource.getByGenre(genre, 20, 0)
            val jamendoSource = JamendoMusicSource { jamendoRepository.getQualityPreferenceSync() }
            _uiState.value = _uiState.value.copy(
                bollywoodResults = bResults,
                jamendoLoading = true,
                jamendoError = null,
                jamendoResults = emptyList(),
                jamendoOffset = 0,
                jamendoHasMore = false
            )
            runCatching { jamendoSource.getByGenre(genre, pageSize, 0) }
                .onSuccess { results ->
                    _uiState.value = _uiState.value.copy(
                        jamendoResults = results,
                        jamendoLoading = false,
                        jamendoError = null,
                        jamendoOffset = results.size,
                        jamendoHasMore = results.size >= pageSize
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        jamendoLoading = false,
                        jamendoError = if (bResults.isNotEmpty()) null else (error.message ?: "Failed to load genre")
                    )
                }
        }
    }

    fun loadMoreJamendo() {
        val state = _uiState.value
        if (state.selectedTab == SearchTab.LOCAL || state.selectedTab == SearchTab.BOLLYWOOD_GLOBAL) return
        if (!state.jamendoHasMore || state.jamendoLoading) return
        val query = state.query.trim()
        val genre = state.selectedGenre

        paginationJob?.cancel()
        paginationJob = viewModelScope.launch {
            performJamendoSearch(
                query = query,
                offset = state.jamendoOffset,
                append = true,
                genre = genre
            )
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        paginationJob?.cancel()
        _uiState.value = SearchUiState()
    }

    fun clearGenre() {
        _uiState.value = _uiState.value.copy(selectedGenre = null)
        searchNow()
    }

    fun removeRecentSearch(query: String) {
        viewModelScope.launch { dataStore.removeJamendoSearchQuery(query) }
    }

    fun clearRecentSearches() {
        viewModelScope.launch { dataStore.clearJamendoSearchHistory() }
    }

    private suspend fun performJamendoSearch(
        query: String,
        offset: Int,
        append: Boolean,
        genre: String? = null
    ) {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank() && genre == null) return

        _uiState.value = _uiState.value.copy(jamendoLoading = true, jamendoError = null)

        val result = runCatching {
            val jamendoSource = JamendoMusicSource { jamendoRepository.getQualityPreferenceSync() }
            if (genre != null) {
                jamendoSource.getByGenre(genre, pageSize, offset)
            } else {
                jamendoSource.search(normalizedQuery, pageSize, offset)
            }
        }

        result.onSuccess { results ->
            val merged = if (append) {
                (_uiState.value.jamendoResults + results).distinctBy { it.id }
            } else {
                results.distinctBy { it.id }
            }

            _uiState.value = _uiState.value.copy(
                jamendoResults = merged,
                jamendoLoading = false,
                jamendoError = null,
                jamendoOffset = merged.size,
                jamendoHasMore = results.size >= pageSize
            )
        }.onFailure { error ->
            _uiState.value = _uiState.value.copy(
                jamendoLoading = false,
                jamendoError = if (_uiState.value.bollywoodResults.isNotEmpty()) null else (error.message ?: "Failed to search Jamendo")
            )
        }
    }
}

