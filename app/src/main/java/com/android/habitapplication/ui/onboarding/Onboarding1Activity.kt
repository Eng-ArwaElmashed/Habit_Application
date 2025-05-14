package com.android.habitapplication.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.android.habitapplication.ui.onboarding.Onboarding2Activity
import com.android.habitapplication.R
import com.google.android.material.button.MaterialButton

class Onboarding1Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding1)

        supportActionBar?.hide()
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN

        val nextButton = findViewById<MaterialButton>(R.id.next_btn)
        nextButton.setOnClickListener {
            // Navigate to next onboarding screen
            startActivity(Intent(this, Onboarding2Activity::class.java))
            finish()
        }
    }


}