package com.android.habitapplication.ui.settings

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
import android.app.AlarmManager
import android.app.PendingIntent
import com.android.habitapplication.ui.AlarmReceiver
import java.util.*
import com.google.firebase.firestore.FirebaseFirestore

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val view = binding.root

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

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

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
                        val morningIntent = Intent(requireContext(), AlarmReceiver::class.java)
                        val morningPendingIntent = PendingIntent.getBroadcast(
                            requireContext(),
                            0,
                            morningIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        alarmManager.cancel(morningPendingIntent)
                        
                        // Cancel evening alarm
                        val eveningIntent = Intent(requireContext(), AlarmReceiver::class.java).apply {
                            putExtra("type", "sleep")
                        }
                        val eveningPendingIntent = PendingIntent.getBroadcast(
                            requireContext(),
                            1,
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

                        // Only schedule if we're between wake and sleep times
                        if (now in wake until sleep) {
                            NotificationScheduler.scheduleRepeatingNotifications(requireContext(), 2 * 60 * 1000L)
                            
                            // Reschedule morning and evening alarms
                            val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
                            
                            // Reschedule morning alarm
                            val morningIntent = Intent(requireContext(), AlarmReceiver::class.java)
                            val morningPendingIntent = PendingIntent.getBroadcast(
                                requireContext(),
                                0,
                                morningIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                            val morningCal = Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, prefs.getInt("wakeHour", 8))
                                set(Calendar.MINUTE, prefs.getInt("wakeMinute", 0))
                                set(Calendar.SECOND, 0)
                                if (before(Calendar.getInstance())) {
                                    add(Calendar.DAY_OF_YEAR, 1)
                                }
                            }
                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                morningCal.timeInMillis,
                                morningPendingIntent
                            )
                            
                            // Reschedule evening alarm
                            val eveningIntent = Intent(requireContext(), AlarmReceiver::class.java).apply {
                                putExtra("type", "sleep")
                            }
                            val eveningPendingIntent = PendingIntent.getBroadcast(
                                requireContext(),
                                1,
                                eveningIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                            val eveningCal = Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, prefs.getInt("sleepHour", 22))
                                set(Calendar.MINUTE, prefs.getInt("sleepMinute", 0))
                                set(Calendar.SECOND, 0)
                                if (before(Calendar.getInstance())) {
                                    add(Calendar.DAY_OF_YEAR, 1)
                                }
                            }
                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                eveningCal.timeInMillis,
                                eveningPendingIntent
                            )

                            Toast.makeText(requireContext(), "Vacation mode disabled: All notifications resumed", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), "Vacation mode disabled: Notifications will resume at wake time", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
        }

        binding.rateUsBtn.setOnClickListener {
            val packageName = requireContext().packageName
            try {
                // افتح التطبيق في Google Play app
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
                startActivity(intent)
            } catch (e: android.content.ActivityNotFoundException) {
                // لو Play Store app مش موجود، افتح في المتصفح
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
                startActivity(intent)
            }
        }

        binding.shareAppBtn.setOnClickListener {
            val packageName = requireContext().packageName
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(
                    Intent.EXTRA_TEXT,
                    "جرب هذا التطبيق الرائع: https://play.google.com/store/apps/details?id=$packageName"
                )
                type = "text/plain"
            }
            startActivity(Intent.createChooser(shareIntent, "Share App via"))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
