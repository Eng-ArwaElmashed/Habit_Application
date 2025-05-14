package com.android.habitapplication.ui.today

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
import com.android.habitapplication.HabitViewModel
import com.android.habitapplication.R
import com.android.habitapplication.databinding.FragmentTodayBinding
import com.android.habitapplication.model.AddHabit
import com.android.habitapplication.ui.all_habits.AddHabitActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton

class TodayFragment : Fragment() {

    private var _binding: FragmentTodayBinding? = null
    private val binding get() = _binding!!
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
    private val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())

    private lateinit var adapter: HabitAdapter
    private lateinit var habitViewModel: HabitViewModel
    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val updatedHabit = result.data?.getSerializableExtra("updatedHabit") as? AddHabit
            // val editedDateMillis = result.data?.getLongExtra("selectedDate", System.currentTimeMillis()) 
            //     ?: System.currentTimeMillis() // This line is removed/commented

            updatedHabit?.let {
                // Reset calendar to today's date (original behavior)
                calendar.timeInMillis = System.currentTimeMillis()
                habitViewModel.setCurrentDate(calendar) 
                setupDateDisplay() 
                setupDayButtons() 
                loadHabitsForCurrentDay() 
            }
        }
    }

    private lateinit var dayButtonsList: List<MaterialButton>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTodayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        habitViewModel = ViewModelProvider(this).get(HabitViewModel::class.java)
        habitViewModel.setCurrentDate(calendar) // Initialize with current date
        
        // Initialize day buttons list
        dayButtonsList = listOf(
            binding.SunBtn,
            binding.MonBtn,
            binding.TueBtn,
            binding.WedBtn,
            binding.ThuBtn,
            binding.FriBtn,
            binding.SatBtn
        )
        
        setupDateDisplay()
        setupRecyclerView()
        setupDayButtons()

        habitViewModel.habitList.observe(viewLifecycleOwner) { habits ->
            adapter.submitList(habits)
        }

        loadHabitsForCurrentDay()

        binding.addBtn.setOnClickListener {
            val intent = Intent(requireContext(), AddHabitActivity::class.java)
            resultLauncher.launch(intent)
        }
    }

    private fun setupDateDisplay() {
        binding.todayText.text = dateFormat.format(calendar.time)
    }

    private fun setupDayButtons() {
        // Get color and dimension resources
        val mainPinkColor = ContextCompat.getColor(requireContext(), R.color.main_pink)
        val lightPinkColor = ContextCompat.getColor(requireContext(), R.color.light_pink)
        val strokeWidth = resources.getDimensionPixelSize(R.dimen.day_button_stroke_width)
        val mainPinkColorStateList = ColorStateList.valueOf(mainPinkColor)
        val lightPinkColorStateList = ColorStateList.valueOf(lightPinkColor)

        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // Calendar.SUNDAY = 1, ... SATURDAY = 7

        dayButtonsList.forEachIndexed { index, button ->
            // Set common properties
            button.strokeWidth = strokeWidth
            button.strokeColor = mainPinkColorStateList

            // Calendar.DAY_OF_WEEK is 1-indexed (Sun=1), our list is 0-indexed (Sun=0)
            if (currentDayOfWeek == (index + 1)) {
                button.backgroundTintList = mainPinkColorStateList
                button.isSelected = true // Keep for consistency if any style relies on it
            } else {
                button.backgroundTintList = lightPinkColorStateList
                button.isSelected = false
            }

            // Set click listeners to update the selected day and reload
            button.setOnClickListener { 
                // Determine which day this button represents (Calendar.SUNDAY, MONDAY, etc.)
                updateSelectedDay(index + 1) // index + 1 will match Calendar.DAY_OF_WEEK constants
            }
        }
        
        // Keep existing forward/backward button logic
        binding.forwardBtn.setOnClickListener {
            calendar.add(Calendar.WEEK_OF_YEAR, 1)
            habitViewModel.setCurrentDate(calendar)
            setupDateDisplay()
            setupDayButtons() // Refresh day button states
            loadHabitsForCurrentDay()
        }

        binding.backwardBtn.setOnClickListener {
            calendar.add(Calendar.WEEK_OF_YEAR, -1)
            habitViewModel.setCurrentDate(calendar)
            setupDateDisplay()
            setupDayButtons() // Refresh day button states
            loadHabitsForCurrentDay()
        }
    }

    private fun updateSelectedDay(dayOfWeek: Int) {
        // Calculate the difference between current day and selected day
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val diff = dayOfWeek - currentDayOfWeek
        
        // Adjust the calendar to the selected day
        calendar.add(Calendar.DAY_OF_WEEK, diff)
        
        // Sync the selected date with ViewModel
        habitViewModel.setCurrentDate(calendar)
        setupDateDisplay()
        setupDayButtons()
        loadHabitsForCurrentDay()
    }

    private fun loadHabitsForCurrentDay() {
        habitViewModel.loadHabitsForDate(calendar)
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
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(userId)
            .collection("habits").document(habit.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Habit deleted", Toast.LENGTH_SHORT).show()
                loadHabitsForCurrentDay()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to delete habit", Toast.LENGTH_SHORT).show()
            }
    }

    private fun editHabit(habit: AddHabit) {
        val intent = Intent(requireContext(), AddHabitActivity::class.java)
        intent.putExtra("habit_id", habit.id)
        intent.putExtra("habitTitle", habit.title)
        intent.putExtra("habitDesc", habit.description)
        intent.putExtra("habitIcon", habit.icon)
        intent.putExtra("selectedDate", calendar.timeInMillis)
        resultLauncher.launch(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

