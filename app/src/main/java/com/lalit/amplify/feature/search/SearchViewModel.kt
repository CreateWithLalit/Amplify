package com.lalit.amplify.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SearchUiState {
    object Idle : SearchUiState()
    object Loading : SearchUiState()
    data class Success(val results: List<MusicSearchResult>) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
    object Empty : SearchUiState()
}

sealed class DownloadLinkState {
    object Idle : DownloadLinkState()
    object Resolving : DownloadLinkState()
    data class Resolved(val track: DownloadableTrack) : DownloadLinkState()
    data class Error(val message: String) : DownloadLinkState()
}

class SearchViewModel(
    private val repository: MusicSearchRepository = MockMusicSearchRepository()
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _selectedResult = MutableStateFlow<MusicSearchResult?>(null)
    val selectedResult: StateFlow<MusicSearchResult?> = _selectedResult.asStateFlow()

    private val _downloadLinkState = MutableStateFlow<DownloadLinkState>(DownloadLinkState.Idle)
    val downloadLinkState: StateFlow<DownloadLinkState> = _downloadLinkState.asStateFlow()

    private var searchJob: Job? = null
    private var resolveJob: Job? = null

    fun onQueryChange(q: String) {
        if (_selectedResult.value != null) {
            clearSelection()
        }
        _query.value = q
        searchJob?.cancel()
        if (q.isBlank()) {
            _uiState.value = SearchUiState.Idle
            return
        }
        // Debounce 600ms
        searchJob = viewModelScope.launch {
            delay(600)
            performSearch(q)
        }
    }

    fun onSearchSubmit() {
        if (_selectedResult.value != null) {
            clearSelection()
        }
        searchJob?.cancel()
        val q = _query.value.trim()
        if (q.isNotBlank()) {
            viewModelScope.launch { performSearch(q) }
        }
    }

    private suspend fun performSearch(q: String) {
        _uiState.value = SearchUiState.Loading
        repository.search(q).fold(
            onSuccess = { results ->
                _uiState.value = if (results.isEmpty()) {
                    SearchUiState.Empty
                } else {
                    SearchUiState.Success(results)
                }
            },
            onFailure = { error ->
                _uiState.value = SearchUiState.Error(
                    error.message ?: "Something went wrong"
                )
            }
        )
    }

    fun selectResult(result: MusicSearchResult) {
        _selectedResult.value = result
        _downloadLinkState.value = DownloadLinkState.Idle
    }

    fun clearSelection() {
        _selectedResult.value = null
        _downloadLinkState.value = DownloadLinkState.Idle
        resolveJob?.cancel()
    }

    fun getDownloadLink() {
        val result = _selectedResult.value ?: return
        resolveJob?.cancel()
        _downloadLinkState.value = DownloadLinkState.Resolving

        viewModelScope.launch {
            repository.resolveDownloadLink(result).fold(
                onSuccess = { track ->
                    _downloadLinkState.value = DownloadLinkState.Resolved(track)
                },
                onFailure = { error ->
                    _downloadLinkState.value = DownloadLinkState.Error(
                        error.message ?: "Failed to get download link"
                    )
                }
            )
        }
    }

    fun clearSearch() {
        _query.value = ""
        _uiState.value = SearchUiState.Idle
        _selectedResult.value = null
        _downloadLinkState.value = DownloadLinkState.Idle
        searchJob?.cancel()
        resolveJob?.cancel()
    }
}
