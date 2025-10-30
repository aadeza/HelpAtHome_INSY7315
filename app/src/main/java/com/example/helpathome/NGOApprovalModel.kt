package com.example.helpathome

data class NGOApprovalModel(
    val ngoId: String,
    val name: String,
    val description: String,
    val contact: String,
    val approved: Boolean,
    val logoUrl: String
)