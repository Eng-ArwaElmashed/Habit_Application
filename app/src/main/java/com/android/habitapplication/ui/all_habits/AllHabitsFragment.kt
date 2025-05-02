package com.android.habitapplication.ui.all_habits

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import com.android.habitapplication.*
import com.android.habitapplication.databinding.FragmentAllHabitsBinding

class AllHabitsFragment : Fragment() {

    private var _binding: FragmentAllHabitsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAllHabitsBinding.inflate(inflater, container, false)

        val habits = listOf(
            HabitModel("Drink Water",R.drawable.water_cup, "75%"),
            HabitModel("Morning Walk", R.drawable.walking_vector,"40%"),
            HabitModel("Cycling", R.drawable.cycling_vector_small,"90%" ),
            HabitModel("Reading",R.drawable.book, "60%")
        )

        binding.rv.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rv.adapter = HabitAdapter(habits) { habit ->
            Toast.makeText(requireContext(), "Clicked: ${habit.name}", Toast.LENGTH_SHORT).show()
        }

        binding.addBtn.setOnClickListener {
            val intent = Intent(requireContext(), AddHabitActivity::class.java)
            startActivity(intent)
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
