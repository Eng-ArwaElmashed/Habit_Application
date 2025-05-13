import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore

class HabitViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val _habitList = MutableLiveData<List<AddHabit>>()
    val habitList: LiveData<List<AddHabit>> get() = _habitList


    fun loadHabitsForDay(day: String) {
        val habitList = mutableListOf<AddHabit>()
        val tempList = mutableListOf<AddHabit>()
        val habitsCollection = db.collection("habits")

        habitsCollection
            .whereEqualTo("day", day)
            .get()
            .addOnSuccessListener { result ->
                val totalHabits = result.size()
                var processedHabits = 0

                for (document in result) {
                    val habit = document.toObject(AddHabit::class.java)
                    habit.id = document.id

                    val tasksCollection = habitsCollection.document(habit.id).collection("tasks")
                    tasksCollection.get()
                        .addOnSuccessListener { tasks ->
                            val totalTasks = tasks.size()
                            val completedTasks = tasks.count { it.getBoolean("done") == true }

                            habit.totalTasks = totalTasks
                            habit.completedTasks = completedTasks

                            tempList.add(habit)
                            processedHabits++

                            if (processedHabits == totalHabits) {
                                _habitList.postValue(tempList)
                            }
                        }
                        .addOnFailureListener { exception ->
                            Log.w("TodayFragment", "Error getting tasks: ", exception)
                            processedHabits++
                        }
                }
            }
            .addOnFailureListener { exception ->
                Log.w("TodayFragment", "Error getting habits: ", exception)
            }
    }

    fun loadTodayHabits() {
        val habitList = mutableListOf<AddHabit>()
        val tempList = mutableListOf<AddHabit>()
        val habitsCollection = db.collection("habits")

        habitsCollection.get()
            .addOnSuccessListener { result ->
                val totalHabits = result.size()
                var processedHabits = 0

                for (document in result) {
                    val habit = document.toObject(AddHabit::class.java)
                    habit.id = document.id

                    val tasksCollection = habitsCollection.document(habit.id).collection("tasks")
                    tasksCollection.get()
                        .addOnSuccessListener { tasks ->
                            val totalTasks = tasks.size()
                            val completedTasks = tasks.count { it.getBoolean("done") == true }

                            habit.totalTasks = totalTasks
                            habit.completedTasks = completedTasks

                            tempList.add(habit)
                            processedHabits++

                            if (processedHabits == totalHabits) {
                                _habitList.postValue(tempList)
                            }
                        }
                        .addOnFailureListener { exception ->
                            Log.w("TodayFragment", "Error getting tasks: ", exception)
                            processedHabits++
                        }
                }
            }
            .addOnFailureListener { exception ->
                Log.w("TodayFragment", "Error getting habits: ", exception)
            }
    }

    fun updateTaskStatus(habitId: String, taskId: String, newStatus: Boolean) {
        val taskRef = db.collection("habits").document(habitId).collection("tasks").document(taskId)
        taskRef.update("done", newStatus)
            .addOnSuccessListener {
                Log.d("TodayFragment", "Task updated successfully")
                loadTodayHabits()
            }
            .addOnFailureListener { exception ->
                Log.w("TodayFragment", "Error updating task: ", exception)
            }
    }

    fun loadAllHabits() {
        db.collection("habits")
            .get()
            .addOnSuccessListener { result ->
                val habits = result.documents.mapNotNull { it.toObject(AddHabit::class.java) }
                _habitList.postValue(habits)
            }
            .addOnFailureListener { exception ->
                Log.e("HabitViewModel", "Error loading habits", exception)
            }
    }

}
