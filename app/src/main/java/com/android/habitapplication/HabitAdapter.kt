package com.android.habitapplication

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.habitapplication.model.AddHabit

class HabitAdapter(
    private val onHabitClick: (AddHabit) -> Unit,
    private val onDeleteClick: (AddHabit) -> Unit,
    private val onEditClick: (AddHabit) -> Unit
) : ListAdapter<AddHabit, HabitAdapter.HabitViewHolder>(HabitDiffCallback()) {

    class HabitDiffCallback : DiffUtil.ItemCallback<AddHabit>() {
        override fun areItemsTheSame(oldItem: AddHabit, newItem: AddHabit): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AddHabit, newItem: AddHabit): Boolean {
            return oldItem == newItem
        }
    }

    inner class HabitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.habit_title)
        val progressText: TextView = itemView.findViewById(R.id.habit_progress_text)
        val centerIcon: ImageView = itemView.findViewById(R.id.center_icon)
        val btnEdit: ImageView = itemView.findViewById(R.id.edit_btn)
        val btnDelete: ImageView = itemView.findViewById(R.id.delete_btn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.today_item_list, parent, false)
        return HabitViewHolder(view)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        val habit = getItem(position)

        val progress = if (habit.totalTasks > 0) {
            (habit.completedTasks.toDouble() / habit.totalTasks) * 100
        } else {
            0.0
        }
        holder.progressText.text = "${progress.toInt()}%"

        val progressIndicator = holder.itemView.findViewById<com.google.android.material.progressindicator.CircularProgressIndicator>(R.id.habit_progress_indicator)
        progressIndicator.progress = progress.toInt()

        holder.title.text = habit.title

        holder.itemView.setOnClickListener {
            onHabitClick(habit)
        }

        holder.btnEdit.setOnClickListener {
            onEditClick(habit)
        }

        holder.btnDelete.setOnClickListener {
            onDeleteClick(habit)
        }

        Log.d("HabitAdapter", "onBindViewHolder: ${habit.title} progress: $progress")
    }


    fun updateHabitProgress(updatedHabit: AddHabit) {
        val currentList = currentList.toMutableList()
        val index = currentList.indexOfFirst { it.id == updatedHabit.id }
        if (index != -1) {
            currentList[index] = updatedHabit
            submitList(currentList)
        }
    }
}
