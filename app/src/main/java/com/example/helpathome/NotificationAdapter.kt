package com.example.helpathome

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.*

class NotificationAdapter(
    private var notifications: MutableList<Notification> = mutableListOf(),
    private val onNotificationClick: ((Notification) -> Unit)? = null,
    private val role: String = "civilian" // "ngo", "admin", "law", etc.
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleText: TextView = itemView.findViewById(R.id.titleText)
        val messageText: TextView = itemView.findViewById(R.id.messageText)
        val timeText: TextView = itemView.findViewById(R.id.timeText)
        val logoImage: ImageView? = itemView.findViewById(R.id.imgNotificationLogo) // Only exists in civilian layout

        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onNotificationClick?.invoke(notifications[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val layoutId = when (role) {
            "ngo" -> R.layout.item_notification_ngo
            else -> R.layout.item_notification
        }
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]

        // Role-specific title fallback
        holder.titleText.text = when {
            !notification.title.isNullOrBlank() -> notification.title
            role == "ngo" -> "NGO Alert"
            role == "admin" -> "Admin Notification"
            role == "law" -> "Law Enforcement Update"
            else -> "Notification"
        }

        // Role-specific message formatting
        holder.messageText.text = when (role) {
            "law" -> formatLawMessage(notification)
            "admin" -> formatAdminMessage(notification)
            else -> notification.message ?: "No message provided"
        }

        // Format timestamp safely
        val formattedTime = try {
            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            sdf.format(Date(notification.timestamp ?: 0L))
        } catch (e: Exception) {
            "Unknown time"
        }
        holder.timeText.text = formattedTime

        // Only load image if layout includes it
        holder.logoImage?.let { imageView ->
            if (!notification.imageUrl.isNullOrEmpty()) {
                Glide.with(holder.itemView.context)
                    .load(notification.imageUrl)
                    .placeholder(R.drawable.ic_placeholder_logo)
                    .error(R.drawable.ic_placeholder_logo)
                    .into(imageView)
            } else {
                imageView.setImageResource(R.drawable.ic_placeholder_logo)
            }
        }
    }

    override fun getItemCount(): Int = notifications.size

    fun updateNotifications(newList: List<Notification>) {
        notifications.clear()
        notifications.addAll(newList.sortedByDescending { it.timestamp ?: 0L })
        notifyDataSetChanged()
    }

    private fun formatAdminMessage(notification: Notification): String {
        return when (notification.type) {
            "ngo_status" -> "NGO status update: ${notification.message}"
            "user_report" -> "User report received: ${notification.message}"
            else -> notification.message ?: "No message"
        }
    }

    private fun formatLawMessage(notification: Notification): String {
        return when (notification.type) {
            "incident_alert" -> "Incident reported: ${notification.message}"
            "verification_request" -> "Verification needed: ${notification.message}"
            else -> notification.message ?: "No message"
        }
    }
}