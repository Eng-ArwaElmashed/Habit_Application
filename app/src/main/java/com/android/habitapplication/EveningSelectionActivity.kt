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
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class EveningSelectionActivity : AppCompatActivity() {

    private lateinit var alarmManager: AlarmManager
    private lateinit var pendingIntent: PendingIntent
    private lateinit var calendar: Calendar
    private lateinit var timePicker: TimePicker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        setContentView(R.layout.activity_evening_selection)

        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        timePicker = findViewById(R.id.time_pk)
        val getStartedButton: MaterialButton = findViewById(R.id.get_started_btn)

        createNotificationChannel()

        getStartedButton.setOnClickListener {
            setSleepAlarm()
            startActivity(Intent(this, MainActivity::class.java))
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
}
