package com.android.habitapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HabitAdapter(
    private val habits: List<HabitModel>,
    private val onHabitClick: (HabitModel) -> Unit
) : RecyclerView.Adapter<HabitAdapter.HabitViewHolder>() {

    inner class HabitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.habit_title)
        private val progressText: TextView = itemView.findViewById(R.id.habit_progress_text)
        private val centerIcon: ImageView = itemView.findViewById(R.id.center_icon)

        fun bind(habit: HabitModel) {
            title.text = habit.name
            progressText.text = habit.progress
            centerIcon.setImageResource(habit.iconResId)

            itemView.setOnClickListener {
                onHabitClick(habit)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.habit_item_list, parent, false)
        return HabitViewHolder(view)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        holder.bind(habits[position])
    }

    override fun getItemCount(): Int = habits.size
}
