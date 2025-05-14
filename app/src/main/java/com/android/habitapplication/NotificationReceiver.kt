package com.android.habitapplication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.android.habitapplication.ui.notifications.NotificationsFragment
import com.android.habitapplication.NotificationScheduler
import java.util.Calendar
import kotlin.random.Random
import android.os.Build
import android.app.AlarmManager
import android.app.PendingIntent
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import android.app.NotificationManager
import android.app.NotificationChannel

class NotificationReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "NotificationReceiver"
        const val ACTION_RANDOM_NOTIFICATION = "com.android.habitapplication.RANDOM_NOTIFICATION"
        const val CHANNEL_ID = "habit_reminders"
        private val motivationalMessages = arrayOf(
            "Keep going! You're doing great!",
            "Stay focused on your goals!",
            "Every small step counts!",
            "You've got this!",
            "Making progress every day!",
            "Building better habits together!",
            "Stay committed to your journey!",
            "Your future self will thank you!"
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received notification intent: ${intent.action}")

        when (intent.action) {
            "com.android.habitapplication.ALARM_WAKE" -> {
                showNotification(
                    context,
                    intent.getStringExtra("channelId") ?: "wake_channel",
                    intent.getStringExtra("title") ?: "Wake Up Time!",
                    intent.getStringExtra("message") ?: "Time to start your day!",
                    intent.getIntExtra("notificationId", 1)
                )
            }
            "com.android.habitapplication.ALARM_SLEEP" -> {
                showNotification(
                    context,
                    intent.getStringExtra("channelId") ?: "sleep_channel",
                    intent.getStringExtra("title") ?: "Sleep Time!",
                    intent.getStringExtra("message") ?: "Time to review your day and prepare for tomorrow!",
                    intent.getIntExtra("notificationId", 2)
                )
            }
            ACTION_RANDOM_NOTIFICATION -> {
                val message = motivationalMessages[Random.nextInt(motivationalMessages.size)]
                showNotification(
                    context,
                    "random_channel",
                    "Habit Check-in",
                    message,
                    Random.nextInt(100, 1000)
                )
            }
            "com.android.habitapplication.RESCHEDULE_NOTIFICATIONS" -> {
                NotificationScheduler.scheduleRandomNotifications(context)
            }
        }
    }

    private fun showNotification(
        context: Context,
        channelId: String,
        title: String,
        message: String,
        notificationId: Int
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channels for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = mapOf(
                "wake_channel" to NotificationChannel(
                    "wake_channel",
                    "Wake Up Notifications",
                    NotificationManager.IMPORTANCE_HIGH
                ),
                "sleep_channel" to NotificationChannel(
                    "sleep_channel",
                    "Sleep Time Notifications",
                    NotificationManager.IMPORTANCE_HIGH
                ),
                "random_channel" to NotificationChannel(
                    "random_channel",
                    "Random Check-in Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
            
            channels[channelId]?.let { channel ->
                channel.description = "Notifications for your habit tracking"
                channel.enableLights(true)
                channel.enableVibration(true)
                notificationManager.createNotificationChannel(channel)
            }
        }

        // Create an intent to open the app when notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        // Show the notification
        notificationManager.notify(notificationId, builder.build())
        Log.d(TAG, "Showed notification: $title")
    }
}
