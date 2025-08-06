package com.example.helpathome.models

data class alerts(
    var userId: String? = null,
    var location: String = "-26.1087, 28.0567 (Sandton, Johannesburg)",
    var timestamp: String = "1744990251996",
    var sosActive: Boolean = true,
    var resolvedAt: Long? = null
)
