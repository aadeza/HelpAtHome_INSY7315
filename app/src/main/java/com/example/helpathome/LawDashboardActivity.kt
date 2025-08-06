package com.example.helpathome

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.helpathome.adapters.AlertAdapter
import com.example.helpathome.models.alerts
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class LawDashboardActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var alertAdapter: AlertAdapter
    private val alertList = mutableListOf<alerts>()
    private lateinit var usersRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_law_dashboard)

        recyclerView = findViewById(R.id.recyclerAlerts)
        recyclerView.layoutManager = LinearLayoutManager(this)

        alertAdapter = AlertAdapter(
            alertList,
            onResolveClick = { alert -> markAsResolved(alert) }
        )
        recyclerView.adapter = alertAdapter

        usersRef = FirebaseDatabase.getInstance().getReference("Users")
        loadAllAlerts()
    }

    private fun loadAllAlerts() {
        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                alertList.clear()
                for (userSnap in snapshot.children) {
                    val userId = userSnap.key ?: continue
                    val sosActive = userSnap.child("sosActive").getValue(Boolean::class.java) ?: false


                    if (sosActive) {
                        val location = userSnap.child("location").getValue(String::class.java) ?: "-26.1087, 28.0567 (Sandton, Johannesburg)"
                        val timestamp = userSnap.child("timestamp").getValue(Long::class.java)?.toString() ?: "1744990251996"

                        val alert = alerts(
                            userId = userId,
                            location = location,
                            timestamp = timestamp,
                            sosActive = true
                        )

                        alertList.add(alert)
                    }
                }
                alertAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@LawDashboardActivity,
                    "Failed to load alerts: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun markAsResolved(alert: alerts) {
        val userId = alert.userId ?: return
        val userRef = usersRef.child(userId)

        userRef.child("sosActive").setValue(false)
            .addOnSuccessListener {
                userRef.child("resolvedBy").setValue(FirebaseAuth.getInstance().currentUser?.uid ?: "unknown")
                userRef.child("resolvedAt").setValue(ServerValue.TIMESTAMP)
                Toast.makeText(this, "Alert marked as resolved", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to mark alert as resolved", Toast.LENGTH_SHORT).show()
            }
    }
}