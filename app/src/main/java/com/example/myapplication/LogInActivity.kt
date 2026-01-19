package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LogInActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        try {
            val tvSignUp = findViewById<TextView>(R.id.tvSignUp)
            tvSignUp.setOnClickListener {
                Toast.makeText(this, "Opening Sign Up...", Toast.LENGTH_SHORT).show()
                navigateToSignUp()
            }
        } catch (e: Exception) {
            Log.e("LoginActivity", "Error: ${e.message}", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun navigateToSignUp() {
        try {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
            // Optional: Add a smooth transition
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        } catch (e: Exception) {
            Log.e("LoginActivity", "Cannot start SignUpActivity: ${e.message}", e)
            Toast.makeText(this, "Cannot open Sign Up. Please check the activity exists.", Toast.LENGTH_LONG).show()
        }
    }
}