package com.android.habitapplication.ui.today

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.habitapplication.HabitModel
import com.android.habitapplication.R
import com.android.habitapplication.databinding.FragmentTodayBinding
import com.android.habitapplication.ui.all_habits.AddHabitActivity

class TodayFragment : Fragment() {

    private var _binding: FragmentTodayBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: HabitsAdapter
    private val habitsList = mutableListOf<HabitModel>()

    companion object {
        const val ADD_HABIT_REQUEST_CODE = 100
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTodayBinding.inflate(inflater, container, false)
        val root = binding.root

        setupInitialHabits()

        adapter = HabitsAdapter(habitsList)
        binding.habitsRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        binding.habitsRecyclerView.adapter = adapter

        binding.addBtn.setOnClickListener {
            val intent = Intent(requireContext(), AddHabitActivity::class.java)
            startActivityForResult(intent, ADD_HABIT_REQUEST_CODE)
        }

        return root
    }

    private fun setupInitialHabits() {
        // عادات ثابتة
        habitsList.add(
            HabitModel(
                name = "Drinking Water",
                progress = "75%",
                iconResId = R.drawable.ic_water_glass_filled
            )
        )
        habitsList.add(
            HabitModel(
                name = "Cycling",
                progress = "40%",
                iconResId = R.drawable.ic_cycling
            )
        )
        habitsList.add(
            HabitModel(
                name = "Water",
                progress = "40%",
                iconResId = R.drawable.ic_water_glass
            )
        )
        habitsList.add(
            HabitModel(
                name = "Walking",
                progress = "40%",
                iconResId = R.drawable.walking_vector
            )
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ADD_HABIT_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val name = data.getStringExtra("habit_name") ?: return
            val progress = data.getStringExtra("habit_progress") ?: "0%"
            val imageResId = data.getIntExtra("habit_image", R.drawable.create)

            val newHabit = HabitModel(
                name = name,
                progress = progress,
                iconResId = imageResId
            )

            habitsList.add(newHabit)
            adapter.notifyItemInserted(habitsList.size - 1)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
