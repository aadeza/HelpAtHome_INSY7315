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

            // 1. Empty field check
            if (fName.isEmpty() || lName.isEmpty() || birthDate.isEmpty() ||
                emailText.isEmpty() || pass.isEmpty() || confirmPass.isEmpty()
            ) {
                Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 2. Email format check
            if (!Patterns.EMAIL_ADDRESS.matcher(emailText).matches()) {
                email.error = "Enter a valid email"
                return@setOnClickListener
            }

            // 3. Password match check
            if (pass != confirmPass) {
                confirmPassword.error = "Passwords do not match"
                return@setOnClickListener
            }

            // 4. Password strength check
            if (!isValidPassword(pass, fName, lName, birthDate, emailText)) {
                return@setOnClickListener
            }

            // 5. If all checks passed → Create account
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

                        val user = mapOf(
                            "firstName" to fName,
                            "lastName" to lName,
                            "dob" to birthDate,
                            "userType" to type,
                            "email" to emailText,
                            "sosActive" to false
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

    fun isValidPassword(
        password: String,
        firstName: String,
        lastName: String,
        dob: String,
        email: String
    ): Boolean {
        // 1) Basic rules
        if (password.length < 8) {
            Toast.makeText(this, "Password must be at least 8 characters long", Toast.LENGTH_SHORT).show()
            return false
        }

        val hasUpper = password.any { it.isUpperCase() }
        val hasLower = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecial = password.any { !it.isLetterOrDigit() }

        if (!(hasUpper && hasLower && hasDigit && hasSpecial)) {
            Toast.makeText(this, "Password must include uppercase, lowercase, digit, and a special character", Toast.LENGTH_SHORT).show()
            return false
        }

        val lowerPassword = password.lowercase()
        val lowerFirstName = firstName.lowercase()
        val lowerLastName = lastName.lowercase()
        val lowerEmail = email.lowercase()
        val emailPrefix = lowerEmail.substringBefore("@")

        // 2) Name/email presence check
        if (lowerPassword.contains(lowerFirstName) || lowerPassword.contains(lowerLastName) || lowerPassword.contains(emailPrefix)) {
            Toast.makeText(this, "Password should not contain your name or email", Toast.LENGTH_SHORT).show()
            return false
        }

        // 3) Robust DOB checks
        val dobTrim = dob.trim()
        if (dobTrim.isNotEmpty()) {
            val variants = mutableSetOf<String>()

            // digits-only (removes any non-digit)
            val digitsOnly = dobTrim.replace(Regex("\\D+"), "")
            if (digitsOnly.length >= 6) { // small guard (e.g. 20020815)
                variants.add(digitsOnly)                 // 20020815
            }

            // Split by any non-digit character to get parts
            val parts = dobTrim.split(Regex("\\D+")).filter { it.isNotEmpty() }
            if (parts.size == 3) {
                // Normalize parts and decide which part is year
                val (p0, p1, p2) = parts
                if (p0.length == 4) {
                    // format: yyyy-mm-dd (or yyyy/m/d)
                    val year = p0
                    val month = p1.padStart(2, '0')
                    val day = p2.padStart(2, '0')

                    variants.add("$year-$month-$day")    // 2002-08-15
                    variants.add("$year$month$day")      // 20020815
                    variants.add("$day$month$year")      // 15082002
                    variants.add("$day-$month-$year")    // 15-08-2002
                    variants.add("$month$day$year")      // 08152002 (useful if MMDDYYYY)
                    variants.add("$month-$day-$year")    // 08-15-2002
                    variants.add("$day/$month/$year")    // 15/08/2002
                    variants.add("$month/$day/$year")    // 08/15/2002
                } else if (p2.length == 4) {
                    // format: dd-mm-yyyy or mm-dd-yyyy
                    val day = p0.padStart(2, '0')
                    val month = p1.padStart(2, '0')
                    val year = p2

                    variants.add("$year-$month-$day")
                    variants.add("$year$month$day")
                    variants.add("$day$month$year")
                    variants.add("$day-$month-$year")
                    variants.add("$month$day$year")
                    variants.add("$month-$day-$year")
                    variants.add("$day/$month/$year")
                    variants.add("$month/$day/$year")
                } else {
                    // fallback: create a couple of safe combos
                    val a = p0.padStart(2, '0')
                    val b = p1.padStart(2, '0')
                    val c = p2.padStart(4, '0')
                    variants.add("$c-$b-$a")
                    variants.add("$c$b$a")
                }
            } else {

            }


            for (v in variants) {
                if (v.isNotEmpty() && lowerPassword.contains(v.lowercase())) {
                    Toast.makeText(this, "Password should not contain your date of birth", Toast.LENGTH_SHORT).show()
                    return false
                }
            }
        }


        return true
    }


}
