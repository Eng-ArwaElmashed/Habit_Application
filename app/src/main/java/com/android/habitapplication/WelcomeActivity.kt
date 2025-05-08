package com.android.habitapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.habitapplication.ui.onboarding.Onboarding1Activity
import com.android.habitapplication.ui.today.TodayFragment
import com.google.firebase.auth.FirebaseAuth

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



        enableEdgeToEdge()
        setContentView(R.layout.activity_welcome)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        supportActionBar?.hide()
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN

        val getStartedBtn = findViewById<Button>(R.id.btnGetStarted)
        getStartedBtn.setOnClickListener {
            startActivity(Intent(this, Onboarding1Activity::class.java))
            finish()
        }
    }
}
