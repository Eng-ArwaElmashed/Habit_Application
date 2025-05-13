package com.android.habitapplication

import AddHabit
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView


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
        val btnEdit: ImageView = itemView.findViewById(R.id.edit_btn)
        val btnDelete: ImageView = itemView.findViewById(R.id.delete_btn)
        val habit_Icon: ImageView = itemView.findViewById(R.id.habit_Icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.today_item_list, parent, false)
        return HabitViewHolder(view)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        val habit = getItem(position)

        val iconResId = holder.itemView.context.resources.getIdentifier(
            habit.icon, "drawable", holder.itemView.context.packageName
        )
        holder.habit_Icon.setImageResource(
            if (iconResId != 0) iconResId else R.drawable.image
        )

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


}
