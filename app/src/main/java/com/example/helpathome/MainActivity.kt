package com.example.helpathome

import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.util.Patterns
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("Users")

        val emailField = findViewById<EditText>(R.id.editLoginEmailAddress)
        val passwordField = findViewById<EditText>(R.id.editLoginPassword)
        val loginButton = findViewById<Button>(R.id.Loginbutton)
        val signUpText = findViewById<TextView>(R.id.ToSignUptextView)

        // Underline "Sign Up" text
        signUpText.paintFlags = signUpText.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        signUpText.setOnClickListener {
            startActivity(Intent(this, SignUp::class.java))
        }

        loginButton.setOnClickListener {
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString()

            // Input validation
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailField.error = "Enter a valid email"
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                passwordField.error = "Enter your password"
                return@setOnClickListener
            }

            // Sign in with Firebase Auth
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid
                        if (userId != null) {
                            // Read the user data from the database
                            database.child(userId)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        if (snapshot.exists()) {
                                            // ✅ Check accountStatus before proceeding
                                            val accountStatus = snapshot.child("accountStatus")
                                                .getValue(String::class.java) ?: "active"

                                            when (accountStatus.lowercase()) {
                                                "removed" -> {
                                                    auth.signOut()
                                                    Toast.makeText(
                                                        this@MainActivity,
                                                        "Your account has been removed. Contact support.",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                    return
                                                }

                                                "suspended" -> {
                                                    auth.signOut()
                                                    Toast.makeText(
                                                        this@MainActivity,
                                                        "Your account is suspended. Please contact support.",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                    return
                                                }
                                                // If active, continue below
                                            }

                                            val userType =
                                                snapshot.child("userType")
                                                    .getValue(String::class.java) ?: ""
                                            val fName =
                                                snapshot.child("firstName")
                                                    .getValue(String::class.java) ?: ""
                                            val lName =
                                                snapshot.child("lastName")
                                                    .getValue(String::class.java) ?: ""

                                            Toast.makeText(
                                                this@MainActivity,
                                                "Welcome $fName $lName",
                                                Toast.LENGTH_SHORT
                                            ).show()

                                            ActivityLogger.log(
                                                actorId = userId,
                                                actorType = userType,
                                                category = "Login",
                                                message = "User $fName $lName logged in successfully",
                                                color = "#4CAF50"
                                            )

                                            // Redirect based on userType
                                            when (userType) {
                                                "Admin" -> startActivity(
                                                    Intent(
                                                        this@MainActivity,
                                                        AdminDashboard::class.java
                                                    )
                                                )

                                                "NGO" -> startActivity(
                                                    Intent(
                                                        this@MainActivity,
                                                        NGOModel::class.java
                                                    )
                                                )

                                                "Civilian" -> startActivity(
                                                    Intent(
                                                        this@MainActivity,
                                                        Home::class.java
                                                    )
                                                )

                                                "Law enforcement" -> startActivity(
                                                    Intent(
                                                        this@MainActivity,
                                                        LawDashboardActivity::class.java
                                                    )
                                                )

                                                else -> Toast.makeText(
                                                    this@MainActivity,
                                                    "User data not found in database!",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                            finish()
                                        } else {
                                            Toast.makeText(
                                                this@MainActivity,
                                                "User data not found in database!",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Database error: ${error.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                })
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "User ID not found!",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Login failed: ${task.exception?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }
}
