package com.example.helpathome

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class UserNgoViewActivity : AppCompatActivity() {

    private lateinit var imgNgoLogo: ImageView
    private lateinit var txtNgoName: TextView
    private lateinit var txtNgoCategory: TextView
    private lateinit var txtApprovalStatus: TextView
    private lateinit var recyclerUpdates: RecyclerView
    private lateinit var edtHelpRequest: EditText
    private lateinit var btnSendHelpRequest: Button

    private lateinit var ngoId: String
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_ngo_view)

        imgNgoLogo = findViewById(R.id.imgNgoLogo)
        txtNgoName = findViewById(R.id.txtNgoName)
        txtNgoCategory = findViewById(R.id.txtNgoCategory)
        txtApprovalStatus = findViewById(R.id.txtApprovalStatus)
        recyclerUpdates = findViewById(R.id.recyclerNgoUpdates)
        edtHelpRequest = findViewById(R.id.edtHelpRequest)
        btnSendHelpRequest = findViewById(R.id.btnSendHelpRequest)

        database = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()

        ngoId = intent.getStringExtra("ngoId") ?: return

        loadNgoDetails()
        loadNgoUpdates()
        setupHelpRequest()
    }

    private fun loadNgoDetails() {
        val ngoRef = database.getReference("ngos").child(ngoId)
        ngoRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val ngo = snapshot.getValue(Ngo::class.java)
                if (ngo != null) {
                    txtNgoName.text = ngo.name
                    txtNgoCategory.text = "Category: ${ngo.category}"
                    txtApprovalStatus.text = if (ngo.approvedByAdmin) {
                        "Approved by admin: ✅"
                    } else {
                        "Approved by admin: ❌"
                    }

                    Glide.with(this@UserNgoViewActivity)
                        .load(ngo.logoUrl)
                        .placeholder(R.drawable.ic_placeholder_logo)
                        .error(R.drawable.ic_placeholder_logo)
                        .into(imgNgoLogo)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@UserNgoViewActivity, "Failed to load NGO details", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadNgoUpdates() {
        val updatesRef = database.getReference("ngo_updates").child(ngoId)
        updatesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val updateList = mutableListOf<NgoUpdate>()
                for (child in snapshot.children) {
                    val update = child.getValue(NgoUpdate::class.java)
                    if (update != null) updateList.add(update)
                }

                recyclerUpdates.layoutManager = LinearLayoutManager(this@UserNgoViewActivity)
                recyclerUpdates.adapter = NgoUpdateAdapter(updateList)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@UserNgoViewActivity, "Failed to load updates", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupHelpRequest() {
        btnSendHelpRequest.setOnClickListener {
            val text = edtHelpRequest.text.toString().trim()
            val wordCount = text.split("\\s+".toRegex()).size

            if (text.isEmpty()) {
                Toast.makeText(this, "Please enter your help request", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (wordCount > 150) {
                Toast.makeText(this, "Help request must be 150 words or less", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userId = auth.currentUser?.uid ?: return@setOnClickListener
            val userRef = database.getReference("Users").child(userId)

            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val firstName = snapshot.child("firstName").getValue(String::class.java) ?: "Unknown"
                    val lastName = snapshot.child("lastName").getValue(String::class.java) ?: ""
                    val email = snapshot.child("email").getValue(String::class.java) ?: "Not provided"
                    val phone = snapshot.child("phoneNumber").getValue(String::class.java) ?: "Not provided"
                    val userName = "$firstName $lastName"

                    // Save help request
                    val requestData = mapOf(
                        "text" to text,
                        "timestamp" to System.currentTimeMillis(),
                        "userName" to userName,
                        "userEmail" to email,
                        "userPhone" to phone
                    )

                    val requestRef = database.getReference("help_requests").child(ngoId).child(userId)
                    requestRef.setValue(requestData)
                        .addOnSuccessListener {
                            Toast.makeText(this@UserNgoViewActivity, "Help request sent", Toast.LENGTH_SHORT).show()
                            edtHelpRequest.text.clear()

                            // Send NGO notification
                            val notificationRef = database.getReference("ngo_notifications").child(ngoId).push()
                            val notificationId = notificationRef.key ?: return@addOnSuccessListener

                            val notificationData = mapOf(
                                "id" to notificationId,
                                "title" to "Help Request Received",
                                "message" to "$userName has sent a help request to your NGO.",
                                "timestamp" to System.currentTimeMillis(),
                                "type" to "help_request",
                                "read" to false,
                                "userId" to userId,
                                "relatedId" to userId,
                                "imageUrl" to ""
                            )

                            notificationRef.setValue(notificationData)
                        }
                        .addOnFailureListener {
                            Toast.makeText(this@UserNgoViewActivity, "Failed to send request", Toast.LENGTH_SHORT).show()
                        }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@UserNgoViewActivity, "Failed to fetch user info", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}