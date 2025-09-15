package com.example.helpathome

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.helpathome.R
import com.example.helpathome.models.alerts
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class AlertAdapter(
    private val alertList: List<alerts>,
    private val onResolveClick: (alerts) -> Unit,
    private val onDeleteClick: (alerts) -> Unit
) : RecyclerView.Adapter<AlertAdapter.AlertViewHolder>() {

    class AlertViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textUserId: TextView = itemView.findViewById(R.id.textUserId)
        val textLocation: TextView = itemView.findViewById(R.id.textLocation)
        val buttonResolve: Button = itemView.findViewById(R.id.buttonResolve)
        val buttonDelete: Button = itemView.findViewById(R.id.buttonDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.alert_item, parent, false)
        return AlertViewHolder(view)
    }

    override fun getItemCount(): Int = alertList.size

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        val alert = alertList[position]


        val userIdText = "User ID: ${alert.userId ?: "Unknown"}"
        val statusEmoji = if (alert.sosActive) "🔴" else "✅"
        val statusText = if (alert.sosActive) "ACTIVE" else "RESOLVED"

        holder.textUserId.text = "$userIdText  •  $statusEmoji $statusText"
        holder.textUserId.text = "User ID: ${alert.userId ?: "Unkown"}"
        holder.textLocation.text = "Location: ${alert.lastKnownLocation?.address ?:"Unknown"}"

        // Tap-to-copy user ID
        holder.textUserId.setOnClickListener {
            val clipboard = holder.itemView.context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("User ID", alert.userId ?: "")
            clipboard.setPrimaryClip(clip)
            Toast.makeText(holder.itemView.context, "User ID copied", Toast.LENGTH_SHORT).show()
        }

        val timestamp = alert.lastKnownLocation?.timestamp ?: alert.resolvedAt
        val formattedTime = formatTimestamp(timestamp)
        val timeAgo = getTimeAgo(timestamp)

        holder.textUserId.text = "User ID: ${alert.userId}"
        holder.textLocation.text = "Location: ${alert.lastKnownLocation?.address ?: "Unknown"}"


        holder.buttonResolve.visibility = if (alert.sosActive) View.VISIBLE else View.GONE
        holder.buttonResolve.setOnClickListener { onResolveClick(alert) }
        holder.buttonDelete.setOnClickListener { onDeleteClick(alert) }
    }

    private fun formatTimestamp(timestamp: Long?): String {
        return timestamp?.let {
            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            sdf.format(Date(it))
        } ?: "Unknown time"
    }
        private fun getTimeAgo(timestamp: Long?): String {
            if (timestamp == null) return "Unknown time"
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            val seconds = diff / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24

            return when {
                days > 0 -> "$days day${if (days > 1) "s" else ""} ago"
                hours > 0 -> "$hours hour${if (hours > 1) "s" else ""} ago"
                minutes > 0 -> "$minutes minute${if (minutes > 1) "s" else ""} ago"
                else -> "Just now"
            }
        }

    }
