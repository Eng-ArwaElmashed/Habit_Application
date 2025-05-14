package com.android.habitapplication

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import com.android.habitapplication.NotificationReceiver
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

object NotificationScheduler {
    private const val TAG = "NotificationScheduler"
    private const val MIN_INTERVAL = 30 * 60 * 1000L

    fun scheduleRandomNotifications(context: Context) {
        Log.d(TAG, "Starting to schedule all notifications")
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // Get wake and sleep times
        val prefs = context.getSharedPreferences("user_times", Context.MODE_PRIVATE)
        val wakeHour = prefs.getInt("wakeHour", 8)
        val wakeMinute = prefs.getInt("wakeMinute", 0)
        val sleepHour = prefs.getInt("sleepHour", 22)
        val sleepMinute = prefs.getInt("sleepMinute", 0)

        // Cancel any existing notifications
        cancelNotifications(context)

        // Calculate time window
        var wakeTimeMinutes = wakeHour * 60 + wakeMinute
        var sleepTimeMinutes = sleepHour * 60 + sleepMinute

        // Handle case where sleep time is before wake time (next day)
        if (sleepTimeMinutes <= wakeTimeMinutes) {
            sleepTimeMinutes += 24 * 60 // Add 24 hours
        }

        val availableMinutes = sleepTimeMinutes - wakeTimeMinutes
        val oneThird = availableMinutes / 3

        // Create all four notifications
        val notifications = mutableListOf<Triple<Calendar, String, Intent>>()

        // 1. Wake-up notification
        val wakeupTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, wakeHour)
            set(Calendar.MINUTE, wakeMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        val wakeupIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = "com.android.habitapplication.ALARM_WAKE"
            putExtra("type", "wake")
            putExtra("title", "Wake Up Time!")
            putExtra("message", "Time to start your day!")
            putExtra("channelId", "wake_channel")
            putExtra("notificationId", 1)
        }
        notifications.add(Triple(wakeupTime, "Wake-up notification", wakeupIntent))

        // 2. First random notification
        val firstRandomTime = Calendar.getInstance().apply {
            val randomMinutes = wakeTimeMinutes + Random.nextInt(oneThird)
            set(Calendar.HOUR_OF_DAY, (randomMinutes / 60) % 24)
            set(Calendar.MINUTE, randomMinutes % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        val firstRandomIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = NotificationReceiver.ACTION_RANDOM_NOTIFICATION
            flags = Intent.FLAG_INCLUDE_STOPPED_PACKAGES
            data = Uri.parse("random://first/${System.currentTimeMillis()}")
        }
        notifications.add(Triple(firstRandomTime, "First random notification", firstRandomIntent))

        // 3. Second random notification
        val secondRandomTime = Calendar.getInstance().apply {
            val randomMinutes = wakeTimeMinutes + (2 * oneThird) + Random.nextInt(oneThird)
            set(Calendar.HOUR_OF_DAY, (randomMinutes / 60) % 24)
            set(Calendar.MINUTE, randomMinutes % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        val secondRandomIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = NotificationReceiver.ACTION_RANDOM_NOTIFICATION
            flags = Intent.FLAG_INCLUDE_STOPPED_PACKAGES
            data = Uri.parse("random://second/${System.currentTimeMillis()}")
        }
        notifications.add(Triple(secondRandomTime, "Second random notification", secondRandomIntent))

        // 4. Sleep notification
        val sleepTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, sleepHour)
            set(Calendar.MINUTE, sleepMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        val sleepIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = "com.android.habitapplication.ALARM_SLEEP"
            putExtra("type", "sleep")
            putExtra("title", "Sleep Time!")
            putExtra("message", "Time to review your day and prepare for tomorrow!")
            putExtra("channelId", "sleep_channel")
            putExtra("notificationId", 2)
        }
        notifications.add(Triple(sleepTime, "Sleep notification", sleepIntent))

        // Schedule all notifications
        notifications.forEachIndexed { index, (calendar, description, intent) ->
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                index, // Use index as unique request code
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
                Log.d(TAG, "Successfully scheduled $description for: ${formatTime(calendar)}")
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling $description: ${e.message}", e)
            }
        }

        // Schedule next day's notifications
        scheduleNextDayReset(context, wakeHour, wakeMinute)
        
        Log.d(TAG, "Completed scheduling all notifications")
    }

    private fun scheduleFixedTimeNotification(
        context: Context,
        hour: Int,
        minute: Int,
        intent: Intent,
        requestCode: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled fixed notification for: ${formatTime(calendar)}")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling fixed notification: ${e.message}", e)
        }
    }

    private fun createNotificationTime(minutesFromMidnight: Int): Calendar {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, minutesFromMidnight / 60 % 24)
            set(Calendar.MINUTE, minutesFromMidnight % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
    }

    private fun scheduleNextDayReset(context: Context, wakeHour: Int, wakeMinute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val nextDay = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, wakeHour)
            set(Calendar.MINUTE, wakeMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = "com.android.habitapplication.RESCHEDULE_NOTIFICATIONS"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            99,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextDay.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                nextDay.timeInMillis,
                pendingIntent
            )
        }
        Log.d(TAG, "Scheduled next day's notification reset for: ${formatTime(nextDay)}")
    }

    private fun formatTime(calendar: Calendar): String {
        return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(calendar.time)
    }

    fun cancelNotifications(context: Context) {
        Log.d(TAG, "Cancelling all notifications")
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // Cancel all possible notification intents
        for (requestCode in 99..120) { // Include reschedule (99) and all possible notification codes
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = NotificationReceiver.ACTION_RANDOM_NOTIFICATION
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }

        // Cancel wake notification
        val wakeIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = "com.android.habitapplication.ALARM_WAKE"
        }
        val wakePendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            wakeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(wakePendingIntent)

        // Cancel sleep notification
        val sleepIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = "com.android.habitapplication.ALARM_SLEEP"
        }
        val sleepPendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            sleepIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(sleepPendingIntent)

        // Cancel reschedule intent
        val rescheduleIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = "com.android.habitapplication.RESCHEDULE_NOTIFICATIONS"
        }
        val reschedulePendingIntent = PendingIntent.getBroadcast(
            context,
            99,
            rescheduleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(reschedulePendingIntent)
        
        Log.d(TAG, "Successfully cancelled all notifications")
    }
}

