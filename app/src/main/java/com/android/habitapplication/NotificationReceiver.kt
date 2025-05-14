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

class NotificationReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "NotificationReceiver"
        const val ACTION_RANDOM_NOTIFICATION = "com.android.habitapplication.RANDOM_NOTIFICATION"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d(TAG, "onReceive called with action: ${intent?.action}")

        when (intent?.action) {
            ACTION_RANDOM_NOTIFICATION -> {
                handleRandomNotification(context)
                // Schedule next notification
                NotificationScheduler.scheduleRepeatingNotifications(context, 2 * 60 * 1000L)
            }
            "com.android.habitapplication.ALARM_WAKE" -> handleWakeAlarm(context)
            "com.android.habitapplication.ALARM_SLEEP" -> handleSleepAlarm(context)
            else -> Log.d(TAG, "Unknown action received: ${intent?.action}")
        }
    }

    private fun handleRandomNotification(context: Context) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Log.e(TAG, "No user logged in")
            return
        }

        val db = FirebaseFirestore.getInstance()

        // Check vacation mode from Firestore
        db.collection("userSettings")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                val isVacationModeOn = document.getBoolean("isVacationModeOn") ?: false
                if (isVacationModeOn) {
                    Log.d(TAG, "Vacation mode is on, skipping notification")
                    return@addOnSuccessListener
                }

                val cal = Calendar.getInstance()
                val now = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

                val prefs = context.getSharedPreferences("user_times", Context.MODE_PRIVATE)
                val wake = prefs.getInt("wakeHour", 8) * 60 + prefs.getInt("wakeMinute", 0)
                val sleep = prefs.getInt("sleepHour", 22) * 60 + prefs.getInt("sleepMinute", 0)

                Log.d(TAG, "Current time: $now, Wake time: $wake, Sleep time: $sleep")

                if (now in wake until sleep) {
                    Log.d(TAG, "Within active hours, sending notification")
                    val messages = listOf(
                        Pair(R.drawable.sun, "Time for a quick stretch! 🧘‍♂️"),
                        Pair(R.drawable.water_cup, "Stay hydrated! Time to drink water 💧"),
                        Pair(R.drawable.tree, "Take a deep breath and stay focused! 🌿"),
                        Pair(R.drawable.sun, "How are your habits going today? 📝"),
                        Pair(R.drawable.water_cup, "Remember to take a short break! ☕"),
                        Pair(R.drawable.tree, "Time to check your progress! 📊")
                    )
                    val (imageResId, message) = messages.random()
                    
                    // Format date and time consistently
                    val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val currentDate = dateFormat.format(cal.time)
                    val currentTime = timeFormat.format(cal.time)

                    val notification = Notification(
                        imageResId = imageResId,
                        title = message,
                        date = currentDate,
                        time = currentTime
                    )
                    
                    showNotification(context, notification)

                    // Save notification to Firestore
                    val notificationData = hashMapOf(
                        "imageResId" to notification.imageResId,
                        "title" to notification.title,
                        "date" to notification.date,
                        "time" to notification.time,
                        "timestamp" to System.currentTimeMillis()
                    )

                    db.collection("userNotifications")
                        .document(user.uid)
                        .collection("notifications")
                        .add(notificationData)
                        .addOnSuccessListener {
                            Log.d(TAG, "Notification saved to Firestore")
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error saving notification: ${e.message}")
                        }
                } else {
                    Log.d(TAG, "Outside active hours, skipping notification")
                    NotificationScheduler.cancelNotifications(context)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking vacation mode: ${e.message}")
            }
    }

    private fun handleWakeAlarm(context: Context) {
        val cal = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        
        val notification = Notification(
            imageResId = R.drawable.sun,
            title = "Wake Up Time!",
            date = dateFormat.format(cal.time),
            time = timeFormat.format(cal.time)
        )
        showNotification(context, notification)
    }

    private fun handleSleepAlarm(context: Context) {
        val cal = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        
        val notification = Notification(
            imageResId = R.drawable.nights,
            title = "Sleep Time!",
            date = dateFormat.format(cal.time),
            time = timeFormat.format(cal.time)
        )
        showNotification(context, notification)
    }

    private fun showNotification(context: Context, notification: Notification) {
        Log.d(TAG, "Attempting to show notification: ${notification.title}")
        val notificationManager = NotificationManagerCompat.from(context)

        val builder = NotificationCompat.Builder(context, NotificationsFragment.CHANNEL_ID)
            .setSmallIcon(notification.imageResId)
            .setContentTitle(notification.title)
            .setContentText("${notification.date} ${notification.time}")
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
                Log.d(TAG, "Notification shown successfully with ID: $notificationId")
            } else {
                Log.e(TAG, "Notification permission not granted")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing notification: ${e.message}", e)
        }
    }
}
