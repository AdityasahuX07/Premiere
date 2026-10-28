package com.example.premiere.data.remote

import com.example.premiere.data.remote.dto.OmdbMovieDto
import retrofit2.http.GET
import retrofit2.http.Query

interface OmdbApiService {
    @GET("/")
    suspend fun getMovieByImdbId(
        @Query("i") imdbId: String,
        @Query("apikey") apiKey: String
    ): OmdbMovieDto
}
