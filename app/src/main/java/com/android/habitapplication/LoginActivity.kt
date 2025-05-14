package com.android.habitapplication
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.habitapplication.NotificationScheduler
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 1001

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        supportActionBar?.hide()
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.insetsController?.hide(android.view.WindowInsets.Type.statusBars())
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        }

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // إعداد Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val googleLoginBtn = findViewById<Button>(R.id.btnGoogleLogin)

        googleLoginBtn.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }

        var isPasswordVisible = false
        val toggle = findViewById<ImageView>(R.id.togglePasswordVisibility)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val loginBtn = findViewById<Button>(R.id.btnLogin)
        val notUser = findViewById<TextView>(R.id.tvSignIn)
        val backBtn = findViewById<ImageButton>(R.id.btnBack)
        val forgetPassword = findViewById<TextView>(R.id.forget)

        toggle.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                etPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                toggle.setImageResource(R.drawable.ic_baseline_eye_24)
            } else {
                etPassword.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                toggle.setImageResource(R.drawable.baseline_visibility_off_24)
            }
            // Move cursor to end of text
            etPassword.setSelection(etPassword.text.length)
        }

        // تسجيل الدخول العادي
        loginBtn.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()


            if (email.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    val user = auth.currentUser
                    if (user != null && user.isEmailVerified) {
                        Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()
                        checkSetupStatus(user.uid)
                    } else {
                        Toast.makeText(this, "Check your email for verification", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Login failed: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }

        forgetPassword.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isBlank()) {
                Toast.makeText(this, "Enter your email", Toast.LENGTH_SHORT).show()
            } else {
                auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Check your email to reset password", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        notUser.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
            finish()
        }

        backBtn.setOnClickListener {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        }
    }

    // النتيجة من Google Sign-In
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                Toast.makeText(this, "Google Sign In failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user: FirebaseUser? = auth.currentUser
                    Toast.makeText(this, "Welcome, ${user?.displayName}", Toast.LENGTH_SHORT).show()
                    if (user != null) {
                        checkSetupStatus(user.uid)
                    }
                } else {
                    Toast.makeText(this, "Firebase Auth failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun checkSetupStatus(userId: String) {
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val isSetupDone = prefs.getBoolean("setupCompleted", false)

        Log.d("LoginActivity", "Checking setup status:")
        Log.d("LoginActivity", "Local isSetupDone: $isSetupDone")

        // First check if setup is marked as complete in Firestore
        db.collection("userSetup").document(userId)
            .get()
            .addOnSuccessListener { setupDoc ->
                val isSetupCompleteInFirestore = setupDoc.getBoolean("setupCompleted") ?: false
                Log.d("LoginActivity", "Firestore setup status: $isSetupCompleteInFirestore")

                if (isSetupCompleteInFirestore) {
                    // If setup is complete in Firestore, ensure local preference is also set
                    if (!isSetupDone) {
                        prefs.edit().putBoolean("setupCompleted", true).apply()
                        Log.d("LoginActivity", "Updated local setupCompleted flag to true")
                    }

                    // Verify wake and sleep times exist
                    db.collection("userWakeTimes").document(userId)
                        .get()
                        .addOnSuccessListener { wakeDoc ->
                            db.collection("userSleepTimes").document(userId)
                                .get()
                                .addOnSuccessListener { sleepDoc ->
                                    if (wakeDoc.exists() && sleepDoc.exists()) {
                                        Log.d("LoginActivity", "All setup complete, going to MainActivity")
                                        startActivity(Intent(this, MainActivity::class.java))
                                    } else {
                                        Log.d("LoginActivity", "Missing wake/sleep times, going to MorningSelectionActivity")
                                        startActivity(Intent(this, MorningSelectionActivity::class.java))
                                    }
                                    finish()
                                }
                                .addOnFailureListener {
                                    Log.d("LoginActivity", "Error checking sleep time, going to MorningSelectionActivity")
                                    startActivity(Intent(this, MorningSelectionActivity::class.java))
                                    finish()
                                }
                        }
                        .addOnFailureListener {
                            Log.d("LoginActivity", "Error checking wake time, going to MorningSelectionActivity")
                            startActivity(Intent(this, MorningSelectionActivity::class.java))
                            finish()
                        }
                } else {
                    Log.d("LoginActivity", "Setup not complete in Firestore, going to MorningSelectionActivity")
                    startActivity(Intent(this, MorningSelectionActivity::class.java))
                    finish()
                }
            }
            .addOnFailureListener {
                Log.d("LoginActivity", "Error checking setup status, going to MorningSelectionActivity")
                startActivity(Intent(this, MorningSelectionActivity::class.java))
                finish()
            }
    }
}
