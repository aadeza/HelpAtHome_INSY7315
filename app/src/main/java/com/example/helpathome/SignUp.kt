package com.example.helpathome

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SignUp : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        auth = FirebaseAuth.getInstance()

        val firstName = findViewById<EditText>(R.id.editFirstName)
        val lastName = findViewById<EditText>(R.id.editLastName)
        val dob = findViewById<EditText>(R.id.editDOB)
        val userType = findViewById<Spinner>(R.id.spinnerUserType)
        val email = findViewById<EditText>(R.id.editEmail)
        val password = findViewById<EditText>(R.id.editPassword)
        val confirmPassword = findViewById<EditText>(R.id.editConfirmPassword)
        val signUpButton = findViewById<Button>(R.id.buttonSignUp)

        signUpButton.setOnClickListener {
            val fName = firstName.text.toString().trim()
            val lName = lastName.text.toString().trim()
            val birthDate = dob.text.toString().trim()
            val type = userType.selectedItem.toString()
            val emailText = email.text.toString().trim()
            val pass = password.text.toString()
            val confirmPass = confirmPassword.text.toString()

            // Validate input
            if (fName.isEmpty() || lName.isEmpty() || birthDate.isEmpty() || emailText.isEmpty() || pass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(emailText).matches()) {
                email.error = "Enter a valid email"
                return@setOnClickListener
            }

            if (pass.length < 6) {
                password.error = "Password must be at least 6 characters"
                return@setOnClickListener
            }

            if (pass != confirmPass) {
                confirmPassword.error = "Passwords do not match"
                return@setOnClickListener
            }

            // Create account
            auth.createUserWithEmailAndPassword(emailText, pass)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid ?: "unknown"


                        ActivityLogger.log(
                            actorId = userId,
                            actorType = type,
                            category = "Sign Up",
                            message = "User $fName $lName signed up successfully",
                            color = "#4CAF50" // green for success
                        )

                        // Now save user profile in DB
                        val user = mapOf(
                            "firstName" to fName,
                            "lastName" to lName,
                            "dob" to birthDate,
                            "userType" to type,
                            "email" to emailText,
                            "sosActive" to false // Set SOS to off on sign-up
                        )

                        FirebaseDatabase.getInstance().getReference("Users")
                            .child(userId)
                            .setValue(user)
                            .addOnCompleteListener { dbTask ->
                                if (dbTask.isSuccessful) {
                                    Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this, MainActivity::class.java))
                                    finish()
                                } else {

                                    ActivityLogger.log(
                                        actorId = userId,
                                        actorType = type,
                                        category = "Error",
                                        message = "Failed to save user profile for $fName $lName: ${dbTask.exception?.message}",
                                        color = "#FF0000" // red for error
                                    )

                                    Toast.makeText(this, "Account created but profile not saved: ${dbTask.exception?.message}", Toast.LENGTH_LONG).show()
                                }
                            }

                    } else {
                        Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

    }
}