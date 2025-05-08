package com.android.habitapplication.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.habitapplication.MainActivity
import com.android.habitapplication.R
import com.android.habitapplication.ui.onboarding.Onboarding1Activity
import com.google.firebase.auth.FirebaseAuth

class MainSplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main_splash)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //  تأخير بسيط عشان تدي شكل Splash Screen
        Handler(Looper.getMainLooper()).postDelayed({
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null && user.isEmailVerified) {
                // ✅ المستخدم مسجل دخول،
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                //  مش مسجل أو الإيميل مش متفعل
                startActivity(Intent(this, Onboarding1Activity::class.java))
            }
            finish() // end of splash
        }, 2000) // 2 ثانية انتظار
    }
}
