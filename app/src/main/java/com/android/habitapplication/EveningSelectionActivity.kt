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

class EveningSelectionActivity : AppCompatActivity() {

    private lateinit var alarmManager: AlarmManager
    private lateinit var pendingIntent: PendingIntent
    private lateinit var calendar: Calendar
    private lateinit var timePicker: TimePicker
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        setContentView(R.layout.activity_evening_selection)

        db = FirebaseFirestore.getInstance()

        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        timePicker = findViewById(R.id.time_pk)
        val getStartedButton: MaterialButton = findViewById(R.id.get_started_btn)

        createNotificationChannel()

        // Check if we need to fetch existing times
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            fetchTimesAndSchedule(user.uid)
        }

        getStartedButton.setOnClickListener {
            setAlarm()
            startActivity(Intent(this, ChooseHabitActivity::class.java))
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

        // Save sleep time to Firestore
        val sleepTimeData = hashMapOf(
            "sleepHour" to timePicker.hour,
            "sleepMinute" to timePicker.minute,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("userSleepTimes")
            .document(user.uid)
            .set(sleepTimeData)
            .addOnSuccessListener {
                // Save to local preferences
                val prefs = getSharedPreferences("user_times", Context.MODE_PRIVATE)
                prefs.edit()
                    .putInt("sleepHour", timePicker.hour)
                    .putInt("sleepMinute", timePicker.minute)
                    .apply()

                // Set alarm
                val intent = Intent(this, AlarmReceiver::class.java).apply {
                    action = "com.android.habitapplication.ALARM_SLEEP"
                    putExtra("type", "sleep")
                    putExtra("title", "Sleep Time!")
                    putExtra("message", "Time to review your day and prepare for tomorrow!")
                    putExtra("channelId", "sleep_channel")
                    putExtra("notificationId", 2)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    this, 2, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
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
                val wakeHour = prefs.getInt("wakeHour", 8)
                val wakeMinute = prefs.getInt("wakeMinute", 0)
                val sleepHour = timePicker.hour
                val sleepMinute = timePicker.minute

                val cal = Calendar.getInstance()
                val now = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
                val wake = wakeHour * 60 + wakeMinute
                val sleep = sleepHour * 60 + sleepMinute

                Log.d("EveningSelection", "Current time: $now, Wake time: $wake, Sleep time: $sleep")

                if (now in wake until sleep) {
                    Log.d("EveningSelection", "Starting random notifications")
                    NotificationScheduler.scheduleRepeatingNotifications(this, 2 * 60 * 1000L)
                    Toast.makeText(this, "Random notifications started", Toast.LENGTH_SHORT).show()
                } else {
                    Log.d("EveningSelection", "Outside active hours, notifications will start at wake time")
                }

                Toast.makeText(this, "Sleep time alarm set for ${timePicker.hour}:${timePicker.minute}", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("EveningSelection", "Error saving sleep time: ${e.message}")
                Toast.makeText(this, "Error saving sleep time", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "sleep_channel",
                "Sleep Time Notification",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for sleep time reminders"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
            Log.d("EveningSelection", "Notification channel created with high importance")
        }
    }

    private fun fetchTimesAndSchedule(userId: String) {
        val wakeTimeDoc = db.collection("userWakeTimes").document(userId)
        val sleepTimeDoc = db.collection("userSleepTimes").document(userId)

        wakeTimeDoc.get()
            .addOnSuccessListener { wakeDoc ->
                sleepTimeDoc.get()
                    .addOnSuccessListener { sleepDoc ->
                        if (wakeDoc.exists() && sleepDoc.exists()) {
                            val wakeHour = wakeDoc.getLong("wakeHour")?.toInt() ?: 8
                            val wakeMinute = wakeDoc.getLong("wakeMinute")?.toInt() ?: 0
                            val sleepHour = sleepDoc.getLong("sleepHour")?.toInt() ?: 22
                            val sleepMinute = sleepDoc.getLong("sleepMinute")?.toInt() ?: 0

                            val prefs = getSharedPreferences("user_times", Context.MODE_PRIVATE)
                            prefs.edit()
                                .putInt("wakeHour", wakeHour)
                                .putInt("wakeMinute", wakeMinute)
                                .putInt("sleepHour", sleepHour)
                                .putInt("sleepMinute", sleepMinute)
                                .apply()

                            Log.d("EveningSelection", "User times saved to preferences")
                            Log.d("EveningSelection", "Wake time: $wakeHour:$wakeMinute")
                            Log.d("EveningSelection", "Sleep time: $sleepHour:$sleepMinute")

                            NotificationScheduler.scheduleRepeatingNotifications(this, 2 * 60 * 1000L)
                            Toast.makeText(this, "Notifications scheduled successfully", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
    }
}
