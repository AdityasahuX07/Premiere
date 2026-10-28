package com.example.premiere.data.repository

import com.example.premiere.data.local.dao.MovieDao
import com.example.premiere.data.local.entity.toEntity
import com.example.premiere.data.preferences.PremierePreferences
import com.example.premiere.data.remote.OmdbApiService
import com.example.premiere.data.remote.TmdbApiService
import com.example.premiere.data.backup.AppBackup
import com.example.premiere.data.backup.BackupManager
import com.example.premiere.data.backup.BackupManager.toBackup
import com.example.premiere.data.backup.BackupManager.toDomain
import com.example.premiere.data.backup.BackupMovieEntry
import com.example.premiere.data.backup.BackupSettings
import com.example.premiere.data.backup.ImportResult
import com.example.premiere.domain.model.Movie
import com.example.premiere.domain.model.Result
import com.example.premiere.data.preferences.TicketSize
import com.example.premiere.data.preferences.TicketOrientation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import coil3.ImageLoader
import coil3.request.ImageRequest
import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MovieRepository @Inject constructor(
    private val dao: MovieDao,
    private val api: TmdbApiService,
    private val omdbApi: OmdbApiService,
    private val prefs: PremierePreferences,
    private val imageLoader: ImageLoader,
    @ApplicationContext private val context: Context,
    private val appScope: CoroutineScope
) {
    private suspend fun getApiKey(): String? {
        val key = prefs.tmdbApiKey.firstOrNull()
        return if (key.isNullOrBlank()) null else key
    }

    private val movieCache = ConcurrentHashMap<Int, Movie>()

    suspend fun getCachedMovie(movieId: Int): Movie? {
        return movieCache[movieId] ?: dao.getMovieById(movieId)?.toDomainModel()
    }

    private suspend fun fetchIndianReleaseData(movieId: Int, apiKey: String, defaultDate: String): Pair<String, String?> {
        return try {
            val datesResponse = api.getMovieReleaseDates(movieId, apiKey)
            val inResult = datesResponse.results.firstOrNull { it.iso31661 == "IN" }
            var cert: String? = null
            var releaseDate: String? = null

            if (inResult != null && inResult.releaseDates.isNotEmpty()) {
                val preferred = inResult.releaseDates.firstOrNull { it.type == 3 }
                    ?: inResult.releaseDates.firstOrNull { it.type == 2 }
                    ?: inResult.releaseDates.first()
                releaseDate = preferred.releaseDate.substringBefore("T")
                
                cert = preferred.certification?.takeIf { it.isNotBlank() }
                    ?: inResult.releaseDates.firstOrNull { !it.certification.isNullOrBlank() }?.certification
            }

            if (cert.isNullOrBlank()) {
                val usResult = datesResponse.results.firstOrNull { it.iso31661 == "US" }
                cert = usResult?.releaseDates?.firstOrNull { !it.certification.isNullOrBlank() }?.certification
            }

            Pair(releaseDate ?: defaultDate, cert ?: "")
        } catch (e: Exception) {
            Pair(defaultDate, null)
        }
    }

    private suspend fun enrichWithIndianReleaseDates(
        dtos: List<com.example.premiere.data.remote.dto.TmdbMovieDto>,
        apiKey: String,
        genreMap: Map<Int, String>
    ): List<Movie> = coroutineScope {
        dtos.map { dto ->
            async {
                val defaultDate = dto.releaseDate ?: ""
                val (inDate, certification) = fetchIndianReleaseData(dto.id, apiKey, defaultDate)
                dto.toDomainModel(genreMap).copy(releaseDate = inDate, certification = certification)
            }
        }.awaitAll()
    }

    val moviesWithReminders: Flow<List<Movie>> =
        dao.getMoviesWithReminders().map { list -> list.map { it.toDomainModel() } }

    val activeTickets: Flow<List<Movie>> =
        dao.getActiveTickets().map { list -> list.map { it.toDomainModel() } }

    val watchedHistory: Flow<List<Movie>> =
        dao.getWatchedHistory().map { list -> list.map { it.toDomainModel() } }

    fun observeMovie(movieId: Int): Flow<Movie?> =
        dao.observeMovie(movieId).map { it?.toDomainModel() }

    suspend fun toggleReminder(movie: Movie): Boolean {
        val newState = !movie.hasReminder
        val cachedMovie = movieCache[movie.id]
        val updatedMovie = if (newState && cachedMovie != null) {
            movie.copy(
                hasReminder = true,
                logoUrl = cachedMovie.logoUrl ?: movie.logoUrl,
                certification = if (cachedMovie.certification.isNullOrEmpty()) movie.certification else cachedMovie.certification,
                releaseDate = if (cachedMovie.releaseDate.isEmpty()) movie.releaseDate else cachedMovie.releaseDate
            )
        } else {
            movie.copy(hasReminder = newState)
        }
        dao.insertMovie(updatedMovie.toEntity())

        if (newState) {
            appScope.launch {
                getMovieDetail(movie.id)
                updatedMovie.posterUrl?.let { url ->
                    imageLoader.enqueue(ImageRequest.Builder(context).data(url).build())
                }
            }
        }
        return newState
    }

    suspend fun toggleWatchedInTheater(movie: Movie): Boolean {
        val newState = !movie.isWatchedInTheater
        // Use whatever is already resolved right now — do NOT block the insert on a network call,
        // so the ticket appears in the list instantly, same as before.
        val cachedMovie = movieCache[movie.id]
        val updatedMovie = if (newState && cachedMovie != null) {
            movie.copy(
                isWatchedInTheater = true,
                logoUrl = cachedMovie.logoUrl ?: movie.logoUrl,
                certification = if (cachedMovie.certification.isNullOrEmpty()) movie.certification else cachedMovie.certification,
                releaseDate = if (cachedMovie.releaseDate.isEmpty()) movie.releaseDate else cachedMovie.releaseDate
            )
        } else {
            movie.copy(isWatchedInTheater = newState)
        }
        if (newState) {
            // Fire the network catch-up FIRST (fire-and-forget on appScope) so it starts running
            // truly in parallel with the DB insert right below it, instead of only starting once
            // the insert line has finished — the logo/rating fetch and the backdrop's own image
            // load (triggered once the ticket recomposes) now begin at essentially the same moment.
            appScope.launch {
                // If logo/certification weren't ready yet (search prefetch hadn't finished),
                // fetch just those two lightweight bits + warm the logo bitmap, and save as soon
                // as ready — this is much faster than waiting for the full getMovieDetail() below
                // (which also pulls cast/trailer/OMDB), so the logo/badge catch up quickly instead
                // of waiting on the slow full detail fetch.
                if (updatedMovie.logoUrl.isNullOrEmpty() || updatedMovie.certification.isNullOrEmpty()) {
                    prefetchLogoAndRating(movie.id)
                    movieCache[movie.id]?.let { refreshed ->
                        dao.insertMovie(
                            updatedMovie.copy(
                                logoUrl = refreshed.logoUrl ?: updatedMovie.logoUrl,
                                certification = if (refreshed.certification.isNullOrEmpty()) updatedMovie.certification else refreshed.certification,
                                releaseDate = if (refreshed.releaseDate.isNotEmpty()) refreshed.releaseDate else updatedMovie.releaseDate
                            ).toEntity()
                        )
                    }
                }

                getMovieDetail(movie.id)
                updatedMovie.posterUrl?.let { url ->
                    imageLoader.enqueue(ImageRequest.Builder(context).data(url).build())
                }
            }
        }

        dao.insertMovie(updatedMovie.toEntity())
        return newState
    }

    suspend fun updateTicketInfo(
        movieId: Int, date: String, time: String, seat: String,
        cinema: String, screenNo: String, format: String, language: String
    ) {
        dao.updateTicketInfo(movieId, date, time, seat, cinema, screenNo, format, language)

        // Also update memory cache if present
        movieCache[movieId]?.let { movie ->
            movieCache[movieId] = movie.copy(
                ticketDate = date,
                ticketTime = time,
                ticketSeat = seat,
                ticketCinema = cinema,
                ticketScreenNo = screenNo,
                ticketFormat = format,
                ticketLanguage = language
            )
        }
    }

    suspend fun resetMovie(movieId: Int) {
        dao.resetMovie(movieId)
        movieCache.remove(movieId)
    }

    suspend fun updateTicketQrImagePath(movieId: Int, qrImagePath: String?) {
        dao.updateTicketQrImagePath(movieId, qrImagePath)
        movieCache[movieId]?.let { movie ->
            movieCache[movieId] = movie.copy(ticketQrImagePath = qrImagePath)
        }
    }

    suspend fun getUpcomingMovies(): Result<List<Movie>> {
        val apiKey = getApiKey() ?: return Result.Error("API Key missing. Please set it in Settings.")
        return try {
            val response = api.getUpcomingMovies(apiKey)
            val genresResponse = api.getGenres(apiKey)
            val genreMap = genresResponse.genres.associate { it.id to it.name }
            val movies = enrichWithIndianReleaseDates(response.results, apiKey, genreMap)
            movies.forEach { movieCache[it.id] = it }
            Result.Success(movies)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to fetch upcoming movies")
        }
    }

    suspend fun searchUpcomingMovies(query: String): Result<List<Movie>> {
        val apiKey = getApiKey() ?: return Result.Error("API Key missing")
        return try {
            val response = if (query.isBlank()) {
                api.getUpcomingMovies(apiKey)
            } else {
                api.searchMovies(apiKey = apiKey, query = query)
            }
            val genresResponse = api.getGenres(apiKey)
            val genreMap = genresResponse.genres.associate { it.id to it.name }
            // Skip the expensive N+1 network calls for Indian release dates during search autocomplete
            val movies = response.results.map { dto ->
                dto.toDomainModel(genreMap).copy(releaseDate = dto.releaseDate ?: "")
            }
            // This was the actual bug behind the logo/certification delay: without registering
            // these movies in movieCache, prefetchLogoAndRating() had nowhere to store what it
            // fetched (it bails out with `if (memMovie != null)`), so every prefetch during
            // search was silently discarded and the app always fell back to the slow full
            // getMovieDetail() fetch after adding. putIfAbsent so we never clobber a richer
            // cache entry (e.g. one that already has a resolved logo) with a bare search result.
            movies.forEach { movieCache.putIfAbsent(it.id, it) }
            Result.Success(movies)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Search failed")
        }
    }

    suspend fun getMovieDetail(movieId: Int): Result<Movie> {
        val apiKey = getApiKey() ?: return Result.Error("API Key missing")
        return try {
            val memMovie = movieCache[movieId]
            if (memMovie != null && memMovie.cast.isNotEmpty() && memMovie.logoUrl != null) {
                return Result.Success(memMovie)
            }

            val localMovie = dao.getMovieById(movieId)?.toDomainModel()

            // If we already have the full details cached locally, return them instantly
            if (localMovie != null && localMovie.hasReminder && localMovie.cast.isNotEmpty() && localMovie.logoUrl != null) {
                return Result.Success(localMovie)
            }

            val detailResponse = api.getMovieDetails(movieId, apiKey)
            val credits = api.getMovieCredits(movieId, apiKey)
            val videos = api.getMovieVideos(movieId, apiKey)
            val images = try { api.getMovieImages(movieId, apiKey) } catch (e: Exception) { null }

            val trailer = videos.results.firstOrNull { it.type == "Trailer" && it.site == "YouTube" }?.key
            val cast = credits.cast.take(10).map { it.toDomainModel() }
            val logoPath = images?.logos?.firstOrNull { it.iso6391 == "en" }?.filePath
                ?: images?.logos?.firstOrNull { it.iso6391 == "xx" }?.filePath
                ?: images?.logos?.firstOrNull()?.filePath ?: ""

            val defaultDate = detailResponse.releaseDate ?: ""
            val (inDate, certification) = fetchIndianReleaseData(movieId, apiKey, defaultDate)

            var imdbRating: String? = null
            val omdbApiKey = prefs.omdbApiKey.firstOrNull()
            if (!omdbApiKey.isNullOrBlank() && !detailResponse.imdbId.isNullOrBlank()) {
                try {
                    val omdbResponse = omdbApi.getMovieByImdbId(detailResponse.imdbId, omdbApiKey)
                    imdbRating = omdbResponse.imdbRating
                } catch (e: Exception) {
                    // Ignore OMDB failure, fallback to TMDB rating
                }
            }

            val updatedMovie = detailResponse.toDomainModel().copy(
                releaseDate = inDate,
                trailerKey = trailer,
                logoUrl = logoPath,
                cast = cast,
                hasReminder = localMovie?.hasReminder ?: false,
                isWatchedInTheater = localMovie?.isWatchedInTheater ?: false,
                imdbRating = imdbRating,
                certification = certification
            )

            movieCache[movieId] = updatedMovie

            if ((localMovie != null && localMovie.hasReminder) || (localMovie != null && localMovie.isWatchedInTheater)) {
                dao.insertMovie(updatedMovie.toEntity())
            }

            Result.Success(updatedMovie)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to fetch details")
        }
    }

    suspend fun prefetchLogoAndRating(movieId: Int) {
        val apiKey = getApiKey() ?: return
        try {
            // Only prefetch if we don't already have it
            val memMovie = movieCache[movieId]
            if (memMovie != null && memMovie.logoUrl != null && memMovie.certification != null) return

            coroutineScope {
                // Run both TMDB lookups in parallel instead of one after another — this is the
                // main lever we have, since the logo/certification each require their own API
                // call before we even know the logo's URL (unlike the backdrop, whose URL comes
                // for free in the original search results).
                val imagesDeferred = async { try { api.getMovieImages(movieId, apiKey) } catch (e: Exception) { null } }
                val defaultDate = memMovie?.releaseDate ?: ""
                val releaseDeferred = async { fetchIndianReleaseData(movieId, apiKey, defaultDate) }

                val images = imagesDeferred.await()
                val logoPath = if (images == null) {
                    null // Network error, keep as null to retry later
                } else {
                    images.logos?.firstOrNull { it.iso6391 == "en" }?.filePath
                        ?: images.logos?.firstOrNull { it.iso6391 == "xx" }?.filePath
                        ?: images.logos?.firstOrNull()?.filePath ?: ""
                }

                // Warm Coil's cache with the actual logo bitmap concurrently with the release-dates
                // call above, rather than waiting for it to finish first.
                val warmJob = if (!logoPath.isNullOrBlank()) {
                    val logoUrl = "https://image.tmdb.org/t/p/w500$logoPath"
                    launch {
                        try {
                            imageLoader.execute(ImageRequest.Builder(context).data(logoUrl).build())
                        } catch (e: Exception) {
                            // Ignore image preload failures, the AsyncImage will just fetch it later
                        }
                    }
                } else null

                val (inDate, certification) = releaseDeferred.await()
                warmJob?.join()

                // Update the cache with this partial info, so it's ready when added!
                if (memMovie != null) {
                    val updated = memMovie.copy(
                        logoUrl = logoPath ?: memMovie.logoUrl,
                        certification = certification ?: memMovie.certification,
                        releaseDate = inDate
                    )
                    movieCache[movieId] = updated

                    // ← CRITICAL: also persist logoUrl to DB so the History screen
                    // never shows an infinite skeleton after the first successful fetch.
                    // Only do it for movies that are actually stored (history or reminder).
                    if (memMovie.isWatchedInTheater || memMovie.hasReminder) {
                        if (logoPath != null || certification != null) {
                            dao.updateLogoAndCertification(movieId, updated.logoUrl, updated.certification)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore prefetch failures
        }
    }

    // ─── Backup / Export ─────────────────────────────────────────────────────

    suspend fun exportBackup(uri: Uri, context: Context): Boolean {
        // Export all movies: theatrical history + home screen reminders
        val allMovies = dao.getAllExportableMoviesOnce().map { it.toDomainModel() }
        val cinemaList = prefs.cinemas.firstOrNull() ?: emptyList()
        val settings = BackupSettings(
            ticketSize = prefs.ticketSize.firstOrNull()?.name ?: "MEDIUM",
            ticketOrientation = prefs.ticketOrientation.firstOrNull()?.name ?: "HORIZONTAL",
            ticketColor = prefs.ticketColor.firstOrNull() ?: "POSTER",
            defaultCinema = prefs.defaultCinema.firstOrNull() ?: "",
            defaultFormat = prefs.defaultFormat.firstOrNull() ?: "",
            tmdbApiKey = prefs.tmdbApiKey.firstOrNull() ?: "",
            omdbApiKey = prefs.omdbApiKey.firstOrNull() ?: ""
        )
        val backup = AppBackup(
            cinemas = cinemaList.map { with(BackupManager) { it.toBackup() } },
            settings = settings,
            theatricalHistory = allMovies.map { m ->
                BackupMovieEntry(
                    id = m.id,
                    title = m.title,
                    originalTitle = m.originalTitle,
                    overview = m.overview,
                    posterPath = m.posterPath,
                    backdropPath = m.backdropPath,
                    logoUrl = m.logoUrl,
                    releaseDate = m.releaseDate,
                    genres = m.genres.joinToString(","),
                    runtime = m.runtime,
                    certification = m.certification,
                    imdbRating = m.imdbRating,
                    originalLanguage = m.originalLanguage,
                    hasReminder = m.hasReminder,
                    isWatchedInTheater = m.isWatchedInTheater,
                    ticketDate = m.ticketDate,
                    ticketTime = m.ticketTime,
                    ticketSeat = m.ticketSeat,
                    ticketCinema = m.ticketCinema,
                    ticketScreenNo = m.ticketScreenNo,
                    ticketFormat = m.ticketFormat,
                    ticketLanguage = m.ticketLanguage
                )
            }
        )
        return BackupManager.exportToUri(uri, backup, context)
    }

    suspend fun importBackup(uri: Uri, context: Context): ImportResult {
        val result = BackupManager.importFromUri(uri, context)
        if (result.isFailure) {
            return ImportResult.Error(result.exceptionOrNull()?.message ?: "Unknown error")
        }
        val backup = result.getOrThrow()

        // Restore cinemas (merge: don't add duplicates by name)
        val existingCinemas = prefs.cinemas.firstOrNull() ?: emptyList()
        val existingNames = existingCinemas.map { it.name }.toSet()
        val newCinemas = backup.cinemas
            .map { with(BackupManager) { it.toDomain() } }
            .filter { it.name !in existingNames }
        prefs.setCinemas(existingCinemas + newCinemas)

        // Restore ticket defaults
        if (backup.settings.defaultCinema.isNotBlank()) prefs.setDefaultCinema(backup.settings.defaultCinema)
        if (backup.settings.defaultFormat.isNotBlank()) prefs.setDefaultFormat(backup.settings.defaultFormat)

        // Restore ticket customization (size, orientation, color)
        runCatching { TicketSize.valueOf(backup.settings.ticketSize) }.getOrNull()
            ?.let { prefs.setTicketSize(it) }
        runCatching { TicketOrientation.valueOf(backup.settings.ticketOrientation) }.getOrNull()
            ?.let { prefs.setTicketOrientation(it) }
        if (backup.settings.ticketColor.isNotBlank()) prefs.setTicketColor(backup.settings.ticketColor)

        // Restore API keys (only if not already set on device)
        if (backup.settings.tmdbApiKey.isNotBlank() && prefs.tmdbApiKey.firstOrNull().isNullOrBlank()) {
            prefs.setTmdbApiKey(backup.settings.tmdbApiKey)
        }
        if (backup.settings.omdbApiKey.isNotBlank() && prefs.omdbApiKey.firstOrNull().isNullOrBlank()) {
            prefs.setOmdbApiKey(backup.settings.omdbApiKey)
        }

        // Restore all movies (merge by id — covers both history and reminders)
        val existingIds = dao.getAllExportableMoviesOnce().map { it.id }.toSet()
        var historyImported = 0
        var remindersImported = 0
        backup.theatricalHistory.forEach { entry ->
            if (entry.id !in existingIds) {
                val entity = com.example.premiere.data.local.entity.MovieEntity(
                    id = entry.id,
                    title = entry.title,
                    originalTitle = entry.originalTitle,
                    overview = entry.overview,
                    posterPath = entry.posterPath,
                    backdropPath = entry.backdropPath,
                    logoUrl = entry.logoUrl,
                    releaseDate = entry.releaseDate,
                    voteAverage = 0.0,
                    voteCount = 0,
                    genreIds = "",
                    genreNames = entry.genres,
                    runtime = entry.runtime,
                    originalLanguage = entry.originalLanguage,
                    certification = entry.certification,
                    imdbRating = entry.imdbRating,
                    hasReminder = entry.hasReminder,
                    isWatchedInTheater = entry.isWatchedInTheater,
                    ticketDate = entry.ticketDate,
                    ticketTime = entry.ticketTime,
                    ticketSeat = entry.ticketSeat,
                    ticketCinema = entry.ticketCinema,
                    ticketScreenNo = entry.ticketScreenNo,
                    ticketFormat = entry.ticketFormat,
                    ticketLanguage = entry.ticketLanguage
                )
                dao.insertMovie(entity)
                if (entry.isWatchedInTheater) {
                    historyImported++
                } else if (entry.hasReminder) {
                    remindersImported++
                }
            }
        }

        return ImportResult.Success(cinemasImported = newCinemas.size, historyImported = historyImported, remindersImported = remindersImported)
    }

    suspend fun forceRefreshMetadata(): kotlin.Result<Int> = coroutineScope {
        try {
            val apiKey = getApiKey() ?: return@coroutineScope kotlin.Result.failure(Exception("TMDB API Key not set"))
            val omdbApiKey = prefs.omdbApiKey.firstOrNull()
            
            val allMovies = dao.getAllMoviesOnce()
            var refreshedCount = 0
            
            allMovies.map { movieEntity ->
                async {
                    try {
                        val detailResponse = api.getMovieDetails(movieEntity.id, apiKey)
                        val credits = api.getMovieCredits(movieEntity.id, apiKey)
                        val videos = api.getMovieVideos(movieEntity.id, apiKey)
                        val images = try { api.getMovieImages(movieEntity.id, apiKey) } catch (e: Exception) { null }

                        val trailer = videos.results.firstOrNull { it.type == "Trailer" && it.site == "YouTube" }?.key
                        val cast = credits.cast.take(10).map { it.toDomainModel() }
                        val logoPath = images?.logos?.firstOrNull { it.iso6391 == "en" }?.filePath
                            ?: images?.logos?.firstOrNull { it.iso6391 == "xx" }?.filePath
                            ?: images?.logos?.firstOrNull()?.filePath ?: ""

                        val defaultDate = detailResponse.releaseDate ?: ""
                        val (inDate, certification) = fetchIndianReleaseData(movieEntity.id, apiKey, defaultDate)

                        var imdbRating = movieEntity.imdbRating
                        if (!omdbApiKey.isNullOrBlank() && !detailResponse.imdbId.isNullOrBlank()) {
                            try {
                                val omdbResponse = omdbApi.getMovieByImdbId(detailResponse.imdbId, omdbApiKey)
                                imdbRating = omdbResponse.imdbRating
                            } catch (e: Exception) {}
                        }

                        // update the entity but preserve local data
                        val updatedMovie = detailResponse.toDomainModel().copy(
                            releaseDate = inDate,
                            trailerKey = trailer,
                            logoUrl = logoPath,
                            cast = cast,
                            hasReminder = movieEntity.hasReminder,
                            isWatchedInTheater = movieEntity.isWatchedInTheater,
                            imdbRating = imdbRating,
                            certification = certification,
                            // Preserve ticket local fields
                            ticketDate = movieEntity.ticketDate,
                            ticketTime = movieEntity.ticketTime,
                            ticketSeat = movieEntity.ticketSeat,
                            ticketCinema = movieEntity.ticketCinema,
                            ticketScreenNo = movieEntity.ticketScreenNo,
                            ticketFormat = movieEntity.ticketFormat,
                            ticketLanguage = movieEntity.ticketLanguage,
                            ticketQrImagePath = movieEntity.ticketQrImagePath
                        )
                        dao.insertMovie(updatedMovie.toEntity())
                        movieCache[movieEntity.id] = updatedMovie
                        refreshedCount++
                    } catch (e: Exception) {
                        // ignore this movie and continue
                    }
                }
            }.awaitAll()
            
            kotlin.Result.success(refreshedCount)
        } catch (e: Exception) {
            kotlin.Result.failure(e)
        }
    }
}