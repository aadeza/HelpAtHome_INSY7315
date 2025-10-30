package com.example.helpathome

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class NGOApprovalAdapter(
    private val ngoList: List<NGOApprovalModel>,
    private val onApprovalToggle: (NGOApprovalModel, Boolean) -> Unit
) : RecyclerView.Adapter<NGOApprovalAdapter.NGOViewHolder>() {

    class NGOViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val logoImage: ImageView = itemView.findViewById(R.id.imgNGOLogo)
        val nameText: TextView = itemView.findViewById(R.id.txtNGOName)
        val descriptionText: TextView = itemView.findViewById(R.id.txtNGODescription)
        val contactText: TextView = itemView.findViewById(R.id.txtNGOContact)
        val approvalSwitch: Switch = itemView.findViewById(R.id.switchNGOApproval)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NGOViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ngo_approval, parent, false)
        return NGOViewHolder(view)
    }

    override fun getItemCount(): Int = ngoList.size

    override fun onBindViewHolder(holder: NGOViewHolder, position: Int) {
        val ngo = ngoList[position]

        holder.nameText.text = ngo.name
        holder.descriptionText.text = "Description: ${ngo.description}"
        holder.contactText.text = "Contact: ${ngo.contact}"
        holder.approvalSwitch.isChecked = ngo.approved

        // Load logo using Glide
        Glide.with(holder.itemView.context)
            .load(ngo.logoUrl)
            .placeholder(R.drawable.ic_placeholder_logo)
            .error(R.drawable.ic_placeholder_logo)
            .into(holder.logoImage)

        holder.approvalSwitch.setOnCheckedChangeListener { _, isChecked ->
            onApprovalToggle(ngo, isChecked)
        }
    }
}