package com.example.helpathome

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class SOSOversightAdapter(
    private val sosList: List<SOSOversightModel>,
    private val onCallOfficer: (String) -> Unit
) : RecyclerView.Adapter<SOSOversightAdapter.SOSViewHolder>() {

    class SOSViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val alertIdText: TextView = itemView.findViewById(R.id.txtAlertId)
        val locationText: TextView = itemView.findViewById(R.id.txtAlertLocation)
        val statusText: TextView = itemView.findViewById(R.id.txtAlertStatus)
        val officerText: TextView = itemView.findViewById(R.id.txtOfficerInfo)
        val callButton: Button = itemView.findViewById(R.id.btnCallOfficer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SOSViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sos_oversight, parent, false)
        return SOSViewHolder(view)
    }

    override fun getItemCount(): Int = sosList.size

    override fun onBindViewHolder(holder: SOSViewHolder, position: Int) {
        val sos = sosList[position]
        holder.alertIdText.text = "Alert ID: ${sos.alertId}"
        holder.locationText.text = "Location: ${sos.location}"
        holder.statusText.text = "Status: ${sos.status}"

        val officerId = sos.dismissedBy ?: sos.resolvedBy ?: "Unknown"
        val action = when {
            sos.dismissedBy != null -> "Dismissed by"
            sos.resolvedBy != null -> "Resolved by"
            else -> "Handled by"
        }

        val timeText = sos.dismissedAt?.let {
            DateFormat.format("dd MMM yyyy, HH:mm", Date(it)).toString()
        } ?: "--"

        holder.officerText.text = "$action: $officerId\nTime: $timeText"

        holder.callButton.setOnClickListener {
            if (officerId != "Unknown") {
                onCallOfficer(officerId)
            }
        }
    }
}