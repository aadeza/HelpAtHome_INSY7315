package com.example.helpathome

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class AdminDashboard : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var userRef: DatabaseReference
    private lateinit var ngoRef: DatabaseReference

    private lateinit var txtUserName: TextView

    private lateinit var recyclerUsers: RecyclerView
    private lateinit var usersAdapter: UsersAdapter
    private val usersList = mutableListOf<Users>()

    private lateinit var recyclerNGOProfiles: RecyclerView
    private lateinit var ngoProfileAdapter: NGOProfilesAdapter
    private val profilesList = mutableListOf<NGOProfile>()

    private lateinit var recyclerActivities : RecyclerView
    private val logs = mutableListOf<SystemActivity>()
    private lateinit var systemActivityAdapter : SystemActivityAdapter
    private val dataBase = FirebaseDatabase.getInstance().reference.child("SystemLogs")

    private lateinit var statsBtn: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_dashboard)

        // Firebase initialization
        auth = FirebaseAuth.getInstance()
        userRef = FirebaseDatabase.getInstance().getReference("Users")
        ngoRef = FirebaseDatabase.getInstance().getReference("NGOs")


        statsBtn = findViewById(R.id.fab)
        recyclerUsers = findViewById(R.id.recyclerManageUsers)
        recyclerNGOProfiles = findViewById(R.id.recyclerProfiles)
        recyclerActivities = findViewById(R.id.recyclerSystemActivities)

        txtUserName = findViewById(R.id.txtUserName)

        setupUserGreeting()
        setupUsersRecycler()
        setupNGORecycler()
        setupSystemActivityRecycler()

        initializeAccountStatus()
        initializeProfileStatus()

        loadUsers()
        loadNGOs()
        fetchSystemActivities()

        statsBtn.setOnClickListener{
            val intent = Intent(this, Statistics::class.java)
            startActivity(intent)
        }

    }

    private fun setupUserGreeting() {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId != null) {
            userRef.child(currentUserId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val firstName = snapshot.child("firstName").getValue(String::class.java)
                        txtUserName.text = firstName?.takeIf { it.isNotEmpty() } ?: "Welcome!"
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("AdminDashboard", "Failed to load user info", error.toException())
                        Toast.makeText(this@AdminDashboard, "Failed to load user info", Toast.LENGTH_SHORT).show()
                        txtUserName.text = "User"
                    }
                })
        } else {
            txtUserName.text = "Guest"
        }
    }


    private fun setupUsersRecycler() {
        recyclerUsers.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        usersAdapter = UsersAdapter(usersList) { user, newStatus ->
            updateStatus(user.id, newStatus)
        }
        recyclerUsers.adapter = usersAdapter

        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(recyclerUsers)

    }

    private fun setupNGORecycler() {
        recyclerNGOProfiles.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        ngoProfileAdapter = NGOProfilesAdapter(profilesList) { ngo, newStatus ->
            updateProfileStatus( ngo.id, newStatus)
        }
        recyclerNGOProfiles.adapter = ngoProfileAdapter

        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(recyclerNGOProfiles)

    }

    private fun setupSystemActivityRecycler() {

        recyclerActivities.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        systemActivityAdapter = SystemActivityAdapter(logs)
        recyclerActivities.adapter = systemActivityAdapter

        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(recyclerActivities)

    }



    private fun fetchSystemActivities() {
        FirebaseDatabase.getInstance().reference
            .child("SystemLogs")
            .orderByChild("timestamp")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    logs.clear()
                    for (logSnap in snapshot.children) {
                        val log = logSnap.getValue(SystemActivity::class.java)
                        log?.let { logs.add(it) }
                    }
                    logs.reverse()
                    systemActivityAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@AdminDashboard, "Failed to load logs", Toast.LENGTH_SHORT).show()
                }
            })
    }


    private fun loadUsers() {
        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                usersList.clear()
                for (userSnap in snapshot.children) {
                    Log.d("AdminDashboard", "User Node: ${userSnap.value}") // 👈 log raw data

                    val userId = userSnap.key ?: continue
                    val firstName = userSnap.child("firstName").getValue(String::class.java) ?: "unknown"
                    val lastName = userSnap.child("lastName").getValue(String::class.java) ?: ""
                    val userType = userSnap.child("userType").getValue(String::class.java) ?: "unknown"
                    val accountStatus = userSnap.child("accountStatus").getValue(String::class.java) ?: "active"

                    usersList.add(Users(userId, firstName, lastName,userType, accountStatus))
                }
                usersAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("AdminDashboard", "Failed to fetch users", error.toException())
                Toast.makeText(this@AdminDashboard, "Failed to fetch users", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadNGOs() {
        ngoRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                profilesList.clear()
                for (ngoSnap in snapshot.children) {
                    Log.d("AdminDashboard", "NGO Node: ${ngoSnap.value}")

                    val ngoId = ngoSnap.key ?: continue
                    val name = ngoSnap.child("name").getValue(String::class.java) ?: ""
                    val founder = ngoSnap.child("founder").getValue(String::class.java) ?: ""
                    val category = ngoSnap.child("category").getValue(String::class.java) ?: ""
                    val dateFounded = ngoSnap.child("dateFounded").getValue(String::class.java) ?: ""
                    val status = ngoSnap.child("profileStatus").getValue(String::class.java) ?: "Awaiting Approval"

                    profilesList.add(NGOProfile(ngoId, name, founder, category, dateFounded, status))
                }
                ngoProfileAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("AdminDashboard", "Failed to fetch NGO profiles", error.toException())
                Toast.makeText(this@AdminDashboard, "Failed to fetch NGO profiles", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun initializeAccountStatus() {
        userRef.get().addOnSuccessListener { snapshot ->
            for (userSnap in snapshot.children) {
                val userId = userSnap.key ?: continue
                val currentStatus = userSnap.child("accountStatus").getValue(String::class.java)


                if (currentStatus == null || currentStatus == "Awaiting Approval") {
                    userRef.child(userId).child("accountStatus").setValue("active")
                        .addOnSuccessListener {
                            Log.d("AdminDashboard", "Initialized accountStatus=active for $userId")
                        }
                        .addOnFailureListener { e ->
                            Log.w("AdminDashboard", "Failed to initialize accountStatus for $userId", e)
                        }
                }

                // Remove stray profileStatus from Users
                if (userSnap.hasChild("profileStatus")) {
                    userRef.child(userId).child("profileStatus").removeValue()
                        .addOnSuccessListener {
                            Log.d("AdminDashboard", "Removed profileStatus from User $userId")
                        }
                        .addOnFailureListener { e ->
                            Log.w("AdminDashboard", "Failed to remove profileStatus from User $userId", e)
                        }
                }
            }
        }.addOnFailureListener { e ->
            Log.w("AdminDashboard", "Error fetching users for status init", e)
        }
    }

    private fun initializeProfileStatus() {
        ngoRef.get().addOnSuccessListener { snapshot ->
            for (ngoSnap in snapshot.children) {
                val ngoId = ngoSnap.key ?: continue
                val currentStatus = ngoSnap.child("profileStatus").getValue(String::class.java)
                if (currentStatus == null) {
                    ngoRef.child(ngoId).child("profileStatus").setValue("Awaiting Approval")
                        .addOnSuccessListener {
                            Log.d("AdminDashboard", "Initialized profileStatus=Awaiting Approval for NGO $ngoId")
                        }
                        .addOnFailureListener { e ->
                            Log.w("AdminDashboard", "Failed to initialize profileStatus for NGO $ngoId", e)
                        }
                }
            }
        }.addOnFailureListener { e ->
            Log.w("AdminDashboard", "Error fetching NGOs for status init", e)
        }
    }

    private fun updateStatus(userId: String, newStatus: String) {
        userRef.child(userId).child("accountStatus").setValue(newStatus)
            .addOnSuccessListener {
                val index = usersList.indexOfFirst { it.id == userId }
                if (index != -1) {
                    usersList[index].accountStatus = newStatus
                    usersAdapter.notifyItemChanged(index)


                }
                val actorId = auth.currentUser?.uid ?: "unknown"

                ActivityLogger.log(
                    actorId = actorId,
                    actorType = "Admin",
                    category = "Updated Account Status",
                    message = "User ${usersList[index].name} ${usersList[index].lastName} status changed to $newStatus",
                    color = "#ABCDDE"
                )
                Toast.makeText(this, "User status updated to $newStatus", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("AdminDashboard", "Error updating user status", e)
            }
    }


    private fun updateProfileStatus(ngoId: String, newStatus: String) {
        ngoRef.child(ngoId).child("profileStatus").setValue(newStatus)
            .addOnSuccessListener {
                val index = profilesList.indexOfFirst { it.id == ngoId }
                if (index != -1) {
                    profilesList[index].status = newStatus
                    ngoProfileAdapter.notifyItemChanged(index)
                }

                val actorId = auth.currentUser?.uid ?: "unknown"
                ActivityLogger.log(
                    actorId = actorId,
                    actorType = "Admin",
                    category = "Approved NGO Profile",
                    message = "User ${usersList[index].name} ${usersList[index].lastName} approved ${profilesList[index].name}'s profile",
                    color = "#ABCDDE"
                )

                Toast.makeText(this, "NGO status updated to $newStatus", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("AdminDashboard", "Error updating NGO status", e)
            }
    }
}