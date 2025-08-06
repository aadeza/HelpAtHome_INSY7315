package com.example.helpathome

import Ngo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView

class NgoAdapter(private val ngos: List<Ngo>) : RecyclerView.Adapter<NgoAdapter.NgoViewHolder>() {

    inner class NgoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ngoName: TextView = itemView.findViewById(R.id.ngoName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NgoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ngo, parent, false)
        return NgoViewHolder(view)
    }

    override fun onBindViewHolder(holder: NgoViewHolder, position: Int) {
        holder.ngoName.text = ngos[position].name
    }

    override fun getItemCount(): Int = ngos.size
}
