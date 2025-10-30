package com.example.helpathome

import android.Manifest
import android.annotation.SuppressLint
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.os.*
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.*
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*

class Home : AppCompatActivity(), EditAccountDialog.OnEditAccountListener {

        override fun onSaveClicked(
            currentPass: String,
            newName: String,
            newSurname: String,
            newPhone: String,
            newEmail: String,
            newPassword: String?
        ) {
            val user = auth.currentUser
            if (user == null) {
                Toast.makeText(this, "No logged-in user", Toast.LENGTH_SHORT).show()
                return
            }

            val credential = EmailAuthProvider.getCredential(user.email!!, currentPass)

            user.reauthenticate(credential)
                .addOnSuccessListener {

                    // Update Email
                    if (newEmail.isNotEmpty() && newEmail != user.email) {
                        user.updateEmail(newEmail)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Email updated successfully", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Email update failed: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    }

                    // Update Password
                    if (!newPassword.isNullOrEmpty()) {
                        user.updatePassword(newPassword)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Password update failed: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    }

                    // Update Database (Name, Surname, Phone)
                    val updates = mutableMapOf<String, Any>()
                    if (newName.isNotEmpty()) updates["firstName"] = newName
                    if (newSurname.isNotEmpty()) updates["lastName"] = newSurname
                    if (newPhone.isNotEmpty()) updates["phoneNumber"] = newPhone

                    if (updates.isNotEmpty()) {
                        database.child(user.uid).updateChildren(updates)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                                if (newName.isNotEmpty()) {
                                    txtUserName.text = newName
                                    cachedUserName = null
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Profile update failed: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    }

                    if (newEmail.isEmpty() && newPassword.isNullOrEmpty() && updates.isEmpty()) {
                        Toast.makeText(this, "Nothing to update", Toast.LENGTH_SHORT).show()
                    }

                }
                .addOnFailureListener {
                    Toast.makeText(this, "Re-authentication failed. Wrong password?", Toast.LENGTH_SHORT).show()
                }
        }

        companion object {
            private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
            private const val sosHoldTime = 6000L
            private var cachedUserName: String? = null
            private var cachedNgoList: List<Ngo>? = null
        }

        private var notificationsEnabled = true
        private lateinit var notificationRef: DatabaseReference
        private lateinit var spinnerNgoCategory: Spinner

        private lateinit var auth: FirebaseAuth
        private lateinit var database: DatabaseReference
        private lateinit var txtUserName: TextView
        private lateinit var btnSos: Button
        private lateinit var txtLocation: TextView
        private lateinit var notificationAdapter: NotificationAdapter

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
            notificationRef = FirebaseDatabase.getInstance().getReference("notifications")
            spinnerNgoCategory = findViewById(R.id.spinnerNgoCategory)

            val currentUserId = auth.currentUser?.uid

            // Load User Name
            if (cachedUserName != null) {
                txtUserName.text = cachedUserName
            } else if (currentUserId != null) {
                database.child(currentUserId)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val firstName = snapshot.child("firstName").getValue(String::class.java) ?: ""
                            val displayName = firstName.trim()
                            cachedUserName = if (displayName.isNotEmpty()) displayName else "Welcome!"
                            txtUserName.text = cachedUserName
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(this@Home, "Failed to load user info", Toast.LENGTH_SHORT).show()
                            txtUserName.text = "User"
                        }
                    })
            } else {
                txtUserName.text = "Guest"
            }

            // Profile button popup
            val profileButton: ImageButton = findViewById(R.id.profileButton)
            profileButton.setOnClickListener { view ->
                val popup = PopupMenu(this, view)
                popup.menuInflater.inflate(R.menu.profile_menu, popup.menu)
                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.menu_edit_account -> {
                            openEditAccountDialog()
                            true
                        }
                        R.id.menu_notifications -> toggleNotifications(item) // now matches type
                        R.id.menu_logout -> {
                            logoutUser()
                            true
                        }
                        else -> false
                    }
                }
                popup.show()
            }

