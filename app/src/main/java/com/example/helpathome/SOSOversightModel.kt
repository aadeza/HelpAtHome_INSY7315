package com.example.helpathome

data class SOSOversightModel(
    val alertId: String,
    val location: String,
    val status: String,
    val dismissedBy: String?,
    val resolvedBy: String?,
    val dismissedAt: Long? // NEW
)