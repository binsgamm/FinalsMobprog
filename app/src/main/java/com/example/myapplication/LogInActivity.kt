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
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
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

    // Supabase client - same as in SignUpActivity
    private val supabaseClient: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = "https://mxxyzcoevcsniinvleos.supabase.co",
            supabaseKey = "sb_publishable_pdEutnY70rVI_FVG6Casaw_03co6UQR"
        ) {
            install(io.github.jan.supabase.postgrest.Postgrest)
            install(io.github.jan.supabase.auth.Auth)

            // ADD THIS for session persistence:
            install(io.github.jan.supabase.auth.Auth) {
                alwaysAutoRefresh = true
                autoLoadFromStorage = true
            }
        }
    }

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

        // Clear previous errors
        etEmail.error = null
        etPassword.error = null

        // Email validation
        if (email.isEmpty()) {
            etEmail.error = "Email is required"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Please enter a valid email"
            isValid = false
        }

        // Password validation
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
                Log.d("LogInActivity", "Attempting login for: $email")

                // Sign in with Supabase Auth
                val session = supabaseClient.auth.signInWith(
                    io.github.jan.supabase.auth.providers.builtin.Email
                ) {
                    this.email = email
                    this.password = password
                }

                // Get current user IMMEDIATELY after login
                val currentUser = supabaseClient.auth.currentUserOrNull()

                Log.d("LogInActivity", "Login successful. User ID: ${currentUser?.id}")
                Log.d("LogInActivity", "User Email: ${currentUser?.email}")
                Log.d("LogInActivity", "Session: $session")

                // Wait a moment and check session again
                delay(500)
                val refreshedUser = supabaseClient.auth.currentUserOrNull()
                Log.d("LogInActivity", "Refreshed User after delay: ${refreshedUser?.id}")

                withContext(Dispatchers.Main) {
                    showLoading(false)

                    if (currentUser != null) {
                        Toast.makeText(
                            this@LogInActivity,
                            "Login successful! Welcome ${currentUser.email}",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Navigate to DashboardActivity WITH user ID
                        navigateToDashboard(currentUser.id)
                    } else {
                        Toast.makeText(
                            this@LogInActivity,
                            "Login succeeded but no user session found",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

            } catch (e: Exception) {
                Log.e("LogInActivity", "Login error: ${e.message}", e)

                withContext(Dispatchers.Main) {
                    showLoading(false)

                    val errorMessage = when {
                        e.message?.contains("Invalid login credentials") == true ->
                            "Invalid email or password"
                        e.message?.contains("Email not confirmed") == true ->
                            "Please verify your email first"
                        e.message?.contains("rate limit") == true ->
                            "Too many attempts. Please try again later"
                        else -> "Login failed: ${e.message ?: "Unknown error"}"
                    }

                    Toast.makeText(
                        this@LogInActivity,
                        errorMessage,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun checkExistingSession() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get current session
                val currentSession = supabaseClient.auth.currentSessionOrNull()
                val currentUser = supabaseClient.auth.currentUserOrNull()

                if (currentSession != null && currentUser != null) {
                    Log.d("LogInActivity", "User already logged in: ${currentUser.email}")

                    withContext(Dispatchers.Main) {
                        // Navigate directly to DashboardActivity WITH user ID
                        navigateToDashboard(currentUser.id)
                    }
                }
            } catch (e: Exception) {
                Log.e("LogInActivity", "Session check error: ${e.message}")
            }
        }
    }

    private fun navigateToDashboard(userId: String? = null) {
        if (isNavigating) return

        isNavigating = true

        val intent = Intent(this, DashboardActivity::class.java)

        // Pass user ID if available
        if (userId != null) {
            intent.putExtra("USER_ID", userId)
            Log.d("LogInActivity", "Passing user ID to DashboardActivity: $userId")
        }

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun navigateToSignUp() {
        if (isNavigating) return

        isNavigating = true
        val intent = Intent(this, SignUpActivity::class.java)
        startActivity(intent)

        // Reset flag after delay
        tvSignUp.postDelayed({ isNavigating = false }, 1000)
    }

    private fun showLoading(isLoading: Boolean) {
        btnLogin.isEnabled = !isLoading
        btnLogin.text = if (isLoading) "Logging in..." else "Login"
    }

    private fun hideKeyboard() {
        val view = currentFocus
        view?.let {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun setTextColors() {
        // Set text colors for better visibility
        val textColor = ContextCompat.getColor(this, android.R.color.black)

        etEmail.setTextColor(textColor)
        etEmail.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)

        etPassword.setTextColor(textColor)
        etPassword.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
    }

    override fun onResume() {
        super.onResume()
        // Reset navigation flag
        isNavigating = false
    }
}