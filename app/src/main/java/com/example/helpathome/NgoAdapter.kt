package com.example.helpathome

import Ngo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NgoAdapter(
    private val ngos: List<Ngo>,
    private val onNgoClick: (Ngo) -> Unit // Called when an NGO item is clicked
) : RecyclerView.Adapter<NgoAdapter.NgoViewHolder>() {

    inner class NgoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ngoName: TextView = itemView.findViewById(R.id.ngoName)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onNgoClick(ngos[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NgoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ngo, parent, false)
        return NgoViewHolder(view)
    }

    override fun onBindViewHolder(holder: NgoViewHolder, position: Int) {
        val ngo = ngos[position]
        holder.ngoName.text = ngo.name
    }

    override fun getItemCount(): Int = ngos.size
}
