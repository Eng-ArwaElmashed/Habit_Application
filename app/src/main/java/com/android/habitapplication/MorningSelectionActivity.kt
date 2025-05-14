package com.android.habitapplication

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.habitapplication.ui.AlarmReceiver
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class MorningSelectionActivity : AppCompatActivity() {

    private lateinit var alarmManager: AlarmManager
    private lateinit var pendingIntent: PendingIntent
    private lateinit var calendar: Calendar
    private lateinit var timePicker: TimePicker
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        setContentView(R.layout.activity_morning_selection)

        db = FirebaseFirestore.getInstance()
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        timePicker = findViewById(R.id.time_pk)
        val getStartedButton: MaterialButton = findViewById(R.id.get_started_btn)

        createNotificationChannel()

        getStartedButton.setOnClickListener {
            setAlarm()
            startActivity(Intent(this, EveningSelectionActivity::class.java))
        }
    }

    private fun setAlarm() {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, timePicker.hour)
            set(Calendar.MINUTE, timePicker.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Save wake time to Firestore
        val wakeTimeData = hashMapOf(
            "wakeHour" to timePicker.hour,
            "wakeMinute" to timePicker.minute,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("userWakeTimes")
            .document(user.uid)
            .set(wakeTimeData)
            .addOnSuccessListener {
                // Save to local preferences
                val prefs = getSharedPreferences("user_times", Context.MODE_PRIVATE)
                prefs.edit()
                    .putInt("wakeHour", timePicker.hour)
                    .putInt("wakeMinute", timePicker.minute)
                    .apply()

                // Set alarm
                val intent = Intent(this, AlarmReceiver::class.java).apply {
                    action = "com.android.habitapplication.ALARM_WAKE"
                    putExtra("type", "wake")
                    putExtra("title", "Wake Up Time!")
                    putExtra("message", "Time to start your day!")
                    putExtra("channelId", "wake_channel")
                    putExtra("notificationId", 1)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    this, 1, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                alarmManager.cancel(pendingIntent)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAlarmClock(
                        AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent),
                        pendingIntent
                    )
                } else {
                    alarmManager.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        AlarmManager.INTERVAL_DAY,
                        pendingIntent
                    )
                }

                // Start random notifications if we're within active hours
                val sleepHour = prefs.getInt("sleepHour", 22)
                val sleepMinute = prefs.getInt("sleepMinute", 0)
                val wakeHour = timePicker.hour
                val wakeMinute = timePicker.minute

                val cal = Calendar.getInstance()
                val now = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
                val wake = wakeHour * 60 + wakeMinute
                val sleep = sleepHour * 60 + sleepMinute

                Log.d("MorningSelection", "Current time: $now, Wake time: $wake, Sleep time: $sleep")

                if (now in wake until sleep) {
                    Log.d("MorningSelection", "Starting random notifications")
                    NotificationScheduler.scheduleRepeatingNotifications(this, 2 * 60 * 1000L)
                    Toast.makeText(this, "Random notifications started", Toast.LENGTH_SHORT).show()
                } else {
                    Log.d("MorningSelection", "Outside active hours, notifications will start at wake time")
                }

                Toast.makeText(this, "Wake up alarm set for ${timePicker.hour}:${timePicker.minute}", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("MorningSelection", "Error saving wake time: ${e.message}")
                Toast.makeText(this, "Error saving wake time", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "wake_channel",
                "Wake Up Notification",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for wake up reminders"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
            Log.d("MorningSelection", "Notification channel created with high importance")
        }
    }
}