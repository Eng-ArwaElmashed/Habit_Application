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
import com.android.habitapplication.databinding.FragmentTodayBinding
import com.android.habitapplication.model.AddHabit
import com.android.habitapplication.ui.all_habits.AddHabitActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

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
            updatedHabit?.let {
                // Reset calendar to today's date
                calendar.timeInMillis = System.currentTimeMillis()
                habitViewModel.setCurrentDate(calendar)
                setupDateDisplay()
                setupDayButtons() // Update button states for today
                loadHabitsForCurrentDay()
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

        habitViewModel = ViewModelProvider(this).get(HabitViewModel::class.java)
        habitViewModel.setCurrentDate(calendar) // Initialize with current date
        setupDateDisplay()
        setupRecyclerView()
        
        habitViewModel.habitList.observe(viewLifecycleOwner) { habits ->
            adapter.submitList(habits)
        }

        loadHabitsForCurrentDay()

        binding.addBtn.setOnClickListener {
            val intent = Intent(requireContext(), AddHabitActivity::class.java)
            resultLauncher.launch(intent)
        }

        setupDayButtons()
    }

    private fun setupDateDisplay() {
        binding.todayText.text = dateFormat.format(calendar.time)
    }

    private fun setupDayButtons() {
        val currentDay = dayFormat.format(calendar.time)
        
        // Reset all buttons to unselected state
        binding.apply {
            SunBtn.isSelected = false
            MonBtn.isSelected = false
            TueBtn.isSelected = false
            WedBtn.isSelected = false
            ThuBtn.isSelected = false
            FriBtn.isSelected = false
            SatBtn.isSelected = false

            // Set the current day button as selected
            when (currentDay) {
                "Sun" -> SunBtn.isSelected = true
                "Mon" -> MonBtn.isSelected = true
                "Tue" -> TueBtn.isSelected = true
                "Wed" -> WedBtn.isSelected = true
                "Thu" -> ThuBtn.isSelected = true
                "Fri" -> FriBtn.isSelected = true
                "Sat" -> SatBtn.isSelected = true
            }

            // Set click listeners for day selection
            SunBtn.setOnClickListener { updateSelectedDay(Calendar.SUNDAY) }
            MonBtn.setOnClickListener { updateSelectedDay(Calendar.MONDAY) }
            TueBtn.setOnClickListener { updateSelectedDay(Calendar.TUESDAY) }
            WedBtn.setOnClickListener { updateSelectedDay(Calendar.WEDNESDAY) }
            ThuBtn.setOnClickListener { updateSelectedDay(Calendar.THURSDAY) }
            FriBtn.setOnClickListener { updateSelectedDay(Calendar.FRIDAY) }
            SatBtn.setOnClickListener { updateSelectedDay(Calendar.SATURDAY) }

            // Forward button to next week
            forwardBtn.setOnClickListener {
                calendar.add(Calendar.WEEK_OF_YEAR, 1)
                habitViewModel.setCurrentDate(calendar)
                setupDateDisplay()
                setupDayButtons()
                loadHabitsForCurrentDay()
            }

            // Backward button to previous week
            backwardBtn.setOnClickListener {
                calendar.add(Calendar.WEEK_OF_YEAR, -1)
                habitViewModel.setCurrentDate(calendar)
                setupDateDisplay()
                setupDayButtons()
                loadHabitsForCurrentDay()
            }
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

