package com.example.premiere.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.premiere.data.backup.ImportResult
import com.example.premiere.data.preferences.PremierePreferences
import com.example.premiere.data.preferences.TicketOrientation
import com.example.premiere.data.preferences.TicketSize
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.premiere.data.repository.MovieRepository
import com.example.premiere.domain.model.Movie
import com.example.premiere.domain.model.Cinema
import java.util.UUID

data class SettingsUiState(
    val apiKey: String = "",
    val apiKeyInput: String = "",
    val isSaved: Boolean = false,
    val omdbApiKey: String = "",
    val omdbApiKeyInput: String = "",
    val isOmdbSaved: Boolean = false,
    val showReleasedTitles: Boolean = false,
    val ticketSize: TicketSize = TicketSize.MEDIUM,
    val ticketOrientation: TicketOrientation = TicketOrientation.HORIZONTAL,
    val ticketColor: String = "POSTER",
    val previewMovie: Movie? = null,
    val watchedHistory: List<Movie> = emptyList(),
    val activeTickets: List<Movie> = emptyList(),
    val cinemas: List<Cinema> = emptyList(),
    // Ticket defaults
    val defaultCinema: String = "",
    val defaultCinemaInput: String = "",
    val defaultFormat: String = "",
    val defaultFormatInput: String = "",
    val isDefaultsSaved: Boolean = false,
    // Backup state
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val backupMessage: String? = null,
    // Force refresh state
    val isForceRefreshing: Boolean = false,
    val forceRefreshMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: PremierePreferences,
    private val movieRepository: MovieRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.tmdbApiKey.collect { key ->
                _uiState.update {
                    it.copy(
                        apiKey = key,
                        apiKeyInput = if (it.apiKeyInput.isBlank()) key else it.apiKeyInput
                    )
                }
            }
        }
        viewModelScope.launch {
            prefs.omdbApiKey.collect { key ->
                _uiState.update {
                    it.copy(
                        omdbApiKey = key,
                        omdbApiKeyInput = if (it.omdbApiKeyInput.isBlank()) key else it.omdbApiKeyInput
                    )
                }
            }
        }
        viewModelScope.launch {
            prefs.showReleasedTitles.collect { show ->
                _uiState.update { it.copy(showReleasedTitles = show) }
            }
        }
        viewModelScope.launch {
            prefs.ticketSize.collect { size ->
                _uiState.update { it.copy(ticketSize = size) }
            }
        }
        viewModelScope.launch {
            prefs.ticketOrientation.collect { orientation ->
                _uiState.update { it.copy(ticketOrientation = orientation) }
            }
        }
        viewModelScope.launch {
            prefs.ticketColor.collect { color ->
                _uiState.update { it.copy(ticketColor = color) }
            }
        }
        viewModelScope.launch {
            movieRepository.watchedHistory.collect { history ->
                _uiState.update { it.copy(previewMovie = history.firstOrNull(), watchedHistory = history) }
            }
        }
        viewModelScope.launch {
            movieRepository.activeTickets.collect { tickets ->
                _uiState.update { it.copy(activeTickets = tickets) }
            }
        }
        viewModelScope.launch {
            prefs.cinemas.collect { cinemasList ->
                _uiState.update { it.copy(cinemas = cinemasList) }
            }
        }
        viewModelScope.launch {
            prefs.defaultCinema.collect { cinema ->
                _uiState.update {
                    it.copy(
                        defaultCinema = cinema,
                        defaultCinemaInput = if (it.defaultCinemaInput.isBlank()) cinema else it.defaultCinemaInput
                    )
                }
            }
        }
        viewModelScope.launch {
            prefs.defaultFormat.collect { format ->
                _uiState.update {
                    it.copy(
                        defaultFormat = format,
                        defaultFormatInput = if (it.defaultFormatInput.isBlank()) format else it.defaultFormatInput
                    )
                }
            }
        }
    }

    fun onApiKeyInputChanged(value: String) {
        _uiState.update { it.copy(apiKeyInput = value, isSaved = false) }
    }

    fun saveApiKey() {
        viewModelScope.launch {
            prefs.setTmdbApiKey(_uiState.value.apiKeyInput.trim())
            _uiState.update { it.copy(apiKey = it.apiKeyInput.trim(), isSaved = true) }
        }
    }

    fun onOmdbApiKeyInputChanged(value: String) {
        _uiState.update { it.copy(omdbApiKeyInput = value, isOmdbSaved = false) }
    }

    fun saveOmdbApiKey() {
        viewModelScope.launch {
            prefs.setOmdbApiKey(_uiState.value.omdbApiKeyInput.trim())
            _uiState.update { it.copy(omdbApiKey = it.omdbApiKeyInput.trim(), isOmdbSaved = true) }
        }
    }

    fun toggleShowReleasedTitles(show: Boolean) {
        viewModelScope.launch {
            prefs.setShowReleasedTitles(show)
        }
    }

    fun setTicketSize(size: TicketSize) {
        viewModelScope.launch {
            prefs.setTicketSize(size)
        }
    }

    fun setTicketOrientation(orientation: TicketOrientation) {
        viewModelScope.launch {
            prefs.setTicketOrientation(orientation)
        }
    }

    fun setTicketColor(color: String) {
        viewModelScope.launch {
            prefs.setTicketColor(color)
        }
    }

    fun addCinema(name: String, logoUrl: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val newCinema = Cinema(id = UUID.randomUUID().toString(), name = name, logoUrl = logoUrl)
            val currentList = _uiState.value.cinemas
            prefs.setCinemas(currentList + newCinema)
        }
    }

    fun deleteCinema(cinemaId: String) {
        viewModelScope.launch {
            val currentList = _uiState.value.cinemas
            prefs.setCinemas(currentList.filter { it.id != cinemaId })
        }
    }

    // ─── Ticket Defaults ────────────────────────────────────────────────────

    fun onDefaultCinemaInputChanged(value: String) {
        _uiState.update { it.copy(defaultCinemaInput = value, isDefaultsSaved = false) }
    }

    fun onDefaultFormatInputChanged(value: String) {
        _uiState.update { it.copy(defaultFormatInput = value, isDefaultsSaved = false) }
    }

    fun saveTicketDefaults() {
        viewModelScope.launch {
            prefs.setDefaultCinema(_uiState.value.defaultCinemaInput.trim())
            prefs.setDefaultFormat(_uiState.value.defaultFormatInput.trim())
            _uiState.update { it.copy(isDefaultsSaved = true) }
        }
    }

    // ─── Backup / Export ────────────────────────────────────────────────────

    fun exportBackup(uri: Uri, context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, backupMessage = null) }
            val success = movieRepository.exportBackup(uri, context)
            _uiState.update {
                it.copy(
                    isExporting = false,
                    backupMessage = if (success) "✓ Backup exported successfully" else "✗ Export failed"
                )
            }
        }
    }

    fun importBackup(uri: Uri, context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, backupMessage = null) }
            when (val result = movieRepository.importBackup(uri, context)) {
                is ImportResult.Success -> _uiState.update {
                    it.copy(
                        isImporting = false,
                        backupMessage = "✓ Imported ${result.historyImported} History, ${result.remindersImported} reminder and ${result.cinemasImported} cinemas"
                    )
                }
                is ImportResult.Error -> _uiState.update {
                    it.copy(isImporting = false, backupMessage = "✗ ${result.message}")
                }
            }
        }
    }

    fun clearBackupMessage() {
        _uiState.update { it.copy(backupMessage = null) }
    }

    // --- Force Refresh ---

    fun forceRefreshMetadata() {
        viewModelScope.launch {
            _uiState.update { it.copy(isForceRefreshing = true, forceRefreshMessage = null) }
            val result = movieRepository.forceRefreshMetadata()
            _uiState.update { 
                it.copy(
                    isForceRefreshing = false,
                    forceRefreshMessage = result.fold(
                        onSuccess = { count -> "Refreshed metadata for $count movies" },
                        onFailure = { error -> "Failed to refresh metadata: ${error.message}" }
                    )
                )
            }
        }
    }

    fun clearForceRefreshMessage() {
        _uiState.update { it.copy(forceRefreshMessage = null) }
    }
}