// Setup notifications RecyclerView
            val recyclerNotifications = findViewById<RecyclerView>(R.id.recyclerNotifications)
            recyclerNotifications.layoutManager = LinearLayoutManager(this)

            notificationAdapter = NotificationAdapter(
                mutableListOf(),
                { notification ->
                    val ngoId = notification.relatedId
                    if (!ngoId.isNullOrEmpty()) {
                        val intent = Intent(this, UserNgoViewActivity::class.java)
                        intent.putExtra("ngoId", ngoId)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "This notification is not linked to an NGO.", Toast.LENGTH_SHORT).show()
                    }
                },
                role = "civilian"
            )
            recyclerNotifications.adapter = notificationAdapter
            loadNotifications()
            // Setup NGOs RecyclerView
            val recyclerNgos = findViewById<RecyclerView>(R.id.recyclerNgos)
            recyclerNgos.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)




            if (cachedNgoList != null) {
                setupNgoAdapter(cachedNgoList!!)
                setupNgoCategorySpinner(cachedNgoList!!)
            } else {
                val ngoRef = FirebaseDatabase.getInstance().getReference("ngos")
                ngoRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val ngoList = mutableListOf<Ngo>()
                        Log.d("NGO_FETCH", "Snapshot has ${snapshot.childrenCount} children")

                        for (child in snapshot.children) {
                            Log.d("NGO_FETCH", "Raw child: ${child.value}")
                            val ngo = child.getValue(Ngo::class.java)
                            if (ngo != null) {
                                Log.d("NGO_FETCH", "Parsed NGO: ${ngo.name}")
                                ngoList.add(ngo)
                            } else {
                                Log.w("NGO_FETCH", "Failed to parse NGO from snapshot: ${child.key}")
                            }
                        }

                        Log.d("NGO_FETCH", "Final NGO list size: ${ngoList.size}")
                        cachedNgoList = ngoList
                        setupNgoAdapter(ngoList)
                        setupNgoCategorySpinner(ngoList)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("NGO_FETCH", "Firebase error: ${error.message}")
                        Toast.makeText(this@Home, "Failed to load NGOs", Toast.LENGTH_SHORT).show()
                    }
                })
            }

            // Setup Resources RecyclerView
            val recyclerResources = findViewById<RecyclerView>(R.id.recyclerResources)
            recyclerResources.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            recyclerResources.adapter = ResourceAdapter(
                listOf(
                    Resource("Mental Health Tips", R.drawable.mentalhealth),
                    Resource("Legal Aid Info", R.drawable.legalaid),
                    Resource("Emergency Contacts", R.drawable.emergencycontact),
                    Resource("Self-Defense Tips", R.drawable.selfdefense),
                    Resource("Shelter Guidelines", R.drawable.shelter),
                    Resource("Safety Planning", R.drawable.safetyplanning)
                )
            ) { resource ->
                when (resource.title) {
                    "Mental Health Tips" -> showMentalHealthTipsDialog()
                    "Legal Aid Info" -> showLegalAidTipsDialog()
                    "Emergency Contacts" -> showEmergencyContactsTipsDialog()
                    "Self-Defense Tips" -> showSelfDefenseTipsDialog()
                    "Shelter Guidelines" -> showShelterGuidelinesTipsDialog()
                    "Safety Planning" -> showSafetyPlanningTipsDialog()
                    else -> Toast.makeText(this, "${resource.title} clicked", Toast.LENGTH_SHORT).show()
                }
            }

            // SOS logic
            if (currentUserId != null) observeSosStatus(currentUserId)
            btnSos.setOnTouchListener { _, event -> handleSosTouch(event) }

            // Location permissions
            if (hasLocationPermissions()) startLocationUpdates()
            else ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }

    // -------------------- NGO Spinner & Adapter --------------------
    private fun setupNgoCategorySpinner(ngoList: List<Ngo>) {
        val categories = mutableListOf("All")
        categories.addAll(ngoList.mapNotNull { it.category }.distinct())

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerNgoCategory.adapter = adapter

        spinnerNgoCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCategory = categories[position]
                val filteredList = if (selectedCategory == "All") ngoList else ngoList.filter { it.category == selectedCategory }
                setupNgoAdapter(filteredList)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                setupNgoAdapter(ngoList)
            }
        }
    }

    private fun setupNgoAdapter(ngoList: List<Ngo>) {
        val recyclerNgos = findViewById<RecyclerView>(R.id.recyclerNgos)
        recyclerNgos.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerNgos.setHasFixedSize(true)
        recyclerNgos.isNestedScrollingEnabled = false
        recyclerNgos.adapter = NgoBrowseAdapter(
            ngos = ngoList,
            onItemClick = { selectedNgo ->
                val intent = Intent(this@Home, UserNgoViewActivity::class.java)
                intent.putExtra("ngoId", selectedNgo.id)
                startActivity(intent)
            }
        )

        Log.d("NGO_ADAPTER", "Adapter set with ${ngoList.size} NGOs")
        Toast.makeText(this, "Loaded ${ngoList.size} NGOs", Toast.LENGTH_SHORT).show()
    }
    // -------------------- Notifications --------------------
    private fun loadNotifications() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        notificationRef.orderByChild("userId").equalTo(currentUserId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val notificationList = mutableListOf<Notification>()
                    for (child in snapshot.children) {
                        val notif = child.getValue(Notification::class.java)
                        if (notif != null) notificationList.add(notif.copy(id = child.key ?: ""))
                    }
                    notificationAdapter.updateNotifications(notificationList)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("NOTIFICATIONS", "Failed to load notifications: ${error.message}")
                }
            })
    }

    private fun toggleNotifications(item: android.view.MenuItem): Boolean {
        notificationsEnabled = !notificationsEnabled

        if (!notificationsEnabled) {
            notificationAdapter.updateNotifications(emptyList())
            item.title = "Switch On Notifications"
            Toast.makeText(this, "Notifications switched off", Toast.LENGTH_SHORT).show()
        } else {
            loadNotifications()
            item.title = "Switch Off Notifications"
            Toast.makeText(this, "Notifications switched on", Toast.LENGTH_SHORT).show()
        }

        return true
    }


    private fun logoutUser(): Boolean {
        // Clear all cached session data
        cachedUserName = null
        cachedNgoList = null
        notificationAdapter.updateNotifications(emptyList())

        // Sign out from Firebase
        auth.signOut()

        // Confirm logout
        Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()

        // Redirect to login screen
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()

        return true
    }

    private fun openEditAccountDialog() {
        val user = auth.currentUser ?: return
        database.child(user.uid).get().addOnSuccessListener { snapshot ->
            val currentName = snapshot.child("firstName").getValue(String::class.java) ?: ""
            val currentSurname = snapshot.child("lastName").getValue(String::class.java) ?: ""
            val currentPhone = snapshot.child("phoneNumber").getValue(String::class.java) ?: ""
            val currentEmail = user.email ?: ""
            val dob = snapshot.child("dob").getValue(String::class.java) ?: " "
            val dialog = EditAccountDialog(currentName, currentSurname, currentEmail, currentPhone, dob)
            dialog.show(supportFragmentManager, "EditAccountDialog")
        }
    }

    // -------------------- SOS --------------------
    private fun observeSosStatus(userId: String) {
        val sosRef = database.child(userId).child("sosActive")
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

            }
        }
        sosRef.addValueEventListener(sosStatusListener!!)
    }

    private fun handleSosTouch(event: MotionEvent): Boolean {
        if (isSosOn) {
            Toast.makeText(this, "SOS is already active.", Toast.LENGTH_SHORT).show()
            return true
        }

        return when (event.action) {
            MotionEvent.ACTION_DOWN -> { startSosHold(); true }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { cancelSosHold(); true }
            else -> false
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
        sosRunnable = Runnable { activateSos() }
        handler.postDelayed(sosRunnable!!, sosHoldTime)
    }

    private fun cancelSosHold() {
        colorAnimator?.cancel()
        handler.removeCallbacks(sosRunnable!!)
        btnSos.setBackgroundColor(Color.parseColor("#880000"))
    }

    private fun activateSos() {
        isSosOn = true
        Toast.makeText(this, "SOS Activated!", Toast.LENGTH_SHORT).show()
        val currentUserId = auth.currentUser?.uid ?: return

        database.child(currentUserId).child("sosActive").setValue(true).addOnSuccessListener {
            database.child(currentUserId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) return

                    val userType = snapshot.child("userType").getValue(String::class.java) ?: ""
                    val fName = snapshot.child("firstName").getValue(String::class.java) ?: ""
                    val lName = snapshot.child("lastName").getValue(String::class.java) ?: ""
                    val locationNode = snapshot.child("lastKnownLocation")
                    val address = locationNode.child("address").getValue(String::class.java) ?: "Unknown"
                    val latitude = locationNode.child("latitude").getValue(Double::class.java) ?: 0.0
                    val longitude = locationNode.child("longitude").getValue(Double::class.java) ?: 0.0



                    // Notify law enforcement
                    val alertRef = database.child("sos_alerts").push()
                    val alertData = mapOf(
                        "userId" to currentUserId,
                        "firstName" to fName,
                        "lastName" to lName,
                        "timestamp" to System.currentTimeMillis(),
                        "location" to address,
                        "latitude" to latitude,
                        "longitude" to longitude,
                        "resolved" to false
                    )
                    alertRef.setValue(alertData)

                    updateSosButtonUI()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@Home, "Failed to read user data.", Toast.LENGTH_SHORT).show()
                }
            })
        }.addOnFailureListener {
            Toast.makeText(this@Home, "Failed to activate SOS.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateSosButtonUI() {
        val sosBanner = findViewById<View>(R.id.sosBanner)
        if (isSosOn) {
            btnSos.setBackgroundColor(Color.RED)
            btnSos.text = getString(R.string.sos_on)
            btnSos.isEnabled = false
            sosBanner.visibility = View.VISIBLE
        } else {
            btnSos.setBackgroundColor(Color.parseColor("#880000"))
            btnSos.text = getString(R.string.hold_for_sos_)
            btnSos.isEnabled = true
            sosBanner.visibility = View.GONE
        }
    }
    // -------------------- Location --------------------
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
        val thresholdMeters = 50
        val shouldSave = lastSavedLocation == null || lastSavedLocation!!.distanceTo(newLocation) > thresholdMeters
        if (!shouldSave) return
        lastSavedLocation = newLocation

        val geocoder = Geocoder(this, Locale.getDefault())
        val addressString = try {
            val addresses = geocoder.getFromLocation(newLocation.latitude, newLocation.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val suburb = address.subLocality ?: address.locality ?: "Unknown suburb"
                val city = address.adminArea ?: "Unknown city"
                "$suburb, $city"
            } else "Unknown location"
        } catch (e: Exception) { "Unknown location" }

        val currentUserId = auth.currentUser?.uid ?: return
        val locationMap = mapOf(
            "latitude" to newLocation.latitude,
            "longitude" to newLocation.longitude,
            "address" to addressString,
            "timestamp" to System.currentTimeMillis()
        )
        database.child(currentUserId).child("lastKnownLocation").setValue(locationMap)
    }

    private fun hasLocationPermissions(): Boolean =
        ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
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
                val timeDiff = System.currentTimeMillis() - lastLocationTime
                val timeString = if (timeDiff < 60_000) "(now)" else "(${timeDiff / 60000} mins ago)"
                txtLocation.text = getString(R.string.sub_city_time, suburb, city, timeString)
            } else txtLocation.text = getString(R.string.location_unavailable)
        } catch (e: Exception) { txtLocation.text = getString(R.string.location_error) }

    }

    // -------------------- Tips Dialogs --------------------
    private fun showMentalHealthTipsDialog() { showTipsDialog("Mental Health Tips", listOf(
        "Take breaks and breathe deeply.",
        "Stay connected with supportive people.",
        "Practice gratitude daily.",
        "Exercise regularly to boost mood.",
        "Seek help if you feel overwhelmed."
    )) }

    private fun showLegalAidTipsDialog() { showTipsDialog("Legal Aid Info", listOf(
        "Know your rights—stay informed about local laws.",
        "Keep records of any legal incidents or communications.",
        "Consult qualified legal professionals when needed.",
        "Use trusted NGOs or organizations for legal support.",
        "Report abuse or injustice to authorities promptly."
    )) }

    private fun showEmergencyContactsTipsDialog() { showTipsDialog("Emergency Contacts", listOf(
        "Save important numbers (police, ambulance, fire) on speed dial.",
        "Share your emergency contacts with trusted friends or family.",
        "Keep a written copy of emergency contacts in your wallet.",
        "Know the location of nearest hospitals and police stations.",
        "Update your emergency contacts regularly."
    )) }

    private fun showSelfDefenseTipsDialog() { showTipsDialog("Self-Defense Tips", listOf(
        "Stay aware of your surroundings at all times.",
        "Trust your instincts—avoid unsafe situations.",
        "Learn basic self-defense moves from a qualified instructor.",
        "Use your voice loudly to deter attackers.",
        "Carry legal self-defense tools if permitted."
    )) }

    private fun showShelterGuidelinesTipsDialog() { showTipsDialog("Shelter Guidelines", listOf(
        "Locate nearby shelters ahead of time.",
        "Pack essentials: ID, medication, clothes, and phone charger.",
        "Inform someone you trust before leaving.",
        "Follow shelter rules and respect staff.",
        "Use shelter resources to plan next steps safely."
    )) }

    private fun showSafetyPlanningTipsDialog() { showTipsDialog("Safety Planning", listOf(
        "Create an emergency escape plan.",
        "Identify safe places to go if you feel threatened.",
        "Keep a packed bag ready with essentials.",
        "Share your plan with trusted friends or family.",
        "Update your plan regularly as situations change."
    )) }

    private fun showTipsDialog(title: String, tips: List<String>) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(tips.mapIndexed { i, s -> "${i + 1}. $s" }.joinToString("\n"))
            .setPositiveButton("Got it") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) startLocationUpdates()
            else Toast.makeText(this, "Location permissions denied", Toast.LENGTH_SHORT).show()
        }
    }
}
