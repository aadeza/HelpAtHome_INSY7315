package com.example.helpathome

import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.util.Patterns
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

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

            // Validate input
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailField.error = "Enter a valid email"
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                passwordField.error = "Enter your password"
                return@setOnClickListener
            }

            // Attempt login
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, Home::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }
}

