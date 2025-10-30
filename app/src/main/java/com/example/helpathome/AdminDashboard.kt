package com.example.helpathome

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class AdminDashboard : AppCompatActivity(), AdminAccountDialog.OnAdminAccountUpdated {

    // Header views
    private lateinit var adminNameText: TextView
    private lateinit var adminProfileButton: ImageButton
    private lateinit var adminTitle: TextView

    // Section 1: User Overview
    private lateinit var userCounts: TextView
    private lateinit var userBarChart: BarChart
    private lateinit var userRecyclerView: RecyclerView

    // Section 2: NGO Approval
    private lateinit var ngoApprovalRecyclerView: RecyclerView

    // Section 3: Message Moderation
    private lateinit var messageModerationRecyclerView: RecyclerView




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        // Bind header views
        adminNameText = findViewById(R.id.adminNameText)
        adminProfileButton = findViewById(R.id.adminProfileButton)
        adminTitle = findViewById(R.id.adminTitle)

        // Bind section views
        userCounts = findViewById(R.id.userCounts)
        userBarChart = findViewById(R.id.userBarChart)
        userRecyclerView = findViewById(R.id.userRecyclerView)

        ngoApprovalRecyclerView = findViewById(R.id.ngoApprovalRecyclerView)
        messageModerationRecyclerView = findViewById(R.id.messageModerationRecyclerView)


        adminProfileButton.setOnClickListener {
            val popup = PopupMenu(this, it)
            popup.menuInflater.inflate(R.menu.admin_profile_menu, popup.menu)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_edit_account -> {
                        val user = FirebaseAuth.getInstance().currentUser ?: return@setOnMenuItemClickListener true
                        val adminRef = FirebaseDatabase.getInstance().getReference("Users").child(user.uid)

                        adminRef.get().addOnSuccessListener { snapshot ->
                            val firstName = snapshot.child("firstName").getValue(String::class.java) ?: ""
                            val lastName = snapshot.child("lastName").getValue(String::class.java) ?: ""
                            val email = snapshot.child("email").getValue(String::class.java) ?: ""
                            val phone = snapshot.child("phoneNumber").getValue(String::class.java) ?: ""
                            val dob = snapshot.child("dob").getValue(String::class.java) ?: ""

                            val dialog = AdminAccountDialog(
                                currentName = firstName,
                                currentSurname = lastName,
                                currentEmail = email,
                                currentPhone = phone,
                                dob = dob
                            )
                            dialog.show(supportFragmentManager, "AdminAccountDialog")
                        }
                        true
                    }
                    R.id.menu_logout -> {
                        Log.d("ADMIN_MENU", "Log Out clicked")
                        FirebaseAuth.getInstance().signOut()
                        Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                        true
                    }
                    else -> false
                }
            }

            popup.show()
        }

        loadAdminProfile()
        loadUserOverview()
        loadNGOApprovals()
        loadMessageModeration()

        loadAdminProfile()


    }

    private fun renderUserBarChart(adminCount: Int, lawCount: Int, civilianCount: Int, ngoCount: Int) {
        val entries = listOf(
            BarEntry(0f, adminCount.toFloat()),
            BarEntry(1f, lawCount.toFloat()),
            BarEntry(2f, civilianCount.toFloat()),
            BarEntry(3f, ngoCount.toFloat())
        )

        val dataSet = BarDataSet(entries, "User Roles")
        dataSet.colors = listOf(
            Color.parseColor("#1976D2"), // Admin - Blue
            Color.parseColor("#388E3C"), // Law - Green
            Color.parseColor("#FBC02D"), // Civilian - Yellow
            Color.parseColor("#D32F2F")  // NGO - Red
        )
        dataSet.valueTextSize = 12f

        val barData = BarData(dataSet)
        userBarChart.data = barData

        val labels = listOf("Admin", "Law", "Civilian", "NGO")
        userBarChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        userBarChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        userBarChart.xAxis.setDrawGridLines(false)
        userBarChart.xAxis.granularity = 1f
        userBarChart.xAxis.labelCount = labels.size

        userBarChart.axisLeft.setDrawGridLines(false)
        userBarChart.axisRight.isEnabled = false
        userBarChart.description.isEnabled = false
        userBarChart.setFitBars(true)
        userBarChart.animateY(800)
        userBarChart.invalidate()
    }
    private fun loadAdminProfile() {
        val usersRef = FirebaseDatabase.getInstance().getReference("Users")
        val adminId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val adminRef = usersRef.child(adminId)

        adminRef.get().addOnSuccessListener { snapshot ->
            val firstName = snapshot.child("firstName").getValue(String::class.java) ?: ""
            val lastName = snapshot.child("lastName").getValue(String::class.java) ?: ""
            val fullName = "$firstName $lastName".trim()

            adminNameText.text = fullName
        }.addOnFailureListener {
            adminNameText.text = "Unknown Admin"
            Toast.makeText(this, "Failed to load admin profile", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onAdminAccountSaved(
        currentPassword: String,
        newName: String,
        newSurname: String,
        newPhone: String,
        newPassword: String?
    ) {
        val adminId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val adminRef = FirebaseDatabase.getInstance().getReference("Users").child(adminId)

        val updates = mapOf(
            "firstName" to newName,
            "lastName" to newSurname,
            "phoneNumber" to newPhone
        )

        adminRef.updateChildren(updates).addOnSuccessListener {
            Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()
            loadAdminProfile()
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show()
        }

        if (!newPassword.isNullOrEmpty()) {
            FirebaseAuth.getInstance().currentUser?.updatePassword(newPassword)
                ?.addOnSuccessListener {
                    Toast.makeText(this, "Password updated", Toast.LENGTH_SHORT).show()
                }
                ?.addOnFailureListener {
                    Toast.makeText(this, "Failed to update password", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun loadUserOverview() {
        val usersRef = FirebaseDatabase.getInstance().getReference("Users")

        usersRef.get().addOnSuccessListener { snapshot ->
            val userList = mutableListOf<UserAdminModel>()
            var adminCount = 0
            var lawCount = 0
            var civilianCount = 0
            var ngoCount = 0

            for (userSnap in snapshot.children) {
                val userId = userSnap.key ?: continue
                val firstName = userSnap.child("firstName").getValue(String::class.java) ?: ""
                val lastName = userSnap.child("lastName").getValue(String::class.java) ?: ""
                val fullName = "$firstName $lastName".trim()

                val rawType = userSnap.child("userType").getValue(String::class.java)
                val userType = rawType?.lowercase()?.trim() ?: "civilian"

                when (userType) {
                    "admin" -> adminCount++
                    "law enforcement" -> lawCount++
                    "ngo" -> ngoCount++
                    "civilian" -> civilianCount++
                    else -> {
                        civilianCount++ // fallback for unknown types
                        Log.w("UserOverview", "Unknown userType for user $userId: $rawType")
                    }
                }

                userList.add(UserAdminModel(userId, fullName, userType))
            }

            // Update count text
            userCounts.text = "Admins: $adminCount | Law Enforcment: $lawCount | Civilians: $civilianCount | NGOs: $ngoCount"

            // Update bar chart
            renderUserBarChart(adminCount, lawCount, civilianCount, ngoCount)

            // Setup adapter
            val adapter = UserAdminAdapter(userList) { user ->
                showEditUserDialog(user)
            }
            userRecyclerView.layoutManager = LinearLayoutManager(this)
            userRecyclerView.adapter = adapter

        }.addOnFailureListener {
            Toast.makeText(this, "Failed to load users", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showEditUserDialog(user: UserAdminModel) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_user, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.editUserName)
        val phoneInput = dialogView.findViewById<EditText>(R.id.editUserPhone)
        val roleSpinner = dialogView.findViewById<Spinner>(R.id.editUserRole)

        nameInput.setText(user.fullName)

        // Setup role spinner
        val roles = listOf("admin", "law", "civilian", "ngo")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, roles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        roleSpinner.adapter = adapter
        roleSpinner.setSelection(roles.indexOf(user.role.lowercase()))

        AlertDialog.Builder(this)
            .setTitle("Edit User")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newName = nameInput.text.toString().trim()
                val newPhone = phoneInput.text.toString().trim()
                val newRole = roleSpinner.selectedItem.toString()

                val usersRef = FirebaseDatabase.getInstance().getReference("Users").child(user.userId)
                val updates = mapOf(
                    "firstName" to newName.split(" ").firstOrNull(),
                    "lastName" to newName.split(" ").drop(1).joinToString(" "),
                    "phone" to newPhone,
                    "role" to newRole
                )

                usersRef.updateChildren(updates).addOnSuccessListener {
                    Toast.makeText(this, "User updated", Toast.LENGTH_SHORT).show()
                    loadUserOverview() // refresh list
                }.addOnFailureListener {
                    Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadNGOApprovals() {
        val ngoRef = FirebaseDatabase.getInstance().getReference("ngos")

        ngoRef.get().addOnSuccessListener { snapshot ->
            val ngoList = mutableListOf<NGOApprovalModel>()

            for (ngoSnap in snapshot.children) {
                val ngo = ngoSnap.getValue(Ngo::class.java) ?: continue
                val ngoId = ngoSnap.key ?: continue
                val resolvedId = if (ngo.id.isBlank()) ngoId else ngo.id
                val contact = ngo.contacts?.joinToString(", ") { it.value } ?: "No contact info"

                ngoList.add(
                    NGOApprovalModel(
                        ngoId = resolvedId,
                        name = ngo.name,
                        description = ngo.mission,
                        contact = contact,
                        approved = ngo.approvedByAdmin,
                        logoUrl = ngo.logoUrl
                    )
                )
            }

            val adapter = NGOApprovalAdapter(ngoList) { ngo, isApproved ->
                val updateRef = FirebaseDatabase.getInstance().getReference("ngos").child(ngo.ngoId)
                updateRef.child("approvedByAdmin").setValue(isApproved).addOnSuccessListener {
                    if (isApproved) {
                        val ngoAccountsRef = FirebaseDatabase.getInstance().getReference("NgoAccounts")
                        ngoAccountsRef.orderByChild("linkedNgoId").equalTo(ngo.ngoId)
                            .get().addOnSuccessListener { accountSnap ->
                                for (account in accountSnap.children) {
                                    val linkedNgoId = account.child("linkedNgoId").getValue(String::class.java) ?: continue

                                    val notificationRef = FirebaseDatabase.getInstance()
                                        .getReference("ngo_notifications")
                                        .child(linkedNgoId)
                                        .push()

                                    val notification = mapOf(
                                        "title" to "NGO Approved",
                                        "message" to "Your NGO '${ngo.name}' has been approved by the admin.",
                                        "imageUrl" to ngo.logoUrl,
                                        "read" to false,
                                        "timestamp" to System.currentTimeMillis()
                                    )

                                    notificationRef.setValue(notification).addOnSuccessListener {
                                        Log.d("AdminDashboard", "Notification sent to $linkedNgoId")
                                    }.addOnFailureListener {
                                        Log.e("AdminDashboard", "Failed to send notification", it)
                                    }
                                }
                            }.addOnFailureListener {
                                Log.e("AdminDashboard", "Failed to find linked NGO account", it)
                            }
                    }
                }
            }

            ngoApprovalRecyclerView.layoutManager = LinearLayoutManager(this)
            ngoApprovalRecyclerView.adapter = adapter

        }.addOnFailureListener {
            Toast.makeText(this, "Failed to load NGOs", Toast.LENGTH_SHORT).show()
        }
    }
    private fun loadMessageModeration() {
        val actionsRef = FirebaseDatabase.getInstance().getReference("help_request_actions")
        val requestsRef = FirebaseDatabase.getInstance().getReference("help_requests")
        val moderationList = mutableListOf<MessageModerationModel>()

        actionsRef.get().addOnSuccessListener { actionsSnapshot ->
            val pendingFetches = mutableListOf<Task<DataSnapshot>>()

            for (ngoSnap in actionsSnapshot.children) {
                val ngoId = ngoSnap.key ?: continue

                for (userSnap in ngoSnap.children) {
                    val userId = userSnap.key ?: continue
                    val dismissReason = userSnap.child("dismiss").getValue(String::class.java)
                    val reportReason = userSnap.child("report").getValue(String::class.java)

                    if (dismissReason == null && reportReason == null) continue

                    val fetchTask = requestsRef.child(ngoId).child(userId).get()
                    pendingFetches.add(fetchTask)

                    fetchTask.addOnSuccessListener { requestSnap ->
                        val content = requestSnap.child("text").getValue(String::class.java) ?: "No message"
                        val userName = requestSnap.child("userName").getValue(String::class.java) ?: "Unknown"
                        val userEmail = requestSnap.child("userEmail").getValue(String::class.java) ?: ""
                        val userPhone = requestSnap.child("userPhone").getValue(String::class.java) ?: ""
                        val timestamp = requestSnap.child("timestamp").getValue(Long::class.java) ?: 0L

                        moderationList.add(
                            MessageModerationModel(
                                messageId = "$ngoId:$userId",
                                senderId = userId,
                                content = content,
                                dismissed = dismissReason != null,
                                reported = reportReason != null,
                                dismissReason = dismissReason ?: reportReason ?: "No reason provided",
                                userName = userName,
                                userEmail = userEmail,
                                userPhone = userPhone,
                                timestamp = timestamp
                            )
                        )
                    }
                }
            }

            Tasks.whenAllComplete(pendingFetches).addOnCompleteListener {
                refreshModerationAdapter(moderationList)
            }

        }.addOnFailureListener {
            Toast.makeText(this, "Failed to load moderation actions", Toast.LENGTH_SHORT).show()
        }
    }
    private fun refreshModerationAdapter(moderationList: List<MessageModerationModel>) {
        if (moderationList.isEmpty()) {
            Toast.makeText(this, "No dismissed help requests found", Toast.LENGTH_SHORT).show()
            return
        }

        val adapter = MessageModerationAdapter(
            messageList = moderationList,
            onDeleteUser = { senderId ->
                val rootRef = FirebaseDatabase.getInstance()
                val actionsRef = rootRef.getReference("help_request_actions")
                val requestsRef = rootRef.getReference("help_requests")
                val usersRef = rootRef.getReference("Users").child(senderId)

                actionsRef.get().addOnSuccessListener { actionsSnapshot ->
                    for (ngoSnap in actionsSnapshot.children) {
                        val ngoId = ngoSnap.key ?: continue
                        if (ngoSnap.hasChild(senderId)) {
                            // Delete all related data
                            val updates = mapOf(
                                "/Users/$senderId" to null,
                                "/help_requests/$ngoId/$senderId" to null,
                                "/help_request_actions/$ngoId/$senderId" to null
                            )

                            rootRef.reference.updateChildren(updates).addOnSuccessListener {
                                Toast.makeText(this, "User and related data deleted", Toast.LENGTH_SHORT).show()
                                loadMessageModeration()
                            }.addOnFailureListener {
                                Toast.makeText(this, "Failed to delete user data", Toast.LENGTH_SHORT).show()
                            }
                            break
                        }
                    }
                }.addOnFailureListener {
                    Toast.makeText(this, "Failed to access moderation data", Toast.LENGTH_SHORT).show()
                }
            },
            onMarkOkay = { senderId ->
                val actionsRef = FirebaseDatabase.getInstance().getReference("help_request_actions")
                actionsRef.get().addOnSuccessListener { actionsSnapshot ->
                    for (ngoSnap in actionsSnapshot.children) {
                        val ngoId = ngoSnap.key ?: continue
                        if (ngoSnap.hasChild(senderId)) {
                            actionsRef.child(ngoId).child(senderId).child("dismiss").removeValue()
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Marked as okay", Toast.LENGTH_SHORT).show()
                                    loadMessageModeration()
                                }.addOnFailureListener {
                                    Toast.makeText(this, "Failed to mark as okay", Toast.LENGTH_SHORT).show()
                                }
                            break
                        }
                    }
                }
            }
        )

        messageModerationRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@AdminDashboard)
            this.adapter = adapter
        }
    }

}