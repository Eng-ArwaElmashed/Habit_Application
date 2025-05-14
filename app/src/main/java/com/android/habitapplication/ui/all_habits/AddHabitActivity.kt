package com.android.habitapplication.ui.all_habits

import IconAdapter
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import com.android.habitapplication.R
import com.android.habitapplication.model.AddHabit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import java.text.SimpleDateFormat
import android.util.Log
import android.content.Intent

class AddHabitActivity : AppCompatActivity() {

    private lateinit var iconSpinner: Spinner
    private lateinit var titleEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var taskInput: EditText
    private lateinit var addTaskBtn: ImageButton
    private lateinit var resetBtn: ImageButton
    private lateinit var tasksContainer: LinearLayout
    private lateinit var progressTextView: TextView

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var selectedIconName: String = "water_cup"
    private val iconNames = listOf("water_cup", "yoga", "book", "lowebody_workout")
    private val tasksList = mutableListOf<String>()
    private val habitId: String by lazy {
        intent.getStringExtra("habit_id") ?: db.collection("dummy").document().id
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_habit)
        supportActionBar?.hide()
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        iconSpinner = findViewById(R.id.iconSpinner)
        titleEditText = findViewById(R.id.habitNameEditText)
        descriptionEditText = findViewById(R.id.habitDescriptionEditText)
        saveButton = findViewById(R.id.saveHabitButton)
        taskInput = findViewById(R.id.taskInput)
        addTaskBtn = findViewById(R.id.addTaskBtn)
        resetBtn = findViewById(R.id.resetBtn)
        tasksContainer = findViewById(R.id.tasksContainer)
        progressTextView = findViewById(R.id.progressTextView)

        
        // Set initial values from intent if editing
        intent.getStringExtra("habitTitle")?.let { titleEditText.setText(it) }
        intent.getStringExtra("habitDesc")?.let { descriptionEditText.setText(it) }
        intent.getStringExtra("habitIcon")?.let { iconName ->
            val position = iconNames.indexOf(iconName)
            if (position != -1) {
                selectedIconName = iconName
            }
        }

        val iconAdapter = IconAdapter(this, iconNames)
        iconSpinner.adapter = iconAdapter

        iconSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedIconName = iconNames[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Add task button functionality
        addTaskBtn.setOnClickListener {
            val taskName = taskInput.text.toString().trim()
            if (taskName.isEmpty()) {
                Toast.makeText(this, "Please enter a task", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            tasksList.add(taskName)
            addTaskToView(taskName, "", false)
            taskInput.text.clear()
        }

        // Delete all tasks button functionality
        resetBtn.setOnClickListener {
            if (tasksList.isEmpty()) {
                Toast.makeText(this, "No tasks to delete", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AlertDialog.Builder(this)
                .setTitle("Delete All Tasks")
                .setMessage("Are you sure you want to delete all tasks?")
                .setPositiveButton("Yes") { _, _ ->
                    tasksList.clear()
                    tasksContainer.removeAllViews()
                    Toast.makeText(this, "All tasks deleted", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        saveButton.setOnClickListener {
            saveHabit()
        }

        loadHabitIcon()
    }

    private fun saveHabit() {
        val userId = auth.currentUser?.uid ?: return
        val title = titleEditText.text.toString().trim()
        val desc = descriptionEditText.text.toString().trim()

        if (title.isEmpty()) {
            titleEditText.error = "Please enter a habit name"
            return
        }

        // Calculate progress
        val (progress, completed, total) = calculateProgress()

        val habit = AddHabit(
            id = habitId,
            title = title,
            description = desc,
            icon = selectedIconName,
            progress = progress,
            completedTasks = completed,
            totalTasks = total,
            selectedDate = Calendar.getInstance().timeInMillis
        )

        val habitDocRef = db.collection("users").document(userId).collection("habits").document(habitId)

        // Create a batch to handle multiple operations
        val batch = db.batch()

        // Set the habit document
        batch.set(habitDocRef, habit)

        // Add tasks with their completion status
        tasksList.forEachIndexed { index, taskName ->
            val taskDocRef = habitDocRef.collection("tasks").document()
            val isCompleted = (tasksContainer.getChildAt(index) as? CheckBox)?.isChecked == true
            
            // Create a completions map with the current date
            val completions = mutableMapOf<String, Boolean>()
            completions[dateFormat.format(Calendar.getInstance().time)] = isCompleted
            
            batch.set(taskDocRef, mapOf(
                "name" to taskName,
                "completions" to completions
            ))
        }

        batch.commit()
            .addOnSuccessListener {
                Log.d("AddHabitActivity", "Saved habit with progress: $progress% ($completed/$total tasks)")
                
                // Return the updated habit data to the calling activity
                val resultIntent = Intent()
                resultIntent.putExtra("updatedHabit", habit)
                setResult(RESULT_OK, resultIntent)
                
                Toast.makeText(this, "Habit saved successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save habit", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadHabitIcon() {
        val userId = auth.currentUser?.uid ?: return
        val habitDocRef = db.collection("users").document(userId).collection("habits").document(habitId)

        habitDocRef.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    titleEditText.setText(document.getString("title"))
                    descriptionEditText.setText(document.getString("description"))

                    val iconName = document.getString("icon") ?: "water_cup"
                    val position = iconNames.indexOf(iconName)
                    if (position != -1) {
                        iconSpinner.setSelection(position)
                        selectedIconName = iconName
                    }

                    // Load tasks only if the document exists
                    loadHabitTasks()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed loading the icon", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadHabitTasks() {
        val userId = auth.currentUser?.uid ?: return
        val habitDocRef = db.collection("users").document(userId).collection("habits").document(habitId)

        // Clear existing tasks before loading
        tasksList.clear()
        tasksContainer.removeAllViews()

        habitDocRef.collection("tasks").get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot) {
                    val taskName = document.getString("name") ?: continue
                    val completions = document.get("completions") as? Map<String, Boolean>
                    val currentDate = dateFormat.format(Calendar.getInstance().time)
                    val isCompleted = completions?.get(currentDate) ?: false
                    
                    // Only add if the task isn't already in the list
                    if (!tasksList.contains(taskName)) {
                        tasksList.add(taskName)
                        addTaskToView(taskName, document.id, isCompleted)
                    }
                }
                // Update progress after loading all tasks
                updateProgress()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed loading the tasks", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addTaskToView(taskName: String, taskId: String, isChecked: Boolean) {
        val checkBox = CheckBox(this).apply {
            text = taskName
            this.isChecked = isChecked
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 8)
            }
            textSize = 16f
            setPadding(24, 12, 24, 12)

            // Update progress when checkbox state changes
            setOnCheckedChangeListener { _, _ ->
                updateProgress()
            }
        }

        checkBox.setOnLongClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete Task")
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
                            .addOnSuccessListener {
                                updateProgress() // Update progress after deleting task
                            }
                    } else {
                        updateProgress() // Update progress for unsaved tasks
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
            true
        }

        tasksContainer.addView(checkBox)
        updateProgress() // Update progress when adding new task
    }

    private fun updateProgress() {
        val (progress, completed, total) = calculateProgress()
        val userId = auth.currentUser?.uid ?: return
        val habitDocRef = db.collection("users").document(userId).collection("habits").document(habitId)
        val currentDate = dateFormat.format(Calendar.getInstance().time)

        // Update the progress in real-time
        habitDocRef.update(
            mapOf(
                "progress" to progress,
                "completedTasks" to completed,
                "totalTasks" to total
            )
        ).addOnSuccessListener {
            Log.d("AddHabitActivity", "Progress updated: $progress% ($completed/$total tasks)")
            
            // Update task completion status
            tasksContainer.children.forEachIndexed { index, view ->
                if (view is CheckBox && index < tasksList.size) {
                    val taskName = tasksList[index]
                    habitDocRef.collection("tasks")
                        .whereEqualTo("name", taskName)
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            for (document in querySnapshot.documents) {
                                val completions = (document.get("completions") as? Map<String, Boolean>)?.toMutableMap() 
                                    ?: mutableMapOf()
                                completions[currentDate] = view.isChecked
                                document.reference.update("completions", completions)
                            }
                        }
                }
            }
        }.addOnFailureListener { e ->
            Log.e("AddHabitActivity", "Failed to update progress", e)
        }
    }

    private fun calculateProgress(): Triple<Int, Int, Int> {
        var completedTasks = 0
        var totalTasks = 0

        // Count completed tasks from the actual checkboxes
        tasksContainer.children.forEach { view ->
            if (view is CheckBox) {
                totalTasks++
                if (view.isChecked) {
                    completedTasks++
                }
            }
        }

        // Calculate percentage using floating-point arithmetic for more accurate results
        val progressPercentage = if (totalTasks > 0) {
            ((completedTasks.toFloat() / totalTasks.toFloat()) * 100).toInt()
        } else {
            0
        }

        // Update the progress TextView
        runOnUiThread {
            progressTextView.text = "Progress: $progressPercentage% ($completedTasks/$totalTasks tasks)"
        }

        Log.d("AddHabitActivity", "Calculated progress: $progressPercentage% ($completedTasks/$totalTasks)")
        return Triple(progressPercentage, completedTasks, totalTasks)
    }
}
