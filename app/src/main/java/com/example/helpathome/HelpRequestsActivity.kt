package com.example.helpathome

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class HelpRequestsActivity : AppCompatActivity() {

    lateinit var database: FirebaseDatabase
    lateinit var auth: FirebaseAuth
    private lateinit var tvStatus: TextView
    private lateinit var etHelpMessage: EditText
    private lateinit var tvNgoName: TextView
    private lateinit var btnSendRequest: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help_request)


        if (!::database.isInitialized) {
            database = FirebaseDatabase.getInstance()
        }
        if (!::auth.isInitialized) {
            auth = FirebaseAuth.getInstance()
        }

        // UI elements
        tvNgoName = findViewById(R.id.tvNgoName)
        etHelpMessage = findViewById(R.id.etHelpMessage)
        btnSendRequest = findViewById(R.id.btnSendRequest)
        tvStatus = findViewById(R.id.tvStatus)

        val ngoName = intent.getStringExtra("ngoName") ?: "Unknown NGO"
        tvNgoName.text = "Requesting Help from: $ngoName"

        btnSendRequest.setOnClickListener {
            val message = etHelpMessage.text.toString().trim()
            if (message.isEmpty()) {
                Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentUser = auth.currentUser
            if (currentUser == null) {
                Toast.makeText(this, "You must be logged in", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userId = currentUser.uid
            val userRef = database.getReference("Users").child(userId)

            // Fetch user info (name, surname) before sending request
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val firstName = snapshot.child("firstName").getValue(String::class.java) ?: "Unknown"
                    val lastName = snapshot.child("lastName").getValue(String::class.java) ?: "User"
                    val userType = snapshot.child("userType").getValue(String::class.java) ?: "Civillian"
                    val fullName = "$firstName $lastName"

                    val request = mapOf(
                        "userId" to userId,
                        "userName" to fullName,
                        "ngoName" to ngoName,
                        "message" to message,
                        "timestamp" to System.currentTimeMillis()
                    )

                    database.getReference("help_requests")
                        .push()
                        .setValue(request)
                        .addOnSuccessListener {
                            tvStatus.visibility = View.VISIBLE
                            tvStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark))
                            tvStatus.text = "✅ Request sent successfully!"
                            etHelpMessage.text.clear()

                            ActivityLogger.log(
                                actorId = userId,
                                actorType = userType,
                                category = "Help Request",
                                message = "User $fullName sent help request to $ngoName",
                                color = "#ff8be2"
                            )
                        }
                        .addOnFailureListener {
                            tvStatus.visibility = View.VISIBLE
                            tvStatus.setTextColor(resources.getColor(android.R.color.holo_red_dark))
                            tvStatus.text = "❌ Failed to send request: ${it.message}"
                        }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@HelpRequestsActivity, "Failed to load user info", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}
