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

    ) : RecyclerView.Adapter<AlertAdapter.AlertViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.alert_item, parent, false)
        return AlertViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        val alert = alertList[position]
        holder.bind(alert)
    }

    override fun getItemCount(): Int = alertList.size

    inner class AlertViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userId:TextView = itemView.findViewById(R.id.textUserId)
        private val locationText: TextView = itemView.findViewById(R.id.textLocation)
        private val timestampText: TextView = itemView.findViewById(R.id.textTimestamp)
        private val statusText: TextView = itemView.findViewById(R.id.textStatus)
        private val resolveButton: Button = itemView.findViewById(R.id.btnResolve)

        fun bind(alert: alerts) {

            userId.text = "UserId: ${alert.userId}"
            locationText.text = "Location: ${alert.location}"
            timestampText.text = "Time: ${alert.timestamp}"
            statusText.text = if (alert.sosActive) "Status: ACTIVE" else "Status: RESOLVED"

            resolveButton.visibility = if (alert.sosActive) View.VISIBLE else View.GONE
            resolveButton.setOnClickListener { onResolveClick(alert) }


        }
    }
}