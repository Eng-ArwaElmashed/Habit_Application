package com.android.habitapplication.ui.notifications

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.habitapplication.Notification
import com.android.habitapplication.NotificationAdapter
import com.android.habitapplication.R
import com.android.habitapplication.databinding.FragmentTodayBinding

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [NotificationsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class NotificationsFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }
    private var _binding: FragmentTodayBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // 1. Inflate the layout
        val rootView = inflater.inflate(R.layout.fragment_notifications, container, false)

        // 2. Access RecyclerView from the inflated view
        val recyclerView = rootView.findViewById<RecyclerView>(R.id.rv)

        // 3. Sample data
        val sampleList = listOf(
            Notification(R.drawable.lowebody_workout, "Your habit is on fire! 🔥", "04/19/2025"),
            Notification(R.drawable.yoga1, "You missed a day! 😬", "04/18/2025"),
            Notification(R.drawable.book, "Streak resumed. Great job!", "04/17/2025")
        )

        // 4. Set adapter
        recyclerView.adapter = NotificationAdapter(sampleList)

        // 5. Return the view
        return rootView
    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment NotificationsFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            NotificationsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}