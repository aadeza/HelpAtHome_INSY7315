package com.example.helpathome

import Ngo
import android.Manifest
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateUtils
import android.view.MotionEvent
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*

class Home : AppCompatActivity() {

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val sosHoldTime = 6000L  // 6 seconds in milliseconds
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var txtUserName: TextView
    private lateinit var btnSos: Button
    private lateinit var txtLocation: TextView

    private var isSosOn = false
    private var handler = Handler(Looper.getMainLooper())
    private var sosRunnable: Runnable? = null
    private var colorAnimator: ValueAnimator? = null

    private var sosStatusListener: ValueEventListener? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lastLocationTime: Long = 0L
    private var lastSavedLocation: Location? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("Users")
        txtUserName = findViewById(R.id.txtUserName)
        btnSos = findViewById(R.id.btnSos)
        txtLocation = findViewById(R.id.txtLocation)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val currentUserId = auth.currentUser?.uid

        // Load user's name
        if (currentUserId != null) {
            database.child(currentUserId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val firstName = snapshot.child("firstName").getValue(String::class.java) ?: ""
                    txtUserName.text = if (firstName.isNotEmpty()) firstName.trim() else "Welcome!"
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@Home, "Failed to load user info", Toast.LENGTH_SHORT).show()
                    txtUserName.text = "User"
                }
            })
        } else {
            txtUserName.text = "Guest"
        }

        // Setup notifications
        val recyclerNotifications = findViewById<RecyclerView>(R.id.recyclerNotifications)
        recyclerNotifications.layoutManager = LinearLayoutManager(this)
        recyclerNotifications.adapter = NotificationAdapter(listOf(
            Notification("Report from NGO", "Shelter needed for family in Zone B", "10 min ago", "#00FF00")
        ))

        // Setup resources
        val recyclerResources = findViewById<RecyclerView>(R.id.recyclerResources)
        recyclerResources.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerResources.adapter = ResourceAdapter(listOf(
            Resource("Mental Health Tips", R.drawable.mentalhealth),
            Resource("Legal Aid Info", R.drawable.legalaid),
            Resource("Emergency Contacts", R.drawable.emergencycontact),
            Resource("Self-Defense Tips", R.drawable.selfdefense),
            Resource("Shelter Guidelines", R.drawable.shelter),
            Resource("Safety Planning", R.drawable.safetyplanning)
        )) { resource ->
            when(resource.title) {
                "Mental Health Tips" -> showMentalHealthTipsDialog()
                "Legal Aid Info" -> showLegalAidTipsDialog()
                "Emergency Contacts" -> showEmergencyContactsTipsDialog()
                "Self-Defense Tips" -> showSelfDefenseTipsDialog()
                "Shelter Guidelines" -> showShelterGuidelinesTipsDialog()
                "Safety Planning" -> showSafetyPlanningTipsDialog()
                else -> Toast.makeText(this, "${resource.title} clicked", Toast.LENGTH_SHORT).show()
            }
        }

        // 🔥 Load NGOs from Firebase
        val recyclerNgos = findViewById<RecyclerView>(R.id.recyclerNgos)
        recyclerNgos.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        val ngoRef = FirebaseDatabase.getInstance().getReference("NGOs")
        ngoRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val ngoList = mutableListOf<Ngo>()
                for (child in snapshot.children) {
                    val ngo = child.getValue(Ngo::class.java)
                    if (ngo != null) {
                        ngoList.add(ngo)
                    }
                }
                recyclerNgos.adapter = NgoAdapter(ngoList)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@Home, "Failed to load NGOs", Toast.LENGTH_SHORT).show()
            }
        })

        // SOS Live Listener
        if (currentUserId != null) {
            val sosRef = database.child(currentUserId).child("sosActive")
            sosStatusListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val active = snapshot.getValue(Boolean::class.java) ?: false
                    if (isSosOn && !active) {
                        Toast.makeText(this@Home, "SOS turned OFF by admin.", Toast.LENGTH_LONG).show()
                    }
                    isSosOn = active
                    updateSosButtonUI()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@Home, "Failed to read SOS status", Toast.LENGTH_SHORT).show()
                }
            }
            sosRef.addValueEventListener(sosStatusListener!!)
        }

        // SOS Touch Logic
        btnSos.setOnTouchListener { _, event ->
            if (isSosOn) {
                Toast.makeText(this, "SOS is already active.", Toast.LENGTH_SHORT).show()
                return@setOnTouchListener true
            }

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startSosHold()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    cancelSosHold()
                    true
                }
                else -> false
            }
        }

        // Location Permission
        if (hasLocationPermissions()) {
            startLocationUpdates()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }


    private fun showMentalHealthTipsDialog() {
        val tips = """
        1. Take breaks and breathe deeply.
        2. Stay connected with supportive people.
        3. Practice gratitude daily.
        4. Exercise regularly to boost mood.
        5. Seek help if you feel overwhelmed.
    """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Mental Health Tips")
            .setMessage(tips)
            .setPositiveButton("Got it") { dialog, _ -> dialog.dismiss() }
            .show()
    }
    private fun showLegalAidTipsDialog() {
        val tips = """
        1. Know your rights—stay informed about local laws.
        2. Keep records of any legal incidents or communications.
        3. Consult qualified legal professionals when needed.
        4. Use trusted NGOs or organizations for legal support.
        5. Report abuse or injustice to authorities promptly.
    """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Legal Aid Info")
            .setMessage(tips)
            .setPositiveButton("Got it") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showEmergencyContactsTipsDialog() {
        val tips = """
        1. Save important numbers (police, ambulance, fire) on speed dial.
        2. Share your emergency contacts with trusted friends or family.
        3. Keep a written copy of emergency contacts in your wallet.
        4. Know the location of nearest hospitals and police stations.
        5. Update your emergency contacts regularly.
    """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Emergency Contacts")
            .setMessage(tips)
            .setPositiveButton("Got it") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showSelfDefenseTipsDialog() {
        val tips = """
        1. Stay aware of your surroundings at all times.
        2. Trust your instincts—avoid unsafe situations.
        3. Learn basic self-defense moves from a qualified instructor.
        4. Use your voice loudly to deter attackers.
        5. Carry legal self-defense tools if permitted.
    """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Self-Defense Tips")
            .setMessage(tips)
            .setPositiveButton("Got it") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showShelterGuidelinesTipsDialog() {
        val tips = """
        1. Locate nearby shelters ahead of time.
        2. Pack essentials: ID, medication, clothes, and phone charger.
        3. Inform someone you trust before leaving.
        4. Follow shelter rules and respect staff.
        5. Use shelter resources to plan next steps safely.
    """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Shelter Guidelines")
            .setMessage(tips)
            .setPositiveButton("Got it") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showSafetyPlanningTipsDialog() {
        val tips = """
        1. Create an emergency escape plan.
        2. Identify safe places to go if you feel threatened.
        3. Keep a packed bag ready with essentials.
        4. Share your plan with trusted friends or family.
        5. Update your plan regularly as situations change.
    """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Safety Planning")
            .setMessage(tips)
            .setPositiveButton("Got it") { dialog, _ -> dialog.dismiss() }
            .show()
    }


    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let { location ->
                lastLocationTime = System.currentTimeMillis()
                updateLocationText(location)
                maybeSaveLocationToFirebase(location)
            }
        }
    }

    private fun maybeSaveLocationToFirebase(newLocation: Location) {
        val thresholdMeters = 50 // only save if moved more than 50 meters

        val shouldSave = lastSavedLocation == null || lastSavedLocation!!.distanceTo(newLocation) > thresholdMeters

        if (shouldSave) {
            lastSavedLocation = newLocation

            val geocoder = Geocoder(this, Locale.getDefault())
            val addressString = try {
                val addresses = geocoder.getFromLocation(newLocation.latitude, newLocation.longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val suburb = address.subLocality ?: address.locality ?: "Unknown suburb"
                    val city = address.adminArea ?: "Unknown city"
                    "$suburb, $city"
                } else {
                    "Unknown location"
                }
            } catch (e: Exception) {
                "Unknown location"
            }

            val currentUserId = auth.currentUser?.uid ?: return

            val locationMap = mapOf(
                "latitude" to newLocation.latitude,
                "longitude" to newLocation.longitude,
                "address" to addressString,
                "timestamp" to System.currentTimeMillis()
            )

            database.child(currentUserId).child("lastKnownLocation").setValue(locationMap)
                .addOnSuccessListener {
                    // Optionally log or toast location saved
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to save location.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun hasLocationPermissions(): Boolean {
        return (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startLocationUpdates()
            } else {
                Toast.makeText(this, "Location permissions denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("MissingPermission") // Permissions are checked before calling
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(3000)
            .build()

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    private fun updateLocationText(location: Location) {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val suburb = address.subLocality ?: address.locality ?: "Unknown suburb"
                val city = address.adminArea ?: "Unknown city"

                val now = System.currentTimeMillis()
                val timeDiff = now - lastLocationTime

                val timeString = if (timeDiff < 60_000) {
                    "(now)"
                } else {
                    val minutesAgo = timeDiff / 60000
                    "($minutesAgo mins ago)"
                }

                txtLocation.text = "$suburb, $city $timeString"
            } else {
                txtLocation.text = "Location unavailable"
            }
        } catch (e: Exception) {
            txtLocation.text = "Location error"
        }
    }

    private fun updateSosButtonUI() {
        if (isSosOn) {
            btnSos.setBackgroundColor(Color.RED)
            btnSos.text = "SOS ON"
            btnSos.isEnabled = false
        } else {
            btnSos.setBackgroundColor(Color.parseColor("#880000")) // dim red
            btnSos.text = "HOLD FOR SOS"
            btnSos.isEnabled = true
        }
    }

    private fun startSosHold() {
        colorAnimator = ValueAnimator.ofObject(ArgbEvaluator(), Color.parseColor("#880000"), Color.RED).apply {
            duration = sosHoldTime
            addUpdateListener { animator ->
                val color = animator.animatedValue as Int
                btnSos.setBackgroundColor(color)
            }
            start()
        }

        sosRunnable = Runnable {
            activateSos()
        }
        handler.postDelayed(sosRunnable!!, sosHoldTime)
    }

    private fun cancelSosHold() {
        colorAnimator?.cancel()
        handler.removeCallbacks(sosRunnable!!)
        btnSos.setBackgroundColor(Color.parseColor("#880000")) // Reset to dim red
    }

    private fun activateSos() {
        isSosOn = true
        Toast.makeText(this, "SOS Activated!", Toast.LENGTH_SHORT).show()

        val currentUserId = auth.currentUser?.uid
        if (currentUserId != null) {
            database.child(currentUserId).child("sosActive").setValue(true)
                .addOnSuccessListener {
                    // Additional logic if needed
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to activate SOS.", Toast.LENGTH_SHORT).show()
                }
        }

        updateSosButtonUI()
    }

    override fun onDestroy() {
        super.onDestroy()
        val currentUserId = auth.currentUser?.uid
        if (currentUserId != null && sosStatusListener != null) {
            database.child(currentUserId).child("sosActive").removeEventListener(sosStatusListener!!)
        }
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}


