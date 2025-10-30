package com.example.helpathome

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.getValue
import com.google.firebase.storage.FirebaseStorage
import java.util.*

// Make sure imports are present

class CreateNgoActivity : AppCompatActivity() {

    private lateinit var edtName: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var txtDateFounded: TextView
    private lateinit var edtFounder: EditText
    private lateinit var edtMission: EditText
    private lateinit var edtVision: EditText
    private lateinit var btnSelectLogo: Button
    private lateinit var btnSubmit: Button
    private lateinit var imgLogoPreview: ImageView
    private lateinit var progressBar: ProgressBar

    // Contacts UI
    private lateinit var rvContacts: RecyclerView
    private lateinit var btnAddContact: Button
    private lateinit var contactAdapter: ContactAdapter
    private val contactList = mutableListOf<Contact>()

    private var logoUri: Uri? = null

    companion object {
        const val PICK_LOGO_REQUEST = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_ngo)

        // Bind UI elements
        edtName = findViewById(R.id.edtNgoName)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        txtDateFounded = findViewById(R.id.txtDateFounded)
        edtFounder = findViewById(R.id.edtFounder)
        edtMission = findViewById(R.id.edtMission)
        edtVision = findViewById(R.id.edtVision)
        btnSelectLogo = findViewById(R.id.btnSelectLogo)
        btnSubmit = findViewById(R.id.btnSubmitNgo)
        imgLogoPreview = findViewById(R.id.imgLogoPreview)
        progressBar = findViewById(R.id.progressBar)

        rvContacts = findViewById(R.id.rvContacts)
        btnAddContact = findViewById(R.id.btnAddContact)

        // Recycler view + adapter
        contactAdapter = ContactAdapter(contactList) { pos ->
            // remove callback
            contactAdapter.removeAt(pos)
        }
        rvContacts.layoutManager = LinearLayoutManager(this)
        rvContacts.adapter = contactAdapter
        rvContacts.isNestedScrollingEnabled = false

        // Setup Spinner
        val categories = resources.getStringArray(R.array.ngo_categories)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter

        // Setup Date Picker
        txtDateFounded.setOnClickListener { showDatePickerDialog() }

