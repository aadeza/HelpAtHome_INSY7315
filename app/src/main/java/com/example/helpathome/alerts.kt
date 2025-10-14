package com.example.helpathome.models

// Nested model for location
data class LastKnownLocation(
    var address: String? = null,
    var latitude: Double? = null,
    var longitude: Double? = null,
    var timestamp: Long? = null
)


data class alerts(
    var userId: String? = null,
    var lastKnownLocation: LastKnownLocation? = null,
    var sosActive: Boolean = true,
    var resolvedAt: Long? = null
)
