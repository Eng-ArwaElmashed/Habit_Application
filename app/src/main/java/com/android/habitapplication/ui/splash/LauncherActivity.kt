package com.android.habitapplication.ui.splash

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.android.habitapplication.LoginActivity
import com.android.habitapplication.MainActivity
import com.android.habitapplication.MorningSelectionActivity
import com.android.habitapplication.WelcomeActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LauncherActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val isOnboardingDone = prefs.getBoolean("onboardingCompleted", false)
        val isSetupDone = prefs.getBoolean("setupCompleted", false)
        val user = FirebaseAuth.getInstance().currentUser
        val db = FirebaseFirestore.getInstance()

        Log.d("LauncherActivity", "Debug values:")
        Log.d("LauncherActivity", "isOnboardingDone: $isOnboardingDone")
        Log.d("LauncherActivity", "isSetupDone: $isSetupDone")
        Log.d("LauncherActivity", "user: ${user?.uid}")

        when {
            user == null && !isOnboardingDone -> {
                // First-time user
                Log.d("LauncherActivity", "Going to WelcomeActivity")
                startActivity(Intent(this, WelcomeActivity::class.java))
            }
            user == null -> {
                // User skipped login last time
                Log.d("LauncherActivity", "Going to LoginActivity")
                startActivity(Intent(this, LoginActivity::class.java))
            }
            else -> {
                // Check if user has completed setup in Firestore
                db.collection("userWakeTimes").document(user.uid)
                    .get()
                    .addOnSuccessListener { wakeDoc ->
                        db.collection("userSleepTimes").document(user.uid)
                            .get()
                            .addOnSuccessListener { sleepDoc ->
                                if (wakeDoc.exists() && sleepDoc.exists() && isSetupDone) {
                                    // All setup is complete
                                    Log.d("LauncherActivity", "Going to MainActivity")
                                    startActivity(Intent(this, MainActivity::class.java))
                                } else {
                                    // Setup not complete
                                    Log.d("LauncherActivity", "Going to MorningSelectionActivity")
                                    startActivity(Intent(this, MorningSelectionActivity::class.java))
                                }
                                finish()
                            }
                            .addOnFailureListener {
                                // On error, assume setup is not complete
                                Log.d("LauncherActivity", "Error checking sleep time, going to MorningSelectionActivity")
                                startActivity(Intent(this, MorningSelectionActivity::class.java))
                                finish()
                            }
                    }
                    .addOnFailureListener {
                        // On error, assume setup is not complete
                        Log.d("LauncherActivity", "Error checking wake time, going to MorningSelectionActivity")
                        startActivity(Intent(this, MorningSelectionActivity::class.java))
                        finish()
                    }
            }
        }
    }
}