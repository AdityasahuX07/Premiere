package com.example.premiere.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.premiere.data.preferences.PremierePreferences
import com.example.premiere.data.repository.MovieRepository
import com.example.premiere.domain.model.Cinema
import com.example.premiere.domain.model.Movie
import com.example.premiere.domain.model.Result
import com.example.premiere.worker.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import coil3.ImageLoader
import coil3.request.ImageRequest
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

data class DetailUiState(
    val movie: Movie? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val reminderToggling: Boolean = false,
    val showAddTicketDialog: Boolean = false,
    val cinemas: List<Cinema> = emptyList(),
    val defaultCinema: String = "",
    val defaultFormat: String = ""
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MovieRepository,
    private val reminderScheduler: ReminderScheduler,
    private val imageLoader: ImageLoader,
    private val prefs: PremierePreferences,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _movieId = MutableStateFlow(savedStateHandle.get<Int>("movieId") ?: -1)
    val movieId: Int get() = _movieId.value

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private var observerJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch {
            prefs.cinemas.collect { list -> _uiState.update { it.copy(cinemas = list) } }
        }
        viewModelScope.launch {
            prefs.defaultCinema.collect { v -> _uiState.update { it.copy(defaultCinema = v) } }
        }
        viewModelScope.launch {
            prefs.defaultFormat.collect { v -> _uiState.update { it.copy(defaultFormat = v) } }
        }
    }

    fun initWithMovieId(id: Int) {
        if (_movieId.value != id) {
            _movieId.value = id
            _uiState.update { it.copy(movie = null, isLoading = true, error = null, showAddTicketDialog = false) }
            loadMovieDetail()
            observeReminderState()
        }
    }

    private fun observeReminderState() {
        observerJob?.cancel()
        observerJob = viewModelScope.launch {
            repository.observeMovie(movieId).collect { localMovie ->
                if (localMovie != null) {
                    _uiState.update { state ->
                        val current = state.movie
                        state.copy(
                            movie = if (current != null) {
                                current.copy(
                                    hasReminder = localMovie.hasReminder,
                                    isWatchedInTheater = localMovie.isWatchedInTheater,
                                    ticketDate = localMovie.ticketDate,
                                    ticketTime = localMovie.ticketTime,
                                    ticketSeat = localMovie.ticketSeat,
                                    ticketCinema = localMovie.ticketCinema,
                                    ticketScreenNo = localMovie.ticketScreenNo,
                                    ticketFormat = localMovie.ticketFormat,
                                    ticketLanguage = localMovie.ticketLanguage,
                                    ticketQrImagePath = localMovie.ticketQrImagePath
                                )
                            } else {
                                localMovie
                            }
                        )
                    }
                }
            }
        }
    }

    fun loadMovieDetail() {
        if (movieId == -1) return
        viewModelScope.launch {
            val cached = repository.getCachedMovie(movieId)
            _uiState.update { it.copy(movie = cached, isLoading = true, error = null) }

            when (val result = repository.getMovieDetail(movieId)) {
                is Result.Success -> {
                    val movie = result.data
                    if (cached == null) {
                        val posterUrl = movie.highResPosterUrl ?: movie.posterUrl
                        if (posterUrl != null) {
                            val request = ImageRequest.Builder(context).data(posterUrl).build()
                            imageLoader.execute(request)
                        }
                    }
                    _uiState.update { it.copy(movie = movie, isLoading = false) }
                }
                is Result.Error -> {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            error = if (cached == null) result.message else null
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun toggleReminder() {
        val movie = _uiState.value.movie ?: return
        if (_uiState.value.reminderToggling) return
        viewModelScope.launch {
            _uiState.update { it.copy(reminderToggling = true) }
            val newState = repository.toggleReminder(movie)
            if (newState) {
                reminderScheduler.scheduleReminders(movie)
            } else {
                reminderScheduler.cancelReminders(movie.id)
            }
            _uiState.update { state ->
                state.copy(
                    movie = state.movie?.copy(hasReminder = newState),
                    reminderToggling = false
                )
            }
        }
    }

    fun removeFromApp() {
        val movie = _uiState.value.movie ?: return
        viewModelScope.launch {
            if (movie.hasReminder) {
                reminderScheduler.cancelReminders(movie.id)
            }
            repository.resetMovie(movie.id)
            
            _uiState.update { state ->
                state.copy(
                    movie = state.movie?.copy(
                        hasReminder = false,
                        isWatchedInTheater = false,
                        ticketDate = "", ticketTime = "", ticketSeat = "", ticketCinema = "", ticketScreenNo = "", ticketFormat = "", ticketLanguage = ""
                    )
                )
            }
        }
    }

    fun openAddTicketDialog() { _uiState.update { it.copy(showAddTicketDialog = true) } }
    fun dismissAddTicketDialog() { _uiState.update { it.copy(showAddTicketDialog = false) } }

    fun saveQuickTicket(
        cinema: String, date: String, time: String, seat: String,
        screenNo: String, format: String, language: String, qrImagePath: String?
    ) {
        val movie = _uiState.value.movie ?: return
        viewModelScope.launch {
            repository.updateTicketInfo(movie.id, date, time, seat, cinema, screenNo, format, language)
            if (qrImagePath != null) repository.updateTicketQrImagePath(movie.id, qrImagePath)
            val dateStr = if (date.isNotBlank()) date else movie.releaseDate
            var shouldArchive = false
            if (dateStr.isNotBlank() && time.isNotBlank()) {
                try {
                    val ticketDate = java.time.LocalDate.parse(dateStr)
                    val ticketTime = parseTimeSafely(time)
                    if (ticketTime != null) {
                        val endDateTime = java.time.LocalDateTime.of(ticketDate, ticketTime)
                            .plusMinutes(movie.runtime?.toLong() ?: 150L)
                        if (java.time.LocalDateTime.now().isAfter(endDateTime)) {
                            shouldArchive = true
                            if (!movie.isWatchedInTheater) {
                                repository.toggleWatchedInTheater(movie)
                            }
                        }
                    }
                } catch (e: Exception) {}
            }

            _uiState.update { state ->
                state.copy(
                    movie = state.movie?.copy(
                        ticketCinema = cinema, ticketDate = date, ticketTime = time,
                        ticketSeat = seat, ticketScreenNo = screenNo, ticketFormat = format,
                        ticketLanguage = language,
                        ticketQrImagePath = qrImagePath ?: state.movie.ticketQrImagePath,
                        isWatchedInTheater = if (shouldArchive) true else state.movie.isWatchedInTheater
                    ),
                    showAddTicketDialog = false
                )
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

    fun deleteTicket() {
        val movie = _uiState.value.movie ?: return
        viewModelScope.launch {
            repository.updateTicketInfo(movie.id, "", "", "", "", "", "", "")
            repository.updateTicketQrImagePath(movie.id, null)
            _uiState.update { state ->
                state.copy(
                    movie = state.movie?.copy(
                        ticketCinema = "", ticketDate = "", ticketTime = "",
                        ticketSeat = "", ticketScreenNo = "", ticketFormat = "",
                        ticketLanguage = "", ticketQrImagePath = null
                    )
                )
            }
        }
    }
}
