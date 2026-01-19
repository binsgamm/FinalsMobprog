package com.example.myapplication
import io.github.jan.supabase.auth.providers.builtin.Email
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivitySignupBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
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

    private lateinit var tilFirstName: TextInputLayout
    private lateinit var tilMiddleName: TextInputLayout
    private lateinit var tilLastName: TextInputLayout
    private lateinit var tilAddress: TextInputLayout
    private lateinit var tilPhone: TextInputLayout
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var tilConfirmPassword: TextInputLayout

    private lateinit var btnSignUp: MaterialButton

    // Supabase client
    private val supabaseClient: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = "https://mxxyzcoevcsniinvleos.supabase.co",
            supabaseKey = "sb_publishable_pdEutnY70rVI_FVG6Casaw_03co6UQR"
        ) {
            install(Auth)
            install(Postgrest)
        }
    }

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

        initializeViews()
        setupClickListeners()
        setupTextWatchers()
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

        tilFirstName = etFirstName.parent.parent as TextInputLayout
        tilMiddleName = etMiddleName.parent.parent as TextInputLayout
        tilLastName = etLastName.parent.parent as TextInputLayout
        tilAddress = etAddress.parent.parent as TextInputLayout
        tilPhone = etPhone.parent.parent as TextInputLayout
        tilEmail = etEmail.parent.parent as TextInputLayout
        tilPassword = etPassword.parent.parent as TextInputLayout
        tilConfirmPassword = etConfirmPassword.parent.parent as TextInputLayout

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
            override fun afterTextChanged(s: Editable?) {
                validatePasswordInRealTime(s?.toString() ?: "")
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        etEmail.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validateEmailInRealTime(s?.toString() ?: "")
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun validateAllFields(): Boolean {
        var isValid = true
        clearAllErrors()

        val firstName = etFirstName.text.toString().trim()
        val middleName = etMiddleName.text.toString().trim()
        val lastName = etLastName.text.toString().trim()
        val address = etAddress.text.toString().trim()
        val phone = etPhone.text.toString().trim().replace(Regex("""\D"""), "")
        val email = etEmail.text.toString().trim().lowercase()
        val password = etPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()

        // First Name
        if (!validateName(firstName)) {
            tilFirstName.error = "First name must be 2-50 letters"
            isValid = false
        }

        // Last Name
        if (!validateName(lastName)) {
            tilLastName.error = "Last name must be 2-50 letters"
            isValid = false
        }

        // Middle Name
        if (middleName.isNotEmpty() && !validateOptionalName(middleName)) {
            tilMiddleName.error = "Only letters, spaces, dots, apostrophes and hyphens allowed"
            isValid = false
        }

        // Address
        if (address.isEmpty()) {
            tilAddress.error = "Address is required"
            isValid = false
        } else if (address.length < 10) {
            tilAddress.error = "Please enter a complete address"
            isValid = false
        }

        // Phone
        if (!validatePhone(phone)) {
            tilPhone.error = "Enter a valid Philippine mobile number (10 digits, starts with 9)"
            isValid = false
        }

        // Email
        if (!validateEmail(email)) {
            tilEmail.error = "Enter a valid email address"
            isValid = false
        }

        // Password
        if (!validatePassword(password)) {
            tilPassword.error = getPasswordError(password)
            isValid = false
        }

        // Confirm Password
        if (!validateConfirmPassword(password, confirmPassword)) {
            tilConfirmPassword.error = if (confirmPassword.isEmpty())
                "Please confirm your password"
            else
                "Passwords do not match"
            isValid = false
        }

        return isValid
    }

    private fun validateName(name: String): Boolean {
        return name.isNotEmpty() &&
                name.length in MIN_NAME_LENGTH..MAX_NAME_LENGTH &&
                NAME_REGEX.matches(name)
    }

    private fun validateOptionalName(name: String): Boolean {
        return name.length <= MAX_NAME_LENGTH && NAME_REGEX.matches(name)
    }

    private fun validatePhone(phone: String): Boolean {
        return phone.isNotEmpty() && PHONE_REGEX.matches(phone)
    }

    private fun validateEmail(email: String): Boolean {
        return email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun validatePassword(password: String): Boolean {
        return password.isNotEmpty() &&
                password.length in MIN_PASSWORD_LENGTH..MAX_PASSWORD_LENGTH &&
                PASSWORD_REGEX.matches(password)
    }

    private fun validateConfirmPassword(password: String, confirmPassword: String): Boolean {
        return confirmPassword.isNotEmpty() && password == confirmPassword
    }

    private fun validatePasswordInRealTime(password: String) {
        if (password.isEmpty()) {
            tilPassword.error = null
            return
        }

        when {
            password.length < MIN_PASSWORD_LENGTH ->
                tilPassword.error = "At least $MIN_PASSWORD_LENGTH characters"
            password.length > MAX_PASSWORD_LENGTH ->
                tilPassword.error = "Maximum $MAX_PASSWORD_LENGTH characters"
            !PASSWORD_REGEX.matches(password) ->
                tilPassword.error = "Include uppercase, lowercase, number & special char"
            else -> tilPassword.error = null
        }
    }

    private fun validateEmailInRealTime(email: String) {
        if (email.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Invalid email format"
        } else {
            tilEmail.error = null
        }
    }

    private fun getPasswordError(password: String): String {
        return when {
            password.isEmpty() -> "Password is required"
            password.length < MIN_PASSWORD_LENGTH -> "Minimum $MIN_PASSWORD_LENGTH characters"
            password.length > MAX_PASSWORD_LENGTH -> "Maximum $MAX_PASSWORD_LENGTH characters"
            !PASSWORD_REGEX.matches(password) -> "Must include uppercase, lowercase, number & special character (@$!%*?&)"
            else -> ""
        }
    }

    private fun clearAllErrors() {
        listOf(
            tilFirstName, tilMiddleName, tilLastName,
            tilAddress, tilPhone, tilEmail,
            tilPassword, tilConfirmPassword
        ).forEach { it.error = null }
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
                // 1. Sign up with Supabase Auth
                val authResult = supabaseClient.auth.signUpWith(Email) {
                    email = userData.email
                    password = userData.password
                }

                // Get the user ID from the result
                val userId = authResult?.id!!

                // 2. Create customer profile in your database
                val customerData = buildMap<String, Any?> {
                    put("user_id", userId)
                    put("f_name", userData.firstName)
                    put("l_name", userData.lastName)
                    put("email", userData.email)
                    put("phone_num", "+63${userData.phone}")
                    put("address", userData.address)

                    // Add middle name only if it's not empty
                    if (userData.middleName.isNotEmpty()) {
                        put("m_name", userData.middleName)
                    }
                }

                // 3. Insert into customers table
                supabaseClient.postgrest.from("customers").insert(customerData)

                withContext(Dispatchers.Main) {
                    showLoading(false)
                    showSuccessAndNavigate()
                }

            } catch (e: Exception) {
                // Log the FULL error for debugging
                Log.e("SignUpActivity", "Sign up error:", e)
                Log.e("SignUpActivity", "Full error message: ${e.message}")
                Log.e("SignUpActivity", "Error cause: ${e.cause}")
                Log.e("SignUpActivity", "Stack trace: ${e.stackTraceToString()}")

                withContext(Dispatchers.Main) {
                    showLoading(false)
                    handleSignUpError(e)
                }
            }
        }
    }

    private fun handleSignUpError(exception: Exception) {
        val errorMessage = exception.message ?: "Unknown error"
        val fullError = exception.toString()

        // Log to console for debugging
        println("SIGNUP ERROR: $fullError")
        println("ERROR MESSAGE: $errorMessage")

        when {
            errorMessage.contains("already registered", ignoreCase = true) ||
                    errorMessage.contains("already exists", ignoreCase = true) -> {
                tilEmail.error = "Email already exists"
            }
            errorMessage.contains("Invalid email", ignoreCase = true) -> {
                tilEmail.error = "Invalid email address"
            }
            errorMessage.contains("Password", ignoreCase = true) &&
                    errorMessage.contains("weak", ignoreCase = true) -> {
                tilPassword.error = "Password is too weak"
            }
            errorMessage.contains("duplicate key", ignoreCase = true) -> {
                if (errorMessage.contains("phone_num", ignoreCase = true)) {
                    tilPhone.error = "Phone number already registered"
                } else if (errorMessage.contains("email", ignoreCase = true)) {
                    tilEmail.error = "Email already registered"
                }
            }
            errorMessage.contains("network", ignoreCase = true) ||
                    errorMessage.contains("connection", ignoreCase = true) ||
                    errorMessage.contains("socket", ignoreCase = true) ||
                    errorMessage.contains("timeout", ignoreCase = true) ||
                    errorMessage.contains("failed to connect", ignoreCase = true) -> {
                Toast.makeText(
                    this,
                    "Network error. Please check your internet connection and try again.",
                    Toast.LENGTH_LONG
                ).show()
            }
            errorMessage.contains("HTTP", ignoreCase = true) -> {
                // Show user-friendly message for HTTP errors
                Toast.makeText(
                    this,
                    "Server error. Please try again later.",
                    Toast.LENGTH_LONG
                ).show()

                // Log the actual error for debugging
                Log.e("SignUpActivity", "HTTP Error details: $fullError")
            }
            else -> {
                // Show the actual error for debugging
                Toast.makeText(
                    this,
                    "Sign up failed: ${errorMessage.take(100)}...", // Limit length
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showSuccessAndNavigate() {
        Toast.makeText(
            this,
            "Account created successfully! Check your email for verification.",
            Toast.LENGTH_LONG
        ).show()

        val intent = Intent(this, LogInActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun showLoading(isLoading: Boolean) {
        btnSignUp.isEnabled = !isLoading
        btnSignUp.text = if (isLoading) "Creating Account..." else "Sign Up"
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        navigateToLogin()
    }
}

data class UserData(
    val firstName: String,
    val middleName: String,
    val lastName: String,
    val address: String,
    val phone: String,
    val email: String,
    val password: String
)
