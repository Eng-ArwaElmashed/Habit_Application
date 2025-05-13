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
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("NotificationReceiver", "onReceive called")
        
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Log.e("NotificationReceiver", "No user logged in")
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
                    Log.d("NotificationReceiver", "Vacation mode is on, skipping notification")
                    return@addOnSuccessListener
                }

                val cal = Calendar.getInstance()
                val now = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

                val prefs = context.getSharedPreferences("user_times", Context.MODE_PRIVATE)
                val wake = prefs.getInt("wakeHour", 8) * 60 + prefs.getInt("wakeMinute", 0)
                val sleep = prefs.getInt("sleepHour", 22) * 60 + prefs.getInt("sleepMinute", 0)

                Log.d("NotificationReceiver", "Current time: $now, Wake time: $wake, Sleep time: $sleep")

                if (now in wake until sleep) {
                    Log.d("NotificationReceiver", "Within active hours, sending notification")
                    val messages = listOf(
                        Notification(R.drawable.sun, "Time for a quick stretch! 🧘‍♂️", "Today"),
                        Notification(R.drawable.water_cup, "Stay hydrated! Time to drink water 💧", "Today"),
                        Notification(R.drawable.tree, "Take a deep breath and stay focused! 🌿", "Today"),
                        Notification(R.drawable.sun, "How are your habits going today? 📝", "Today"),
                        Notification(R.drawable.water_cup, "Remember to take a short break! ☕", "Today"),
                        Notification(R.drawable.tree, "Time to check your progress! 📊", "Today")
                    )
                    val notification = messages.random()
                    showNotification(context, notification)
                    
                    // Save notification to Firestore with timestamp
                    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    val currentTime = dateFormat.format(Calendar.getInstance().time)
                    val notificationData = hashMapOf(
                        "imageResId" to notification.imageResId,
                        "title" to notification.title,
                        "date" to "Today",
                        "time" to currentTime,
                        "timestamp" to System.currentTimeMillis()
                    )
                    
                    db.collection("userNotifications")
                        .document(user.uid)
                        .collection("notifications")
                        .add(notificationData)
                        .addOnSuccessListener {
                            Log.d("NotificationReceiver", "Notification saved to Firestore")
                        }
                        .addOnFailureListener { e ->
                            Log.e("NotificationReceiver", "Error saving notification: ${e.message}")
                        }
                    
                    // No need to schedule next notification as we're using setRepeating
                } else {
                    Log.d("NotificationReceiver", "Outside active hours, skipping notification")
                }
            }
            .addOnFailureListener { e ->
                Log.e("NotificationReceiver", "Error checking vacation mode: ${e.message}")
            }
    }

    private fun showNotification(context: Context, notification: Notification) {
        val notificationManager = NotificationManagerCompat.from(context)
        
        val builder = NotificationCompat.Builder(context, NotificationsFragment.CHANNEL_ID)
            .setSmallIcon(notification.imageResId)
            .setContentTitle(notification.title)
            .setContentText(notification.date)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        try {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                notificationManager.notify(Random.nextInt(), builder.build())
                Log.d("NotificationReceiver", "Notification shown successfully")
            } else {
                Log.e("NotificationReceiver", "Notification permission not granted")
            }
        } catch (e: Exception) {
            Log.e("NotificationReceiver", "Error showing notification: ${e.message}")
        }
    }
}
