package com.example.helpathome

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.helpathome.adapters.AlertAdapter
import com.example.helpathome.models.LastKnownLocation
import com.example.helpathome.models.alerts
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class LawDashboardActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var alertAdapter: AlertAdapter
    private val alertList = mutableListOf<alerts>()

    private lateinit var resolvedRecyclerView: RecyclerView
    private lateinit var resolvedAlertAdapter: AlertAdapter
    private val resolvedAlertList = mutableListOf<alerts>()

    private lateinit var usersRef: DatabaseReference
    private lateinit var summaryTextView: TextView
    private lateinit var syncTimeTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_law_dashboard)

        usersRef = FirebaseDatabase.getInstance().getReference("Users")

        summaryTextView = findViewById(R.id.textSummary)
        syncTimeTextView = findViewById(R.id.textSyncTime)

        recyclerView = findViewById(R.id.recyclerAlerts)
        recyclerView.layoutManager = LinearLayoutManager(this)
        alertAdapter = AlertAdapter(
            alertList,
            onResolveClick = { alert -> markAsResolved(alert) },
            onDeleteClick = { alert -> deleteAlert(alert) }
        )
        recyclerView.adapter = alertAdapter

        resolvedRecyclerView = findViewById(R.id.recyclerResolvedAlerts)
        resolvedRecyclerView.layoutManager = LinearLayoutManager(this)
        resolvedAlertAdapter = AlertAdapter(
            resolvedAlertList,
            onResolveClick = { /* No action */ },
            onDeleteClick = { alert -> deleteAlert(alert) }
        )
        resolvedRecyclerView.adapter = resolvedAlertAdapter

        loadAllAlerts()
        loadResolvedAlerts()
    }

    private fun loadAllAlerts() {
        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                alertList.clear()
                snapshot.children.forEach { userSnap ->
                    val userId = userSnap.key ?: return@forEach
                    val sosActive = userSnap.child("sosActive").getValue(Boolean::class.java) ?: false
                    if (!sosActive) return@forEach

                    val lastLocation = userSnap.child("lastKnownLocation")
                        .getValue(LastKnownLocation::class.java)

                    val alert = alerts(
                        userId = userId,
                        lastKnownLocation = lastLocation,
                        sosActive = true,
                        resolvedAt = null
                    )
                    alertList.add(alert)
                }
                alertAdapter.notifyDataSetChanged()
                updateSummaryAndSyncTime()
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

    private fun loadResolvedAlerts() {
        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                resolvedAlertList.clear()
                snapshot.children.forEach { userSnap ->
                    val userId = userSnap.key ?: return@forEach
                    val sosActive = userSnap.child("sosActive").getValue(Boolean::class.java) ?: false
                    if (sosActive) return@forEach

                    val resolvedAt = userSnap.child("resolvedAt").getValue(Long::class.java)
                    if (resolvedAt == null) return@forEach

                    val lastLocation = userSnap.child("lastKnownLocation")
                        .getValue(LastKnownLocation::class.java)

                    val alert = alerts(
                        userId = userId,
                        lastKnownLocation = lastLocation,
                        sosActive = false,
                        resolvedAt = resolvedAt
                    )
                    resolvedAlertList.add(alert)
                }
                resolvedAlertAdapter.notifyDataSetChanged()
                updateSummaryAndSyncTime()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@LawDashboardActivity,
                    "Failed to load resolved alerts: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun updateSummaryAndSyncTime() {
        val activeCount = alertList.size
        val resolvedCount = resolvedAlertList.size
        summaryTextView.text = "Active: $activeCount | Resolved: $resolvedCount"

        val lastSync = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        syncTimeTextView.text = "Last synced at: $lastSync"
    }

    private fun markAsResolved(alert: alerts) {
        val userId = alert.userId ?: return
        val updates = mapOf(
            "sosActive" to false,
            "resolvedAt" to System.currentTimeMillis()
        )
        usersRef.child(userId).updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Alert marked as resolved", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to mark alert as resolved", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteAlert(alert: alerts) {
        val userId = alert.userId ?: return
        usersRef.child(userId).removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Alert deleted successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to delete alert", Toast.LENGTH_SHORT).show()
            }
    }
}
