package com.example.helpathome

import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class NGOActionPerformanceTest {

    @Before
    fun setup() {
        FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext())
    }

    // ✅ Test 1: NGO Registration (creating a new NGO)
    @Test
    fun testNGORegistrationPerformance() {
        val db = FirebaseDatabase.getInstance().getReference("NGOs")
        val ngoData = mapOf(
            "name" to "Test Shelter ${System.currentTimeMillis()}",
            "founder" to "Jane Doe",
            "category" to "Shelter Support",
            "dateFounded" to "2022-10-10"
        )

        val latch = CountDownLatch(1)
        val start = System.currentTimeMillis()
        var duration = 0L
        var success = false

        db.push().setValue(ngoData)
            .addOnSuccessListener {
                duration = System.currentTimeMillis() - start
                println("✅ NGO Registered in ${duration}ms")
                success = true
                latch.countDown()
            }
            .addOnFailureListener {
                println("❌ NGO Registration Failed: ${it.message}")
                latch.countDown()
            }

        latch.await(10, TimeUnit.SECONDS)
        assertTrue("❌ NGO registration failed", success)
        assertTrue("❌ NGO registration took too long: ${duration}ms", duration in 1..8000)
    }

    // ✅ Test 2: NGO Posting an Update
    @Test
    fun testNGOPostUpdatePerformance() {
        val db = FirebaseDatabase.getInstance().getReference("NGOUpdates")
        val update = mapOf(
            "message" to "We have new volunteers available!",
            "timestamp" to System.currentTimeMillis()
        )

        val latch = CountDownLatch(1)
        val start = System.currentTimeMillis()
        var duration = 0L
        var success = false

        db.push().setValue(update)
            .addOnSuccessListener {
                duration = System.currentTimeMillis() - start
                println("✅ NGO Update posted in ${duration}ms")
                success = true
                latch.countDown()
            }
            .addOnFailureListener {
                println("❌ NGO Update failed: ${it.message}")
                latch.countDown()
            }

        latch.await(10, TimeUnit.SECONDS)
        assertTrue("❌ NGO update post failed", success)
        assertTrue("❌ NGO update took too long (${duration}ms)", duration in 1..7000)
    }

    // ✅ Test 3: Fetching NGO Statistics (count NGOs and HelpRequests)
    @Test
    fun testNGOStatisticsLoadPerformance() {
        val db = FirebaseDatabase.getInstance().reference
        val latch = CountDownLatch(1)
        var duration = 0L
        var success = false

        val start = System.currentTimeMillis()
        db.get().addOnSuccessListener { snapshot ->
            val ngoCount = snapshot.child("NGOs").childrenCount
            val helpCount = snapshot.child("HelpRequests").childrenCount
            duration = System.currentTimeMillis() - start
            println("✅ NGOs: $ngoCount | HelpRequests: $helpCount loaded in ${duration}ms")
            success = true
            latch.countDown()
        }.addOnFailureListener {
            println("❌ Statistics fetch failed: ${it.message}")
            latch.countDown()
        }

        latch.await(10, TimeUnit.SECONDS)
        assertTrue("❌ NGO statistics failed to load", success)
        assertTrue("❌ NGO stats load took too long (${duration}ms)", duration in 1..8000)
    }
}