package com.example.helpathome

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NotificationAdapter(
    private var notifications: MutableList<Notification> = mutableListOf(),
    private val onItemClick: ((Notification) -> Unit)? = null
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    inner class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.titleText)
        val message: TextView = view.findViewById(R.id.messageText)
        val time: TextView = view.findViewById(R.id.timeText)
        val colorBar: View = view.findViewById(R.id.colorBar)

        init {
            view.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick?.invoke(notifications[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]
        holder.title.text = notification.title
        holder.message.text = notification.message
        holder.time.text = notification.time
        try {
            holder.colorBar.setBackgroundColor(Color.parseColor(notification.color))
        } catch (e: IllegalArgumentException) {
            holder.colorBar.setBackgroundColor(Color.GRAY)  // fallback color
        }
    }

    override fun getItemCount(): Int = notifications.size

    // Call this method to update notifications dynamically
    fun updateNotifications(newNotifications: List<Notification>) {
        (notifications as MutableList).clear()
        notifications.addAll(newNotifications)
        notifyDataSetChanged()
    }

}
