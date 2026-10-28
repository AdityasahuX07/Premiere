package com.example.premiere.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.premiere.data.repository.MovieRepository
import com.example.premiere.domain.model.Movie
import com.example.premiere.domain.model.Cinema
import com.example.premiere.data.preferences.PremierePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditTicketUiState(
    val movieId: Int = 0,
    val movieTitle: String = "",
    val movieOverview: String = "",
    val moviePosterPath: String? = null,
    val movieBackdropPath: String? = null,
    val movieLogoUrl: String? = null,
    val movieRuntime: Int? = null,
    val movieGenres: List<String> = emptyList(),
    val movieImdbRating: String? = null,
    val movieCertification: String? = null,
    val movieReleaseDate: String = "",
    val ticketDate: String = "",
    val ticketTime: String = "",
    val ticketSeat: String = "",
    val ticketCinema: String = "",
    val ticketScreenNo: String = "",
    val ticketFormat: String = "",
    val ticketLanguage: String = "",
    val isLoading: Boolean = true,
    val cinemas: List<Cinema> = emptyList()
)

@HiltViewModel
class EditTicketViewModel @Inject constructor(
    private val repository: MovieRepository,
    private val prefs: PremierePreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditTicketUiState())
    val uiState: StateFlow<EditTicketUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.cinemas.collect { cinemasList ->
                _uiState.update { it.copy(cinemas = cinemasList) }
            }
        }
    }

    fun loadMovie(movieId: Int) {
        viewModelScope.launch {
            val defaultCinema = prefs.defaultCinema.firstOrNull() ?: ""
            val defaultFormat = prefs.defaultFormat.firstOrNull() ?: ""
            val movie = repository.getCachedMovie(movieId)
            if (movie != null) {
                _uiState.update {
                    it.copy(
                        movieId = movie.id,
                        movieTitle = movie.title,
                        movieOverview = movie.overview,
                        moviePosterPath = movie.posterPath,
                        movieBackdropPath = movie.backdropPath,
                        movieLogoUrl = movie.fullLogoUrl,
                        movieRuntime = movie.runtime,
                        movieGenres = movie.genres,
                        movieImdbRating = movie.imdbRating,
                        movieCertification = movie.certification,
                        movieReleaseDate = movie.releaseDate,
                        ticketDate = movie.ticketDate,
                        ticketTime = movie.ticketTime,
                        ticketSeat = movie.ticketSeat,
                        // Auto-fill with default if blank
                        ticketCinema = movie.ticketCinema.ifBlank { defaultCinema },
                        ticketScreenNo = movie.ticketScreenNo,
                        // Auto-fill format with default if blank
                        ticketFormat = movie.ticketFormat.ifBlank { defaultFormat },
                        ticketLanguage = movie.ticketLanguage.ifBlank {
                            val rawLang = movie.originalLanguage.ifBlank { "en" }
                            val langName = if (rawLang.length == 2) java.util.Locale(rawLang).displayLanguage else rawLang
                            langName.replaceFirstChar { ch -> ch.titlecase() }
                        },
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onDateChanged(date: String) { _uiState.update { it.copy(ticketDate = date) } }
    fun onTimeChanged(time: String) { _uiState.update { it.copy(ticketTime = time) } }
    fun onSeatChanged(seat: String) { _uiState.update { it.copy(ticketSeat = seat) } }
    fun onCinemaChanged(cinema: String) { _uiState.update { it.copy(ticketCinema = cinema) } }
    fun onScreenNoChanged(screenNo: String) { _uiState.update { it.copy(ticketScreenNo = screenNo) } }
    fun onFormatChanged(format: String) { _uiState.update { it.copy(ticketFormat = format) } }
    fun onLanguageChanged(language: String) { _uiState.update { it.copy(ticketLanguage = language) } }

    fun removeFromHistory(onDone: () -> Unit) {
        viewModelScope.launch {
            val movieId = _uiState.value.movieId
            repository.resetMovie(movieId)
            onDone()
        }
    }

    fun saveTicketInfo(onDone: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            repository.updateTicketInfo(
                movieId = state.movieId,
                date = state.ticketDate.trim(),
                time = state.ticketTime.trim(),
                seat = state.ticketSeat.trim(),
                cinema = state.ticketCinema.trim(),
                screenNo = state.ticketScreenNo.trim(),
                format = state.ticketFormat.trim(),
                language = state.ticketLanguage.trim()
            )
            onDone()
        }
    }
}
