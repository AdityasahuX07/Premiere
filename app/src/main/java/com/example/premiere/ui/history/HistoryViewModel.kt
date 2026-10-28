package com.example.premiere.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.premiere.data.preferences.PremierePreferences
import com.example.premiere.data.repository.MovieRepository
import com.example.premiere.domain.model.Movie
import com.example.premiere.domain.model.Cinema
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: MovieRepository,
    private val prefs: PremierePreferences
) : ViewModel() {

    val history: StateFlow<List<Movie>> = repository.watchedHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Auto-fetch logos for any history movie that still has logoUrl == null
        // (covers imported movies and any ticket added before the logo fetch completed)
        viewModelScope.launch {
            repository.watchedHistory.collect { movies ->
                movies.filter { it.logoUrl == null || it.certification == null }.forEach { movie ->
                    launch { repository.prefetchLogoAndRating(movie.id) }
                }
            }
        }
    }

    val ticketSize: StateFlow<com.example.premiere.data.preferences.TicketSize> = prefs.ticketSize
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = com.example.premiere.data.preferences.TicketSize.MEDIUM
        )

    val cinemas: StateFlow<List<Cinema>> = prefs.cinemas
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val ticketOrientation: StateFlow<com.example.premiere.data.preferences.TicketOrientation> = prefs.ticketOrientation
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = com.example.premiere.data.preferences.TicketOrientation.HORIZONTAL
        )

    val defaultCinema: StateFlow<String> = prefs.defaultCinema
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    val ticketColor: StateFlow<String> = prefs.ticketColor
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "POSTER"
        )

    val randomTicketColors: StateFlow<Map<Int, String>> = history.map { movieList ->
        val map = mutableMapOf<Int, String>()
        val baseColors = listOf(
            "#F44336", "#E91E63", "#9C27B0", "#673AB7", "#3F51B5", "#2196F3",
            "#03A9F4", "#00BCD4", "#009688", "#4CAF50", "#FF9800", "#FF5722"
        )
        var shuffledColors = baseColors.shuffled()
        movieList.forEachIndexed { index, movie ->
            if (index > 0 && index % 12 == 0) {
                shuffledColors = baseColors.shuffled()
            }
            map[movie.id] = shuffledColors[index % 12]
        }
        map
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    fun removeFromHistory(movie: Movie) {
        viewModelScope.launch {
            repository.resetMovie(movie.id)
        }
    }

    fun addToHistory(movie: Movie) {
        viewModelScope.launch {
            if (!movie.isWatchedInTheater) {
                repository.toggleWatchedInTheater(movie)
            }
        }
    }
}
