package com.example.helpathome

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ManageNgosActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var ngoAdapter: NgoAdapter
    private val ngoList = mutableListOf<Ngo>()

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private val TAG = "ManageNgosActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_ngos)

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.rvNgoList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)

        // Firebase instances
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("ngos")

        loadNgos()
    }

    private fun loadNgos() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to manage NGOs", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Query only NGOs created by current user
        val query = database.orderByChild("createdBy").equalTo(currentUser.uid)

        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                ngoList.clear()
                for (ngoSnapshot in snapshot.children) {
                    val ngo = ngoSnapshot.getValue(Ngo::class.java)
                    if (ngo != null) {
                        // Assign the Firebase key as the ID
                        ngo.id = ngoSnapshot.key.toString()
                        ngoList.add(ngo)
                        Log.d(TAG, "Loaded NGO: ${ngo.name}, ID: ${ngo.id}")
                    }
                }

                if (ngoList.isNotEmpty()) {
                    ngoAdapter = NgoAdapter(
                        ngos = ngoList,
                        onNgoClick = { selectedNgo ->
                            Toast.makeText(this@ManageNgosActivity, "Clicked: ${selectedNgo.name}", Toast.LENGTH_SHORT).show()
                        },
                        onSeeMoreClick = { selectedNgo ->
                            if (selectedNgo.id != null) {
                                val intent = Intent(this@ManageNgosActivity, NgoDetailsActivity::class.java)
                                intent.putExtra("ngoId", selectedNgo.id)
                                startActivity(intent)
                            } else {
                                Toast.makeText(this@ManageNgosActivity, "Failed to load NGO details", Toast.LENGTH_SHORT).show()
                                Log.w(TAG, "See More clicked but NGO ID is null for ${selectedNgo.name}")
                            }
                        }
                    )
                    recyclerView.adapter = ngoAdapter
                } else {
                    Toast.makeText(this@ManageNgosActivity, "You have not created any NGOs", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ManageNgosActivity, "Failed to load NGOs: ${error.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Firebase load cancelled: ${error.message}", error.toException())
            }
        })
    }
}
