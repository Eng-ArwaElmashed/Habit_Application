package com.android.habitapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class ChooseHabitActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_choose_habit)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        supportActionBar?.hide()
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN

        db = FirebaseFirestore.getInstance()
        val user = FirebaseAuth.getInstance().currentUser

        // Set up click listeners for all habit buttons
        setupHabitButton(R.id.drinking_water_btn, "Drinking Water", "Stay hydrated throughout the day", "water_cup")
        setupHabitButton(R.id.morning_walk_btn, "Morning Walk", "Start your day with a refreshing walk", "morning_walk")
        setupHabitButton(R.id.lowerbody_workout_btn, "Lowerbody Workout", "Strengthen your lower body", "lowebody_workout")
        setupHabitButton(R.id.cycling_btn, "Cycling", "Get your cardio in with cycling", "cycling_vector")
        setupHabitButton(R.id.yoga_btn, "Yoga", "Find your inner peace with yoga", "yoga")
        setupHabitButton(R.id.reading_btn, "Reading", "Expand your knowledge through reading", "book")
    }

    private fun setupHabitButton(buttonId: Int, title: String, description: String, iconName: String) {
        val button = findViewById<ImageButton>(buttonId)
        button.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                // Create a new habit document
                val habitId = UUID.randomUUID().toString()
                val habit = hashMapOf(
                    "id" to habitId,
                    "title" to title,
                    "description" to description,
                    "icon" to iconName,
                    "createdAt" to System.currentTimeMillis(),
                    "isActive" to true
                )

                // Save the habit to Firestore
                db.collection("users").document(user.uid)
                    .collection("habits").document(habitId)
                    .set(habit)
                    .addOnSuccessListener {
                        Log.d("ChooseHabit", "Habit saved successfully: $title")
                        
                        // Save setup completion to Firestore
                        val setupData = hashMapOf(
                            "setupCompleted" to true,
                            "timestamp" to System.currentTimeMillis()
                        )
                        
                        db.collection("userSetup").document(user.uid)
                            .set(setupData)
                            .addOnSuccessListener {
                                // Set local preference
                                val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
                                prefs.edit().putBoolean("setupCompleted", true).apply()
                                
                                Log.d("ChooseHabit", "Setup completion saved to Firestore")
                                Log.d("ChooseHabit", "Local setupCompleted flag set to true")
                                
                                // Start MainActivity and clear the back stack
                                val intent = Intent(this, MainActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Failed to save setup status", Toast.LENGTH_SHORT).show()
                                Log.e("ChooseHabit", "Failed to save setup status: ${it.message}")
                            }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to save habit: ${e.message}", Toast.LENGTH_SHORT).show()
                        Log.e("ChooseHabit", "Failed to save habit: ${e.message}")
                    }
            } else {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Override back button to prevent going back to EveningSelectionActivity
    override fun onBackPressed() {
        // Do nothing to prevent going back
    }
}