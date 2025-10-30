package com.example.helpathome

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class NgoBrowseAdapter(
    private val ngos: List<Ngo>,
    private val onItemClick: (Ngo) -> Unit
) : RecyclerView.Adapter<NgoBrowseAdapter.NgoViewHolder>() {

    inner class NgoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgLogo: ImageView = itemView.findViewById(R.id.imgNgoLogo)
        val txtName: TextView = itemView.findViewById(R.id.txtNgoName)
        val txtCategory: TextView = itemView.findViewById(R.id.txtNgoCategory)
        val txtFounded: TextView = itemView.findViewById(R.id.txtNgoFounded)
        val txtApprovalStatus: TextView = itemView.findViewById(R.id.txtApprovalStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NgoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ngo_browse, parent, false)
        return NgoViewHolder(view)
    }

    override fun onBindViewHolder(holder: NgoViewHolder, position: Int) {
        val ngo = ngos[position]
        holder.txtName.text = ngo.name
        holder.txtCategory.text = ngo.category
        holder.txtFounded.text = "Founded: ${ngo.dateFounded}"

        Glide.with(holder.itemView.context)
            .load(ngo.logoUrl)
            .placeholder(R.drawable.ic_launcher_foreground)
            .error(R.drawable.ic_launcher_foreground)
            .into(holder.imgLogo)

        // Show approval badge if not approved
        if (ngo.approvedByAdmin) {
            holder.txtApprovalStatus.visibility = View.GONE
        } else {
            holder.txtApprovalStatus.visibility = View.VISIBLE
        }

        holder.itemView.setOnClickListener {
            onItemClick(ngo)
        }
    }

    override fun getItemCount(): Int = ngos.size
}