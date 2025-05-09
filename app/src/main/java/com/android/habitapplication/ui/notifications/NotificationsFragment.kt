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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class NotificationsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private val notificationList = mutableListOf<Notification>()
    private lateinit var adapter: NotificationAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_notifications, container, false)
        recyclerView = rootView.findViewById(R.id.rv)
        adapter = NotificationAdapter(notificationList)
        recyclerView.adapter = adapter

        fetchNotificationsFromFirestore()

        return rootView
    }

    private fun fetchNotificationsFromFirestore() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("userNotifications")
            .document(user.uid)
            .collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                notificationList.clear()
                for (document in result) {
                    val title = document.getString("title") ?: ""
                    val date = document.getString("date") ?: ""
                    val imageResId = (document.getLong("imageResId") ?: R.drawable.ic_launcher_background).toInt()

                    notificationList.add(Notification(imageResId, title, date))
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load notifications", Toast.LENGTH_SHORT).show()
            }
    }
    companion object {
        const val CHANNEL_ID = "habit_reminder_channel"
    }
}