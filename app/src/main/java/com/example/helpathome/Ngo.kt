package com.example.helpathome


data class Ngo(
    var id: String = "",              // Firebase-generated NGO ID
    val name: String = "",
    val category: String = "",
    val dateFounded: String = "",
    val founder: String = "",
    val mission: String = "",
    val vision: String = "",
    val logoUrl: String = "",
    val createdBy: String = "",        // who created it
    val approvedByAdmin: Boolean = false,  // matches your Firebase field
    val contacts: List<Contact>? = emptyList()
)