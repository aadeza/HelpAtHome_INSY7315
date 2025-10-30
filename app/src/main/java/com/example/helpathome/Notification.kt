package com.example.helpathome

data class Notification(
    val id: String = "",          // Firebase key
    val type: String = "",        // "new_ngo", "ngo_update", "sos_on", etc.
    val title: String = "",       // Heading: e.g., "XYZ NGO has been created"
    val message: String = "",     // Optional longer message
    val imageUrl: String = "",    // e.g., NGO logo
    val timestamp: Long = System.currentTimeMillis(),
    val relatedId: String? = null, // Optional related entity (e.g., NGO ID)
    val read: Boolean = false      // Track if user has read this notification
)


