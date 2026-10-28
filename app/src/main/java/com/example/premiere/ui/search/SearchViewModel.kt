package com.example.premiere.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.premiere.data.repository.MovieRepository
import com.example.premiere.domain.model.Movie
import com.example.premiere.domain.model.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import java.time.LocalDate
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<Movie> = emptyList(),
    val searchHistory: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showNoApiKey: Boolean = false
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: MovieRepository,
    private val prefs: com.example.premiere.data.preferences.PremierePreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val queryFlow = MutableStateFlow("")
    var forceShowReleased = false
    private var prefetchJob: kotlinx.coroutines.Job? = null

    private var searchHistoryJob: kotlinx.coroutines.Job? = null
    var source: String = "home"
        set(value) {
            if (field != value) {
                field = value
                loadHistory()
            }
        }

    private fun loadHistory() {
        searchHistoryJob?.cancel()
        searchHistoryJob = viewModelScope.launch {
            prefs.getSearchHistory(source).collectLatest { history ->
                _uiState.update { it.copy(searchHistory = history) }
            }
        }
    }

    init {
        loadHistory()

        @OptIn(FlowPreview::class)
        viewModelScope.launch {
            queryFlow
                .debounce(400)
                .distinctUntilChanged()
                .collectLatest { query ->
                    performSearch(query)
                }
        }
    }

    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(query = query) }
        queryFlow.value = query
    }

    fun saveCurrentQuery() {
        val currentQuery = _uiState.value.query
        if (currentQuery.isNotBlank()) {
            viewModelScope.launch {
                prefs.addSearchHistory(currentQuery, source)
            }
        }
    }

    fun removeSearchHistory(query: String) {
        viewModelScope.launch {
            prefs.removeSearchHistory(query, source)
        }
    }

    fun loadUpcoming() {
        val currentQuery = _uiState.value.query
        if (currentQuery.isNotBlank()) {
            viewModelScope.launch { performSearch(currentQuery) }
        } else {
            _uiState.update { it.copy(results = emptyList(), isLoading = false, error = null) }
        }
    }

    private suspend fun performSearch(query: String) {
        if (query.isBlank()) {
            _uiState.update { it.copy(results = emptyList(), isLoading = false, error = null) }
            return
        }
        
        _uiState.update { it.copy(isLoading = true, error = null, showNoApiKey = false) }
        when (val result = repository.searchUpcomingMovies(query)) {
            is Result.Success -> {
                val today = LocalDate.now().toString()
                val finalMovies = if (forceShowReleased) {
                    result.data.filter { it.releaseDate.isNotBlank() && it.releaseDate <= today }
                } else {
                    val showReleased = prefs.showReleasedTitles.first()
                    if (showReleased) {
                        result.data
                    } else {
                        result.data.filter { it.releaseDate.isNotBlank() && it.releaseDate > today }
                    }
                }
                
                // Prefetch logo and rating for top visible results (up to 15) so they appear instantly when added
                prefetchJob?.cancel()
                prefetchJob = viewModelScope.launch {
                    finalMovies.take(15).map { movie ->
                        async { repository.prefetchLogoAndRating(movie.id) }
                    }.awaitAll()
                }
                
                _uiState.update {
                    it.copy(results = finalMovies, isLoading = false)
                }
            }
            is Result.Error -> {
                val noKey = result.message.contains("API key", ignoreCase = true)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = if (!noKey) result.message else null,
                        showNoApiKey = noKey,
                        results = emptyList()
                    )
                }
            }
            is Result.Loading -> {}
        }
    }
}
