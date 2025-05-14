package com.android.habitapplication.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.android.habitapplication.LoginActivity
import com.android.habitapplication.R
import com.android.habitapplication.SignupActivity
import com.google.android.material.button.MaterialButton

class Onboarding3Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding3)

        supportActionBar?.hide()
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN

        val nextButton = findViewById<MaterialButton>(R.id.next_btn)
        nextButton.setOnClickListener {
            // Mark onboarding as completed
            getSharedPreferences("AppPrefs", MODE_PRIVATE)
                .edit()
                .putBoolean("onboardingCompleted", true)
                .apply()

            // Navigate to login
            startActivity(Intent(this, SignupActivity::class.java))
            finish()
        }
    }

    // Prevent going back during onboarding
    override fun onBackPressed() {
        // Do nothing
    }
}