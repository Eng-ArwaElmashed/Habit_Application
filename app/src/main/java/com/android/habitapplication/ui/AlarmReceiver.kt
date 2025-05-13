package com.android.habitapplication.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.android.habitapplication.MainActivity
import com.android.habitapplication.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
}