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

    fun scheduleRepeatingNotifications(context: Context, intervalMillis: Long) {
        Log.d(TAG, "Starting to schedule repeating notifications")
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = NotificationReceiver.ACTION_RANDOM_NOTIFICATION
            flags = Intent.FLAG_INCLUDE_STOPPED_PACKAGES
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            101,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Cancel any existing notifications first
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Cancelled existing notifications")

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
        Log.d(TAG, "Scheduling next notification for: $formattedTime")

        try {
            // Use setRepeating instead of setExact to ensure notifications continue
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
                Log.d(TAG, "Scheduled exact notification for: $formattedTime")
                
                // Schedule the next one
                val nextCalendar = Calendar.getInstance().apply {
                    timeInMillis = calendar.timeInMillis + intervalMillis
                }
                val nextPendingIntent = PendingIntent.getBroadcast(
                    context,
                    102,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextCalendar.timeInMillis,
                    nextPendingIntent
                )
                Log.d(TAG, "Scheduled next notification for: ${dateFormat.format(nextCalendar.time)}")
            } else {
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    intervalMillis,
                    pendingIntent
                )
                Log.d(TAG, "Scheduled repeating notifications every 2 minutes")
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

    fun cancelNotifications(context: Context) {
        Log.d(TAG, "Cancelling all notifications")
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = NotificationReceiver.ACTION_RANDOM_NOTIFICATION
        }
        
        // Cancel both current and next pending intents
        for (requestCode in 101..102) {
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
        Log.d(TAG, "Successfully cancelled all notifications")
    }
}

