package com.example.helpathome

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.compose.animation.core.snap
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.database.*

class Statistics : AppCompatActivity() {

    private lateinit var barChart: BarChart
    private lateinit var usersRef: DatabaseReference
    private lateinit var activeUsers : TextView
    private lateinit var suspendedUsers : TextView
    private lateinit var pendingNGOs : TextView
    private lateinit var approvedNGOs : TextView
    private lateinit var ngoRef : DatabaseReference



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)

        barChart = findViewById(R.id.barChart)
        usersRef = FirebaseDatabase.getInstance().getReference("Users")
        activeUsers = findViewById(R.id.numActiveUsers)
        suspendedUsers = findViewById(R.id.numSuspendedUsers)
        pendingNGOs = findViewById(R.id.numPending)
        approvedNGOs = findViewById(R.id.numApproved)
        ngoRef = FirebaseDatabase.getInstance().getReference("NGOs")


        fetchCurrentAlerts()
        fetchUsers()
        fetchNGOs()
        loadNgoCategories()

    }

    private fun loadNgoCategories() {
        val ngoRef = FirebaseDatabase.getInstance().getReference("NGOs")
        val categoryCounts = mutableMapOf<String, Int>()

        ngoRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (ngoSnapshot in snapshot.children) {
                    val category = ngoSnapshot.child("category").getValue(String::class.java) ?: "Unknown"
                    categoryCounts[category] = categoryCounts.getOrDefault(category, 0) + 1
                }
                setupPieChart(categoryCounts)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@Statistics, "Error loading data: ${error.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun setupPieChart(categoryCounts: Map<String, Int>) {
        val entries = ArrayList<PieEntry>()
        for ((category, count) in categoryCounts) {
            entries.add(PieEntry(count.toFloat(), category))
        }

        val dataSet = PieDataSet(entries, "NGO Categories")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.valueTextSize = 14f
        dataSet.sliceSpace = 2f

        val data = PieData(dataSet)
        data.setValueTextSize(12f)
        data.setValueTextColor(android.graphics.Color.WHITE)

        val pieChart = findViewById<PieChart>(R.id.pieChartCategories)
        pieChart.data = data
        pieChart.description.isEnabled = false
        pieChart.isRotationEnabled = true
        pieChart.centerText = "NGO Categories"
        pieChart.setCenterTextSize(16f)
        pieChart.animateY(1000)
        pieChart.invalidate()
    }


    private fun fetchUsers(){
        usersRef.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                var activeUsersCount = 0
                var suspendedUsersCount = 0

                snapshot.children.forEach{ userSnap ->
                    val accountStatus = userSnap.child("accountStatus").getValue(String::class.java) ?: "active"
                    Log.d("Account Status", "User ${userSnap.key} accountStatus=$accountStatus")
                    if( accountStatus == "active"){
                        activeUsersCount++
                    }else if(accountStatus == "suspended")
                    suspendedUsersCount++
                }

                activeUsers.text = activeUsersCount.toString();
                suspendedUsers.text = suspendedUsersCount.toString();

            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("StatisticsActivity", "Error: ${error.message}")
            }
        })
    }

    private fun fetchNGOs(){
        ngoRef.addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                var pendingCount = 0
                var approvedCount = 0

                snapshot.children.forEach{ ngoSnap ->
                    val status = ngoSnap.child("profileStatus").getValue(String::class.java) ?: "Awaiting Approval"
                    Log.d("Profile Status", "NGO ${ngoSnap.key} status$status")
                    if(status == "Awaiting Approval")
                        pendingCount++ else approvedCount++


                    pendingNGOs.text = pendingCount.toString();
                    approvedNGOs.text = approvedCount.toString();
                }

            }


            override fun onCancelled(error: DatabaseError) {
                Log.e("StatisticsActivity", "Error: ${error.message}")
            }
        })
    }
    private fun fetchCurrentAlerts() {
        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var activeCount = 0
                var resolvedCount = 0

                snapshot.children.forEach { userSnap ->
                    val sosActive =
                        userSnap.child("sosActive").getValue(Boolean::class.java) ?: false
                    Log.d("StatisticsActivity", "User ${userSnap.key} sosActive=$sosActive")
                    if (sosActive) activeCount++ else resolvedCount++
                }

                Log.d("StatisticsActivity", "Active=$activeCount Resolved=$resolvedCount")
                displayBarChart(activeCount, resolvedCount)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("StatisticsActivity", "Error: ${error.message}")
            }
        })
    }

    private fun displayBarChart(activeCount: Int, resolvedCount: Int) {
        val entries = ArrayList<BarEntry>()
        // index 0 = Active, index 1 = Resolved
        entries.add(BarEntry(0f, activeCount.toFloat()))
        entries.add(BarEntry(1f, resolvedCount.toFloat()))

        val dataSet = BarDataSet(entries, "Alerts Overview").apply {
            setColors(
                resources.getColor(android.R.color.holo_red_dark),
                resources.getColor(android.R.color.holo_green_dark)
            )
            valueTextSize = 14f
        }

        val barData = BarData(dataSet)
        barData.barWidth = 0.5f // width of each bar

        barChart.data = barData

        // Format X-axis labels
        val xAxis = barChart.xAxis
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return when (value.toInt()) {
                    0 -> "Active Alerts"
                    1 -> "Resolved Alerts"
                    else -> ""
                }
            }
        }
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)

        barChart.axisRight.isEnabled = false
        barChart.description.isEnabled = false
        barChart.setFitBars(true)
        barChart.animateY(1000)
        barChart.invalidate()
    }
}
