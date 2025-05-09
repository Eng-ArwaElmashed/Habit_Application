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

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val vacationPref = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        if (vacationPref.getBoolean("isVacationModeOn", false)) return
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
        }

// Reschedule next notification
        NotificationScheduler.scheduleRepeatingNotifications(context, 2 * 60 * 1000L)
    }

    private fun showNotification(context: Context, data: Notification) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val builder = NotificationCompat.Builder(context, NotificationsFragment.CHANNEL_ID)
            .setSmallIcon(data.imageResId)
            .setContentTitle("Reminder")
            .setContentText(data.title)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        NotificationManagerCompat.from(context).notify(Random.nextInt(), builder.build())
    }
}
