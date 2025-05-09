
package com.android.habitapplication.ui.splash

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.android.habitapplication.LoginActivity
import com.android.habitapplication.MainActivity
import com.android.habitapplication.MorningSelectionActivity
import com.android.habitapplication.WelcomeActivity
import com.google.firebase.auth.FirebaseAuth
//
//class LauncherActivity : AppCompatActivity() {
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        // No delay, just immediately check the authentication status
//        val user = FirebaseAuth.getInstance().currentUser
//        if (user != null && user.isEmailVerified) {
//            // If the user is authenticated and email is verified
//            startActivity(Intent(this, MainActivity::class.java))
//        } else {
//            // If the user is not authenticated or email is not verified
//            startActivity(Intent(this, WelcomeActivity::class.java))
//        }
//        finish() // Close LauncherActivity to prevent going back to it
//    }
//}
class LauncherActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val isOnboardingDone = prefs.getBoolean("onboardingCompleted", false)
        val isSetupDone = prefs.getBoolean("setupCompleted", false)
        val user = FirebaseAuth.getInstance().currentUser

        when {
            user == null && !isOnboardingDone -> {
                // First-time user
                startActivity(Intent(this, WelcomeActivity::class.java))
            }
            user == null -> {
                // User skipped login last time
                startActivity(Intent(this, LoginActivity::class.java))
            }
            !isSetupDone -> {
                // Logged in but didn't finish wake/evening setup
                startActivity(Intent(this, MorningSelectionActivity::class.java))
            }
            else -> {
                // All done
                startActivity(Intent(this, MainActivity::class.java))
            }
        }

        finish()
    }
}
