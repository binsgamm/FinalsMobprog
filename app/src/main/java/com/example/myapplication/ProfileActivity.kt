package com.example.myapplication

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ProfileActivity : AppCompatActivity() {

    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Get user ID from intent
        userId = intent.getStringExtra("USER_ID")

        // TODO: Implement profile logic
        Toast.makeText(this, "Profile screen - Coming soon!", Toast.LENGTH_SHORT).show()
    }
}