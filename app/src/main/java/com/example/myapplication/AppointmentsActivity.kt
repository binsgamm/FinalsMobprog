package com.example.myapplication

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AppointmentsActivity : AppCompatActivity() {

    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_appointments)

        // Get user ID from intent
        userId = intent.getStringExtra("USER_ID")

        // TODO: Implement appointments list logic
        Toast.makeText(this, "Appointments screen - Coming soon!", Toast.LENGTH_SHORT).show()
    }
}