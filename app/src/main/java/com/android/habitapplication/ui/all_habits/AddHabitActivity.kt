package com.android.habitapplication.ui.all_habits

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.habitapplication.R

class AddHabitActivity : AppCompatActivity() {

    private lateinit var habitNameEditText: EditText
    private lateinit var saveHabitButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_habit)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        habitNameEditText = findViewById(R.id.habitNameEditText)
        saveHabitButton = findViewById(R.id.saveHabitButton)

        saveHabitButton.setOnClickListener {
            saveHabit()
        }
    }

    private fun saveHabit() {
        val habitName = habitNameEditText.text.toString().trim()

        if (habitName.isNotBlank()) {
            val habitProgress = "0%" // بداية مبدئية
            val habitImage = R.drawable.ic_add // صورة افتراضية (ممكن تغيريها لاحقاً)

            val resultIntent = Intent().apply {
                putExtra("habit_name", habitName)
                putExtra("habit_progress", habitProgress)
                putExtra("habit_image", habitImage)
            }

            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        } else {
            Toast.makeText(this, "Please enter a habit name", Toast.LENGTH_SHORT).show()
        }
    }
}
