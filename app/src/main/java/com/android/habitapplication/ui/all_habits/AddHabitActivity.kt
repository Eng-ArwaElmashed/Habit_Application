package com.android.habitapplication.ui.all_habits

import AddHabit
import IconPickerDialogFragment
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.android.habitapplication.R
import com.google.firebase.firestore.FirebaseFirestore

class AddHabitActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
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

        db = FirebaseFirestore.getInstance()

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
            override fun onItemSelected(parentView: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedIcon = icons[position]
                selectedIconResId = selectedIcon
                findViewById<ImageView>(R.id.selectedIconImage).setImageResource(selectedIcon)
            }

            override fun onNothingSelected(parentView: AdapterView<*>) {
                //
            }
        }

        if (habitId.isNotEmpty()) {
            val tasksCollection = db.collection("habits").document(habitId).collection("tasks")
            tasksCollection.get()
                .addOnSuccessListener { querySnapshot ->
                    for (document in querySnapshot) {
                        val taskName = document.getString("name") ?: continue
                        val isChecked = document.getBoolean("done") ?: false
                        tasksList.add(taskName)
                        addTaskToView(taskName, document.id, isChecked)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed loading the tasks", Toast.LENGTH_SHORT).show()
                }
        }

        val saveButton: Button = findViewById(R.id.saveHabitButton)

        addTaskBtn.setOnClickListener {
            val taskName = taskInput.text.toString().trim()
            if (taskName.isNotEmpty()) {
                tasksList.add(taskName)
                addTaskToView(taskName, "", false)
                taskInput.text.clear()
            }
        }

        saveButton.setOnClickListener {
            val title = titleEditText.text.toString()
            val desc = descEditText.text.toString()

            if (habitId.isEmpty()) {
                habitId = db.collection("habits").document().id

                val newHabit = AddHabit(
                    id = habitId,
                    title = title,
                    description = desc,
                    icon = selectedIconResId.toString()
                )


                val habitDocRef = db.collection("habits").document(habitId)
                habitDocRef.set(newHabit)
                    .addOnSuccessListener {
                        val tasksCollection = habitDocRef.collection("tasks")
                        val batch = db.batch()

                        tasksList.forEach { taskName ->
                            val taskMap = hashMapOf("name" to taskName, "done" to false)
                            tasksCollection.add(taskMap)
                        }

                        batch.commit().addOnSuccessListener {
                            val resultIntent = Intent()
                            resultIntent.putExtra("updatedHabit", newHabit)
                            setResult(Activity.RESULT_OK, resultIntent)
                            finish()
                        }.addOnFailureListener {
                            Toast.makeText(this, "Failed to add tasks", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to add habit", Toast.LENGTH_SHORT).show()
                    }
            } else {
                val habitDocRef = db.collection("habits").document(habitId)
                val updatedHabit = AddHabit(habitId, title, desc, icon = selectedIconResId.toString())

                val tasksCollection = habitDocRef.collection("tasks")
                tasksCollection.get().addOnSuccessListener { snapshot ->
                    val oldTasks = snapshot.documents.map { it.id to (it.getString("name") ?: "") }.toMap()

                    val newTaskSet = tasksList.toSet()
                    val oldTaskSet = oldTasks.values.toSet()

                    val tasksToDelete = oldTasks.filterValues { it !in newTaskSet }
                    val tasksToAdd = newTaskSet.filter { it !in oldTaskSet }

                    val batch = db.batch()

                    for ((docId, _) in tasksToDelete) {
                        val docRef = tasksCollection.document(docId)
                        batch.delete(docRef)
                    }

                    for (task in tasksToAdd) {
                        val taskMap = hashMapOf("name" to task, "done" to false)
                        tasksCollection.add(taskMap)
                    }

                    batch.commit().addOnSuccessListener {
                        habitDocRef.set(updatedHabit)
                            .addOnSuccessListener {
                                val resultIntent = Intent()
                                resultIntent.putExtra("updatedHabit", updatedHabit)
                                setResult(Activity.RESULT_OK, resultIntent)
                                finish()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Failed to update habit", Toast.LENGTH_SHORT).show()
                            }
                    }.addOnFailureListener {
                        Toast.makeText(this, "Failed to update tasks", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }


    private fun addTaskToView(taskName: String, taskId: String, isChecked: Boolean) {
        val checkBox = CheckBox(this)
        checkBox.text = taskName
        checkBox.isChecked = isChecked

        if (checkBox.parent != null) {
            (checkBox.parent as ViewGroup).removeView(checkBox)
        }

        checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (taskId.isNotEmpty()) {
                val taskRef = db.collection("habits").document(habitId).collection("tasks").document(taskId)
                taskRef.update("done", isChecked)
            }
        }

        checkBox.setOnLongClickListener {
            android.app.AlertDialog.Builder(this)
                .setTitle("Delete task")
                .setMessage("Are you sure you want to delete this task?")
                .setPositiveButton("Yes") { _, _ ->
                    tasksContainer.removeView(checkBox)
                    tasksList.remove(taskName)
                    if (taskId.isNotEmpty()) {
                        val taskRef = db.collection("habits").document(habitId).collection("tasks").document(taskId)
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
