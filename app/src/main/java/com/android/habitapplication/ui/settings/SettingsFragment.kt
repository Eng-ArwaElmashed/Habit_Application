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
import androidx.fragment.app.Fragment
import com.android.habitapplication.LoginActivity
import com.android.habitapplication.MainActivity
import com.android.habitapplication.databinding.FragmentSettingsBinding
import com.android.habitapplication.ui.profile.HomeViewModel
import com.android.habitapplication.ui.profile.ProfileFragment
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
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }

        binding.profileBtn.setOnClickListener {
            val intent = Intent(requireContext(), Profile::class.java)
            startActivity(intent)
        }
        binding.soundSwitch.setOnCheckedChangeListener { _, isChecked ->
            val audioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (isChecked) {
                // تشغيل الصوت – نرفعه لأقصى درجة
                val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)
                Toast.makeText(requireContext(), "Sound ON", Toast.LENGTH_SHORT).show()
            } else {
                // كتم الصوت
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
                Toast.makeText(requireContext(), "Sound OFF", Toast.LENGTH_SHORT).show()
            }
        }

        // Vacation Mode Switch logic
        binding.vacationSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Toast.makeText(requireContext(), "Vacation mode enabled", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Vacation mode disabled", Toast.LENGTH_SHORT).show()
            }
        }
        binding.loginWithWebBtn.setOnClickListener {
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
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
