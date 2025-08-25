package com.example.helpathome.models

// Model for user's last known location
data class LastKnownLocation(
    var address: String? = null,
    var latitude: Double? = null,
    var longitude: Double? = null,
    var timestamp: Long? = null // Unix time in milliseconds
)

// Model for SOS alerts
data class alerts(
    var userId: String? = null,
    var lastKnownLocation: LastKnownLocation? = null,
    var sosActive: Boolean = true,
    var resolvedAt: Long? = null // Null if unresolved
)

