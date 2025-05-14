package com.android.habitapplication.ui

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.SystemClock
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.android.habitapplication.MainActivity
import com.android.habitapplication.Notification
import com.android.habitapplication.NotificationReceiver
import com.android.habitapplication.NotificationScheduler
import com.android.habitapplication.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.random.Random
import java.text.SimpleDateFormat
import java.util.*

class AlarmReceiver : BroadcastReceiver() {

    // Data class for cleaner destructuring
    data class NotificationData(
        val channelId: String,
        val title: String,
        val message: String,
        val imageResId: Int,
        val notificationId: Int
    )

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AlarmReceiver", "onReceive called with type: ${intent.getStringExtra("type")}")

        // Check vacation mode first
        val vacationPref = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        if (vacationPref.getBoolean("isVacationModeOn", false)) {
            Log.d("AlarmReceiver", "Vacation mode is on, skipping notification")
            return
        }

        // Get notification type from intent
        val type = intent.getStringExtra("type")
        if (type == null) {
            Log.e("AlarmReceiver", "No notification type provided in intent")
            return
        }
        Log.d("AlarmReceiver", "Processing notification type: $type")

        // Get notification data based on type
        val data = when (type) {
            "wake" -> NotificationData(
                "wake_channel",
                "Wake Up Time!",
                "Time to start your day!",
                R.drawable.sun,
                1
            )
            "sleep" -> NotificationData(
                "sleep_channel",
                "Sleep Time!",
                "Time to review your day and prepare for tomorrow!",
                R.drawable.nights,
                2
            )
            else -> {
                Log.e("AlarmReceiver", "Unknown notification type: $type")
                return
            }
        }

        try {
            // Create intent to open app when notification is tapped
            val notificationIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            // Build notification
            val builder = NotificationCompat.Builder(context, data.channelId)
                .setSmallIcon(data.imageResId)
                .setContentTitle(data.title)
                .setContentText(data.message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            // Create notification channel (for Android 8+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    data.channelId,
                    "$type notifications",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Channel for $type time reminders"
                    enableLights(true)
                    lightColor = Color.MAGENTA
                    enableVibration(true)
                    setShowBadge(true)
                }

                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(channel)
                Log.d("AlarmReceiver", "Created notification channel: ${data.channelId}")
            }

            // Show the notification
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(data.notificationId, builder.build())
            Log.d("AlarmReceiver", "Notification sent: ${data.title}")

            // Save to Firestore
            saveNotificationToFirestore(data.title, data.message, data.imageResId)

            // Handle notification based on type
            when (type) {
                "wake" -> handleWakeAlarm(context)
                "sleep" -> handleSleepAlarm(context)
            }
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "Error showing notification: ${e.message}")
        }
    }

    private fun saveNotificationToFirestore(title: String, message: String, imageResId: Int) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val date = dateFormat.format(Date())
        val time = timeFormat.format(Date())

        val notification = hashMapOf(
            "title" to title,
            "message" to message,
            "imageResId" to imageResId,
            "timestamp" to System.currentTimeMillis(),
            "date" to date,
            "time" to time
        )

        db.collection("userNotifications")
            .document(user.uid)
            .collection("notifications")
            .add(notification)
            .addOnSuccessListener {
                Log.d("AlarmReceiver", "Notification saved to Firestore")
            }
            .addOnFailureListener { e ->
                Log.e("AlarmReceiver", "Error saving notification to Firestore: ${e.message}")
            }
    }

    private fun handleWakeAlarm(context: Context) {
        // Handle wake-up notification
        val notification = Notification(
            R.drawable.sun,
            "Wake Up Time!",
            "Time to start your day!"
        )
        showNotification(context, notification)

        // Schedule random notifications to start after 2 minutes
        val calendar = Calendar.getInstance().apply {
            add(Calendar.MINUTE, 2)  // Add 2 minutes to current time
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        Log.d("AlarmReceiver", "Scheduling random notifications to start at: ${calendar.time}")

        // Create a delayed intent for random notifications
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = NotificationReceiver.ACTION_RANDOM_NOTIFICATION
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            101,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
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
    }

    private fun handleSleepAlarm(context: Context) {
        // First send the sleep time notification
        val notification = Notification(
            R.drawable.nights,
            "Sleep Time!",
            "Time to review your day and prepare for tomorrow!"
        )
        showNotification(context, notification)

        // Wait a short moment to ensure the sleep notification is shown
        Handler(Looper.getMainLooper()).postDelayed({
            // Then cancel random notifications
            Log.d("AlarmReceiver", "Cancelling random notifications at sleep time")
            NotificationScheduler.cancelNotifications(context)
        }, 1000) // 1 second delay to ensure sleep notification is shown first
    }

    private fun showNotification(context: Context, notification: Notification) {
        Log.d("AlarmReceiver", "Attempting to show notification: ${notification.title}")
        val notificationManager = NotificationManagerCompat.from(context)

        // Determine channel ID based on notification type
        val channelId = when {
            notification.title.contains("Wake Up") -> "wake_channel"
            notification.title.contains("Sleep") -> "sleep_channel"
            else -> "random_channel"
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(notification.imageResId)
            .setContentTitle(notification.title)
            .setContentText(notification.date)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        try {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val notificationId = Random.nextInt()
                notificationManager.notify(notificationId, builder.build())
                Log.d("AlarmReceiver", "Notification shown successfully with ID: $notificationId")
            } else {
                Log.e("AlarmReceiver", "Notification permission not granted")
            }
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "Error showing notification: ${e.message}", e)
        }
    }
}