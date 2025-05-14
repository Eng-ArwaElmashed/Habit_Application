package com.android.habitapplication.model
// import com.android.habitapplication.R // Not used
import java.io.Serializable

data class AddHabit(
    var id: String = "",
    val title: String = "",
    val description: String = "",
    var progress: Int = 0, // This will be updated by ViewModel based on DisplayTasks
    var completedTasks: Int = 0, // This will be updated by ViewModel based on DisplayTasks
    var totalTasks: Int = 0, // This will be updated by ViewModel based on DisplayTasks
    val icon: String = "",
    var completedDates: Int = 0,
    val selectedDate: Long = System.currentTimeMillis(), // Specific date for the habit (original creation or specific target)
    val repeatedDays: List<Boolean> = List(7) { false }, // Sunday to Saturday (index 0 for Sunday)
) : Serializable