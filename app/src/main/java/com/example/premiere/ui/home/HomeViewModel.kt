package com.example.premiere.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.premiere.data.repository.MovieRepository
import com.example.premiere.domain.model.Movie
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.first

data class HomeUiState(
    val movies: List<Movie> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MovieRepository
) : ViewModel() {

    init {
        viewModelScope.launch {
            repository.moviesWithReminders.collect { movies ->
                checkAndArchive(movies)
            }
        }
        viewModelScope.launch {
            while (isActive) {
                delay(30_000) // Check every 30 seconds
                try {
                    val movies = repository.moviesWithReminders.first()
                    checkAndArchive(movies)
                } catch (e: Exception) {}
            }
        }
    }

    private suspend fun checkAndArchive(movies: List<Movie>) {
        val now = java.time.LocalDateTime.now()
        for (movie in movies) {
            val dateStr = if (movie.ticketDate.isNotBlank()) movie.ticketDate else movie.releaseDate
            if (dateStr.isNotBlank() && movie.ticketTime.isNotBlank()) {
                try {
                    val ticketDate = java.time.LocalDate.parse(dateStr)
                    val ticketTime = parseTimeSafely(movie.ticketTime)
                    if (ticketTime != null) {
                        val endDateTime = java.time.LocalDateTime.of(ticketDate, ticketTime)
                            .plusMinutes(movie.runtime?.toLong() ?: 150L)
                        
                        if (now.isAfter(endDateTime)) {
                            if (!movie.isWatchedInTheater) {
                                repository.toggleWatchedInTheater(movie)
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("HomeViewModel", "Auto-archive failed for ${movie.title}: ${e.message}")
                }
            }
        }
    }

    private fun parseTimeSafely(timeStr: String): java.time.LocalTime? {
        return try {
            val clean = timeStr.trim().uppercase(java.util.Locale.US)
            if (clean.contains("AM") || clean.contains("PM")) {
                val spaceClean = if (clean.contains(" ")) clean else clean.replace("AM", " AM").replace("PM", " PM")
                val formatter = java.time.format.DateTimeFormatter.ofPattern("h:mm a", java.util.Locale.US)
                java.time.LocalTime.parse(spaceClean, formatter)
            } else {
                val parts = clean.split(":")
                if (parts.size == 2) {
                    val h = parts[0].padStart(2, '0')
                    val m = parts[1].padStart(2, '0')
                    java.time.LocalTime.parse("$h:$m")
                } else {
                    java.time.LocalTime.parse(clean)
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    val uiState: StateFlow<HomeUiState> = repository.moviesWithReminders
        .map { movies -> HomeUiState(movies = movies, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeUiState(isLoading = true)
        )
}
