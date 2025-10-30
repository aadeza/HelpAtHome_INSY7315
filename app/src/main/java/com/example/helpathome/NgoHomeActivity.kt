package com.example.helpathome

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class NgoHomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private lateinit var txtUserName: TextView
    private lateinit var profileButton: ImageButton
    private lateinit var recyclerNgoNotifications: RecyclerView
    private lateinit var btnCreateNgo: Button
    private lateinit var btnManageNgos: Button
    private var notificationsEnabled = true

    private lateinit var notificationAdapter: NotificationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ngo_home)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        txtUserName = findViewById(R.id.txtUserName)
        profileButton = findViewById(R.id.profileButton)
        recyclerNgoNotifications = findViewById(R.id.recyclerNgoNotifications)
        btnCreateNgo = findViewById(R.id.btnCreateNGO)
        btnManageNgos = findViewById(R.id.btnManageNGOs)

        recyclerNgoNotifications.layoutManager = LinearLayoutManager(this)
        notificationAdapter = NotificationAdapter(mutableListOf(), role = "ngo")
        recyclerNgoNotifications.adapter = notificationAdapter

        btnCreateNgo.setOnClickListener {
            startActivity(Intent(this, CreateNgoActivity::class.java))
        }

        btnManageNgos.setOnClickListener {
            startActivity(Intent(this, ManageNgosActivity::class.java))
        }

        profileButton.setOnClickListener { view ->
            showProfileMenu(view)
        }

        loadUserProfile()
        loadNotifications()
    }

    private fun showProfileMenu(anchor: View) {
        val popup = PopupMenu(this, anchor, Gravity.END)
        popup.menuInflater.inflate(R.menu.profile_menu, popup.menu)

        popup.menu.findItem(R.id.menu_notifications).title =
            if (notificationsEnabled) "Switch Notifications Off" else "Switch Notifications On"

        popup.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.menu_edit_account -> {
                    val user = auth.currentUser
                    if (user != null) {
                        database.child("Users").child(user.uid).get()
                            .addOnSuccessListener { snapshot ->
                                val f = snapshot.child("firstName").getValue(String::class.java) ?: ""
                                val l = snapshot.child("lastName").getValue(String::class.java) ?: ""
                                val e = snapshot.child("email").getValue(String::class.java) ?: (user.email ?: "")
                                val p = snapshot.child("phoneNumber").getValue(String::class.java) ?: ""
                                val dob = snapshot.child("dob").getValue(String::class.java) ?: ""

                                val dialog = EditAccountDialog(f, l, e, p, dob)
                                dialog.show(supportFragmentManager, "EditAccountDialog")
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Failed to load profile for editing", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this, "No logged-in user", Toast.LENGTH_SHORT).show()
                    }
                    true
                }

                R.id.menu_notifications -> {
                    toggleNotifications()
                    true
                }

                R.id.menu_logout -> {
                    // Clear cached session data
                    val sharedPref = getSharedPreferences("session", MODE_PRIVATE)
                    sharedPref.edit().clear().apply()
                    notificationAdapter.updateNotifications(emptyList())
                    notificationsEnabled = true
                    txtUserName.text = "Guest"

                    // Sign out from Firebase
                    auth.signOut()

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

    private fun loadUserProfile() {
        val user = auth.currentUser ?: run {
            txtUserName.text = "Guest"
            return
        }
        val uid = user.uid
        database.child("Users").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val firstName = snapshot.child("firstName").getValue(String::class.java) ?: ""
                    val lastName = snapshot.child("lastName").getValue(String::class.java) ?: ""
                    val displayName = if ((firstName + lastName).isBlank()) user.email ?: "NGO User" else "$firstName $lastName"
                    txtUserName.text = displayName

                    notificationsEnabled = snapshot.child("notificationsEnabled").getValue(Boolean::class.java) ?: true
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@NgoHomeActivity, "Failed to load user profile", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun toggleNotifications() {
        val user = auth.currentUser ?: return
        val uid = user.uid
        val newValue = !notificationsEnabled

        database.child("Users").child(uid).child("notificationsEnabled").setValue(newValue)
            .addOnSuccessListener {
                notificationsEnabled = newValue
                Toast.makeText(this, if (newValue) "Notifications enabled" else "Notifications disabled", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update notification setting", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadNotifications() {
        val currentUserId = auth.currentUser?.uid ?: return

        database.child("NgoAccounts").child(currentUserId).child("linkedNgoId")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val ngoId = snapshot.getValue(String::class.java) ?: return

                    database.child("ngo_notifications").child(ngoId)
                        .orderByChild("timestamp").limitToLast(10)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val notifications = mutableListOf<Notification>()
                                for (child in snapshot.children) {
                                    val notif = child.getValue(Notification::class.java)
                                    if (notif != null) notifications.add(notif)
                                }
                                notificationAdapter.updateNotifications(notifications)
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(this@NgoHomeActivity, "Failed to load NGO notifications", Toast.LENGTH_SHORT).show()
                            }
                        })
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@NgoHomeActivity, "Failed to find linked NGO", Toast.LENGTH_SHORT).show()
                }
            })
    }
}