package com.example.premiere.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.premiere.MainActivity
import com.example.premiere.R
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val movieId = inputData.getInt(KEY_MOVIE_ID, -1)
        val movieTitle = inputData.getString(KEY_MOVIE_TITLE) ?: return Result.failure()
        val releaseDate = inputData.getString(KEY_RELEASE_DATE) ?: ""
        val daysUntil = inputData.getInt(KEY_DAYS_UNTIL, 0)

        createNotificationChannel()
        showNotification(movieId, movieTitle, releaseDate, daysUntil)
        return Result.success()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Movie Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for upcoming theatrical releases"
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun showNotification(movieId: Int, title: String, releaseDate: String, daysUntil: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("movie_id", movieId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, movieId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = when (daysUntil) {
            0 -> "🎬 \"$title\" is in cinemas TODAY!"
            1 -> "\"$title\" hits theaters TOMORROW!"
            else -> "\"$title\" releases in $daysUntil days — $releaseDate"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(if (daysUntil == 0) "Now in Cinemas!" else "Upcoming Release")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(movieId * 100 + daysUntil, notification)
    }

    companion object {
        const val CHANNEL_ID = "premiere_reminders"
        const val KEY_MOVIE_ID = "movie_id"
        const val KEY_MOVIE_TITLE = "movie_title"
        const val KEY_RELEASE_DATE = "release_date"
        const val KEY_DAYS_UNTIL = "days_until"
    }
}
