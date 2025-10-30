package com.example.helpathome

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class MessageModerationAdapter(
    private val messageList: List<MessageModerationModel>,
    private val onDeleteUser: (String) -> Unit,
    private val onMarkOkay: (String) -> Unit // new callback for "Okay" action
) : RecyclerView.Adapter<MessageModerationAdapter.MessageViewHolder>() {

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val senderIdText: TextView = itemView.findViewById(R.id.txtSenderId)
        val messageContentText: TextView = itemView.findViewById(R.id.txtMessageContent)
        val statusText: TextView = itemView.findViewById(R.id.txtMessageStatus)
        val dismissReasonText: TextView = itemView.findViewById(R.id.txtDismissReason)
        val userInfoText: TextView = itemView.findViewById(R.id.txtUserInfo)
        val timestampText: TextView = itemView.findViewById(R.id.txtTimestamp)
        val deleteButton: Button = itemView.findViewById(R.id.btnDeleteUser)
        val okayButton: Button = itemView.findViewById(R.id.btnOkay) // new button
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message_moderation, parent, false)
        return MessageViewHolder(view)
    }

    override fun getItemCount(): Int = messageList.size

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messageList[position]

        holder.senderIdText.text = "Sender ID: ${message.senderId}"
        holder.messageContentText.text = message.content
        holder.statusText.text = when {
            message.reported && message.dismissed -> "Status: Reported & Dismissed"
            message.reported -> "Status: Reported"
            message.dismissed -> "Status: Dismissed"
            else -> "Status: Normal"
        }
        holder.dismissReasonText.text = "Dismiss Reason: ${message.dismissReason}"
        holder.userInfoText.text = "${message.userName} | ${message.userEmail} | ${message.userPhone}"

        val formattedTime = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            .format(Date(message.timestamp))
        holder.timestampText.text = "Submitted: $formattedTime"

        holder.deleteButton.setOnClickListener {
            onDeleteUser(message.senderId)
        }

        holder.okayButton.setOnClickListener {
            onMarkOkay(message.senderId)
        }
    }
}