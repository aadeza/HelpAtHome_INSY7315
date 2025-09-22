package com.example.helpathome

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class CallUserActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var dbRef: DatabaseReference
    private val contactList = mutableListOf<Pair<String, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call_user)


        dbRef = FirebaseDatabase.getInstance().getReference("Contacts")

        // UI Elements
        listView = findViewById(R.id.userListView)
        val editName = findViewById<EditText>(R.id.editContactName)
        val editPhone = findViewById<EditText>(R.id.editContactPhone)
        val btnSave = findViewById<Button>(R.id.btnSaveContact)

        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1)
        listView.adapter = adapter


        btnSave.setOnClickListener {
            val name = editName.text.toString().trim()
            val phone = editPhone.text.toString().trim()

            if (name.isNotEmpty() && phone.isNotEmpty()) {
                val contact = mapOf("name" to name, "phone" to phone)
                dbRef.push().setValue(contact).addOnSuccessListener {
                    Toast.makeText(this, "✅ Contact Saved", Toast.LENGTH_SHORT).show()
                    editName.text.clear()
                    editPhone.text.clear()

                    listView.setOnItemClickListener { _, _, position, _ ->
                        val intent = Intent(Intent.ACTION_DIAL)
                        intent.data = Uri.parse("tel: $phone")
                        startActivity(intent)
                    }


                }.addOnFailureListener {
                    Toast.makeText(this, "❌ Failed to save contact", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please fill in both fields", Toast.LENGTH_SHORT).show()
            }
        }


        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                adapter.clear()
                contactList.clear()

                for (contactSnap in snapshot.children) {
                    val name = contactSnap.child("name").getValue(String::class.java) ?: "Unknown"
                    val phone = contactSnap.child("phone").getValue(String::class.java) ?: ""

                    adapter.add("$name - $phone")
                    contactList.add(Pair(name, phone))
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@CallUserActivity, "Failed to load contacts", Toast.LENGTH_SHORT)
                    .show()
            }
        })


    }

}
