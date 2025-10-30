package com.example.helpathome


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NgoUpdateAdapter(private val updates: List<NgoUpdate>) :
    RecyclerView.Adapter<NgoUpdateAdapter.UpdateViewHolder>() {

    inner class UpdateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtUpdateText: TextView = itemView.findViewById(R.id.txtUpdateText)
        val txtUpdateTimestamp: TextView = itemView.findViewById(R.id.txtUpdateTimestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UpdateViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ngo_update, parent, false)
        return UpdateViewHolder(view)
    }

    override fun onBindViewHolder(holder: UpdateViewHolder, position: Int) {
        val update = updates[position]
        holder.txtUpdateText.text = update.text

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(update.timestamp))
        holder.txtUpdateTimestamp.text = "Posted on: $formattedDate"
    }

    override fun getItemCount(): Int = updates.size
}