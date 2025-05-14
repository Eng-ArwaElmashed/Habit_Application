package com.android.habitapplication.ui.profile

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import calculateStreak
import com.android.habitapplication.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private var currentStreak = 0
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val TAG = "ProfileFragment"

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

        // Load user profile data first
        loadUserProfile(userId, firestore)

        // Then check habit completion percentage and update streak
        checkHabitCompletionAndUpdateStreak(userId, firestore)

        return root
    }

    private fun checkHabitCompletionAndUpdateStreak(userId: String, firestore: FirebaseFirestore) {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        
        val todayFormatted = dateFormat.format(today)
        Log.d(TAG, "Checking streak for date: $todayFormatted")

        // Directly proceed to check habits without checking last update time
        checkHabitsAndUpdateStreak(userId, firestore, todayFormatted)
    }

    private fun checkHabitsAndUpdateStreak(userId: String, firestore: FirebaseFirestore, todayFormatted: String) {
        firestore.collection("users").document(userId)
            .collection("habits")
            .get()
            .addOnSuccessListener { habits ->
                if (!habits.isEmpty) {
                    var totalHabitTasksCompleted = 0
                    var totalHabitTasks = 0
                    var habitsProcessed = 0

                    Log.d(TAG, "Found ${habits.size()} habits to process")

                    for (habit in habits.documents) {
                        // Get tasks for this habit
                        habit.reference.collection("tasks")
                            .get()
                            .addOnSuccessListener { tasks ->
                                val totalTasks = tasks.size()
                                var completedTasks = 0

                                for (task in tasks) {
                                    val completions = task.get("completions") as? Map<String, Boolean>
                                    if (completions?.get(todayFormatted) == true) {
                                        completedTasks++
                                    }
                                }

                                totalHabitTasks += totalTasks
                                totalHabitTasksCompleted += completedTasks
                                habitsProcessed++

                                Log.d(TAG, "Habit ${habit.id}: Completed $completedTasks/$totalTasks tasks")

                                // Once all habits are processed, calculate the overall completion percentage
                                if (habitsProcessed == habits.size()) {
                                    val completionPercentage = if (totalHabitTasks > 0) {
                                        (totalHabitTasksCompleted.toFloat() / totalHabitTasks.toFloat()) * 100
                                    } else {
                                        0f
                                    }

                                    Log.d(TAG, "Final completion: $totalHabitTasksCompleted/$totalHabitTasks tasks ($completionPercentage%)")

                                    updateStreakInFirestore(userId, firestore, completionPercentage >= 75, todayFormatted)
                                }
                            }
                    }
                } else {
                    Log.d(TAG, "No habits found")
                }
            }
    }

    private fun updateStreakInFirestore(userId: String, firestore: FirebaseFirestore, isCompleted: Boolean, todayFormatted: String) {
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { userDoc ->
                val currentStreak = userDoc.getLong("current_streak") ?: 0
                val longestStreak = userDoc.getLong("longest_streak") ?: 0
                val lastUpdate = userDoc.getString("last_streak_update")
                
                Log.d(TAG, "Before update - Current streak: $currentStreak, Longest streak: $longestStreak, Last update: $lastUpdate")
                
                if (isCompleted) {
                    val newStreak = currentStreak + 1
                    val updates = mutableMapOf<String, Any>(
                        "current_streak" to newStreak,
                        "last_streak_update" to todayFormatted
                    )
                    
                    // Update longest streak if necessary
                    if (newStreak > longestStreak) {
                        updates["longest_streak"] = newStreak
                    }

                    Log.d(TAG, "Updating streak to $newStreak")

                    firestore.collection("users").document(userId)
                        .update(updates)
                        .addOnSuccessListener {
                            Log.d(TAG, "Successfully updated streak to $newStreak")
                            binding.streakTextView.text = "🔥 Streak: $newStreak Days"
                            showStreakAlert(newStreak)
                            
                            // Verify the update
                            firestore.collection("users").document(userId)
                                .get()
                                .addOnSuccessListener { updatedDoc ->
                                    val verifiedStreak = updatedDoc.getLong("current_streak") ?: 0
                                    Log.d(TAG, "Verified streak after update: $verifiedStreak")
                                }
                        }
                } else {
                    Log.d(TAG, "Resetting streak to 0")
                    val updates = mutableMapOf<String, Any>(
                        "current_streak" to 0,
                        "last_streak_update" to todayFormatted
                    )
                    
                    firestore.collection("users").document(userId)
                        .update(updates)
                        .addOnSuccessListener {
                            Log.d(TAG, "Successfully reset streak to 0")
                            binding.streakTextView.text = "🔥 Streak: 0 Days"
                        }
                }
            }
    }

    private fun showStreakAlert(streak: Long) {
        Toast.makeText(
            context,
            "🎉 Congratulations! You've completed more than 75% of your habits. Your streak is now $streak days!",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun loadUserProfile(userId: String, firestore: FirebaseFirestore) {
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val username = document.getString("username") ?: "User"
                    val email = document.getString("email") ?: "example@email.com"
                    val totalHours = document.getLong("total_hours") ?: 0
                    val tasksCompleted = document.getLong("tasks_completed") ?: 0
                    val currentStreak = document.getLong("current_streak") ?: 0

                    Log.d(TAG, "Loading profile - Current streak: $currentStreak")

                    binding.tv.text = username
                    binding.tv2.text = email
                    binding.tv4.text = totalHours.toString()
                    binding.tv6.text = tasksCompleted.toString()
                    binding.streakTextView.text = "🔥 Streak: $currentStreak Days"
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading profile", e)
                Toast.makeText(context, "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
