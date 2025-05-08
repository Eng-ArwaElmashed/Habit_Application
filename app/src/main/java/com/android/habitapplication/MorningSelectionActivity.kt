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

class MorningSelectionActivity : AppCompatActivity() {

    private lateinit var alarmManager: AlarmManager
    private lateinit var pendingIntent: PendingIntent
    private lateinit var calendar: Calendar
    private lateinit var timePicker: TimePicker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        setContentView(R.layout.activity_morning_selection)

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
        calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, timePicker.hour)
            set(Calendar.MINUTE, timePicker.minute)
            set(Calendar.SECOND, 0)
        }

        // تخزين الوقت في Firestore
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val wakeTime = hashMapOf(
                "wakeHour" to calendar.get(Calendar.HOUR_OF_DAY),
                "wakeMinute" to calendar.get(Calendar.MINUTE),
                "timestamp" to calendar.timeInMillis
            )
            FirebaseFirestore.getInstance().collection("userWakeTimes")
                .document(user.uid)
                .set(wakeTime)
                .addOnSuccessListener {
                    Toast.makeText(this, "Wake time saved!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to save wake time.", Toast.LENGTH_SHORT).show()
                }
        }

        // ضبط المنبه
        val intent = Intent(this, AlarmReceiver::class.java)
        pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )

        Toast.makeText(this, "Alarm set successfully!", Toast.LENGTH_SHORT).show()


    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "wake_channel",
                "Wake Up Notification",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
