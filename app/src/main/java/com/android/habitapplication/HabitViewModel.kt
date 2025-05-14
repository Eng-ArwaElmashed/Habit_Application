package com.android.habitapplication

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.android.habitapplication.model.AddHabit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class HabitViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val _habitList = MutableLiveData<List<AddHabit>>()
    val habitList: LiveData<List<AddHabit>> get() = _habitList
    private val calendar = Calendar.getInstance()

    private fun getUserId(): String? {
        return FirebaseAuth.getInstance().currentUser?.uid
    }

    fun loadHabitsForDate(selectedDate: Calendar) {
        val userId = getUserId() ?: return
        val habitsCollection = db.collection("users").document(userId).collection("habits")
        val tempList = mutableListOf<AddHabit>()

        // Format for storing dates in Firestore
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        // Get the selected date
        val selectedDateStr = dateFormat.format(selectedDate.time)
        val selectedDateStart = selectedDate.clone() as Calendar
        val selectedDateEnd = selectedDate.clone() as Calendar
        
        // Set time to start and end of day
        selectedDateStart.set(Calendar.HOUR_OF_DAY, 0)
        selectedDateStart.set(Calendar.MINUTE, 0)
        selectedDateStart.set(Calendar.SECOND, 0)
        selectedDateEnd.set(Calendar.HOUR_OF_DAY, 23)
        selectedDateEnd.set(Calendar.MINUTE, 59)
        selectedDateEnd.set(Calendar.SECOND, 59)

        habitsCollection
            .whereGreaterThanOrEqualTo("selectedDate", selectedDateStart.timeInMillis)
            .whereLessThanOrEqualTo("selectedDate", selectedDateEnd.timeInMillis)
            .get()
            .addOnSuccessListener { result ->
                val totalHabits = result.size()
                var processedHabits = 0

                if (totalHabits == 0) {
                    _habitList.postValue(emptyList())
                    return@addOnSuccessListener
                }

                for (document in result) {
                    val habit = document.toObject(AddHabit::class.java)
                    habit.id = document.id

                    // Get the tasks for this habit
                    val tasksCollection = habitsCollection.document(habit.id).collection("tasks")
                    tasksCollection.get()
                        .addOnSuccessListener { tasks ->
                            val totalTasks = tasks.size()
                            // Count completed tasks for the selected date only
                            val completedTasks = tasks.count { task ->
                                val completions = task.get("completions") as? Map<String, Boolean>
                                completions?.get(selectedDateStr) == true
                            }

                            habit.totalTasks = totalTasks
                            habit.completedTasks = completedTasks
                            tempList.add(habit)

                            processedHabits++
                            if (processedHabits == totalHabits) {
                                _habitList.postValue(tempList)
                            }
                        }
                        .addOnFailureListener { exception ->
                            Log.w("HabitViewModel", "Error getting tasks: ", exception)
                            processedHabits++
                            if (processedHabits == totalHabits) {
                                _habitList.postValue(tempList)
                            }
                        }
                }
            }
            .addOnFailureListener { exception ->
                Log.w("HabitViewModel", "Error getting habits: ", exception)
                _habitList.postValue(emptyList())
            }
    }

    fun updateTaskStatus(habitId: String, taskId: String, newStatus: Boolean) {
        val userId = getUserId() ?: return
        val taskRef = db.collection("users").document(userId)
            .collection("habits").document(habitId)
            .collection("tasks").document(taskId)

        // Get the selected date in yyyy-MM-dd format
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val selectedDateStr = dateFormat.format(calendar.time)

        // Update the completion status for the selected date
        taskRef.get().addOnSuccessListener { document ->
            val completions = (document.get("completions") as? Map<String, Boolean>)?.toMutableMap() 
                ?: mutableMapOf()
            
            completions[selectedDateStr] = newStatus

            taskRef.update("completions", completions)
                .addOnSuccessListener {
                    Log.d("HabitViewModel", "Task updated successfully for date: $selectedDateStr")
                    // Reload habits for the current date
                    loadHabitsForDate(calendar)
                }
                .addOnFailureListener { exception ->
                    Log.w("HabitViewModel", "Error updating task: ", exception)
                }
        }
    }

    // Set the current calendar date
    fun setCurrentDate(date: Calendar) {
        calendar.timeInMillis = date.timeInMillis
    }

    fun loadAllHabits() {
        val userId = getUserId() ?: return
        db.collection("users").document(userId).collection("habits")
            .get()
            .addOnSuccessListener { result ->
                val habits = result.documents.mapNotNull { 
                    val habit = it.toObject(AddHabit::class.java)
                    habit?.id = it.id
                    habit
                }
                _habitList.postValue(habits)
            }
            .addOnFailureListener { exception ->
                Log.e("HabitViewModel", "Error loading habits", exception)
                _habitList.postValue(emptyList())
            }
    }
}
