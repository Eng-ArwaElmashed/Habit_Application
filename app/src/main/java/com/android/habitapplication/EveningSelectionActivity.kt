package com.android.habitapplication

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.habitapplication.NotificationScheduler
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

        getStartedButton.setOnClickListener {
            setSleepAlarm()
            val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
            prefs.edit().putBoolean("setupCompleted", true).apply()
            startActivity(Intent(this, ChooseHabitActivity::class.java))
            finish()
        }
    }

    private fun setSleepAlarm() {
        calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, timePicker.hour)
            set(Calendar.MINUTE, timePicker.minute)
            set(Calendar.SECOND, 0)
        }

        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val sleepTime = hashMapOf(
                "sleepHour" to calendar.get(Calendar.HOUR_OF_DAY),
                "sleepMinute" to calendar.get(Calendar.MINUTE),
                "timestamp" to calendar.timeInMillis
            )
            FirebaseFirestore.getInstance().collection("userSleepTimes")
                .document(user.uid)
                .set(sleepTime)
                .addOnSuccessListener {
                    Toast.makeText(this, "Sleep time saved!", Toast.LENGTH_SHORT).show()
                    fetchTimesAndSchedule(user.uid)
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to save sleep time.", Toast.LENGTH_SHORT).show()
                }
        }

        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("type", "sleep")  // بنحدد نوع الإشعار
        }

        pendingIntent = PendingIntent.getBroadcast(
            this, 1, intent,  // نستخدم requestCode مختلف
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )

        Toast.makeText(this, "Sleep alarm set!", Toast.LENGTH_SHORT).show()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "sleep_channel",
                "Sleep Notification",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    private fun fetchTimesAndSchedule(userId: String) {
        val userDocRef = db.collection("Users").document(userId)

        userDocRef.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val wakeHour = (document.getLong("wakeHour") ?: 8).toInt()
                    val wakeMinute = (document.getLong("wakeMinute") ?: 0).toInt()
                    val sleepHour = (document.getLong("sleepHour") ?: 22).toInt()
                    val sleepMinute = (document.getLong("sleepMinute") ?: 0).toInt()

                    val prefs = getSharedPreferences("user_times", Context.MODE_PRIVATE)
                    prefs.edit()
                        .putInt("wakeHour", wakeHour)
                        .putInt("wakeMinute", wakeMinute)
                        .putInt("sleepHour", sleepHour)
                        .putInt("sleepMinute", sleepMinute)
                        .apply()

                    val intervalMillis = 2 * 60 * 1000L // كل دقيقتين للتجربة
                    NotificationScheduler.scheduleRepeatingNotifications(this, intervalMillis)

                    Toast.makeText(this, "Notifications scheduled successfully", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MorningSelectionActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching user data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}

