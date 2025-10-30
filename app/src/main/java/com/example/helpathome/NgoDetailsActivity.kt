package com.example.helpathome

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.database.FirebaseDatabase
import java.util.UUID
import android.graphics.Color

class NgoDetailsActivity : AppCompatActivity() {

    private lateinit var imgLogo: ImageView
    private lateinit var txtName: TextView
    private lateinit var txtDateFounded: TextView
    private lateinit var txtFounder: TextView
    private lateinit var txtCategory: TextView
    private lateinit var txtMission: TextView
    private lateinit var txtVision: TextView
    private lateinit var txtContact: TextView
    private lateinit var txtApproved: TextView

    private lateinit var edtUpdate: EditText
    private lateinit var btnPostUpdate: Button

    // Store the currently loaded NGO
    private var currentNgo: Ngo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ngo_details)

        // Initialize views
        imgLogo = findViewById(R.id.imgNgoLogo)
        txtName = findViewById(R.id.txtNgoName)
        txtDateFounded = findViewById(R.id.txtDateFounded)
        txtFounder = findViewById(R.id.txtFounder)
        txtCategory = findViewById(R.id.txtCategory)
        txtMission = findViewById(R.id.txtMission)
        txtVision = findViewById(R.id.txtVision)
        txtContact = findViewById(R.id.txtContactInfo)
        txtApproved = findViewById(R.id.txtApproved)

        edtUpdate = findViewById(R.id.edtUpdate)
        btnPostUpdate = findViewById(R.id.btnPostUpdate)

        val ngoId = intent.getStringExtra("ngoId")
        if (ngoId.isNullOrEmpty()) {
            Toast.makeText(this, "Invalid NGO ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadNgoDetails(ngoId)


        btnPostUpdate.setOnClickListener {
            val updateText = edtUpdate.text.toString().trim()
            val wordCount = updateText.split("\\s+".toRegex()).size

            when {
                updateText.isEmpty() -> {
                    Toast.makeText(this, "Please enter an update", Toast.LENGTH_SHORT).show()
                }
                wordCount > 150 -> {
                    Toast.makeText(this, "Update exceeds 150 words", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    val updatesRef = FirebaseDatabase.getInstance()
                        .getReference("ngo_updates")
                        .child(ngoId)

                    val newUpdate = NgoUpdate(updateText)
                    updatesRef.push().setValue(newUpdate)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Update posted successfully!", Toast.LENGTH_SHORT).show()
                            edtUpdate.text.clear()

                            // --- PUSH NOTIFICATION WITH LOGO ---
                            val notifRef = FirebaseDatabase.getInstance().reference.child("notifications")
                            val notifId = notifRef.push().key ?: UUID.randomUUID().toString()
                            val notifData = mapOf(
                                "id" to notifId,
                                "title" to "NGO Update Posted",
                                "message" to "NGO '${txtName.text}' posted a new update.",
                                "type" to "ngo_update",
                                "relatedId" to ngoId,
                                "timestamp" to System.currentTimeMillis(),
                                "read" to false,
                                "imageUrl" to (currentNgo?.logoUrl ?: "")  // <-- safe access
                            )
                            notifRef.child(notifId).setValue(notifData)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to post update: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        }
    }

    private fun loadNgoDetails(ngoId: String) {
        val ref = FirebaseDatabase.getInstance().getReference("ngos").child(ngoId)


        ref.get().addOnSuccessListener { snapshot ->
            val ngo = snapshot.getValue(Ngo::class.java)
            if (ngo != null) {
                currentNgo = ngo // store for later use

                txtName.text = ngo.name
                txtDateFounded.text = "Founded: ${ngo.dateFounded}"
                txtCategory.text = "Category: ${ngo.category}"
                txtFounder.text = "Founder: ${ngo.founder}"
                txtMission.text = "Mission: ${ngo.mission}"
                txtVision.text = "Vision: ${ngo.vision}"

                // Display contacts properly
                val contactText = ngo.contacts?.joinToString(separator = "\n") { contact ->
                    "${contact.label}: ${contact.value} (${contact.type})"
                } ?: "No contacts available"
                txtContact.text = "Contacts:\n$contactText"

                // Approved status
                txtApproved.text = if (ngo.approvedByAdmin)
                    "Approved by admin: ✅"
                else
                    "Approved by admin: ❌"

                // Load logo
                Glide.with(this)
                    .load(ngo.logoUrl.ifEmpty { null })
                    .placeholder(R.drawable.ic_placeholder_logo)
                    .error(R.drawable.ic_placeholder_logo)
                    .into(imgLogo)

                loadHelpRequests(ngoId, ngo.name)


            } else {
                Toast.makeText(this, "Failed to load NGO details", Toast.LENGTH_SHORT).show()
                finish()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error loading data", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadHelpRequests(ngoId: String, ngoName: String) {
        val helpRequestsRef = FirebaseDatabase.getInstance().getReference("help_requests").child(ngoId)
        val container = findViewById<LinearLayout>(R.id.helpRequestsContainer)
        container.removeAllViews()

        helpRequestsRef.get().addOnSuccessListener { snapshot ->
            for (child in snapshot.children) {
                val userId = child.key ?: continue
                val request = child.getValue(HelpRequest::class.java)
                if (request != null) {
                    val actionsRef = FirebaseDatabase.getInstance()
                        .getReference("help_request_actions")
                        .child(ngoId)
                        .child(userId)

                    actionsRef.get().addOnSuccessListener { actionSnapshot ->
                        val card = layoutInflater.inflate(R.layout.item_help_request_card, container, false)

                        val txtUserName = card.findViewById<TextView>(R.id.txtUserName)
                        val txtUserMessage = card.findViewById<TextView>(R.id.txtUserMessage)
                        val txtRequestStatus = card.findViewById<TextView>(R.id.txtRequestStatus)
                        val btnDismiss = card.findViewById<Button>(R.id.btnDismiss)
                        val btnRespond = card.findViewById<Button>(R.id.btnRespond)
                        val btnReport = card.findViewById<Button>(R.id.btnReport)

                        txtUserName.text = request.userName
                        txtUserMessage.text = request.text

                        // Determine status
                        val statusText = when {
                            actionSnapshot.hasChild("dismiss") -> "Dismissed"
                            actionSnapshot.hasChild("response") -> "Responded"
                            actionSnapshot.hasChild("report") -> "Reported"
                            else -> "Pending"
                        }
                        txtRequestStatus.text = "Status: $statusText"
                        txtRequestStatus.setTextColor(
                            when (statusText) {
                                "Dismissed" -> Color.parseColor("#B0BEC5")
                                "Responded" -> Color.parseColor("#4CAF50")
                                "Reported" -> Color.parseColor("#F44336")
                                else -> Color.parseColor("#757575")
                            }
                        )

                        fun showInputDialog(title: String, onSubmit: (String) -> Unit) {
                            val input = EditText(this)
                            input.hint = "Enter your message"
                            AlertDialog.Builder(this)
                                .setTitle(title)
                                .setView(input)
                                .setPositiveButton("Submit") { _, _ ->
                                    val message = input.text.toString().trim()
                                    if (message.isNotEmpty()) {
                                        onSubmit(message)
                                    } else {
                                        Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .setNegativeButton("Cancel", null)
                                .show()
                        }

                        fun sendNotification(
                            toPath: String,
                            title: String,
                            message: String,
                            type: String,
                            includeNgoName: Boolean = false
                        ) {
                            val fullMessage = if (includeNgoName) {
                                "$ngoName: $message"
                            } else {
                                message
                            }

                            val notifRef = FirebaseDatabase.getInstance().getReference(toPath).push()
                            val notifData = mapOf(
                                "id" to notifRef.key,
                                "title" to title,
                                "message" to fullMessage,
                                "timestamp" to System.currentTimeMillis(),
                                "type" to type,
                                "read" to false,
                                "userId" to userId,
                                "relatedId" to ngoId,
                                "imageUrl" to ""
                            )
                            notifRef.setValue(notifData)
                        }

                        btnDismiss.setOnClickListener {
                            showInputDialog("Reason for Dismissal") { reason ->
                                val actionRef = FirebaseDatabase.getInstance().getReference("help_request_actions")
                                    .child(ngoId).child(userId).child("dismiss")
                                actionRef.setValue(reason)

                                sendNotification(
                                    toPath = "notifications",
                                    title = "Help Request Dismissed",
                                    message = "Your help request was dismissed. Reason: $reason",
                                    type = "dismissed",
                                    includeNgoName = true
                                )

                                container.removeView(card)
                            }
                        }

                        btnRespond.setOnClickListener {
                            showInputDialog("Response to User") { response ->
                                val actionRef = FirebaseDatabase.getInstance().getReference("help_request_actions")
                                    .child(ngoId).child(userId).child("response")
                                actionRef.setValue(response)

                                sendNotification(
                                    toPath = "notifications",
                                    title = "Response to Your Help Request",
                                    message = response,
                                    type = "response",
                                    includeNgoName = true
                                )

                                container.removeView(card)
                            }
                        }

                        btnReport.setOnClickListener {
                            showInputDialog("Reason for Reporting") { reason ->
                                val actionRef = FirebaseDatabase.getInstance().getReference("help_request_actions")
                                    .child(ngoId).child(userId).child("report")
                                actionRef.setValue(reason)

                                sendNotification(
                                    toPath = "admin_notifications",
                                    title = "Help Request Reported",
                                    message = "Reported reason: $reason",
                                    type = "report",
                                    includeNgoName = true
                                )

                                container.removeView(card)
                            }
                        }

                        container.addView(card)
                    }
                }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to load help requests", Toast.LENGTH_SHORT).show()
        }
    }
}
