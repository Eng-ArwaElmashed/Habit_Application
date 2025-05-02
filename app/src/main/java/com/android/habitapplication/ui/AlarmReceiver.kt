package com.android.habitapplication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val builder = NotificationCompat.Builder(context, "wake_channel")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Good Morning!")
            .setContentText("It's time to start your day 🌞")
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        with(NotificationManagerCompat.from(context)) {
            notify(123, builder.build())
        }
    }
}
