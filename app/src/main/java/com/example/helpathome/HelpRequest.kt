package com.example.helpathome

data class HelpRequest(
    val text: String = "",
    val timestamp: Long = 0L,
    val userEmail: String = "",
    val userName: String = "",
    val userPhone: String = ""
)