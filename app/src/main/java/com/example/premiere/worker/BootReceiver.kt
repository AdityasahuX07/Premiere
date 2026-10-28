package com.example.premiere.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receives BOOT_COMPLETED to ensure WorkManager reschedules any pending reminders.
 * WorkManager already persists scheduled work across reboots, so this is a no-op safety net.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // WorkManager automatically restores pending work items after boot.
            // No additional action required.
        }
    }
}
