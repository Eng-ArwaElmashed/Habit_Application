package com.android.habitapplication.ui.notifications
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.android.habitapplication.Notification
import com.android.habitapplication.NotificationAdapter
import com.android.habitapplication.R
import com.android.habitapplication.databinding.FragmentNotificationsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ListenerRegistration

class NotificationsFragment : Fragment() {
    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    private lateinit var recyclerView: RecyclerView
    private val notificationList = mutableListOf<Notification>()
    private lateinit var adapter: NotificationAdapter
    private var listenerRegistration: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        recyclerView = binding.rv
        adapter = NotificationAdapter(notificationList)
        recyclerView.adapter = adapter

        setupFirestoreListener()
        setupClearAllButton()
    }

    private fun setupClearAllButton() {
        binding.clearAllButton.setOnClickListener {
            showClearAllConfirmationDialog()
        }
    }

    private fun showClearAllConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Clear All Notifications")
            .setMessage("Are you sure you want to clear all notifications? This action cannot be undone.")
            .setPositiveButton("Clear") { _, _ ->
                clearAllNotifications()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearAllNotifications() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        // Clear from Firestore
        db.collection("userNotifications")
            .document(user.uid)
            .collection("notifications")
            .get()
            .addOnSuccessListener { documents ->
                val batch = db.batch()
                documents.forEach { document ->
                    batch.delete(document.reference)
                }
                batch.commit()
                    .addOnSuccessListener {
                        // Clear from local list
                        notificationList.clear()
                        adapter.notifyDataSetChanged()
                        Toast.makeText(requireContext(), "All notifications cleared", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Failed to clear notifications", Toast.LENGTH_SHORT).show()
                    }
            }
    }

    private fun setupFirestoreListener() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        listenerRegistration = db.collection("userNotifications")
            .document(user.uid)
            .collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Toast.makeText(requireContext(), "Error loading notifications", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                notificationList.clear()
                snapshot?.documents?.forEach { document ->
                    val title = document.getString("title") ?: ""
                    val date = document.getString("date") ?: ""
                    val time = document.getString("time") ?: ""
                    val imageResId = (document.getLong("imageResId") ?: R.drawable.ic_launcher_background).toInt()

                    notificationList.add(Notification(imageResId, title, date, time))
                }
                adapter.notifyDataSetChanged()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listenerRegistration?.remove()
        _binding = null
    }

    companion object {
        const val CHANNEL_ID = "habit_reminder_channel"
    }
}