package com.android.habitapplication

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.android.habitapplication.NotificationReceiver
import java.text.SimpleDateFormat
import java.util.*

object NotificationScheduler {
    private const val TAG = "NotificationScheduler"
    private const val INTERVAL_MINUTES = 2L
    private const val INTERVAL_MILLIS = INTERVAL_MINUTES * 60 * 1000 // 2 minutes in milliseconds

    fun scheduleRepeatingNotifications(context: Context, intervalMillis: Long) {
        Log.d(TAG, "Starting to schedule repeating notifications")

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = NotificationReceiver.ACTION_RANDOM_NOTIFICATION
            flags = Intent.FLAG_INCLUDE_STOPPED_PACKAGES
        }

        // Cancel any existing notifications first
        cancelNotifications(context)

        // Calculate the next interval
        val calendar = Calendar.getInstance()
        val currentMinute = calendar.get(Calendar.MINUTE)
        val nextMinute = ((currentMinute / INTERVAL_MINUTES) + 1) * INTERVAL_MINUTES
        calendar.set(Calendar.MINUTE, nextMinute.toInt())
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // If the calculated time has already passed, add the interval
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.MINUTE, INTERVAL_MINUTES.toInt())
        }

        val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val formattedTime = dateFormat.format(calendar.time)
        Log.d(TAG, "Scheduling next notification for: $formattedTime")

        try {
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                101,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
                Log.d(TAG, "Scheduled exact notification for: $formattedTime")
            } else {
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    INTERVAL_MILLIS,
                    pendingIntent
                )
                Log.d(TAG, "Scheduled repeating notifications every $INTERVAL_MINUTES minutes")
            }

            // Verify the alarm is set
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmInfo = alarmManager.getNextAlarmClock()
                Log.d(TAG, "Next alarm info: ${alarmInfo?.triggerTime}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling notification: ${e.message}", e)
        }
    }

    // Convenience method for scheduling with default interval
    fun scheduleRandomNotifications(context: Context) {
        scheduleRepeatingNotifications(context, INTERVAL_MILLIS)
    }

    fun cancelNotifications(context: Context) {
        Log.d(TAG, "Cancelling all notifications")
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = NotificationReceiver.ACTION_RANDOM_NOTIFICATION
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            101,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Successfully cancelled notifications")
    }
}
