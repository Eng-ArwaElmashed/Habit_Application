package com.android.habitapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NotificationAdapter(private val notifications: List<Notification>) :
    RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.image)
        val titleView: TextView = view.findViewById(R.id.tv)
        val dateView: TextView = view.findViewById(R.id.tv2)
        val timeView: TextView = view.findViewById(R.id.tv3)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.notification_item_list, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]
        holder.imageView.setImageResource(notification.imageResId)
        holder.titleView.text = notification.title
        holder.dateView.text = notification.date
        holder.timeView.text = notification.time
    }

    override fun getItemCount(): Int = notifications.size
}
