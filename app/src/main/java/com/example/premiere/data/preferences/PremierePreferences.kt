package com.example.premiere.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.example.premiere.domain.model.Cinema
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

private val moshi = Moshi.Builder()
    .addLast(KotlinJsonAdapterFactory())
    .build()
private val cinemaListType = Types.newParameterizedType(List::class.java, Cinema::class.java)
private val cinemaAdapter = moshi.adapter<List<Cinema>>(cinemaListType)
private val stringListType = Types.newParameterizedType(List::class.java, String::class.java)
private val stringListAdapter = moshi.adapter<List<String>>(stringListType)

enum class TicketSize { SMALL, MEDIUM, LARGE }
enum class TicketOrientation { VERTICAL, HORIZONTAL }

class PremierePreferences(private val context: Context) {

    private val API_KEY = stringPreferencesKey("tmdb_api_key")
    private val OMDB_API_KEY = stringPreferencesKey("omdb_api_key")
    private val SHOW_RELEASED_TITLES = booleanPreferencesKey("show_released_titles")
    
    private val TICKET_SIZE = stringPreferencesKey("ticket_size")
    private val TICKET_ORIENTATION = stringPreferencesKey("ticket_orientation")
    private val TICKET_COLOR = stringPreferencesKey("ticket_color")
    private val CINEMAS = stringPreferencesKey("cinemas_list")
    private val DEFAULT_CINEMA = stringPreferencesKey("default_cinema")
    private val DEFAULT_FORMAT = stringPreferencesKey("default_format")
    fun getSearchHistory(source: String): Flow<List<String>> = context.dataStore.data.map {
        val key = stringPreferencesKey("search_history_$source")
        val json = it[key] ?: "[]"
        runCatching { stringListAdapter.fromJson(json) ?: emptyList() }.getOrDefault(emptyList())
    }

    val tmdbApiKey: Flow<String> = context.dataStore.data.map { it[API_KEY] ?: "" }
    val omdbApiKey: Flow<String> = context.dataStore.data.map { it[OMDB_API_KEY] ?: "" }
    val showReleasedTitles: Flow<Boolean> = context.dataStore.data.map { it[SHOW_RELEASED_TITLES] ?: false }
    
    val ticketSize: Flow<TicketSize> = context.dataStore.data.map { 
        val name = it[TICKET_SIZE] ?: TicketSize.MEDIUM.name
        runCatching { TicketSize.valueOf(name) }.getOrDefault(TicketSize.MEDIUM)
    }
    
    val ticketOrientation: Flow<TicketOrientation> = context.dataStore.data.map {
        val name = it[TICKET_ORIENTATION] ?: TicketOrientation.HORIZONTAL.name
        runCatching { TicketOrientation.valueOf(name) }.getOrDefault(TicketOrientation.HORIZONTAL)
    }
    
    val ticketColor: Flow<String> = context.dataStore.data.map {
        it[TICKET_COLOR] ?: "POSTER"
    }
    
    val cinemas: Flow<List<Cinema>> = context.dataStore.data.map {
        val json = it[CINEMAS] ?: "[]"
        runCatching { cinemaAdapter.fromJson(json) ?: emptyList() }.getOrDefault(emptyList())
    }

    val defaultCinema: Flow<String> = context.dataStore.data.map { it[DEFAULT_CINEMA] ?: "" }
    val defaultFormat: Flow<String> = context.dataStore.data.map { it[DEFAULT_FORMAT] ?: "" }

    suspend fun setTmdbApiKey(key: String) {
        context.dataStore.edit { it[API_KEY] = key }
    }

    suspend fun setOmdbApiKey(key: String) {
        context.dataStore.edit { it[OMDB_API_KEY] = key }
    }

    suspend fun setShowReleasedTitles(show: Boolean) {
        context.dataStore.edit { it[SHOW_RELEASED_TITLES] = show }
    }

    suspend fun setTicketSize(size: TicketSize) {
        context.dataStore.edit { it[TICKET_SIZE] = size.name }
    }

    suspend fun setTicketOrientation(orientation: TicketOrientation) {
        context.dataStore.edit { it[TICKET_ORIENTATION] = orientation.name }
    }

    suspend fun setTicketColor(color: String) {
        context.dataStore.edit { it[TICKET_COLOR] = color }
    }

    suspend fun setCinemas(cinemasList: List<Cinema>) {
        val json = cinemaAdapter.toJson(cinemasList)
        context.dataStore.edit { it[CINEMAS] = json }
    }

    suspend fun setDefaultCinema(cinema: String) {
        context.dataStore.edit { it[DEFAULT_CINEMA] = cinema }
    }

    suspend fun setDefaultFormat(format: String) {
        context.dataStore.edit { it[DEFAULT_FORMAT] = format }
    }

    suspend fun addSearchHistory(query: String, source: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        val key = stringPreferencesKey("search_history_$source")
        context.dataStore.edit { prefs ->
            val json = prefs[key] ?: "[]"
            val currentList = runCatching { stringListAdapter.fromJson(json) ?: emptyList() }.getOrDefault(emptyList())
            val newList = (listOf(trimmed) + currentList.filter { !it.equals(trimmed, ignoreCase = true) }).take(15)
            prefs[key] = stringListAdapter.toJson(newList)
        }
    }

    suspend fun removeSearchHistory(query: String, source: String) {
        val trimmed = query.trim()
        val key = stringPreferencesKey("search_history_$source")
        context.dataStore.edit { prefs ->
            val json = prefs[key] ?: "[]"
            val currentList = runCatching { stringListAdapter.fromJson(json) ?: emptyList() }.getOrDefault(emptyList())
            val newList = currentList.filter { !it.equals(trimmed, ignoreCase = true) }
            prefs[key] = stringListAdapter.toJson(newList)
        }
    }
}
