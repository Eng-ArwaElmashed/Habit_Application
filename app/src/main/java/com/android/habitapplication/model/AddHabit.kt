package com.android.habitapplication.model

import com.android.habitapplication.R
import java.io.Serializable

data class AddHabit(
    var id: String = "",
    val title: String = "",
    val description: String = "",
    val progrees : Int =0,
    var completedTasks: Int = 0,
    var totalTasks: Int = 0
) : Serializable



