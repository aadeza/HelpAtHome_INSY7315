package com.example.helpathome

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class NGOModel : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var txtUserName: TextView
    private lateinit var txtResults: TextView
    private lateinit var db: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ngomodel)

        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance().reference

        txtUserName = findViewById(R.id.txtUserName)
        txtResults = findViewById(R.id.txtViewResults)

        val currentUser = auth.currentUser
        txtUserName.text = currentUser?.email ?: "NGO User"

        val btnSubmitNGO = findViewById<Button>(R.id.btnCreateNGO)
        val btnPostUpdate = findViewById<Button>(R.id.btnPostUpdate)
        val btnHelpRequests = findViewById<Button>(R.id.btnHelpRequests)

        val editName = findViewById<EditText>(R.id.editName)
        val editFounder = findViewById<EditText>(R.id.editFounder)
        val editDateFounded = findViewById<EditText>(R.id.editDateFounded)
        val spinner = findViewById<Spinner>(R.id.spinnerCategory)
        val editPostMessage = findViewById<EditText>(R.id.editPostMessage)

        val categories = arrayOf("Food Drive", "Pet Rescue", "Legal Aid", "Shelter Support")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        spinner.adapter = adapter

        // ✅ Submit NGO
        btnSubmitNGO.setOnClickListener {
            val name = editName.text.toString()
            val founder = editFounder.text.toString()
            val dateFounded = editDateFounded.text.toString()
            val category = spinner.selectedItem.toString()

            if (name.isNotEmpty() && founder.isNotEmpty() && dateFounded.isNotEmpty()) {
                val ngo = mapOf(
                    "name" to name,
                    "founder" to founder,
                    "category" to category,
                    "dateFounded" to dateFounded
                )

                val currentUserEmail = auth.currentUser?.email ?: "Unknown User"
                val createdDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())

                // Push NGO and get the key
                val ngoRef = db.child("NGOs").push()
                ngoRef.setValue(ngo).addOnSuccessListener {
                    Toast.makeText(this, "✅ NGO Saved!", Toast.LENGTH_SHORT).show()
                    loadNGOData()

                    // Create notification map
                    val notification = mapOf(
                        "title" to "New NGO Created",
                        "message" to "Name: $name\nCreated by: $currentUserEmail\nDate: $createdDate",
                        "time" to createdDate,
                        "color" to "#4CAF50"  // green color
                    )

                    // Push notification to Firebase
                    db.child("notifications").push().setValue(notification)
                }.addOnFailureListener {
                    Toast.makeText(this, "❌ Failed to save NGO", Toast.LENGTH_SHORT).show()
                }

            } else {
                Toast.makeText(this, "❌ Fill all NGO fields", Toast.LENGTH_SHORT).show()
            }
        }


        // ✅ Post Update
        btnPostUpdate.setOnClickListener {
            val message = editPostMessage.text.toString()
            if (message.split("\\s+".toRegex()).size <= 50 && message.isNotEmpty()) {
                val update = mapOf(
                    "message" to message,
                    "timestamp" to System.currentTimeMillis()
                )
                db.child("NGOUpdates").push().setValue(update)
                Toast.makeText(this, "📢 Update posted!", Toast.LENGTH_SHORT).show()
                loadUpdates()
            } else {
                Toast.makeText(this, "❌ Max 50 words", Toast.LENGTH_SHORT).show()
            }
        }

        // ✅ View & Delete Help Requests
        btnHelpRequests.setOnClickListener {
            db.child("help_requests").get().addOnSuccessListener { snapshot ->
                if (!snapshot.hasChildren()) {
                    txtResults.text = "📥 No help requests found."
                    return@addOnSuccessListener
                }

                val builder = StringBuilder()
                val requests = snapshot.children.toList()
                val totalRequests = requests.size
                var processed = 0

                txtResults.text = "Loading help requests..."

                for (request in requests) {
                    val requestData = request.value as Map<*, *>
                    val message = requestData["message"] as? String ?: "No message"
                    val ngo = requestData["ngoName"] as? String ?: "Unknown NGO"
                    val userId = requestData["userId"] as? String ?: ""
                    val timestamp = requestData["timestamp"]?.toString()?.toLongOrNull()

                    val formattedTime = timestamp?.let {
                        val date = java.text.SimpleDateFormat("dd MMM, hh:mm a").format(java.util.Date(it))
                        " at $date"
                    } ?: ""

                    db.child("Users").child(userId).get().addOnSuccessListener { userSnapshot ->
                        val userMap = userSnapshot.value as? Map<*, *>
                        val firstName = userMap?.get("firstName") as? String ?: "Unknown"
                        val lastName = userMap?.get("lastName") as? String ?: "User"
                        val email = userMap?.get("email") as? String ?: "No email"
                        val fullName = "$firstName $lastName"

                        builder.append("• $message\n")
                        builder.append("  → For: $ngo$formattedTime\n")
                        builder.append("  → From: $fullName\n")
                        builder.append("  → Contact: $email\n")
                        builder.append("  🗑 Tap to remove this request\n\n")

                        processed++
                        if (processed == totalRequests) {
                            txtResults.text = builder.toString()
                        }

                        // Handle click for deletion
                        txtResults.setOnClickListener {
                            showRequestDeletionDialog(request.key!!)
                        }

                    }.addOnFailureListener {
                        processed++
                        if (processed == totalRequests) {
                            txtResults.text = builder.toString()
                        }
                    }
                }

            }.addOnFailureListener {
                Toast.makeText(this, "❌ Failed to load help requests", Toast.LENGTH_SHORT).show()
            }
        }

        loadNGOData()
        loadUpdates()
    }

    private fun showRequestDeletionDialog(requestId: String) {
        AlertDialog.Builder(this)
            .setTitle("Remove Help Request")
            .setMessage("Are you sure you want to delete this help request?")
            .setPositiveButton("Yes") { _, _ ->
                db.child("help_requests").child(requestId).removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(this, "🗑 Help request removed", Toast.LENGTH_SHORT).show()
                        txtResults.text = ""
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "❌ Failed to delete", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadNGOData() {
        db.child("NGOs").get().addOnSuccessListener { snapshot ->
            val builder = StringBuilder()
            builder.append("📌 NGOs Registered:\n")
            snapshot.children.forEach {
                val data = it.value as Map<*, *>
                builder.append("• ${data["name"]} (${data["category"]})\n")
            }
            txtResults.text = builder.toString()
        }
    }

    private fun loadUpdates() {
        db.child("NGOUpdates").get().addOnSuccessListener { snapshot ->
            val builder = StringBuilder()
            builder.append("\n📢 Recent Updates:\n")
            snapshot.children.forEach {
                val data = it.value as Map<*, *>
                builder.append("• ${data["message"]}\n")
            }
            txtResults.append(builder.toString())
        }
    }
}