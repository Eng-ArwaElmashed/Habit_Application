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

    fun scheduleRepeatingNotifications(context: Context, intervalMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java)
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            101,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Cancel any existing notifications first
        alarmManager.cancel(pendingIntent)
        Log.d("NotificationScheduler", "Cancelled existing notifications")

        // Calculate the next 2-minute mark
        val calendar = Calendar.getInstance()
        val currentMinute = calendar.get(Calendar.MINUTE)
        val nextMinute = ((currentMinute / 2) + 1) * 2
        calendar.set(Calendar.MINUTE, nextMinute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.MINUTE, 2)
        }

        val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val formattedTime = dateFormat.format(calendar.time)
        Log.d("NotificationScheduler", "Scheduling next notification for: $formattedTime")

        try {
            // Use setRepeating instead of setExact to ensure notifications continue
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                intervalMillis, // 2 minutes in milliseconds
                pendingIntent
            )
            Log.d("NotificationScheduler", "Scheduled repeating notifications every 2 minutes")
        } catch (e: Exception) {
            Log.e("NotificationScheduler", "Error scheduling notification: ${e.message}")
        }
    }

    fun cancelNotifications(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            101,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d("NotificationScheduler", "Cancelled all notifications")
    }
}

