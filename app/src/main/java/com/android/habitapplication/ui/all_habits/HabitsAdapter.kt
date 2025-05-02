package com.android.habitapplication.ui.today

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.habitapplication.HabitModel
import com.android.habitapplication.R

class HabitsAdapter(private val habitsList: List<HabitModel>) :
    RecyclerView.Adapter<HabitsAdapter.HabitViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_habit_card, parent, false)
        return HabitViewHolder(view)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        holder.bind(habitsList[position])
    }

    override fun getItemCount(): Int = habitsList.size

    class HabitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val habitName: TextView = itemView.findViewById(R.id.habitName)
        private val habitProgress: TextView = itemView.findViewById(R.id.habitProgress)
        private val habitImage: ImageView = itemView.findViewById(R.id.habitImage)

        fun bind(habit: HabitModel) {
            habitName.text = habit.name
            habitProgress.text = (habit.progress ?: "0%").toString()
            habitImage.setImageResource(habit.iconResId)
        }
    }
}
