package com.example.helpathome

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UsersAdapter(
    private val users: List<Users>,
    private val onStatusChange: (Users, String) -> Unit
) : RecyclerView.Adapter<UsersAdapter.UsersViewHolder>() {

    inner class UsersViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val username: TextView = view.findViewById(R.id.userName)
        val userType: TextView = view.findViewById(R.id.userType)
        val statusText: TextView = view.findViewById(R.id.accStatus)
        val suspend: Button = view.findViewById(R.id.buttonSuspend)
        val remove: Button = view.findViewById(R.id.buttonRemove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsersViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_users, parent, false)
        return UsersViewHolder(view)
    }

    override fun onBindViewHolder(holder: UsersViewHolder, position: Int) {
        val user = users[position]
        holder.username.text = "${user.name} ${user.lastName}"
        holder.userType.text = "User Type: ${user.userType}"
        holder.statusText.text = "Account Status: ${user.accountStatus}"

        // 🔹 Change button texts dynamically based on status
        when (user.accountStatus.lowercase()) {
            "active" -> {
                holder.suspend.visibility = View.VISIBLE
                holder.suspend.text = "Suspend"
                holder.remove.text = "Remove"
            }
            "suspended" -> {
                holder.suspend.visibility = View.VISIBLE
                holder.suspend.text = "Unsuspend"
                holder.remove.text = "Remove"
            }
            "removed" -> {
                holder.suspend.visibility = View.GONE
                holder.remove.text = "Add"
            }
            else -> {
                holder.suspend.visibility = View.VISIBLE
                holder.suspend.text = "Suspend"
                holder.remove.text = "Remove"
            }
        }


        holder.suspend.setOnClickListener {
            when (user.accountStatus.lowercase()) {
                "active" -> {
                    onStatusChange(user, "suspended")
                }
                "suspended" -> {
                    onStatusChange(user, "active")
                }
            }
        }


        holder.remove.setOnClickListener {
            when (user.accountStatus.lowercase()) {
                "active", "suspended" -> {
                    onStatusChange(user, "removed")
                }
                "removed" -> {
                    onStatusChange(user, "active")
                }
            }
        }
    }

    override fun getItemCount(): Int = users.size
}
