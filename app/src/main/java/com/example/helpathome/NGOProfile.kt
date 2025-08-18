package com.example.helpathome
import com.google.android.gms.common.api.Status

data class NGOProfile(
    val id: String = "",
    val name: String = "",
    val founder: String = "",
    val category: String = "",
    val dateFounded: String = "",
    var status: String = "Awaiting Approval"

)