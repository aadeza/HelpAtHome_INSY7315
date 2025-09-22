package com.example.helpathome

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class CallUsersActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL

        val userList = listOf(
            Pair("John Doe", "0821234567"),
            Pair("Jane Smith", "0837654321")
        )

        for ((name, number) in userList) {
            val text = TextView(this)
            text.text = getString(R.string.name_number, name, number)
            text.textSize = 18f
            text.setOnClickListener {
                val intent = Intent(Intent.ACTION_DIAL)
                intent.data = Uri.parse("tel:$number")
                startActivity(intent)
            }
            layout.addView(text)
        }

        setContentView(layout)
    }
}