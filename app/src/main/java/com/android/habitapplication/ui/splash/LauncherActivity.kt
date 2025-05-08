package com.android.habitapplication.ui.splash

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.android.habitapplication.MainActivity
import com.android.habitapplication.WelcomeActivity
import com.android.habitapplication.ui.onboarding.Onboarding1Activity
import com.google.firebase.auth.FirebaseAuth

class LauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // No delay, just immediately check the authentication status
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null && user.isEmailVerified) {
            // If the user is authenticated and email is verified
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            // If the user is not authenticated or email is not verified
            startActivity(Intent(this, WelcomeActivity::class.java))
        }
        finish() // Close LauncherActivity to prevent going back to it
    }
}
