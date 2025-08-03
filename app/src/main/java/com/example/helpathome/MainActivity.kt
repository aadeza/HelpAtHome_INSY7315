package com.example.helpathome

import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val signUpText = findViewById<TextView>(R.id.ToSignUptextView)

        // Underline the text (optional)
        signUpText.paintFlags = signUpText.paintFlags or Paint.UNDERLINE_TEXT_FLAG

        // Go to the SignUp screen when clicked
        signUpText.setOnClickListener {
            val intent = Intent(this, SignUp::class.java)
            startActivity(intent)
        }

        val loginbutton = findViewById<Button>(R.id.Loginbutton)

        loginbutton.setOnClickListener(){
            val intent = Intent(this, Home::class.java)
            startActivity(intent)
            finish()
        }
    }
}

