package com.example.helpathome

data class MessageModerationModel(
    val messageId: String,
    val senderId: String,
    val content: String,
    val dismissed: Boolean,
    val reported: Boolean,
    val dismissReason: String,
    val userName: String,
    val userEmail: String,
    val userPhone: String,
    val timestamp: Long
)