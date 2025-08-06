package com.example.helpathome

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class NGOModel : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var txtUserName: TextView
    private lateinit var txtResults: TextView
    private lateinit var db: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ngomodel)

        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance().reference

        txtUserName = findViewById(R.id.txtUserName)
        txtResults = findViewById(R.id.txtViewResults)

        val currentUser = auth.currentUser
        txtUserName.text = currentUser?.email ?: "NGO User"

        val btnSubmitNGO = findViewById<Button>(R.id.btnCreateNGO)
        val btnPostUpdate = findViewById<Button>(R.id.btnPostUpdate)
        val btnHelpRequests = findViewById<Button>(R.id.btnHelpRequests)
        val btnCallUser = findViewById<Button>(R.id.btnCallUser)

        val editName = findViewById<EditText>(R.id.editName)
        val editFounder = findViewById<EditText>(R.id.editFounder)
        val editDateFounded = findViewById<EditText>(R.id.editDateFounded)
        val spinner = findViewById<Spinner>(R.id.spinnerCategory)
        val editPostMessage = findViewById<EditText>(R.id.editPostMessage)

        val categories = arrayOf("Food Drive", "Pet Rescue", "Legal Aid", "Shelter Support")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        spinner.adapter = adapter

        // ✅ Submit NGO
        btnSubmitNGO.setOnClickListener {
            val name = editName.text.toString()
            val founder = editFounder.text.toString()
            val dateFounded = editDateFounded.text.toString()
            val category = spinner.selectedItem.toString()

            if (name.isNotEmpty() && founder.isNotEmpty() && dateFounded.isNotEmpty()) {
                val ngo = mapOf(
                    "name" to name,
                    "founder" to founder,
                    "category" to category,
                    "dateFounded" to dateFounded
                )
                db.child("NGOs").push().setValue(ngo)
                Toast.makeText(this, "✅ NGO Saved!", Toast.LENGTH_SHORT).show()
                loadNGOData()
            } else {
                Toast.makeText(this, "❌ Fill all NGO fields", Toast.LENGTH_SHORT).show()
            }
        }

        // ✅ Post Update
        btnPostUpdate.setOnClickListener {
            val message = editPostMessage.text.toString()
            if (message.split("\\s+".toRegex()).size <= 50 && message.isNotEmpty()) {
                val update = mapOf(
                    "message" to message,
                    "timestamp" to System.currentTimeMillis()
                )
                db.child("NGOUpdates").push().setValue(update)
                Toast.makeText(this, "📢 Update posted!", Toast.LENGTH_SHORT).show()
                loadUpdates()
            } else {
                Toast.makeText(this, "❌ Max 50 words", Toast.LENGTH_SHORT).show()
            }
        }

        // ✅ View Help Requests
        btnHelpRequests.setOnClickListener {
            val helpRequests = """
                📥 Help Requests:
                • Zone A: Family needs food
                • Zone B: Legal aid needed
                • Zone C: Shelter request for 3 people
            """.trimIndent()
            txtResults.text = helpRequests
        }

        // ✅ Call user → new screen (we'll build it later)
        btnCallUser.setOnClickListener {
            val intent = Intent(this, CallUsersActivity::class.java)
            startActivity(intent)
        }

        loadNGOData()
        loadUpdates()
    }

    // Load NGO entries
    private fun loadNGOData() {
        db.child("NGOs").get().addOnSuccessListener { snapshot ->
            val builder = StringBuilder()
            builder.append("📌 NGOs Registered:\n")
            snapshot.children.forEach {
                val data = it.value as Map<*, *>
                builder.append("• ${data["name"]} (${data["category"]})\n")
            }
            txtResults.text = builder.toString()
        }
    }

    // Load update posts
    private fun loadUpdates() {
        db.child("NGOUpdates").get().addOnSuccessListener { snapshot ->
            val builder = StringBuilder()
            builder.append("\n📢 Recent Updates:\n")
            snapshot.children.forEach {
                val data = it.value as Map<*, *>
                builder.append("• ${data["message"]}\n")
            }
            txtResults.append(builder.toString())
        }
    }
}
