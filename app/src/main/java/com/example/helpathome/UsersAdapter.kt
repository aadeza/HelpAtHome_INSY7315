package com.example.helpathome

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class UsersAdapter(private val users: List<Users>, private val onStatusChange: (Users, String) -> Unit) :
    RecyclerView.Adapter<UsersAdapter.UsersViewHolder>(){


    inner class UsersViewHolder(view: View): RecyclerView.ViewHolder(view){

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

        holder.suspend.setOnClickListener{
            onStatusChange(user, "suspended")


        }


        holder.remove.setOnClickListener{
            onStatusChange(user, "removed")
        }
    }


    override fun getItemCount(): Int = users.size

}