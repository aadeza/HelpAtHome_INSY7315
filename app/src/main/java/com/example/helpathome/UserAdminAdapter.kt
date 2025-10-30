package com.example.helpathome

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UserAdminAdapter(
    private val userList: List<UserAdminModel>,
    private val onEditClick: (UserAdminModel) -> Unit
) : RecyclerView.Adapter<UserAdminAdapter.UserViewHolder>() {

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userIdText: TextView = itemView.findViewById(R.id.txtUserId)
        val userNameText: TextView = itemView.findViewById(R.id.txtUserName)
        val userRoleText: TextView = itemView.findViewById(R.id.txtUserRole)
        val editButton: Button = itemView.findViewById(R.id.btnEditUser)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_user, parent, false)
        return UserViewHolder(view)
    }

    override fun getItemCount(): Int = userList.size

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        holder.userIdText.text = "ID: ${user.userId}"
        holder.userNameText.text = "Name: ${user.fullName}"
        holder.userRoleText.text = "Role: ${user.role.capitalize()}"

        holder.editButton.setOnClickListener {
            onEditClick(user)
        }
    }
}