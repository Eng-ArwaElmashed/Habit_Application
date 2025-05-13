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
        val user = FirebaseAuth.getInstance().currentUser ?: return
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

                if (now in wake until sleep) {
                    val messages = listOf(
                        Notification(R.drawable.sun, "Stretch your body!", "Today"),
                        Notification(R.drawable.water_cup, "Time to drink water 💧", "Today"),
                        Notification(R.drawable.tree, "Focus time: Stay on track!", "Today")
                    )
                    val notification = messages.random()
                    showNotification(context, notification)
                    
                    // Save notification to Firestore
                    saveNotificationToFirestore(notification)
                    
                    // Schedule next notification
                    NotificationScheduler.scheduleRepeatingNotifications(context, 2 * 60 * 1000L)
                }
            }
    }

    private fun showNotification(context: Context, data: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val builder = NotificationCompat.Builder(context, NotificationsFragment.CHANNEL_ID)
            .setSmallIcon(data.imageResId)
            .setContentTitle("Reminder")
            .setContentText(data.title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(Random.nextInt(), builder.build())
        } catch (e: Exception) {
            Log.e("NotificationReceiver", "Error showing notification: ${e.message}")
        }
    }

    private fun saveNotificationToFirestore(notification: Notification) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val date = dateFormat.format(Date())
        val time = timeFormat.format(Date())

        val notificationData = hashMapOf(
            "title" to notification.title,
            "message" to notification.title, // Using title as message since that's what we show
            "imageResId" to notification.imageResId,
            "timestamp" to System.currentTimeMillis(),
            "date" to date,
            "time" to time
        )

        db.collection("userNotifications")
            .document(user.uid)
            .collection("notifications")
            .add(notificationData)
            .addOnSuccessListener {
                Log.d("NotificationReceiver", "Notification saved to Firestore")
            }
            .addOnFailureListener { e ->
                Log.e("NotificationReceiver", "Error saving notification to Firestore: ${e.message}")
            }
    }
}
