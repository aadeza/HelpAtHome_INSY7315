package com.example.helpathome

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class Home : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var txtUserName: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Firebase setup
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("Users")
        txtUserName = findViewById(R.id.txtUserName)

        // Load user's name
        val currentUserId = auth.currentUser?.uid
        if (currentUserId != null) {
            database.child(currentUserId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val firstName = snapshot.child("firstName").getValue(String::class.java) ?: ""
                    val displayName = firstName.trim()
                    txtUserName.text = if (displayName.isNotEmpty()) displayName else "Welcome!"
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@Home, "Failed to load user info", Toast.LENGTH_SHORT).show()
                    txtUserName.text = "User"
                }
            })
        } else {
            txtUserName.text = "Guest"
        }

        // Notification setup
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerNotifications)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val notifications = listOf(
            Notification("Report from NGO", "Shelter needed for family in Zone B", "10 min ago", "#00FF00")
        )

        recyclerView.adapter = NotificationAdapter(notifications)
    }
}
