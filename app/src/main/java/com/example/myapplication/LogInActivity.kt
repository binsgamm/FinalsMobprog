package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputEditText

class LogInActivity : AppCompatActivity() {

    private var isNavigating = false
    private lateinit var tvSignUp: TextView // Declare the variable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize the TextView
        tvSignUp = findViewById(R.id.tvSignUp)

        // Make the "Sign Up" text clickable
        tvSignUp.setOnClickListener {
            navigateToSignUp()
        }

        // Ensure text colors are visible
        setTextColors()
    }

    private fun setTextColors() {
        // Get email and password fields
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)

        // Set text colors to black for visibility
        val textColor = ContextCompat.getColor(this, android.R.color.black)

        etEmail?.setTextColor(textColor)
        etEmail?.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)

        etPassword?.setTextColor(textColor)
        etPassword?.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
    }

    private fun navigateToSignUp() {
        // Prevent multiple clicks
        if (isNavigating) {
            Log.d("NAVIGATION", "Already navigating, skipping")
            return
        }

        isNavigating = true
        Log.d("NAVIGATION", "Navigating to SignUpActivity")

        val intent = Intent(this, SignUpActivity::class.java)
        startActivity(intent)

        // Reset navigation flag after delay
        tvSignUp.postDelayed({
            isNavigating = false
            Log.d("NAVIGATION", "Navigation flag reset")
        }, 1000)
    }

    override fun onResume() {
        super.onResume()
        // Reset navigation flag when returning to this activity
        isNavigating = false
        Log.d("NAVIGATION", "LoginActivity resumed, navigation reset")
    }
}