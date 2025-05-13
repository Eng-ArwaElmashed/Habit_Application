package com.android.habitapplication.ui.today

import AddHabit
import HabitViewModel
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.android.habitapplication.HabitAdapter
import com.android.habitapplication.databinding.FragmentTodayBinding
import com.android.habitapplication.ui.all_habits.AddHabitActivity
import com.google.firebase.firestore.FirebaseFirestore

class TodayFragment : Fragment() {

    private var _binding: FragmentTodayBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: HabitAdapter
    private lateinit var habitViewModel: HabitViewModel

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val updatedHabit = result.data?.getSerializableExtra("updatedHabit") as? AddHabit
            updatedHabit?.let {
                habitViewModel.loadTodayHabits()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTodayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        habitViewModel = ViewModelProvider(this)[HabitViewModel::class.java]

        setupRecyclerView()

        habitViewModel.habitList.observe(viewLifecycleOwner) { habits ->
            adapter.submitList(habits)
        }

        habitViewModel.loadTodayHabits()

        binding.addBtn.setOnClickListener {
            val intent = Intent(requireContext(), AddHabitActivity::class.java)
            resultLauncher.launch(intent)
        }
    }

    private fun setupRecyclerView() {
        binding.todayRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        adapter = HabitAdapter(
            onHabitClick = { habit -> editHabit(habit) },
            onDeleteClick = { habit -> deleteHabit(habit) },
            onEditClick = { habit -> editHabit(habit) }
        )

        binding.todayRecyclerView.adapter = adapter
    }

    private fun deleteHabit(habit: AddHabit) {
        FirebaseFirestore.getInstance().collection("habits").document(habit.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Habit deleted", Toast.LENGTH_SHORT).show()
                habitViewModel.loadTodayHabits()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to delete habit", Toast.LENGTH_SHORT).show()
            }
    }

    private fun editHabit(habit: AddHabit) {
        val intent = Intent(requireContext(), AddHabitActivity::class.java).apply {
            putExtra("habitId", habit.id)
            putExtra("habitTitle", habit.title)
            putExtra("habitDesc", habit.description)
        }
        resultLauncher.launch(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
