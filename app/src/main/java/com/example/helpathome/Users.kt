package com.example.helpathome

data class Users(
    val id: String = "",
    val name: String= "",
    val lastName: String = "",
    val userType: String = "",
    val location: String = "",
    var accountStatus: String = "active"
)