package com.example.helpathome

import android.content.Intent
import android.widget.ImageButton
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.helpathome.adapters.AlertAdapter
import com.example.helpathome.models.LastKnownLocation
import com.example.helpathome.models.alerts
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class LawDashboardActivity : AppCompatActivity(), OfficerAccountDialog.OnOfficerAccountUpdated {

    private lateinit var recyclerView: RecyclerView
    private lateinit var alertAdapter: AlertAdapter
    private val alertList = mutableListOf<alerts>()

    private lateinit var resolvedRecyclerView: RecyclerView
    private lateinit var resolvedAlertAdapter: AlertAdapter
    private val resolvedAlertList = mutableListOf<alerts>()

    private lateinit var usersRef: DatabaseReference
    private lateinit var summaryTextView: TextView
    private lateinit var syncTimeTextView: TextView
    private lateinit var officerStatsTextView: TextView
    private lateinit var officerNameTextView: TextView
    private lateinit var txtUserName: TextView
    private lateinit var profileButton: ImageButton

    private lateinit var officerId: String
    private lateinit var officerName: String

    private val officerNameMap = mutableMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_law_dashboard)

        officerId = FirebaseAuth.getInstance().currentUser?.uid ?: "unknown_officer"
        officerName = intent.getStringExtra("officerName") ?: "Unknown Officer"

        usersRef = FirebaseDatabase.getInstance().getReference("Users")

        syncTimeTextView = findViewById(R.id.textSyncTime)
        officerStatsTextView = findViewById(R.id.textOfficerStats)
        officerNameTextView = findViewById(R.id.textOfficerName)
        txtUserName = findViewById(R.id.txtUserName) // ✅ Added for profile button name
        profileButton = findViewById(R.id.profileButton)

        profileButton.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menuInflater.inflate(R.menu.officer_profile_menu, popup.menu)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_edit_account -> {
                        val user = FirebaseAuth.getInstance().currentUser ?: return@setOnMenuItemClickListener true
                        val officerRef = FirebaseDatabase.getInstance().getReference("Users").child(user.uid)

                        officerRef.get().addOnSuccessListener { snapshot ->
                            val firstName = snapshot.child("firstName").getValue(String::class.java) ?: ""
                            val lastName = snapshot.child("lastName").getValue(String::class.java) ?: ""
                            val email = snapshot.child("email").getValue(String::class.java) ?: ""
                            val phone = snapshot.child("phoneNumber").getValue(String::class.java) ?: ""
                            val dob = snapshot.child("dob").getValue(String::class.java) ?: ""

                            val dialog = OfficerAccountDialog(
                                currentName = firstName,
                                currentSurname = lastName,
                                currentEmail = email,
                                currentPhone = phone,
                                dob = dob
                            )
                            dialog.show(supportFragmentManager, "OfficerAccountDialog")
                        }
                        true
                    }

                    R.id.menu_logout -> {
                        // Clear cached session data
                        val sharedPref = getSharedPreferences("session", MODE_PRIVATE)
                        sharedPref.edit().clear().apply()
                        txtUserName.text = "Guest"

                        // Confirm logout
                        Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()

                        // Redirect to login screen with backstack cleared
                        val intent = Intent(this, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()

                        true
                    }

                    else -> false
                }
            }

            popup.show()
        }

        recyclerView = findViewById(R.id.recyclerAlerts)
        recyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        alertAdapter = AlertAdapter(
            alertList,
            officerNameMap = officerNameMap,
            onResolveClick = { resolveAlert(it) },
            onDeleteClick = { dismissAlert(it) }
        )
        recyclerView.adapter = alertAdapter

        resolvedRecyclerView = findViewById(R.id.recyclerResolvedAlerts)
        resolvedRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        resolvedAlertAdapter = AlertAdapter(
            resolvedAlertList,
            officerNameMap = officerNameMap,
            onResolveClick = { resolveAlert(it) },
            onDeleteClick = { dismissAlert(it) }
        )
        resolvedRecyclerView.adapter = resolvedAlertAdapter

        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(recyclerView)
        snapHelper.attachToRecyclerView(resolvedRecyclerView)

        loadOfficerStats()
        loadAllAlerts()
        loadResolvedAlerts()
    }

    private fun loadOfficerStats() {
        val usersRef = FirebaseDatabase.getInstance().getReference("Users")
        val officerRef = usersRef.child(officerId)

        // Step 1: Load officer profile info
        officerRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                Toast.makeText(this, "Officer data not found", Toast.LENGTH_SHORT).show()
                officerName = "Unknown Officer"
                officerNameTextView.text = "Officer: $officerName"
                txtUserName.text = officerName
                officerStatsTextView.text = "Resolved: -- | Dismissed: --"
                return@addOnSuccessListener
            }

            val firstName = snapshot.child("firstName").getValue(String::class.java) ?: ""
            val lastName = snapshot.child("lastName").getValue(String::class.java) ?: ""
            officerName = "$firstName $lastName".trim()

            // ✅ Store in name map for fallback use
            officerNameMap[officerId] = officerName

            officerNameTextView.text = "Officer: $officerName"
            txtUserName.text = officerName

            // Step 2: Count resolved and dismissed alerts by this officer
            usersRef.get().addOnSuccessListener { usersSnapshot ->
                var resolvedCount = 0
                var dismissedCount = 0

                for (userSnap in usersSnapshot.children) {
                    val resolvedBy = userSnap.child("resolvedBy").getValue(String::class.java)
                    val dismissedBy = userSnap.child("dismissedBy").getValue(String::class.java)
                    val resolvedAt = userSnap.child("resolvedAt").getValue(Long::class.java)
                    val dismissedAt = userSnap.child("dismissedAt").getValue(Long::class.java)

                    if (resolvedAt != null && resolvedBy == officerId) resolvedCount++
                    if (dismissedAt != null && dismissedBy == officerId) dismissedCount++
                }

                officerStatsTextView.text = "Resolved: $resolvedCount | Dismissed: $dismissedCount"
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to calculate officer stats", Toast.LENGTH_SHORT).show()
                officerStatsTextView.text = "Resolved: -- | Dismissed: --"
            }

        }.addOnFailureListener {
            Toast.makeText(this, "Failed to load officer profile", Toast.LENGTH_SHORT).show()
            officerName = "Unknown Officer"
            officerNameTextView.text = "Officer: $officerName"
            txtUserName.text = officerName
            officerStatsTextView.text = "Resolved: -- | Dismissed: --"
        }
    }
    private fun loadAllAlerts() {
        usersRef.get().addOnSuccessListener { snapshot ->
            alertList.clear()

            snapshot.children.forEach { userSnap ->
                val userId = userSnap.key ?: return@forEach
                val firstName = userSnap.child("firstName").getValue(String::class.java) ?: ""
                val lastName = userSnap.child("lastName").getValue(String::class.java) ?: ""
                val fullName = "$firstName $lastName".trim()

                val sosActive = userSnap.child("sosActive").getValue(Boolean::class.java) ?: false
                if (!sosActive) return@forEach // Only include active alerts

                val resolvedAt = userSnap.child("resolvedAt").getValue(Long::class.java)
                val resolvedBy = userSnap.child("resolvedBy").getValue(String::class.java)
                val resolvedByName = userSnap.child("resolvedByName").getValue(String::class.java)

                val dismissedAt = userSnap.child("dismissedAt").getValue(Long::class.java)
                val dismissedBy = userSnap.child("dismissedBy").getValue(String::class.java)
                val dismissedByName = userSnap.child("dismissedByName").getValue(String::class.java)

                val lastLocation = userSnap.child("lastKnownLocation").getValue(LastKnownLocation::class.java)

                val alert = alerts(
                    userId = userId,
                    userName = fullName,
                    lastKnownLocation = lastLocation,
                    sosActive = sosActive,
                    resolvedAt = resolvedAt,
                    resolvedBy = resolvedBy,
                    resolvedByName = resolvedByName,
                    dismissedAt = dismissedAt,
                    dismissedBy = dismissedBy,
                    dismissedByName = dismissedByName
                )

                alertList.add(alert)
            }

            alertList.sortByDescending { it.lastKnownLocation?.timestamp ?: 0L }

            alertAdapter.notifyDataSetChanged()
            updateSummaryAndSyncTime()
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to load alerts", Toast.LENGTH_SHORT).show()
        }
    }
    private fun loadResolvedAlerts() {
        usersRef.get().addOnSuccessListener { snapshot ->
            resolvedAlertList.clear()

            snapshot.children.forEach { userSnap ->
                val userId = userSnap.key ?: return@forEach

                val sosActive = userSnap.child("sosActive").getValue(Boolean::class.java) ?: false
                val resolvedAt = userSnap.child("resolvedAt").getValue(Long::class.java)
                val dismissedAt = userSnap.child("dismissedAt").getValue(Long::class.java)

                // ✅ Only include if resolved or dismissed AND previously triggered SOS
                if (sosActive || (resolvedAt == null && dismissedAt == null)) return@forEach

                val firstName = userSnap.child("firstName").getValue(String::class.java) ?: ""
                val lastName = userSnap.child("lastName").getValue(String::class.java) ?: ""
                val fullName = "$firstName $lastName".trim()

                val resolvedBy = userSnap.child("resolvedBy").getValue(String::class.java)
                val resolvedByName = userSnap.child("resolvedByName").getValue(String::class.java)
                val dismissedBy = userSnap.child("dismissedBy").getValue(String::class.java)
                val dismissedByName = userSnap.child("dismissedByName").getValue(String::class.java)

                val lastLocation = userSnap.child("lastKnownLocation").getValue(LastKnownLocation::class.java)

                val alert = alerts(
                    userId = userId,
                    userName = fullName,
                    lastKnownLocation = lastLocation,
                    sosActive = false,
                    resolvedAt = resolvedAt,
                    resolvedBy = resolvedBy,
                    resolvedByName = resolvedByName,
                    dismissedAt = dismissedAt,
                    dismissedBy = dismissedBy,
                    dismissedByName = dismissedByName
                )

                resolvedAlertList.add(alert)
            }

            resolvedAlertList.sortByDescending {
                maxOf(it.resolvedAt ?: 0L, it.dismissedAt ?: 0L)
            }

            resolvedAlertAdapter.notifyDataSetChanged()
            updateSummaryAndSyncTime()
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to load resolved alerts", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateSummaryAndSyncTime() {
        val activeCount = alertList.count { it.sosActive }
        val resolvedCount = resolvedAlertList.size



        val lastSync = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        syncTimeTextView.text = "Last synced at: $lastSync"
    }

    private fun resolveAlert(alert: alerts) {
        val userId = alert.userId ?: run {
            Log.e("RESOLVE_ALERT", "Alert has no userId")
            return
        }

        if (!alert.sosActive) {
            Toast.makeText(this, "This alert is already resolved or dismissed.", Toast.LENGTH_SHORT).show()
            Log.d("RESOLVE_ALERT", "Alert inactive — skipping resolution")
            return
        }

        Log.d("RESOLVE_ALERT", "Fetching user $userId...")

        usersRef.child(userId).get().addOnSuccessListener { userSnap ->
            val phoneNumber = userSnap.child("phoneNumber").getValue(String::class.java)
            val firstName = userSnap.child("firstName").getValue(String::class.java) ?: ""
            val lastName = userSnap.child("lastName").getValue(String::class.java) ?: ""
            val fullName = "$firstName $lastName".trim()

            Log.d("RESOLVE_ALERT", "User found: $fullName | Phone: $phoneNumber")

            if (phoneNumber.isNullOrEmpty()) {
                Toast.makeText(this, "No phone number found for $fullName", Toast.LENGTH_SHORT).show()
                Log.e("RESOLVE_ALERT", "Missing phone number for $fullName")
                return@addOnSuccessListener
            }

            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            startActivity(dialIntent)
            Log.d("RESOLVE_ALERT", "Dialer launched for $phoneNumber")

            val updates = mapOf(
                "sosActive" to false,
                "resolvedAt" to System.currentTimeMillis(),
                "resolvedBy" to officerId,
                "resolvedByName" to officerName
            )

            usersRef.child(userId).updateChildren(updates).addOnSuccessListener {
                Log.d("RESOLVE_ALERT", "Alert marked as resolved for $fullName")
                sendNotificationToUser(
                    userId = userId,
                    title = "Alert Resolved",
                    message = "Your SOS alert was resolved by Officer $officerName.",
                    type = "resolved"
                )
                loadOfficerStats()
                loadResolvedAlerts()
                loadAllAlerts()
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to update alert status", Toast.LENGTH_SHORT).show()
                Log.e("RESOLVE_ALERT", "Failed to update alert: ${it.message}")
            }

        }.addOnFailureListener {
            Toast.makeText(this, "Failed to fetch user details", Toast.LENGTH_SHORT).show()
            Log.e("RESOLVE_ALERT", "Failed to fetch user $userId: ${it.message}")
        }
    }
    private fun dismissAlert(alert: alerts) {
        val userId = alert.userId ?: run {
            Log.e("DISMISS_ALERT", "Alert has no userId")
            return
        }

        if (!alert.sosActive) {
            Toast.makeText(this, "This alert is already resolved or dismissed.", Toast.LENGTH_SHORT).show()
            Log.d("DISMISS_ALERT", "Alert inactive — skipping dismissal")
            return
        }

        Log.d("DISMISS_ALERT", "Fetching user $userId...")

        usersRef.child(userId).get().addOnSuccessListener { userSnap ->
            val firstName = userSnap.child("firstName").getValue(String::class.java) ?: ""
            val lastName = userSnap.child("lastName").getValue(String::class.java) ?: ""
            val fullName = "$firstName $lastName".trim()

            Log.d("DISMISS_ALERT", "User found: $fullName")

            AlertDialog.Builder(this)
                .setTitle("Dismiss Alert for $fullName")
                .setMessage("Are you sure you want to dismiss this alert?")
                .setPositiveButton("Yes") { _, _ ->
                    val updates = mapOf(
                        "sosActive" to false,
                        "dismissedAt" to System.currentTimeMillis(),
                        "dismissedBy" to officerId,
                        "dismissedByName" to officerName
                    )

                    Log.d("DISMISS_ALERT", "Applying dismissal updates for $fullName")

                    usersRef.child(userId).updateChildren(updates).addOnSuccessListener {
                        Log.d("DISMISS_ALERT", "Alert dismissed for $fullName")

                        sendNotificationToUser(
                            userId = userId,
                            title = "Alert Dismissed",
                            message = "Your SOS alert was dismissed by Officer $officerName.",
                            type = "dismissed"
                        )

                        loadOfficerStats()
                        loadResolvedAlerts()
                        loadAllAlerts()
                    }.addOnFailureListener {
                        Toast.makeText(this, "Failed to update alert status", Toast.LENGTH_SHORT).show()
                        Log.e("DISMISS_ALERT", "Failed to update alert: ${it.message}")
                    }
                }
                .setNegativeButton("Cancel") { _, _ ->
                    Log.d("DISMISS_ALERT", "Dismissal cancelled by user")
                }
                .show()
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to fetch user details", Toast.LENGTH_SHORT).show()
            Log.e("DISMISS_ALERT", "Failed to fetch user $userId: ${it.message}")
        }
    }
    private fun sendNotificationToUser(
        userId: String,
        title: String,
        message: String,
        type: String
    ) {
        Log.d("SEND_NOTIFICATION", "Preparing notification for user $userId")

        usersRef.child(userId).get().addOnSuccessListener { userSnap ->
            val firstName = userSnap.child("firstName").getValue(String::class.java) ?: ""
            val lastName = userSnap.child("lastName").getValue(String::class.java) ?: ""
            val fullName = "$firstName $lastName".trim()

            val notifRef = FirebaseDatabase.getInstance().getReference("notifications").push()
            val notifData = mapOf(
                "id" to notifRef.key,
                "title" to title,
                "message" to message,
                "timestamp" to System.currentTimeMillis(),
                "type" to type,
                "read" to false,
                "userId" to userId,
                "userName" to fullName,
                "relatedId" to officerId,
                "officerName" to officerName,
                "imageUrl" to ""
            )

            notifRef.setValue(notifData).addOnSuccessListener {
                Log.d("SEND_NOTIFICATION", "Notification sent to $fullName")
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to send notification", Toast.LENGTH_SHORT).show()
                Log.e("SEND_NOTIFICATION", "Failed to send notification: ${it.message}")
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to fetch user name", Toast.LENGTH_SHORT).show()
            Log.e("SEND_NOTIFICATION", "Failed to fetch user $userId: ${it.message}")
        }
    }
    private fun incrementOfficerCount(field: String) {
        val officerRef =
            FirebaseDatabase.getInstance().getReference("LawEnforcers").child(officerId)
        officerRef.child(field).runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val current = currentData.getValue(Int::class.java) ?: 0
                currentData.value = current + 1
                return Transaction.success(currentData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                snapshot: DataSnapshot?
            ) {
                if (committed) {
                    loadOfficerStats()
                } else {
                    Toast.makeText(
                        this@LawDashboardActivity,
                        "Failed to update officer stats",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })
    }

    override fun onOfficerAccountSaved(
        currentPassword: String,
        newName: String,
        newSurname: String,
        newPhone: String,
        newPassword: String?
    ) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)

        user.reauthenticate(credential)
            .addOnSuccessListener {
                val updates = mutableMapOf<String, Any>()
                if (newName.isNotEmpty()) updates["firstName"] = newName
                if (newSurname.isNotEmpty()) updates["lastName"] = newSurname
                if (newPhone.isNotEmpty()) updates["phoneNumber"] = newPhone

                val updateRef = FirebaseDatabase.getInstance().getReference("Users").child(user.uid)

                val updateTasks = mutableListOf<Task<Void>>()

                if (updates.isNotEmpty()) {
                    updateTasks.add(updateRef.updateChildren(updates))
                }

                if (!newPassword.isNullOrEmpty()) {
                    updateTasks.add(user.updatePassword(newPassword))
                }

                Tasks.whenAllComplete(updateTasks)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()
                        loadOfficerStats() // ✅ Refresh name immediately
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Update failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }

                if (updates.isEmpty() && newPassword.isNullOrEmpty()) {
                    Toast.makeText(this, "Nothing to update", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Wrong password", Toast.LENGTH_SHORT).show()
            }
    }


}