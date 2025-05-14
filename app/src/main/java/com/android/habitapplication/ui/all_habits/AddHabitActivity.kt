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
import com.google.android.material.button.MaterialButton
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat

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

    private lateinit var dayButtons: List<MaterialButton>
    private val selectedDays = BooleanArray(7) { false }

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var selectedIconName: String = "water_cup"
    private val iconNames = listOf("water_cup", "yoga", "book", "lowebody_workout", "walking_vector", "cycling_vector", "congrates", "morning_walk", "small_drinking_water")
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

        initializeDayButtons()

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
                    updateProgress()
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

    private fun initializeDayButtons() {
        dayButtons = listOf(
            findViewById(R.id.Sun_btn), findViewById(R.id.Mon_btn), findViewById(R.id.Tue_btn),
            findViewById(R.id.Wed_btn), findViewById(R.id.Thu_btn), findViewById(R.id.Fri_btn),
            findViewById(R.id.Sat_btn)
        )
        val strokeWidth = resources.getDimensionPixelSize(R.dimen.day_button_stroke_width)
        val mainPinkColor = ContextCompat.getColor(this, R.color.main_pink)
        val lightPinkColor = ContextCompat.getColor(this, R.color.light_pink)

        dayButtons.forEachIndexed { index, button ->
            button.strokeWidth = strokeWidth
            button.strokeColor = ColorStateList.valueOf(mainPinkColor)
            if (selectedDays[index]) {
                button.backgroundTintList = ColorStateList.valueOf(mainPinkColor)
                button.isSelected = true
            } else {
                button.backgroundTintList = ColorStateList.valueOf(lightPinkColor)
                button.isSelected = false
            }
            button.setOnClickListener {
                selectedDays[index] = !selectedDays[index]
                if (selectedDays[index]) {
                    button.backgroundTintList = ColorStateList.valueOf(mainPinkColor)
                    button.isSelected = true
                } else {
                    button.backgroundTintList = ColorStateList.valueOf(lightPinkColor)
                    button.isSelected = false
                }
            }
        }
    }

    private fun saveHabit() {
        val userId = auth.currentUser?.uid ?: return
        val title = titleEditText.text.toString().trim()
        val desc = descriptionEditText.text.toString().trim()

        if (title.isEmpty()) {
            titleEditText.error = "Please enter a habit name"
            return
        }

        val (progress, completed, total) = calculateProgress()

        val habit = AddHabit(
            id = habitId, title = title, description = desc, icon = selectedIconName,
            progress = progress, completedTasks = completed, totalTasks = total,
            selectedDate = Calendar.getInstance().timeInMillis,
            repeatedDays = selectedDays.toList()
        )

        val habitDocRef = db.collection("users").document(userId).collection("habits").document(habitId)
        val batch = db.batch()
        batch.set(habitDocRef, habit)

        tasksList.forEachIndexed { index, taskName ->
            val taskDocRef = habitDocRef.collection("tasks").document()
            val isCompleted = (tasksContainer.getChildAt(index) as? CheckBox)?.isChecked == true
            val completions = mutableMapOf<String, Boolean>()
            val dateKey = dateFormat.format(Calendar.getInstance().time)
            completions[dateKey] = isCompleted
            batch.set(taskDocRef, mapOf("name" to taskName, "completions" to completions))
        }

        batch.commit()
            .addOnSuccessListener {
                Log.d("AddHabitActivity", "Saved habit with progress: $progress% ($completed/$total tasks)")
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
                    val loadedRepeatedDays = document.get("repeatedDays") as? List<Boolean>
                    if (loadedRepeatedDays != null && loadedRepeatedDays.size == 7) {
                        loadedRepeatedDays.forEachIndexed { idx, isDaySelected ->
                            selectedDays[idx] = isDaySelected
                            if (::dayButtons.isInitialized && idx < dayButtons.size) {
                                val currentButton = dayButtons[idx]
                                val mainPinkColor = ContextCompat.getColor(this, R.color.main_pink)
                                val lightPinkColor = ContextCompat.getColor(this, R.color.light_pink)
                                currentButton.strokeWidth = resources.getDimensionPixelSize(R.dimen.day_button_stroke_width)
                                currentButton.strokeColor = ColorStateList.valueOf(mainPinkColor)
                                if (isDaySelected) {
                                    currentButton.backgroundTintList = ColorStateList.valueOf(mainPinkColor)
                                    currentButton.isSelected = true
                                } else {
                                    currentButton.backgroundTintList = ColorStateList.valueOf(lightPinkColor)
                                    currentButton.isSelected = false
                                }
                            }
                        }
                    }
                    loadHabitTasks()
                } else {
                    Log.d("AddHabitActivity", "No such document for habitId: $habitId, could be a new habit.")
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error loading habit details: ${exception.message}", Toast.LENGTH_SHORT).show()
                Log.e("AddHabitActivity", "Error loading habit details", exception)
            }
    }

    private fun loadHabitTasks() {
        val userId = auth.currentUser?.uid ?: return
        val habitDocRef = db.collection("users").document(userId).collection("habits").document(habitId)

        tasksList.clear()
        tasksContainer.removeAllViews()

        habitDocRef.collection("tasks").get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot) {
                    val taskId = document.id
                    val taskName = document.getString("name") ?: continue
                    val completions = document.get("completions") as? Map<String, Boolean>
                    val dateKey = dateFormat.format(Calendar.getInstance().time)
                    val isCompleted = completions?.get(dateKey) ?: false
                    
                    tasksList.add(taskName)
                    addTaskToView(taskName, taskId, isCompleted)
                }
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
                        val taskRef = db.collection("users").document(userId)
                            .collection("habits").document(habitId)
                            .collection("tasks").document(taskId)
                        taskRef.delete()
                            .addOnSuccessListener {
                                Log.d("AddHabitActivity", "Task $taskId deleted from Firestore.")
                                updateProgress()
                            }
                            .addOnFailureListener { e ->
                                Log.e("AddHabitActivity", "Failed to delete task $taskId from Firestore", e)
                                updateProgress()
                            }
                    } else {
                        updateProgress()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
            true
        }
        tasksContainer.addView(checkBox)
        updateProgress()
    }

    private fun updateProgress() {
        val (progress, completed, total) = calculateProgress()
        
        runOnUiThread {
            progressTextView.text = "Progress: $progress% ($completed/$total tasks)"
        }

        val userId = auth.currentUser?.uid
        if (userId != null) {
            val habitDocRef = db.collection("users").document(userId).collection("habits").document(habitId)
            
            habitDocRef.update(
                mapOf("progress" to progress, "completedTasks" to completed, "totalTasks" to total)
            ).addOnSuccessListener {
            }.addOnFailureListener { e ->
            }

            val currentDateKeyForUpdate = dateFormat.format(Calendar.getInstance().time)

            tasksContainer.children.forEachIndexed { index, view ->
                if (view is CheckBox && index < tasksList.size) {
                    val taskName = tasksList[index]

                    habitDocRef.collection("tasks")
                        .whereEqualTo("name", taskName) 
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            for (document in querySnapshot.documents) {
                                val existingCompletions = (document.get("completions") as? Map<String, Boolean>)?.toMutableMap()
                                    ?: mutableMapOf()
                                existingCompletions[currentDateKeyForUpdate] = view.isChecked
                                document.reference.update("completions", existingCompletions)
                                    .addOnSuccessListener { Log.d("AddHabitActivity", "Task '${taskName}' completion for $currentDateKeyForUpdate updated to ${view.isChecked}") }
                                    .addOnFailureListener { e -> Log.e("AddHabitActivity", "Failed to update task '${taskName}' completion for $currentDateKeyForUpdate", e) }
                            }
                        }
                        .addOnFailureListener { e ->
                             Log.e("AddHabitActivity", "Failed to find task '${taskName}' to update completion for $currentDateKeyForUpdate", e)
                        }
                }
            }
        }
    }

    private fun calculateProgress(): Triple<Int, Int, Int> {
        var completedTasks = 0
        var totalTasks = 0
        tasksContainer.children.forEach { view ->
            if (view is CheckBox) {
                totalTasks++
                if (view.isChecked) {
                    completedTasks++
                }
            }
        }
        val progressPercentage = if (totalTasks > 0) {
            ((completedTasks.toFloat() / totalTasks.toFloat()) * 100).toInt()
        } else {
            0
        }
        runOnUiThread {
            progressTextView.text = "Progress: $progressPercentage% ($completedTasks/$totalTasks tasks)"
        }
        Log.d("AddHabitActivity", "Calculated progress: $progressPercentage% ($completedTasks/$totalTasks)")
        return Triple(progressPercentage, completedTasks, totalTasks)
    }
}
