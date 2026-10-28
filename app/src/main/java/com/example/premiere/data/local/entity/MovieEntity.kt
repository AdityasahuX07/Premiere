package com.example.premiere.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.premiere.domain.model.Movie
import com.example.premiere.domain.model.CastMember
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

private val moshi = Moshi.Builder()
    .addLast(KotlinJsonAdapterFactory())
    .build()
private val castListType = Types.newParameterizedType(List::class.java, CastMember::class.java)
private val castAdapter = moshi.adapter<List<CastMember>>(castListType)

@Entity(tableName = "movies")
data class MovieEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val originalTitle: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val logoUrl: String? = null,
    val originalLanguage: String = "",
    val releaseDate: String,
    val voteAverage: Double,
    val voteCount: Int,
    val genreIds: String,       // Comma-separated IDs  e.g. "28,12"
    val genreNames: String = "", // Comma-separated names e.g. "Action,Adventure"
    val runtime: Int? = null,
    val trailerKey: String? = null,
    val imdbRating: String? = null,
    val castJson: String = "",
    val hasReminder: Boolean = false,
    val isWatchedInTheater: Boolean = false,
    val ticketDate: String = "",
    val ticketTime: String = "",
    val ticketSeat: String = "",
    val ticketCinema: String = "",
    val ticketScreenNo: String = "",
    val ticketFormat: String = "",
    val ticketLanguage: String = "",
    val certification: String? = null,
    val ticketQrImagePath: String? = null
) {
    fun toDomainModel(): Movie = Movie(
        id = id,
        title = title,
        originalTitle = originalTitle,
        overview = overview,
        posterPath = posterPath,
        backdropPath = backdropPath,
        logoUrl = logoUrl,
        releaseDate = releaseDate,
        voteAverage = voteAverage,
        voteCount = voteCount,
        originalLanguage = originalLanguage,
        genreIds = if (genreIds.isBlank()) emptyList() else genreIds.split(",").mapNotNull { it.toIntOrNull() },
        genres = if (genreNames.isBlank()) emptyList() else genreNames.split(",").filter { it.isNotBlank() },
        runtime = runtime,
        trailerKey = trailerKey,
        imdbRating = imdbRating,
        cast = if (castJson.isBlank()) emptyList() else castAdapter.fromJson(castJson) ?: emptyList(),
        hasReminder = hasReminder,
        isWatchedInTheater = isWatchedInTheater,
        ticketDate = ticketDate,
        ticketTime = ticketTime,
        ticketSeat = ticketSeat,
        ticketCinema = ticketCinema,
        ticketScreenNo = ticketScreenNo,
        ticketFormat = ticketFormat,
        ticketLanguage = ticketLanguage,
        certification = certification,
        ticketQrImagePath = ticketQrImagePath
    )
}

fun Movie.toEntity(): MovieEntity = MovieEntity(
    id = id,
    title = title,
    originalTitle = originalTitle,
    overview = overview,
    posterPath = posterPath,
    backdropPath = backdropPath,
    logoUrl = logoUrl,
    releaseDate = releaseDate,
    voteAverage = voteAverage,
    voteCount = voteCount,
    originalLanguage = originalLanguage,
    genreIds = genreIds.joinToString(","),
    genreNames = genres.joinToString(","),
    runtime = runtime,
    trailerKey = trailerKey,
    imdbRating = imdbRating,
    castJson = castAdapter.toJson(cast),
    hasReminder = hasReminder,
    isWatchedInTheater = isWatchedInTheater,
    ticketDate = ticketDate,
    ticketTime = ticketTime,
    ticketSeat = ticketSeat,
    ticketCinema = ticketCinema,
    ticketScreenNo = ticketScreenNo,
    ticketFormat = ticketFormat,
    ticketLanguage = ticketLanguage,
    certification = certification,
    ticketQrImagePath = ticketQrImagePath
)
