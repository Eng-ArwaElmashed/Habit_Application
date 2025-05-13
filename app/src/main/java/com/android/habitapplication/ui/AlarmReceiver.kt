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
        // Check vacation mode first
        val vacationPref = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        if (vacationPref.getBoolean("isVacationModeOn", false)) {
            Log.d("AlarmReceiver", "Vacation mode is on, skipping notification")
            return
        }

        val type = intent.getStringExtra("type") ?: "wake"

        val data = when (type) {
            "sleep" -> NotificationData(
                "sleep_channel",
                "Time to Sleep",
                "Get ready for bed and relax.",
                R.drawable.nights, // Replace with actual icon
                1
            )
            else -> NotificationData(
                "wake_channel",
                "Good Morning!",
                "Time to rise and shine!",
                R.drawable.sun, // Replace with actual icon
                0
            )
        }

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
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        // Show the notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(data.notificationId, builder.build())

        // Save to Firestore
        saveNotificationToFirestore(data.title, data.message, data.imageResId)
    }

    private fun saveNotificationToFirestore(title: String, message: String, imageResId: Int) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        val date = dateFormat.format(Date())

        val notification = hashMapOf(
            "title" to title,
            "message" to message,
            "imageResId" to imageResId,
            "timestamp" to System.currentTimeMillis(),
            "date" to date
        )

        db.collection("userNotifications")
            .document(user.uid)
            .collection("notifications")
            .add(notification)
    }
}