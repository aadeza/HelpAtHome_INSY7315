package com.example.helpathome

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

class NgoAdapter(
    private val ngos: List<Ngo>,
    private val onNgoClick: (Ngo) -> Unit,       // Click on entire card
    private val onSeeMoreClick: (Ngo) -> Unit   // Click on "See More" button
) : RecyclerView.Adapter<NgoAdapter.NgoViewHolder>() {

    inner class NgoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgLogo: ImageView = itemView.findViewById(R.id.imgNgoLogo)
        val txtName: TextView = itemView.findViewById(R.id.txtNgoName)
        val txtFounded: TextView = itemView.findViewById(R.id.txtDateFounded)
        val btnSeeMore: Button = itemView.findViewById(R.id.btnSeeMore)

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
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ngo_manage, parent, false)
        return NgoViewHolder(view)
    }

    override fun onBindViewHolder(holder: NgoViewHolder, position: Int) {
        val ngo = ngos[position]

        holder.txtName.text = ngo.name
        holder.txtFounded.text = "Founded: ${ngo.dateFounded}"

        // Load logo from URL using Glide; fallback to default icon if null
        val logoUrl = ngo.logoUrl
        if (!logoUrl.isNullOrEmpty()) {
            Glide.with(holder.imgLogo.context)
                .load(logoUrl)
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(holder.imgLogo)
        } else {
            holder.imgLogo.setImageResource(R.drawable.ic_launcher_foreground)
        }

        // "See More" button click
        holder.btnSeeMore.setOnClickListener {
            onSeeMoreClick(ngo)
        }
    }

    override fun getItemCount(): Int = ngos.size
}
