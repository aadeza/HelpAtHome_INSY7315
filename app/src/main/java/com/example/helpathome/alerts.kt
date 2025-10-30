package com.example.helpathome.models

// Nested model for location
data class LastKnownLocation(
    var address: String? = null,
    var latitude: Double? = null,
    var longitude: Double? = null,
    var timestamp: Long? = null
)

data class alerts(
    val userId: String? = null,
    val userName: String? = null,
    val lastKnownLocation: LastKnownLocation? = null,
    val sosActive: Boolean = false,
    val resolvedAt: Long? = null,
    val resolvedBy: String? = null,
    val resolvedByName: String? = null,
    val dismissedAt: Long? = null,
    val dismissedBy: String? = null,
    val dismissedByName: String? = null
)