package com.example.helpathome

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SystemActivityAdapter(private val activities: List<SystemActivity>) :
    RecyclerView.Adapter<SystemActivityAdapter.SystemActivityViewHolder>() {

    inner class SystemActivityViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.titleText)
        val actorType: TextView = view.findViewById(R.id.actorType)
        val message: TextView = view.findViewById(R.id.messageText)
        val time: TextView = view.findViewById(R.id.timeText)
        val colorBar: View = view.findViewById(R.id.colorBar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SystemActivityViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sysactivity, parent, false)
        return SystemActivityViewHolder(view)
    }

    override fun onBindViewHolder(holder: SystemActivityViewHolder, position: Int) {
        val activity = activities[position]
        holder.title.text = activity.category
        holder.actorType.text = activity.actorType
        holder.message.text = activity.message
        holder.time.text = SimpleDateFormat(
            "dd MMM yyyy HH:mm:ss",
            Locale.getDefault()
        ).format(Date(activity.timestamp))
        holder.colorBar.setBackgroundColor(Color.parseColor(activity.color))
    }

    override fun getItemCount(): Int = activities.size
}