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

        val taskCompletionsDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val selectedDateForCompletionsStr = taskCompletionsDateFormat.format(selectedDate.time)

        val dayOfWeek = selectedDate.get(Calendar.DAY_OF_WEEK)

        habitsCollection.get()
            .addOnSuccessListener { habitsSnapshot ->
                val allHabits = habitsSnapshot.documents.mapNotNull {
                    val habit = it.toObject(AddHabit::class.java)
                    habit?.id = it.id
                    habit
                }

                val filteredHabits = allHabits.filter { habit ->
                    val dayIndex = dayOfWeek - 1
                    if (dayIndex >= 0 && dayIndex < habit.repeatedDays.size) {
                        habit.repeatedDays[dayIndex]
                    } else {
                        false
                    }
                }

                if (filteredHabits.isEmpty()) {
                    _habitList.postValue(emptyList())
                    return@addOnSuccessListener
                }

                val finalHabitsToShow = mutableListOf<AddHabit>()
                var processedHabitsCount = 0

                filteredHabits.forEach { habit ->
                    val tasksCollection = habitsCollection.document(habit.id).collection("tasks")
                    tasksCollection.get()
                        .addOnSuccessListener { tasksSnapshot ->
                            val totalTasks = tasksSnapshot?.size() ?: 0
                            val completedTasks = tasksSnapshot?.count { taskDoc ->
                                val completions = taskDoc.get("completions") as? Map<String, Boolean>
                                completions?.get(selectedDateForCompletionsStr) == true
                            } ?: 0

                            habit.totalTasks = totalTasks
                            habit.completedTasks = completedTasks
                            habit.progress = if (totalTasks > 0) (completedTasks * 100 / totalTasks) else 0
                            
                            finalHabitsToShow.add(habit)
                            processedHabitsCount++
                            if (processedHabitsCount == filteredHabits.size) {
                                _habitList.postValue(finalHabitsToShow.sortedBy { it.title })
                            }
                        }
                        .addOnFailureListener { exception ->
                            Log.w("HabitViewModel", "Error getting tasks for habit ${habit.id}: ", exception)
                            habit.totalTasks = 0
                            habit.completedTasks = 0
                            habit.progress = 0
                            finalHabitsToShow.add(habit)

                            processedHabitsCount++
                            if (processedHabitsCount == filteredHabits.size) {
                                _habitList.postValue(finalHabitsToShow.sortedBy { it.title })
                            }
                        }
                }
            }
            .addOnFailureListener { exception ->
                Log.w("HabitViewModel", "Error getting all habits: ", exception)
                _habitList.postValue(emptyList())
            }
    }

    fun updateTaskStatus(habitId: String, taskId: String, newStatus: Boolean) {
        val userId = getUserId() ?: return
        val taskRef = db.collection("users").document(userId)
            .collection("habits").document(habitId)
            .collection("tasks").document(taskId)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val selectedDateStr = dateFormat.format(calendar.time) 

        taskRef.get().addOnSuccessListener { documentSnapshot ->
            val completions = (documentSnapshot.get("completions") as? Map<String, Boolean>)?.toMutableMap()
                ?: mutableMapOf()

            completions[selectedDateStr] = newStatus

            taskRef.update("completions", completions)
                .addOnSuccessListener {
                    Log.d("HabitViewModel", "Task $taskId status updated to $newStatus for date: $selectedDateStr")
                    loadHabitsForDate(calendar) 
                }
                .addOnFailureListener { exception ->
                    Log.w("HabitViewModel", "Error updating task $taskId: ", exception)
                }
        }.addOnFailureListener {
             Log.w("HabitViewModel", "Error fetching task $taskId for update: ", it)
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
