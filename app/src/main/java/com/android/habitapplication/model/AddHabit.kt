package com.android.habitapplication.model

import com.android.habitapplication.R
import java.io.Serializable

data class AddHabit(
    var id: String = "",
    var title: String = "",
    var description: String = "",
    var progress: String = "0%",
    var iconResId: Int = R.drawable.edit,
   var iconName : String="",
    val tasks: List<Task> = listOf()
): Serializable

data class Task(
    val name: String,
    val isCompleted: Boolean
)
