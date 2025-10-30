package com.example.helpathome.adapters

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.helpathome.R
import com.example.helpathome.models.alerts

class AlertAdapter(
    private val alertList: List<alerts>,
    private val officerNameMap: Map<String, String> = emptyMap(), // optional fallback
    private val onResolveClick: (alerts) -> Unit,
    private val onDeleteClick: (alerts) -> Unit
) : RecyclerView.Adapter<AlertAdapter.AlertViewHolder>() {

    class AlertViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textUserId: TextView = itemView.findViewById(R.id.txtUserName)
        val textLocation: TextView = itemView.findViewById(R.id.txtLocation)
        val textStatus: TextView = itemView.findViewById(R.id.textStatus)
        val buttonResolve: Button = itemView.findViewById(R.id.btnResolve)
        val buttonDelete: Button = itemView.findViewById(R.id.btnDismiss)
        val cardView: CardView = itemView.findViewById(R.id.alertCardView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.alert_item, parent, false)
        return AlertViewHolder(view)
    }

    override fun getItemCount(): Int = alertList.size

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        val alert = alertList[position]
        val context = holder.itemView.context

        holder.textUserId.text = "Name: ${alert.userName}"
        holder.textLocation.text = "Location: ${alert.lastKnownLocation?.address ?: "Unknown"}"

        val isResolved = alert.resolvedAt != null
        val isDismissed = alert.dismissedAt != null

        val timestamp = alert.resolvedAt ?: alert.dismissedAt
        val formattedTime = timestamp?.let {
            android.text.format.DateFormat.format("HH:mm", java.util.Date(it)).toString()
        }

        when {
            alert.sosActive -> {
                holder.textStatus.text = "Status: Unresolved"
                holder.textStatus.setTextColor(context.getColor(R.color.police_red))
                holder.buttonResolve.visibility = View.VISIBLE
                holder.buttonDelete.visibility = View.VISIBLE
                holder.cardView.setCardBackgroundColor(Color.WHITE)
            }
            isResolved -> {
                val byId = alert.resolvedBy ?: "Unknown"
                val byName = alert.resolvedByName
                    ?: officerNameMap[alert.resolvedBy]
                    ?: "Unknown"
                holder.textStatus.text = "Resolved by $byId at ${formattedTime ?: "--"}\nName: $byName"
                holder.textStatus.setTextColor(context.getColor(R.color.ngo_green))
                holder.buttonResolve.visibility = View.GONE
                holder.buttonDelete.visibility = View.GONE
                holder.cardView.setCardBackgroundColor(Color.parseColor("#EEEEEE"))
            }
            isDismissed -> {
                val byId = alert.dismissedBy ?: "Unknown"
                val byName = alert.dismissedByName
                    ?: officerNameMap[alert.dismissedBy]
                    ?: "Unknown"
                holder.textStatus.text = "Dismissed by $byId at ${formattedTime ?: "--"}\nName: $byName"
                holder.textStatus.setTextColor(Color.GRAY)
                holder.buttonResolve.visibility = View.GONE
                holder.buttonDelete.visibility = View.GONE
                holder.cardView.setCardBackgroundColor(Color.parseColor("#EEEEEE"))
            }
            else -> {
                holder.textStatus.text = "Status: Handled"
                holder.textStatus.setTextColor(Color.DKGRAY)
                holder.buttonResolve.visibility = View.GONE
                holder.buttonDelete.visibility = View.GONE
                holder.cardView.setCardBackgroundColor(Color.parseColor("#EEEEEE"))
            }
        }

        holder.buttonResolve.setOnClickListener {
            onResolveClick(alert)
        }

        holder.buttonDelete.setOnClickListener {
            AlertDialog.Builder(context)
                .setTitle("Dismiss Alert")
                .setMessage("Are you sure you want to dismiss this alert?")
                .setPositiveButton("Yes") { _, _ ->
                    onDeleteClick(alert)
                }
                .setNegativeButton("No", null)
                .show()
        }
    }
}