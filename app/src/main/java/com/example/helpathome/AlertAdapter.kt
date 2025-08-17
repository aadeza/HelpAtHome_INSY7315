package com.example.helpathome.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.helpathome.R
import com.example.helpathome.models.alerts

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
        holder.textUserId.text = "User ID: ${alert.userId}"
        holder.textLocation.text = "Location: ${alert.lastKnownLocation?.address ?: "Unknown"}"

        // Show resolve button only for active alerts
        holder.buttonResolve.visibility = if (alert.sosActive) View.VISIBLE else View.GONE

        holder.buttonResolve.setOnClickListener {
            onResolveClick(alert)
        }

        holder.buttonDelete.setOnClickListener {
            onDeleteClick(alert)
        }
    }
}
