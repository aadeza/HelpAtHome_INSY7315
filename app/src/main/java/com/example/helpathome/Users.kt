package com.example.helpathome

data class Users(
    val id: String = "",
    val name: String= "",
    val lastName: String = "",
    val phoneNumber: String = "",
    val userType: String = "",
    var accountStatus: String = "active"
)