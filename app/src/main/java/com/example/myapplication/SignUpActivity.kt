package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.example.myapplication.databinding.ActivitySignupBinding
import com.google.android.material.textfield.TextInputEditText
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding

    // Views
    private lateinit var etFirstName: TextInputEditText
    private lateinit var etMiddleName: TextInputEditText
    private lateinit var etLastName: TextInputEditText
    private lateinit var etAddress: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var btnSignUp: AppCompatButton

    // Constants for validation
    companion object {
        private const val MIN_PASSWORD_LENGTH = 8
        private const val MAX_PASSWORD_LENGTH = 32
        private const val MIN_NAME_LENGTH = 2
        private const val MAX_NAME_LENGTH = 50
        private val PASSWORD_REGEX = Regex("""^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$""")
        private val PHONE_REGEX = Regex("""^9\d{9}$""")
        private val NAME_REGEX = Regex("""^[a-zA-ZÀ-ÿ\s.'-]+$""")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // --- FIX FOR ADMIN REDIRECT BUG ---
        // Clear any existing login session (like an old Admin session)
        // the moment the user enters the sign-up screen.
        CoroutineScope(Dispatchers.IO).launch {
            try {
                SupabaseManager.client.auth.signOut()
            } catch (e: Exception) { Log.e("SignUp", "Signout error") }
        }

        initializeViews()
        setupClickListeners()
        setupTextWatchers()
        setupBackPressedHandler()
    }

    private fun setupBackPressedHandler() {
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateToLogin()
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    private fun initializeViews() {
        etFirstName = binding.etFirstName
        etMiddleName = binding.etMiddleName
        etLastName = binding.etLastName
        etAddress = binding.etAddress
        etPhone = binding.etPhone
        etEmail = binding.etEmail
        etPassword = binding.etPassword
        etConfirmPassword = binding.etConfirmPassword
        btnSignUp = binding.btnSignUp
    }

    private fun setupClickListeners() {
        btnSignUp.setOnClickListener {
            hideKeyboard()
            if (validateAllFields()) {
                performSignUp()
            }
        }

        binding.tvLogin.setOnClickListener {
            navigateToLogin()
        }

        binding.cardView.setOnClickListener {
            hideKeyboard()
        }
    }

    private fun setupTextWatchers() {
        etPassword.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { validatePasswordInRealTime(s?.toString() ?: "") }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        etEmail.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { validateEmailInRealTime(s?.toString() ?: "") }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun validateAllFields(): Boolean {
        var isValid = true
        val firstName = etFirstName.text.toString().trim()
        val middleName = etMiddleName.text.toString().trim()
        val lastName = etLastName.text.toString().trim()
        val address = etAddress.text.toString().trim()
        val phone = etPhone.text.toString().trim().replace(Regex("""\D"""), "")
        val email = etEmail.text.toString().trim().lowercase()
        val password = etPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()

        clearAllErrors()

        if (!validateName(firstName)) { showError(etFirstName, "First name must be 2-50 letters"); isValid = false }
        if (!validateName(lastName)) { showError(etLastName, "Last name must be 2-50 letters"); isValid = false }
        if (middleName.isNotEmpty() && !validateOptionalName(middleName)) { showError(etMiddleName, "Invalid middle name"); isValid = false }
        if (address.isEmpty() || address.length < 10) { showError(etAddress, "Complete address required"); isValid = false }
        if (!validatePhone(phone)) { showError(etPhone, "Enter a valid 10-digit number starting with 9"); isValid = false }
        if (!validateEmail(email)) { showError(etEmail, "Enter a valid email address"); isValid = false }
        if (!validatePassword(password)) { showError(etPassword, getPasswordError(password)); isValid = false }
        if (!validateConfirmPassword(password, confirmPassword)) { showError(etConfirmPassword, "Passwords do not match"); isValid = false }

        return isValid
    }

    private fun showError(editText: TextInputEditText, message: String) { editText.error = message }

    private fun clearAllErrors() {
        listOf(etFirstName, etMiddleName, etLastName, etAddress, etPhone, etEmail, etPassword, etConfirmPassword).forEach { it.error = null }
    }

    private fun validateName(name: String): Boolean = name.isNotEmpty() && name.length in MIN_NAME_LENGTH..MAX_NAME_LENGTH && NAME_REGEX.matches(name)
    private fun validateOptionalName(name: String): Boolean = name.length <= MAX_NAME_LENGTH && NAME_REGEX.matches(name)
    private fun validatePhone(phone: String): Boolean = phone.isNotEmpty() && PHONE_REGEX.matches(phone)
    private fun validateEmail(email: String): Boolean = email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    private fun validatePassword(password: String): Boolean = password.isNotEmpty() && password.length in MIN_PASSWORD_LENGTH..MAX_PASSWORD_LENGTH && PASSWORD_REGEX.matches(password)
    private fun validateConfirmPassword(password: String, confirmPassword: String): Boolean = confirmPassword.isNotEmpty() && password == confirmPassword

    private fun validatePasswordInRealTime(password: String) {
        when {
            password.isEmpty() -> etPassword.error = null
            password.length < MIN_PASSWORD_LENGTH -> etPassword.error = "At least $MIN_PASSWORD_LENGTH characters"
            !PASSWORD_REGEX.matches(password) -> etPassword.error = "Include uppercase, lowercase, number & special char"
            else -> etPassword.error = null
        }
    }

    private fun validateEmailInRealTime(email: String) {
        if (email.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) etEmail.error = "Invalid email format" else etEmail.error = null
    }

    private fun getPasswordError(password: String): String {
        return when {
            password.isEmpty() -> "Password is required"
            password.length < MIN_PASSWORD_LENGTH -> "Minimum $MIN_PASSWORD_LENGTH characters"
            !PASSWORD_REGEX.matches(password) -> "Must include uppercase, lowercase, number & special char (@$!%*?&)"
            else -> ""
        }
    }

    private fun hideKeyboard() {
        val view = currentFocus
        view?.let {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LogInActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun performSignUp() {
        showLoading(true)

        val userData = UserData(
            firstName = etFirstName.text.toString().trim(),
            middleName = etMiddleName.text.toString().trim(),
            lastName = etLastName.text.toString().trim(),
            address = etAddress.text.toString().trim(),
            phone = etPhone.text.toString().trim().replace(Regex("""\D"""), ""),
            email = etEmail.text.toString().trim().lowercase(),
            password = etPassword.text.toString()
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("SignUpActivity", "=== STARTING SIGNUP PROCESS ===")

                // 1. Create Auth Account using Singleton
                val authResult = SupabaseManager.client.auth.signUpWith(Email) {
                    this.email = userData.email
                    this.password = userData.password
                }

                val userId = authResult?.id ?: SupabaseManager.client.auth.currentUserOrNull()?.id
                if (userId == null) throw Exception("Failed to get user ID after signup")

                // 2. Create customer data object
                val customerData = CustomerInsert(
                    user_id = userId,
                    f_name = userData.firstName,
                    m_name = userData.middleName.ifEmpty { null },
                    l_name = userData.lastName,
                    email = userData.email,
                    phone_num = "+63${userData.phone}",
                    address = userData.address
                )

                // 3. Insert into customers table using Singleton
                SupabaseManager.client.from("customers").insert(customerData)

                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(this@SignUpActivity, "Account created successfully!", Toast.LENGTH_LONG).show()
                    navigateToLogin()
                }

            } catch (e: Exception) {
                Log.e("SignUpActivity", "SIGNUP ERROR: ${e.message}")
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(this@SignUpActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        btnSignUp.isEnabled = !isLoading
        btnSignUp.text = if (isLoading) "Creating Account..." else "Sign Up"
    }
}