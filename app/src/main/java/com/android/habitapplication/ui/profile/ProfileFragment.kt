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

        // Check if we should update streak based on sleep time
        checkIfShouldUpdateStreak(userId, firestore)

        return root
    }

    private fun checkIfShouldUpdateStreak(userId: String, firestore: FirebaseFirestore) {
        // First get the user's sleep time
        firestore.collection("userSleepTimes").document(userId)
            .get()
            .addOnSuccessListener { sleepDoc ->
                if (sleepDoc.exists()) {
                    val sleepHour = sleepDoc.getLong("hour") ?: 22 // Default to 10 PM if not set
                    val sleepMinute = sleepDoc.getLong("minute") ?: 0

                    val now = Calendar.getInstance()
                    val currentHour = now.get(Calendar.HOUR_OF_DAY)
                    val currentMinute = now.get(Calendar.MINUTE)

                    // Check if we're approaching sleep time (within 2 hours before sleep time)
                    val isNearSleepTime = isApproachingSleepTime(currentHour, currentMinute, sleepHour.toInt(), sleepMinute.toInt())
                    
                    if (isNearSleepTime) {
                        Log.d(TAG, "Near sleep time, checking streak")
                        checkHabitCompletionAndUpdateStreak(userId, firestore)
                    } else {
                        Log.d(TAG, "Not near sleep time yet, streak will be updated later")
                        Toast.makeText(context, "Streak will be updated before your sleep time", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private fun isApproachingSleepTime(currentHour: Int, currentMinute: Int, sleepHour: Int, sleepMinute: Int): Boolean {
        // Convert everything to minutes for easier comparison
        val currentTimeInMinutes = currentHour * 60 + currentMinute
        val sleepTimeInMinutes = sleepHour * 60 + sleepMinute
        
        // Consider the case where sleep time might be on the next day (e.g., 1 AM)
        val adjustedSleepTime = if (sleepTimeInMinutes < 4 * 60) { // If sleep time is before 4 AM
            sleepTimeInMinutes + 24 * 60 // Add 24 hours
        } else {
            sleepTimeInMinutes
        }

        val adjustedCurrentTime = if (currentHour < 4) { // If current time is before 4 AM
            currentTimeInMinutes + 24 * 60 // Add 24 hours
        } else {
            currentTimeInMinutes
        }

        // Check if we're within 2 hours before sleep time
        val twoHoursInMinutes = 2 * 60
        val timeUntilSleep = adjustedSleepTime - adjustedCurrentTime

        Log.d(TAG, "Time until sleep: $timeUntilSleep minutes")
        return timeUntilSleep in 0..twoHoursInMinutes
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

        // Check if we've already updated the streak today
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { userDoc ->
                val lastStreakUpdate = userDoc.getString("last_streak_update")
                
                if (lastStreakUpdate != todayFormatted) {
                    checkHabitsAndUpdateStreak(userId, firestore, todayFormatted)
                } else {
                    Log.d(TAG, "Streak already updated today")
                    Toast.makeText(context, "Streak already updated for today", Toast.LENGTH_SHORT).show()
                }
            }
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
