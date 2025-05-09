package com.android.habitapplication.ui.settings

import android.content.Context
import android.content.Intent
import android.media.AudioManager
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
import com.android.habitapplication.MainActivity
import com.android.habitapplication.databinding.FragmentSettingsBinding
import com.android.habitapplication.ui.profile.HomeViewModel
import com.android.habitapplication.ui.profile.ProfileFragment
import com.android.habitapplication.NotificationScheduler
import com.google.firebase.auth.FirebaseAuth

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


        // Vacation Mode Switch logic
        binding.vacationSwitch.setOnCheckedChangeListener { _, isChecked ->
            val sharedPref = requireContext().getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                putBoolean("isVacationModeOn", isChecked)
                apply()
            }

            if (isChecked) {
                NotificationScheduler.cancelNotifications(requireContext())
                Toast.makeText(requireContext(), "Vacation mode enabled: Notifications disabled", Toast.LENGTH_SHORT).show()
            } else {
                NotificationScheduler.scheduleRepeatingNotifications(requireContext(), intervalMillis =2 * 60 * 1000L )
                Toast.makeText(requireContext(), "Vacation mode disabled: Notifications resumed", Toast.LENGTH_SHORT).show()
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


        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
