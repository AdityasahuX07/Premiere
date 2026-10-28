package com.example.premiere.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.premiere.data.local.dao.MovieDao
import com.example.premiere.data.local.entity.MovieEntity

@Database(
    entities = [MovieEntity::class],
    version = 12,
    exportSchema = false
)
abstract class PremiereDatabase : RoomDatabase() {
    abstract fun movieDao(): MovieDao

    companion object {
        const val DATABASE_NAME = "premiere_db"

        /** v1 → v2: added genreNames (TEXT) and runtime (INTEGER) columns */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE movies ADD COLUMN genreNames TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE movies ADD COLUMN runtime INTEGER")
            }
        }

        /** v2 → v3: added trailerKey (TEXT), imdbRating (TEXT), and castJson (TEXT) columns */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE movies ADD COLUMN trailerKey TEXT")
                database.execSQL("ALTER TABLE movies ADD COLUMN imdbRating TEXT")
                database.execSQL("ALTER TABLE movies ADD COLUMN castJson TEXT NOT NULL DEFAULT ''")
            }
        }

        /** v3 → v4: added isWatchedInTheater (INTEGER) column */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE movies ADD COLUMN isWatchedInTheater INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** v4 → v5: added ticket fields */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE movies ADD COLUMN ticketDate TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE movies ADD COLUMN ticketTime TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE movies ADD COLUMN ticketSeat TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE movies ADD COLUMN ticketCinema TEXT NOT NULL DEFAULT ''")
            }
        }

        /** v5 → v6: added screen no and format */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE movies ADD COLUMN ticketScreenNo TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE movies ADD COLUMN ticketFormat TEXT NOT NULL DEFAULT ''")
            }
        }

        /** v6 → v7: added logoUrl */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE movies ADD COLUMN logoUrl TEXT")
            }
        }

        /** v7 → v8: added certification */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE movies ADD COLUMN certification TEXT")
            }
        }

        /** v8 → v9: added ticketLanguage column */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE movies ADD COLUMN ticketLanguage TEXT NOT NULL DEFAULT ''")
            }
        }

        /** v9 → v10: added personalRating (REAL) and watchNotes (TEXT) */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE movies ADD COLUMN personalRating REAL NOT NULL DEFAULT 0.0")
                database.execSQL("ALTER TABLE movies ADD COLUMN watchNotes TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Get list of existing columns in `movies`
                val cursor = database.query("PRAGMA table_info(movies)")
                val existingColumns = mutableListOf<String>()
                while (cursor.moveToNext()) {
                    existingColumns.add(cursor.getString(1)) // column name
                }
                cursor.close()

                val newTableColumns = listOf(
                    "id", "title", "originalTitle", "overview", "posterPath", "backdropPath", 
                    "logoUrl", "originalLanguage", "releaseDate", "voteAverage", "voteCount", 
                    "genreIds", "genreNames", "runtime", "trailerKey", "imdbRating", "castJson", 
                    "hasReminder", "isWatchedInTheater", "ticketDate", "ticketTime", "ticketSeat", 
                    "ticketCinema", "ticketScreenNo", "ticketFormat", "ticketLanguage", "certification"
                )

                // Build the SELECT statement dynamically mapping existing columns or default values
                val selectColumns = newTableColumns.joinToString(", ") { col ->
                    if (existingColumns.contains(col)) {
                        "`$col`"
                    } else {
                        when (col) {
                            "runtime", "trailerKey", "imdbRating", "logoUrl", "posterPath", "backdropPath", "certification" -> "NULL AS `$col`"
                            "voteAverage" -> "0.0 AS `$col`"
                            "voteCount", "isWatchedInTheater", "hasReminder" -> "0 AS `$col`"
                            else -> "'' AS `$col`" // default for string
                        }
                    }
                }

                // To drop columns safely across all SQLite versions, we create a new table, copy data, and swap.
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `movies_new` (
                        `id` INTEGER NOT NULL, 
                        `title` TEXT NOT NULL, 
                        `originalTitle` TEXT NOT NULL, 
                        `overview` TEXT NOT NULL, 
                        `posterPath` TEXT, 
                        `backdropPath` TEXT, 
                        `logoUrl` TEXT, 
                        `originalLanguage` TEXT NOT NULL, 
                        `releaseDate` TEXT NOT NULL, 
                        `voteAverage` REAL NOT NULL, 
                        `voteCount` INTEGER NOT NULL, 
                        `genreIds` TEXT NOT NULL, 
                        `genreNames` TEXT NOT NULL, 
                        `runtime` INTEGER, 
                        `trailerKey` TEXT, 
                        `imdbRating` TEXT, 
                        `castJson` TEXT NOT NULL, 
                        `hasReminder` INTEGER NOT NULL, 
                        `isWatchedInTheater` INTEGER NOT NULL, 
                        `ticketDate` TEXT NOT NULL, 
                        `ticketTime` TEXT NOT NULL, 
                        `ticketSeat` TEXT NOT NULL, 
                        `ticketCinema` TEXT NOT NULL, 
                        `ticketScreenNo` TEXT NOT NULL, 
                        `ticketFormat` TEXT NOT NULL, 
                        `ticketLanguage` TEXT NOT NULL, 
                        `certification` TEXT, 
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                database.execSQL("""
                    INSERT INTO `movies_new` (${newTableColumns.joinToString(", ") { "`$it`" }})
                    SELECT $selectColumns FROM `movies`
                """.trimIndent())

                database.execSQL("DROP TABLE `movies`")
                database.execSQL("ALTER TABLE `movies_new` RENAME TO `movies`")
            }
        }
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE movies ADD COLUMN ticketQrImagePath TEXT")
            }
        }
    }
}
