package com.android.habitapplication

import android.os.Message

data class Notification(
    val imageResId: Int = 0,
    val title: String = "",
    val date: String = "",
    val time: String = "",
)