        // Select logo from device
        btnSelectLogo.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
            startActivityForResult(intent, PICK_LOGO_REQUEST)
        }

        // Load owner's phone and prepopulate first contact
        prefillOwnerContact()

        // Add contact action
        btnAddContact.setOnClickListener { showAddContactDialog() }

        // Submit NGO
        btnSubmit.setOnClickListener {
            val name = edtName.text.toString().trim()
            val category = spinnerCategory.selectedItem.toString()
            val dateFounded = txtDateFounded.text.toString().trim()
            val founder = edtFounder.text.toString().trim()
            val mission = edtMission.text.toString().trim()
            val vision = edtVision.text.toString().trim()

            if (name.isEmpty() || founder.isEmpty()) {
                Toast.makeText(this, "❌ Fill all required fields (name & founder)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (logoUri == null) {
                Toast.makeText(this, "❌ Please select a logo", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (contactList.isEmpty()) {
                Toast.makeText(this, "❌ Add at least one contact method", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            uploadLogoAndSaveNgo(name, category, dateFounded, founder, mission, vision, contactList)
        }
    }

    private fun prefillOwnerContact() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val uid = user.uid
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(uid)
        userRef.get().addOnSuccessListener { snap ->
            val userObj = snap.getValue(Users::class.java)
            val phone = userObj?.phoneNumber ?: ""
            if (phone.isNotBlank()) {
                // Format: number in bold, subtype (mobile) and label Owner
                val ownerContact = Contact(
                    type = "phone",
                    subtype = "mobile",
                    value = phone,
                    label = "Owner"
                )
                contactList.add(ownerContact)
                contactAdapter.notifyItemInserted(contactList.size - 1)
            }
        }.addOnFailureListener {
            // ignore quietly - user may add manually
        }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                txtDateFounded.text = selectedDate
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }

    // add contact dialog flow
    private fun showAddContactDialog() {
        val types = arrayOf("Phone", "Email")
        AlertDialog.Builder(this)
            .setTitle("Contact type")
            .setItems(types) { dialog, which ->
                dialog.dismiss()
                if (which == 0) { // Phone
                    showAddPhoneDialog()
                } else {
                    showAddEmailDialog()
                }
            }
            .show()
    }

    private fun showAddPhoneDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_phone, null)
        val edtPhone = view.findViewById<EditText>(R.id.edtPhoneNumber)
        val spinnerSubtype = view.findViewById<Spinner>(R.id.spinnerSubtype)
        val edtLabel = view.findViewById<EditText>(R.id.edtContactLabel)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, arrayOf("mobile", "telephone"))
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSubtype.adapter = adapter

        AlertDialog.Builder(this)
            .setTitle("Add phone contact")
            .setView(view)
            .setPositiveButton("Add") { d, _ ->
                val num = edtPhone.text.toString().trim()
                val subtype = spinnerSubtype.selectedItem.toString()
                val label = edtLabel.text.toString().trim().take(50)

                if (!isValidPhone(num)) {
                    Toast.makeText(this, "Enter a valid phone number", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (label.length > 50) {
                    Toast.makeText(this, "Label max 50 characters", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val contact = Contact(type = "phone", subtype = subtype, value = num, label = label)
                contactAdapter.add(contact)
                d.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddEmailDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_email, null)
        val edtEmail = view.findViewById<EditText>(R.id.edtEmail)
        val edtLabel = view.findViewById<EditText>(R.id.edtContactLabel)

        AlertDialog.Builder(this)
            .setTitle("Add email contact")
            .setView(view)
            .setPositiveButton("Add") { d, _ ->
                val email = edtEmail.text.toString().trim()
                val label = edtLabel.text.toString().trim().take(50)

                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(this, "Enter a valid email", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (label.length > 50) {
                    Toast.makeText(this, "Label max 50 characters", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val contact = Contact(type = "email", subtype = "", value = email, label = label)
                contactAdapter.add(contact)
                d.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun isValidPhone(input: String): Boolean {
        // Basic validation: digits, spaces, + and dashes allowed; at least 7 digits
        val cleaned = input.replace("[^0-9]".toRegex(), "")
        return cleaned.length >= 7
    }

    // Upload logo then save NGO (contacts saved as list of maps)
    private fun uploadLogoAndSaveNgo(
        name: String,
        category: String,
        dateFounded: String,
        founder: String,
        mission: String,
        vision: String,
        contacts: List<Contact>
    ) {
        val storageRef = FirebaseStorage.getInstance().reference
            .child("ngo_logos/${UUID.randomUUID()}.jpg")

        logoUri?.let { uri ->
            storageRef.putFile(uri)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { logoUrl ->
                        saveNgoToDatabase(name, category, dateFounded, founder, mission, vision, contacts, logoUrl.toString())
                    }
                }
                .addOnFailureListener { e ->
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, "❌ Logo upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun saveNgoToDatabase(
        name: String,
        category: String,
        dateFounded: String,
        founder: String,
        mission: String,
        vision: String,
        contacts: List<Contact>,
        logoUrl: String
    ) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "unknown_user"
        val dbRef = FirebaseDatabase.getInstance().reference.child("ngos")
        val ngoId = dbRef.push().key ?: UUID.randomUUID().toString()

        // Convert contacts to maps so Firebase stores them cleanly
        val contactsForDb = contacts.map {
            mapOf(
                "type" to it.type,
                "subtype" to it.subtype,
                "value" to it.value,
                "label" to it.label
            )
        }

        val ngoData = mapOf(
            "id" to ngoId,
            "name" to name,
            "category" to category,
            "dateFounded" to dateFounded,
            "founder" to founder,
            "mission" to mission,
            "vision" to vision,
            "contacts" to contactsForDb,
            "logoUrl" to logoUrl,
            "createdBy" to userId,
            "approvedByAdmin" to false // default not approved
        )

        dbRef.child(ngoId)
            .setValue(ngoData)
            .addOnSuccessListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "✅ NGO '$name' created successfully!", Toast.LENGTH_SHORT).show()

                // ✅ ADDED: Automatically link this user to the NGO they just created
                val linkRef = FirebaseDatabase.getInstance().reference
                    .child("NgoAccounts")
                    .child(userId)
                    .child("linkedNgoId")
                linkRef.setValue(ngoId)

                // Civilian notification
                val notifRef = FirebaseDatabase.getInstance().reference.child("notifications")
                val notifId = notifRef.push().key ?: UUID.randomUUID().toString()
                val notifData = mapOf(
                    "id" to notifId,
                    "title" to "New NGO Created",
                    "message" to "NGO '$name' was created and pending approval.",
                    "type" to "new_ngo",
                    "relatedId" to ngoId,
                    "timestamp" to System.currentTimeMillis(),
                    "read" to false,
                    "imageUrl" to logoUrl
                )
                notifRef.child(notifId).setValue(notifData)

                // NGO-specific notification
                val ngoNotifRef = FirebaseDatabase.getInstance().reference
                    .child("ngo_notifications")
                    .child(ngoId)
                    .push()

                val ngoNotifData = mapOf(
                    "title" to "NGO Created",
                    "message" to "Your NGO '$name' is pending approval.",
                    "type" to "ngo_status",
                    "timestamp" to System.currentTimeMillis(),
                    "read" to false,
                    "imageUrl" to logoUrl,
                    "userId" to userId
                )
                ngoNotifRef.setValue(ngoNotifData)

                finish()
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Toast.makeText(this, "❌ Failed to save NGO: ${e.message}", Toast.LENGTH_LONG).show()
            }


        //new notification for ngo
        val ngoNotifRef = FirebaseDatabase.getInstance().reference
            .child("ngo_notifications")
            .child(ngoId)
            .push()

        val ngoNotifData = mapOf(
            "title" to "NGO Created",
            "message" to "Your NGO '$name' is pending approval.",
            "type" to "ngo_status",
            "timestamp" to System.currentTimeMillis(),
            "read" to false,
            "imageUrl" to logoUrl,
            "userId" to userId
        )
        ngoNotifRef.setValue(ngoNotifData)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_LOGO_REQUEST && resultCode == Activity.RESULT_OK) {
            logoUri = data?.data
            Glide.with(this).load(logoUri).into(imgLogoPreview)
        }
    }
}
