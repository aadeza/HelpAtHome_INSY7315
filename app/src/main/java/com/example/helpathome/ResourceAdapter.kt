package com.example.helpathome

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ResourceAdapter(
    private val resources: List<Resource>,
    private val onResourceClick: (Resource) -> Unit
) : RecyclerView.Adapter<ResourceAdapter.ResourceViewHolder>() {

    class ResourceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val resourceImage: ImageView = itemView.findViewById(R.id.resourceImage)
        val resourceTitle: TextView = itemView.findViewById(R.id.resourceTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResourceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_resource, parent, false)
        return ResourceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ResourceViewHolder, position: Int) {
        val resource = resources[position]
        holder.resourceTitle.text = resource.title
        holder.resourceImage.setImageResource(resource.imageResId)

        holder.itemView.setOnClickListener {
            onResourceClick(resource)
        }
    }

    override fun getItemCount(): Int = resources.size
}

