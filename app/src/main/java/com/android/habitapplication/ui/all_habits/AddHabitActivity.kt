package com.android.habitapplication.ui.all_habits

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.android.habitapplication.R
import com.android.habitapplication.model.AddHabit
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AddHabitActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var habitId: String
    private lateinit var habitTitle: String
    private lateinit var habitDesc: String
    private lateinit var taskInput: EditText
    private lateinit var addTaskBtn: Button
    private lateinit var tasksContainer: LinearLayout
    private val tasksList = mutableListOf<String>()
    private var selectedIconResId: Int = R.drawable.image

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_habit)
        supportActionBar?.hide()
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        if (auth.currentUser == null) {
            Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        habitId = intent.getStringExtra("habitId") ?: ""
        habitTitle = intent.getStringExtra("habitTitle") ?: ""
        habitDesc = intent.getStringExtra("habitDesc") ?: ""

        taskInput = findViewById(R.id.taskInput)
        addTaskBtn = findViewById(R.id.addTaskBtn)
        tasksContainer = findViewById(R.id.tasksContainer)

        val titleEditText: EditText = findViewById(R.id.habitNameEditText)
        val descEditText: EditText = findViewById(R.id.habitDescriptionEditText)

        if (habitId.isEmpty()) {
            titleEditText.setText("")
            descEditText.setText("")
        } else {
            titleEditText.setText(habitTitle)
            descEditText.setText(habitDesc)
            loadHabitTasks()
        }

        val icons = listOf(
            R.drawable.water_cup,
            R.drawable.yoga,
            R.drawable.book,
            R.drawable.lowebody_workout
        )

        val spinner: Spinner = findViewById(R.id.iconSpinner)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, icons)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedIconResId = icons[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Delete all tasks button functionality
        findViewById<Button>(R.id.resetBtn).setOnClickListener {
            if (tasksList.isEmpty()) {
                Toast.makeText(this, "No tasks to delete", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AlertDialog.Builder(this)
                .setTitle("Delete All Tasks")
                .setMessage("Are you sure you want to delete all tasks?")
                .setPositiveButton("Yes") { _, _ ->
                    // Clear UI
                    tasksContainer.removeAllViews()
                    
                    // Delete from database if editing existing habit
                    if (habitId.isNotEmpty()) {
                        val userId = auth.currentUser?.uid ?: return@setPositiveButton
                        val habitDocRef = db.collection("users")
                            .document(userId)
                            .collection("habits")
                            .document(habitId)
                            
                        habitDocRef.collection("tasks").get()
                            .addOnSuccessListener { snapshot ->
                                snapshot.documents.forEach { doc ->
                                    doc.reference.delete()
                                }
                            }
                    }
                    
                    // Clear list
                    tasksList.clear()
                    Toast.makeText(this, "All tasks deleted", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        addTaskBtn.setOnClickListener {
            val taskName = taskInput.text.toString().trim()
            if (taskName.isNotEmpty()) {
                if (!tasksList.contains(taskName)) {
                    tasksList.add(taskName)
                    addTaskToView(taskName, "", false)
                    taskInput.text.clear()
                } else {
                    Toast.makeText(this, "Task already exists", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val saveButton: Button = findViewById(R.id.saveHabitButton)
        saveButton.setOnClickListener {
            val title = titleEditText.text.toString()
            val desc = descEditText.text.toString()
            val userId = auth.currentUser?.uid ?: return@setOnClickListener

            if (habitId.isEmpty()) {
                habitId = db.collection("users").document(userId).collection("habits").document().id
            }

            val habitDocRef = db.collection("users").document(userId).collection("habits").document(habitId)

            val habit = AddHabit(
                id = habitId,
                title = title,
                description = desc,
                icon = selectedIconResId.toString(),
                selectedDate = Calendar.getInstance().timeInMillis
            )

            val batch = db.batch()
            batch.set(habitDocRef, habit)

            tasksList.forEach { taskName ->
                val taskDocRef = habitDocRef.collection("tasks").document()
                batch.set(taskDocRef, mapOf(
                    "name" to taskName,
                    "completions" to mapOf<String, Boolean>()
                ))
            }

            batch.commit()
                .addOnSuccessListener {
                    val resultIntent = Intent()
                    resultIntent.putExtra("updatedHabit", habit)
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to save habit", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun loadHabitTasks() {
        val userId = auth.currentUser?.uid ?: return
        val habitDocRef = db.collection("users").document(userId).collection("habits").document(habitId)

        habitDocRef.collection("tasks").get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot) {
                    val taskName = document.getString("name") ?: continue
                    val completions = document.get("completions") as? Map<String, Boolean>
                    addTaskToView(taskName, document.id, completions?.values?.any { it } ?: false)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed loading the tasks", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addTaskToView(taskName: String, taskId: String, isChecked: Boolean) {
        val checkBox = CheckBox(this)
        checkBox.text = taskName
        checkBox.isChecked = isChecked

        // عند الضغط على الـ checkbox، نحدث حالته
        checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (taskId.isNotEmpty()) {
                val userId = auth.currentUser?.uid ?: return@setOnCheckedChangeListener
                val taskRef = db.collection("users")
                    .document(userId)
                    .collection("habits")
                    .document(habitId)
                    .collection("tasks")
                    .document(taskId)

                val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                taskRef.update("completions.$todayDate", isChecked)
            }
        }

        // حذف المهمة عند الضغط المطول
        checkBox.setOnLongClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete task")
                .setMessage("Are you sure you want to delete this task?")
                .setPositiveButton("Yes") { _, _ ->
                    tasksContainer.removeView(checkBox)
                    tasksList.remove(taskName)
                    if (taskId.isNotEmpty()) {
                        val userId = auth.currentUser?.uid ?: return@setPositiveButton
                        val taskRef = db.collection("users")
                            .document(userId)
                            .collection("habits")
                            .document(habitId)
                            .collection("tasks")
                            .document(taskId)
                        taskRef.delete()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
            true
        }

        tasksContainer.addView(checkBox)
    }
}
