package com.example.premiere.domain.model

data class Movie(
    val id: Int,
    val title: String,
    val originalTitle: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val logoUrl: String? = null,
    val originalLanguage: String = "",
    val releaseDate: String,       // "YYYY-MM-DD"
    val voteAverage: Double,
    val voteCount: Int,
    val popularity: Double = 0.0,
    val genreIds: List<Int> = emptyList(),
    val genres: List<String> = emptyList(),
    val runtime: Int? = null,
    val trailerKey: String? = null,
    val cast: List<CastMember> = emptyList(),
    val hasReminder: Boolean = false,
    val isWatchedInTheater: Boolean = false,
    val imdbRating: String? = null,
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
    val posterUrl: String? get() = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
    val backdropUrl: String? get() = backdropPath?.let { "https://image.tmdb.org/t/p/w780$it" }
    val fullLogoUrl: String? get() = logoUrl?.takeIf { it.isNotBlank() }?.let { "https://image.tmdb.org/t/p/w500$it" }
    val highResPosterUrl: String? get() = posterPath?.let { "https://image.tmdb.org/t/p/original$it" }
    val formattedRating: String get() = String.format("%.1f", voteAverage)
    val formattedRuntime: String? get() = runtime?.let {
        val h = it / 60
        val m = it % 60
        if (h > 0) "${h}h ${m}m" else "${m}m"
    }
}

data class CastMember(
    val id: Int,
    val name: String,
    val character: String,
    val profilePath: String?
) {
    val profileUrl: String? get() = profilePath?.let { "https://image.tmdb.org/t/p/w185$it" }
}
