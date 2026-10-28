package com.example.premiere.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.premiere.data.local.entity.MovieEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDao {
    @Query("SELECT * FROM movies WHERE hasReminder = 1 AND isWatchedInTheater = 0 ORDER BY releaseDate ASC")
    fun getMoviesWithReminders(): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE id = :movieId")
    fun observeMovie(movieId: Int): Flow<MovieEntity?>

    @Query("SELECT * FROM movies WHERE id = :movieId")
    suspend fun getMovieById(movieId: Int): MovieEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovie(movie: MovieEntity)

    @Query("UPDATE movies SET hasReminder = :hasReminder WHERE id = :movieId")
    suspend fun updateReminderStatus(movieId: Int, hasReminder: Boolean)

    @Query("UPDATE movies SET hasReminder = 0, isWatchedInTheater = 0, ticketDate = '', ticketTime = '', ticketSeat = '', ticketCinema = '', ticketScreenNo = '', ticketFormat = '', ticketLanguage = '', ticketQrImagePath = null WHERE id = :movieId")
    suspend fun resetMovie(movieId: Int)

    @Query("SELECT * FROM movies WHERE (ticketDate != '' OR ticketTime != '' OR ticketCinema != '') AND isWatchedInTheater = 0 ORDER BY releaseDate ASC")
    fun getActiveTickets(): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE isWatchedInTheater = 1 ORDER BY COALESCE(NULLIF(ticketDate, ''), releaseDate) DESC, ticketTime DESC")
    fun getWatchedHistory(): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE isWatchedInTheater = 1 ORDER BY releaseDate DESC")
    suspend fun getAllWatchedHistoryOnce(): List<MovieEntity>

    @Query("SELECT * FROM movies WHERE isWatchedInTheater = 1 OR hasReminder = 1 ORDER BY releaseDate DESC")
    suspend fun getAllExportableMoviesOnce(): List<MovieEntity>

    @Query("SELECT * FROM movies")
    suspend fun getAllMoviesOnce(): List<MovieEntity>

    @Query("UPDATE movies SET ticketDate = :date, ticketTime = :time, ticketSeat = :seat, ticketCinema = :cinema, ticketScreenNo = :screenNo, ticketFormat = :format, ticketLanguage = :language WHERE id = :movieId")
    suspend fun updateTicketInfo(movieId: Int, date: String, time: String, seat: String, cinema: String, screenNo: String, format: String, language: String)

    @Query("UPDATE movies SET logoUrl = :logoUrl, certification = :certification WHERE id = :movieId")
    suspend fun updateLogoAndCertification(movieId: Int, logoUrl: String?, certification: String?)

    @Query("UPDATE movies SET ticketQrImagePath = :qrImagePath WHERE id = :movieId")
    suspend fun updateTicketQrImagePath(movieId: Int, qrImagePath: String?)

    @Query("UPDATE movies SET isWatchedInTheater = :isWatched WHERE id = :movieId")
    suspend fun updateWatchedStatus(movieId: Int, isWatched: Boolean)

    @Query("DELETE FROM movies WHERE hasReminder = 0 AND releaseDate < date('now', '-7 days')")
    suspend fun cleanupOldMovies()
}
