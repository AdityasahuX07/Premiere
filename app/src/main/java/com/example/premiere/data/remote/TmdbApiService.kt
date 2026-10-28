package com.example.premiere.data.remote

import com.example.premiere.data.remote.dto.TmdbCreditsResponse
import com.example.premiere.data.remote.dto.TmdbGenreDto
import com.example.premiere.data.remote.dto.TmdbMovieDetailDto
import com.example.premiere.data.remote.dto.TmdbMovieListResponse
import com.example.premiere.data.remote.dto.TmdbReleaseDatesResponse
import com.example.premiere.data.remote.dto.TmdbVideosResponse
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApiService {

    @GET("movie/upcoming")
    suspend fun getUpcomingMovies(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1,
        @Query("region") region: String = "IN"
    ): TmdbMovieListResponse

    @GET("search/movie")
    suspend fun searchMovies(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("region") region: String = "IN"
    ): TmdbMovieListResponse

    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String
    ): TmdbMovieDetailDto

    @GET("movie/{movie_id}/credits")
    suspend fun getMovieCredits(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String
    ): TmdbCreditsResponse

    @GET("movie/{movie_id}/videos")
    suspend fun getMovieVideos(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String
    ): TmdbVideosResponse

    @GET("movie/{movie_id}/release_dates")
    suspend fun getMovieReleaseDates(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String
    ): TmdbReleaseDatesResponse

    @GET("movie/{movie_id}/images")
    suspend fun getMovieImages(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String,
        @Query("include_image_language") includeImageLanguage: String = "en,null,xx"
    ): com.example.premiere.data.remote.dto.TmdbImagesResponse

    @GET("genre/movie/list")
    suspend fun getGenres(
        @Query("api_key") apiKey: String
    ): GenreListResponse
}

@JsonClass(generateAdapter = true)
data class GenreListResponse(
    @Json(name = "genres") val genres: List<TmdbGenreDto>
)
