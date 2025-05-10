package com.android.habitapplication.ui.today

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.habitapplication.HabitAdapter
import com.android.habitapplication.R
import com.android.habitapplication.databinding.FragmentTodayBinding
import com.android.habitapplication.model.AddHabit
import com.android.habitapplication.ui.all_habits.AddHabitActivity
import com.google.firebase.firestore.FirebaseFirestore

class TodayFragment : Fragment() {

    private var _binding: FragmentTodayBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: HabitAdapter
    private lateinit var db: FirebaseFirestore

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val updatedHabit = result.data?.getSerializableExtra("updatedHabit") as? AddHabit
            updatedHabit?.let {

                val habitList = adapter.currentList.toMutableList()
                val index = habitList.indexOfFirst { it.id == updatedHabit.id }
                if (index != -1) {
                    habitList[index] = updatedHabit
                    adapter.submitList(habitList)
                }
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

        db = FirebaseFirestore.getInstance()

        setupRecyclerView()
        loadTodayHabits()

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

    private fun loadTodayHabits() {
        val habitList = mutableListOf<AddHabit>()

        db.collection("habits")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val habit = document.toObject(AddHabit::class.java)
                    habitList.add(habit)
                }
                adapter.submitList(habitList)
            }
            .addOnFailureListener { exception ->
                Log.w("TodayFragment", "Error getting documents: ", exception)
            }
    }

    private fun deleteHabit(habit: AddHabit) {
        habit.id.let { habitId ->
            db.collection("habits").document(habitId)
                .delete()
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Habit deleted", Toast.LENGTH_SHORT).show()
                    loadTodayHabits()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to delete habit", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun editHabit(habit: AddHabit) {
        val intent = Intent(requireContext(), AddHabitActivity::class.java)
        intent.putExtra("habitId", habit.id)
        intent.putExtra("habitTitle", habit.title)
        intent.putExtra("habitDesc", habit.description)
        resultLauncher.launch(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

