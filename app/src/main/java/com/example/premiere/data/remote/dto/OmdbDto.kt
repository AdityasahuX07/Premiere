package com.example.premiere.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OmdbMovieDto(
    @Json(name = "imdbRating") val imdbRating: String?
)
