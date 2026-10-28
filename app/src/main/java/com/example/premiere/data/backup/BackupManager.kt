package com.example.premiere.data.backup

import android.content.Context
import android.net.Uri
import com.example.premiere.domain.model.Cinema
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.time.Instant

// ─── Backup schema ────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class BackupSettings(
    val ticketSize: String = "MEDIUM",
    val ticketOrientation: String = "HORIZONTAL",
    val ticketColor: String = "POSTER",
    val defaultCinema: String = "",
    val defaultFormat: String = "",
    val tmdbApiKey: String = "",
    val omdbApiKey: String = ""
)

@JsonClass(generateAdapter = true)
data class BackupMovieEntry(
    val id: Int,
    val title: String,
    val originalTitle: String = "",
    val overview: String = "",
    val posterPath: String? = null,
    val backdropPath: String? = null,
    val logoUrl: String? = null,
    val releaseDate: String = "",
    val genres: String = "",
    val runtime: Int? = null,
    val certification: String? = null,
    val imdbRating: String? = null,
    val originalLanguage: String = "",
    val hasReminder: Boolean = false,
    val isWatchedInTheater: Boolean = false,
    val ticketDate: String = "",
    val ticketTime: String = "",
    val ticketSeat: String = "",
    val ticketCinema: String = "",
    val ticketScreenNo: String = "",
    val ticketFormat: String = "",
    val ticketLanguage: String = ""
)

@JsonClass(generateAdapter = true)
data class BackupCinema(
    val id: String,
    val name: String,
    val logoUrl: String = ""
)

@JsonClass(generateAdapter = true)
data class AppBackup(
    val version: Int = 1,
    val exportedAt: String = Instant.now().toString(),
    val cinemas: List<BackupCinema> = emptyList(),
    val settings: BackupSettings = BackupSettings(),
    val theatricalHistory: List<BackupMovieEntry> = emptyList()
)

// ─── Result ───────────────────────────────────────────────────────────────────

sealed class ImportResult {
    data class Success(val cinemasImported: Int, val historyImported: Int, val remindersImported: Int) : ImportResult()
    data class Error(val message: String) : ImportResult()
}

// ─── Manager ─────────────────────────────────────────────────────────────────

object BackupManager {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val backupAdapter = moshi.adapter(AppBackup::class.java).indent("  ")

    fun exportToUri(uri: Uri, backup: AppBackup, context: Context): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                writer.write(backupAdapter.toJson(backup))
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun importFromUri(uri: Uri, context: Context): Result<AppBackup> {
        return try {
            val json = context.contentResolver.openInputStream(uri)
                ?.bufferedReader()
                ?.readText()
                ?: return Result.failure(Exception("Could not open file"))
            val backup = backupAdapter.fromJson(json)
                ?: return Result.failure(Exception("Invalid backup file format"))
            Result.success(backup)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to parse backup: ${e.message}"))
        }
    }

    // ─── Mapping helpers ──────────────────────────────────────────────────────

    fun Cinema.toBackup() = BackupCinema(id = id, name = name, logoUrl = logoUrl)

    fun BackupCinema.toDomain() = Cinema(id = id, name = name, logoUrl = logoUrl)
}
