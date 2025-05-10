package com.android.habitapplication

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
            .inflate(R.layout.habit_item_list, parent, false)
        return HabitViewHolder(view)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        val habit = getItem(position)

        holder.title.text = habit.title

        val totalTasks = habit.tasks.size
        val completedTasks = habit.tasks.count { it.isCompleted }
        val progressPercentage = if (totalTasks == 0) 0 else (completedTasks * 100) / totalTasks

        holder.progressText.text = "$progressPercentage%"

        holder.itemView.setOnClickListener {
            onHabitClick(habit)
        }

        holder.btnEdit.setOnClickListener {
            onEditClick(habit)
        }

        holder.btnDelete.setOnClickListener {
            onDeleteClick(habit)
        }
    }

}
