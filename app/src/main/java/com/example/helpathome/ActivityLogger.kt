package com.example.helpathome

import com.google.firebase.database.FirebaseDatabase


object ActivityLogger {

    private val database = FirebaseDatabase.getInstance().reference.child("SystemLogs")

    fun log(actorId : String, actorType: String, category: String, message: String, color: String) {
        val logId = database.push().key ?: return

        val log = SystemActivity(
            actorId = actorId,
            actorType = actorType,
            category = category,
            message = message,
            timestamp = System.currentTimeMillis(),
            color = color
        )
        database.child(logId).setValue(log)

    }
}