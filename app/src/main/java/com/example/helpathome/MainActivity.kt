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


        signUpText.paintFlags = signUpText.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        signUpText.setOnClickListener {
            startActivity(Intent(this, SignUp::class.java))
        }

        loginButton.setOnClickListener {
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString()


            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailField.error = "Enter a valid email"
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                passwordField.error = "Enter your password"
                return@setOnClickListener
            }


            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid

                        if (userId != null) {
                            database.child(userId)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        if (snapshot.exists()) {


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
                                            }

                                            val userType = snapshot.child("userType")
                                                .getValue(String::class.java) ?: ""
                                            val fName = snapshot.child("firstName")
                                                .getValue(String::class.java) ?: ""
                                            val lName = snapshot.child("lastName")
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
                                                    "User type not recognized!",
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


                        val attemptedEmail = email
                        ActivityLogger.log(
                            actorId = attemptedEmail,
                            actorType = "Unknown",
                            category = "Login Failed",
                            message = "Login attempt failed for email: $attemptedEmail. Reason: ${task.exception?.message}",
                            color = "#F44336"
                        )

                        val userId = auth.currentUser?.uid
                        if (userId != null) {
                            val userRef = database.child(userId).child("failedLoginAttempts")
                            userRef.runTransaction(object : Transaction.Handler {
                                override fun doTransaction(currentData: MutableData): Transaction.Result {
                                    var count = currentData.getValue(Int::class.java) ?: 0
                                    count++
                                    currentData.value = count
                                    return Transaction.success(currentData)
                                }

                                override fun onComplete(
                                    error: DatabaseError?,
                                    committed: Boolean,
                                    currentData: DataSnapshot?
                                ) {
                                    if (committed) {
                                        val attempts = currentData?.getValue(Int::class.java) ?: 0
                                        if (attempts >= 5) {
                                            database.child(userId).child("accountStatus").setValue("suspended")

                                            Toast.makeText(
                                                this@MainActivity,
                                                "Too many failed attempts. Account suspended.",
                                                Toast.LENGTH_LONG
                                            ).show()

                                            ActivityLogger.log(
                                                actorId = attemptedEmail,
                                                actorType = "Unknown",
                                                category = "Multiple Failed Attempts",
                                                message = "User $attemptedEmail has attempted to log in $attempts times ",
                                                color = "#F44336"
                                            )
                                        }
                                    }
                                }
                            })
                            }
                        }
                }

        }

    }
}

