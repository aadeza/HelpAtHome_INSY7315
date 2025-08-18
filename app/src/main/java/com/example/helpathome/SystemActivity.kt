package com.example.helpathome

data class SystemActivity(
    val actorId: String = "",
    val actorType: String = "",
    val category: String = "",
    val message: String = "",
    val timestamp: Long = 0,
    val color: String = ""
)