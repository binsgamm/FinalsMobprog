package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.util.TypedValue
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LogInActivity : AppCompatActivity() {

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var tvSignUp: MaterialTextView
    private var isNavigating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initializeViews()
        setupClickListeners()
        setTextColors()

        // Check if user is already logged in
        checkExistingSession()
    }

    private fun initializeViews() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvSignUp = findViewById(R.id.tvSignUp)
    }

    private fun setupClickListeners() {
        btnLogin.setOnClickListener {
            hideKeyboard()
            if (validateInputs()) {
                performLogin()
            }
        }

        tvSignUp.setOnClickListener {
            navigateToSignUp()
        }
    }

    private fun validateInputs(): Boolean {
        val email = etEmail.text.toString().trim().lowercase()
        val password = etPassword.text.toString()
        var isValid = true

        etEmail.error = null
        etPassword.error = null

        if (email.isEmpty()) {
            etEmail.error = "Email is required"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Please enter a valid email"
            isValid = false
        }

        if (password.isEmpty()) {
            etPassword.error = "Password is required"
            isValid = false
        } else if (password.length < 8) {
            etPassword.error = "Password must be at least 8 characters"
            isValid = false
        }

        return isValid
    }

    private fun performLogin() {
        showLoading(true)
        val email = etEmail.text.toString().trim().lowercase()
        val password = etPassword.text.toString()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Authenticate with Supabase
                SupabaseManager.client.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }

                val currentUser = SupabaseManager.client.auth.currentUserOrNull()

                if (currentUser != null) {
                    // 2. Check user role and navigate
                    checkUserRoleAndNavigate(currentUser.id, currentUser.email ?: "")
                } else {
                    withContext(Dispatchers.Main) {
                        showLoading(false)
                        Toast.makeText(this@LogInActivity, "Session not found", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                Log.e("LogInActivity", "Login error: ${e.message}")
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(this@LogInActivity, "Login failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * CORE LOGIC: Differentiates between Admin and Customer
     */
    private suspend fun checkUserRoleAndNavigate(userId: String, email: String) {
        try {
            // Query the admins table to see if this user_id exists there
            val adminCheck = SupabaseManager.client.from("admins").select {
                filter { eq("user_id", userId) }
            }

            // If the response data is not "[]", the user is an admin
            val isAdmin = adminCheck.data != "[]" && adminCheck.data.length > 5

            withContext(Dispatchers.Main) {
                showLoading(false)
                if (isAdmin) {
                    Log.d("LogInActivity", "Admin detected. Navigating to Admin Panel.")
                    navigateToAdmin(userId)
                } else {
                    Log.d("LogInActivity", "Customer detected. Navigating to Dashboard.")
                    navigateToCustomer(userId, email)
                }
            }
        } catch (e: Exception) {
            Log.e("LogInActivity", "Role check failed: ${e.message}")
            withContext(Dispatchers.Main) {
                showLoading(false)
                // Fallback to customer dashboard if check fails
                navigateToCustomer(userId, email)
            }
        }
    }

    private fun checkExistingSession() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentUser = SupabaseManager.client.auth.currentUserOrNull()
                if (currentUser != null) {
                    checkUserRoleAndNavigate(currentUser.id, currentUser.email ?: "")
                }
            } catch (e: Exception) {
                Log.e("LogInActivity", "Session check error")
            }
        }
    }

    private fun navigateToCustomer(userId: String, email: String) {
        if (isNavigating) return
        isNavigating = true

        Toast.makeText(this, "Welcome back, $email", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, DashboardActivity::class.java).apply {
            putExtra("USER_ID", userId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun navigateToAdmin(userId: String) {
        if (isNavigating) return
        isNavigating = true

        Toast.makeText(this, "Admin Access Granted", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, AdminAppointmentsActivity::class.java).apply {
            putExtra("USER_ID", userId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun navigateToSignUp() {
        if (isNavigating) return
        isNavigating = true
        startActivity(Intent(this, SignUpActivity::class.java))
    }

    private fun showLoading(isLoading: Boolean) {
        btnLogin.isEnabled = !isLoading
        btnLogin.text = if (isLoading) "Checking credentials..." else "Login"
    }

    private fun hideKeyboard() {
        val view = currentFocus
        view?.let {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun setTextColors() {
        val textColor = ContextCompat.getColor(this, android.R.color.black)
        etEmail.setTextColor(textColor)
        etPassword.setTextColor(textColor)
    }

    override fun onResume() {
        super.onResume()
        isNavigating = false
    }
}