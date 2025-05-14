package com.android.habitapplication.ui.settings

import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract.Profile
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.android.habitapplication.LoginActivity
import com.android.habitapplication.databinding.FragmentSettingsBinding
import com.android.habitapplication.NotificationScheduler
import com.google.firebase.auth.FirebaseAuth
import android.app.PendingIntent
import com.android.habitapplication.ui.AlarmReceiver
import java.util.*
import com.google.firebase.firestore.FirebaseFirestore
import android.os.Build
import android.util.Log

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: FirebaseFirestore
    private lateinit var alarmManager: AlarmManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val view = binding.root

        db = FirebaseFirestore.getInstance()
        alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager

        binding.logoutBtn.setOnClickListener {
            // Sign out from Firebase
            FirebaseAuth.getInstance().signOut()

            // Clear SharedPreferences flags (if used)
            val prefs = requireContext().getSharedPreferences("AppPrefs", AppCompatActivity.MODE_PRIVATE)
            prefs.edit().clear().apply()

            // Redirect to Login and clear back stack
            val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)

            // Finish the current activity
            requireActivity().finish()
        }

        binding.profileBtn.setOnClickListener {
            val intent = Intent(requireContext(), Profile::class.java)
            startActivity(intent)
        }

        // Set up morning time change button
        binding.morningTimeBtn.setOnClickListener {
            showTimePickerDialog(true)
            val intent = Intent(requireContext(), AlarmReceiver::class.java).apply {
            putExtra("type", "wake")
            putExtra("title", "Wake Up Time!")
            putExtra("message", "Time to start your day!")
            putExtra("channelId", "wake_channel")
            putExtra("notificationId", 1)
        }
        }

        // Set up evening time change button
        binding.eveningTimeBtn.setOnClickListener {
            showTimePickerDialog(false)
            val intent = Intent(requireContext(), AlarmReceiver::class.java).apply {
            putExtra("type", "sleep")
            putExtra("title", "Sleep Time!")
            putExtra("message", "Time to review your day and prepare for tomorrow!")
            putExtra("channelId", "sleep_channel")
            putExtra("notificationId", 2)
        }
        }

        return view
    }

    private fun showTimePickerDialog(isMorning: Boolean) {
        val prefs = requireContext().getSharedPreferences("user_times", Context.MODE_PRIVATE)
        val currentHour = if (isMorning) prefs.getInt("wakeHour", 8) else prefs.getInt("sleepHour", 22)
        val currentMinute = if (isMorning) prefs.getInt("wakeMinute", 0) else prefs.getInt("sleepMinute", 0)

        TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                updateTime(isMorning, hourOfDay, minute)
            },
            currentHour,
            currentMinute,
            true
        ).show()
    }

    private fun updateTime(isMorning: Boolean, hour: Int, minute: Int) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val prefs = requireContext().getSharedPreferences("user_times", Context.MODE_PRIVATE)
        val collection = if (isMorning) "userWakeTimes" else "userSleepTimes"
        val timeField = if (isMorning) "wake" else "sleep"

        // Update Firestore
        val timeData = hashMapOf(
            "${timeField}Hour" to hour,
            "${timeField}Minute" to minute,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection(collection)
            .document(user.uid)
            .set(timeData)
            .addOnSuccessListener {
                // Update local preferences
                prefs.edit()
                    .putInt("${timeField}Hour", hour)
                    .putInt("${timeField}Minute", minute)
                    .apply()

                // Update alarm
                updateAlarm(isMorning, hour, minute)

                Toast.makeText(
                    requireContext(),
                    "${if (isMorning) "Morning" else "Evening"} time updated to ${String.format("%02d:%02d", hour, minute)}",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener {
                Toast.makeText(
                    requireContext(),
                    "Failed to update time",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun updateAlarm(isMorning: Boolean, hour: Int, minute: Int) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If the time has already passed today, set it for tomorrow
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            Log.d("SettingsFragment", "Time has passed, setting for tomorrow")
        }

        val intent = Intent(requireContext(), AlarmReceiver::class.java).apply {
            putExtra("type", if (isMorning) "wake" else "sleep")
            putExtra("title", if (isMorning) "Wake Up Time!" else "Sleep Time!")
            putExtra("message", if (isMorning) "Time to start your day!" else "Time to review your day and prepare for tomorrow!")
            putExtra("channelId", if (isMorning) "wake_channel" else "sleep_channel")
            putExtra("notificationId", if (isMorning) 1 else 2)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            if (isMorning) 1 else 2,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        try {
            // Cancel existing alarm
            alarmManager.cancel(pendingIntent)
            Log.d("SettingsFragment", "Cancelled existing alarm")

            // Set new alarm using setAlarmClock for better reliability
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent),
                    pendingIntent
                )
                Log.d("SettingsFragment", "Alarm clock set for: ${calendar.time}")
            } else {
                // For older Android versions, use setRepeating
                val interval = AlarmManager.INTERVAL_DAY
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    interval,
                    pendingIntent
                )
                Log.d("SettingsFragment", "Repeating alarm set for: ${calendar.time}")
            }

            Toast.makeText(
                requireContext(),
                "${if (isMorning) "Morning" else "Evening"} alarm set for ${String.format("%02d:%02d", hour, minute)}",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Log.e("SettingsFragment", "Error setting alarm: ${e.message}")
            Toast.makeText(
                requireContext(),
                "Error setting alarm: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val user = FirebaseAuth.getInstance().currentUser ?: return

        // Initialize vacation mode switch state from Firestore
        db.collection("userSettings")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                val isVacationModeOn = document.getBoolean("isVacationModeOn") ?: false
                binding.vacationSwitch.isChecked = isVacationModeOn
            }

        // Vacation Mode Switch logic
        binding.vacationSwitch.setOnCheckedChangeListener { _, isChecked ->
            // Save vacation mode state to Firestore
            db.collection("userSettings")
                .document(user.uid)
                .set(hashMapOf("isVacationModeOn" to isChecked))
                .addOnSuccessListener {
                    if (isChecked) {
                        // Cancel all notifications when vacation mode is enabled
                        NotificationScheduler.cancelNotifications(requireContext())
                        
                        // Cancel morning and evening alarms
                        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
                        
                        // Cancel morning alarm
                        val morningIntent = Intent(requireContext(), AlarmReceiver::class.java).apply {
                            action = "com.android.habitapplication.ALARM_WAKE"
                            putExtra("type", "wake")
                        }
                        val morningPendingIntent = PendingIntent.getBroadcast(
                            requireContext(),
                            1,
                            morningIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        alarmManager.cancel(morningPendingIntent)
                        
                        // Cancel evening alarm
                        val eveningIntent = Intent(requireContext(), AlarmReceiver::class.java).apply {
                            action = "com.android.habitapplication.ALARM_SLEEP"
                            putExtra("type", "sleep")
                        }
                        val eveningPendingIntent = PendingIntent.getBroadcast(
                            requireContext(),
                            2,
                            eveningIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        alarmManager.cancel(eveningPendingIntent)

                        Toast.makeText(requireContext(), "Vacation mode enabled: All notifications disabled", Toast.LENGTH_SHORT).show()
                    } else {
                        // Reschedule notifications when vacation mode is disabled
                        val prefs = requireContext().getSharedPreferences("user_times", Context.MODE_PRIVATE)
                        val wake = prefs.getInt("wakeHour", 8) * 60 + prefs.getInt("wakeMinute", 0)
                        val sleep = prefs.getInt("sleepHour", 22) * 60 + prefs.getInt("sleepMinute", 0)
                        
                        val cal = Calendar.getInstance()
                        val now = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

                        // Schedule notifications if within active hours
                        if (now in wake until sleep) {
                            NotificationScheduler.scheduleRandomNotifications(requireContext())
                            Toast.makeText(requireContext(), "Notifications rescheduled with new times", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), "Notifications will start at next wake time", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
        }

        binding.rateUsBtn.setOnClickListener {
            val packageName = requireContext().packageName
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
                startActivity(intent)
            } catch (e: android.content.ActivityNotFoundException) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
                startActivity(intent)
            }
        }

        binding.shareAppBtn.setOnClickListener {
            val packageName = requireContext().packageName
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Check out this awesome habit tracking app: https://play.google.com/store/apps/details?id=$packageName")
            }
            startActivity(Intent.createChooser(shareIntent, "Share via"))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
