package com.example.helpathome

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ContactAdapter(
    private val items: MutableList<Contact>,
    private val onRemove: (Int) -> Unit
) : RecyclerView.Adapter<ContactAdapter.Holder>() {

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val txtValue: TextView = view.findViewById(R.id.txtContactValue)
        val txtMeta: TextView = view.findViewById(R.id.txtContactMeta)
        val btnRemove: ImageButton = view.findViewById(R.id.btnRemoveContact)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.contact_item, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val c = items[position]
        holder.txtValue.text = c.value
        val subtypeText = if (c.type == "phone") c.subtype.capitalize() else "Email"
        val labelText = if (c.label.isNotBlank()) " • ${c.label}" else ""
        holder.txtMeta.text = "$subtypeText$labelText"

        holder.btnRemove.setOnClickListener {
            onRemove(position)
        }
    }

    override fun getItemCount(): Int = items.size

    fun add(contact: Contact) {
        items.add(contact)
        notifyItemInserted(items.size - 1)
    }

    fun removeAt(pos: Int) {
        items.removeAt(pos)
        notifyItemRemoved(pos)
    }

    fun all(): List<Contact> = items.toList()
}
