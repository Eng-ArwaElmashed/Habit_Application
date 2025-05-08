package com.android.habitapplication.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.android.habitapplication.NotificationReceiver

object NotificationScheduler {

    // Interval in milliseconds (e.g., every 24 hours)
    const val INTERVAL_MILLIS = 24 * 60 * 60 * 1000L // 24 hours

    fun scheduleRepeatingNotifications(context: Context, intervalMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        // Schedule the notification to repeat at the given interval
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis(),
            INTERVAL_MILLIS, // Use the interval defined here
            pendingIntent
        )
    }
}

