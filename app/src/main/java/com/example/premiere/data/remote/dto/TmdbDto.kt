package com.example.premiere.data.remote.dto

import com.example.premiere.domain.model.CastMember
import com.example.premiere.domain.model.Movie
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TmdbMovieListResponse(
    @Json(name = "results") val results: List<TmdbMovieDto>
)

@JsonClass(generateAdapter = true)
data class TmdbMovieDto(
    @Json(name = "id") val id: Int,
    @Json(name = "title") val title: String,
    @Json(name = "original_title") val originalTitle: String?,
    @Json(name = "overview") val overview: String,
    @Json(name = "poster_path") val posterPath: String?,
    @Json(name = "backdrop_path") val backdropPath: String?,
    @Json(name = "release_date") val releaseDate: String?,
    @Json(name = "vote_average") val voteAverage: Double,
    @Json(name = "vote_count") val voteCount: Int,
    @Json(name = "popularity") val popularity: Double?,
    @Json(name = "genre_ids") val genreIds: List<Int>?,
    @Json(name = "original_language") val originalLanguage: String?
) {
    fun toDomainModel(genresMap: Map<Int, String> = emptyMap()): Movie = Movie(
        id = id,
        title = title,
        originalTitle = originalTitle ?: title,
        overview = overview,
        posterPath = posterPath,
        backdropPath = backdropPath,
        releaseDate = releaseDate ?: "",
        voteAverage = voteAverage,
        voteCount = voteCount,
        popularity = popularity ?: 0.0,
        genreIds = genreIds ?: emptyList(),
        genres = genreIds?.mapNotNull { genresMap[it] } ?: emptyList(),
        originalLanguage = originalLanguage ?: ""
    )
}

@JsonClass(generateAdapter = true)
data class TmdbMovieDetailDto(
    @Json(name = "id") val id: Int,
    @Json(name = "title") val title: String,
    @Json(name = "original_title") val originalTitle: String?,
    @Json(name = "overview") val overview: String,
    @Json(name = "poster_path") val posterPath: String?,
    @Json(name = "backdrop_path") val backdropPath: String?,
    @Json(name = "release_date") val releaseDate: String?,
    @Json(name = "vote_average") val voteAverage: Double,
    @Json(name = "vote_count") val voteCount: Int,
    @Json(name = "runtime") val runtime: Int?,
    @Json(name = "imdb_id") val imdbId: String?,
    @Json(name = "genres") val genres: List<TmdbGenreDto>?,
    @Json(name = "original_language") val originalLanguage: String?
) {
    fun toDomainModel(): Movie = Movie(
        id = id,
        title = title,
        originalTitle = originalTitle ?: title,
        overview = overview,
        posterPath = posterPath,
        backdropPath = backdropPath,
        releaseDate = releaseDate ?: "",
        voteAverage = voteAverage,
        voteCount = voteCount,
        genreIds = genres?.map { it.id } ?: emptyList(),
        genres = genres?.map { it.name } ?: emptyList(),
        runtime = runtime,
        originalLanguage = originalLanguage ?: ""
    )
}

@JsonClass(generateAdapter = true)
data class TmdbGenreDto(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String
)

@JsonClass(generateAdapter = true)
data class TmdbCreditsResponse(
    @Json(name = "cast") val cast: List<TmdbCastDto>
)

@JsonClass(generateAdapter = true)
data class TmdbCastDto(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "character") val character: String?,
    @Json(name = "profile_path") val profilePath: String?
) {
    fun toDomainModel(): CastMember = CastMember(
        id = id,
        name = name,
        character = character ?: "",
        profilePath = profilePath
    )
}

@JsonClass(generateAdapter = true)
data class TmdbVideosResponse(
    @Json(name = "results") val results: List<TmdbVideoDto>
)

@JsonClass(generateAdapter = true)
data class TmdbVideoDto(
    @Json(name = "id") val id: String,
    @Json(name = "key") val key: String,
    @Json(name = "site") val site: String,
    @Json(name = "type") val type: String
)

@JsonClass(generateAdapter = true)
data class TmdbReleaseDatesResponse(
    @Json(name = "id") val id: Int,
    @Json(name = "results") val results: List<TmdbReleaseDatesResultDto>
)

@JsonClass(generateAdapter = true)
data class TmdbReleaseDatesResultDto(
    @Json(name = "iso_3166_1") val iso31661: String,
    @Json(name = "release_dates") val releaseDates: List<TmdbReleaseDateItemDto>
)

@JsonClass(generateAdapter = true)
data class TmdbReleaseDateItemDto(
    @Json(name = "release_date") val releaseDate: String,
    @Json(name = "certification") val certification: String?,
    @Json(name = "type") val type: Int
)

@JsonClass(generateAdapter = true)
data class TmdbImagesResponse(
    @Json(name = "logos") val logos: List<TmdbImageDto>? = null
)

@JsonClass(generateAdapter = true)
data class TmdbImageDto(
    @Json(name = "file_path") val filePath: String,
    @Json(name = "iso_639_1") val iso6391: String? = null
)
