package com.android.habitapplication.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import calculateStreak
import com.android.habitapplication.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val firestore = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return root

        val habitId = "habit1"

        firestore.collection("users")
            .document(userId)
            .collection("habits")
            .document(habitId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val completedDates = document["completedDates"] as? List<String> ?: emptyList()
                    val streak = calculateStreak(completedDates)
                    binding.streakTextView.text = "🔥 Streak: $streak Days"
                }
            }
            .addOnFailureListener {
                binding.streakTextView.text = "Failed to Load the data"
            }

        val db = FirebaseFirestore.getInstance()

        if (userId != null) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val username = document.getString("username") ?: "User"
                        val email = document.getString("email") ?: "example@email.com"
                        val totalHours = document.getLong("total_hours") ?: 0
                        val tasksCompleted = document.getLong("tasks_completed") ?: 0
                        val longestStreak = document.getLong("longest_streak") ?: 0

                        binding.tv.text = username
                        binding.tv2.text = email
                        binding.tv4.text = totalHours.toString()
                        binding.tv6.text = tasksCompleted.toString()
                     // binding.longestStreakText.text = "$longestStreak Days"
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to load profile", Toast.LENGTH_SHORT).show()
                }
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
