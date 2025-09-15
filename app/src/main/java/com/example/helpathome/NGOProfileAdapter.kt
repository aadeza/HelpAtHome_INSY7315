package com.example.helpathome

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NGOProfilesAdapter(private val profiles: List<NGOProfile>, private val onStatusChange : (NGOProfile, String) -> Unit) :
    RecyclerView.Adapter<NGOProfilesAdapter.NGOProfilesViewHolder>() {

    inner class NGOProfilesViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ngoTitle: TextView = view.findViewById(R.id.ngoTitle)
        val ngoName: TextView = view.findViewById(R.id.ngoName)
        val ngoFounder: TextView = view.findViewById(R.id.ngoFounder)
        val category: TextView = view.findViewById(R.id.ngoCategory)
        val date: TextView = view.findViewById(R.id.dateFounded)
        val status: TextView = view.findViewById(R.id.profileStaus)
        val btnApprove: Button = view.findViewById(R.id.buttonApprove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NGOProfilesViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ngoprofiles, parent, false)
        return NGOProfilesViewHolder(view)
    }

    override fun onBindViewHolder(holder: NGOProfilesViewHolder, position: Int) {
        val profile = profiles[position]
        holder.ngoTitle.text = "NGO Profile Info"
        holder.ngoName.text = "NGO Name: ${profile.name}"
        holder.ngoFounder.text = "Founder: ${profile.founder}"
        holder.category.text = "Category: ${profile.category}"
        holder.date.text = "Founded: ${profile.dateFounded}"
        holder.status.text = "Status: ${profile.status}"

        holder.btnApprove.setOnClickListener{
            onStatusChange(profile, "Approved")
        }

    }

    override fun getItemCount(): Int = profiles.size
}