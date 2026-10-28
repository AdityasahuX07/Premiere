package com.example.premiere.worker

import android.content.Context
import androidx.work.*
import com.example.premiere.domain.model.Movie
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    private val reminderIntervals = listOf(3) // days before release

    fun scheduleReminders(movie: Movie) {
        val releaseDate = try {
            LocalDate.parse(movie.releaseDate)
        } catch (_: Exception) {
            return
        }

        val today = LocalDate.now()
        val daysUntilRelease = ChronoUnit.DAYS.between(today, releaseDate)

        reminderIntervals.forEach { daysBefore ->
            val daysUntilNotify = daysUntilRelease - daysBefore
            if (daysUntilNotify >= 0) {
                scheduleNotification(movie, daysBefore, daysUntilNotify)
            }
        }
    }

    fun cancelReminders(movieId: Int) {
        // Cancel all potential old intervals to clean up legacy data
        listOf(30, 14, 7, 3, 1, 0).forEach { daysBefore ->
            workManager.cancelUniqueWork(workId(movieId, daysBefore))
        }
    }

    private fun scheduleNotification(movie: Movie, daysBefore: Int, daysUntilTrigger: Long) {
        val inputData = workDataOf(
            ReminderWorker.KEY_MOVIE_ID to movie.id,
            ReminderWorker.KEY_MOVIE_TITLE to movie.title,
            ReminderWorker.KEY_RELEASE_DATE to movie.releaseDate,
            ReminderWorker.KEY_DAYS_UNTIL to daysBefore
        )

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInputData(inputData)
            .setInitialDelay(daysUntilTrigger, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .build()

        workManager.enqueueUniqueWork(
            workId(movie.id, daysBefore),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun workId(movieId: Int, daysBefore: Int) =
        "reminder_${movieId}_${daysBefore}d"
}
