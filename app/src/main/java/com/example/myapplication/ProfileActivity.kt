package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

class ProfileActivity : AppCompatActivity() {

    private val TAG = "ProfileActivity"

    // Views
    private lateinit var tvHeaderName: TextView
    private lateinit var tvHeaderEmail: TextView
    private lateinit var etFirstName: EditText
    private lateinit var etMiddleName: EditText
    private lateinit var etLastName: EditText
    private lateinit var tvEmail: TextView
    private lateinit var etPhone: EditText
    private lateinit var etAddress: EditText
    private lateinit var btnEditSave: MaterialButton
    private lateinit var btnLogout: MaterialButton

    private var isEditMode = false
    private var userId: String? = null

    // Initialize Supabase
    private val supabaseClient: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = "https://mxxyzcoevcsniinvleos.supabase.co",
            supabaseKey = "sb_publishable_pdEutnY70rVI_FVG6Casaw_03co6UQR"
        ) {
            install(io.github.jan.supabase.postgrest.Postgrest)
            install(io.github.jan.supabase.auth.Auth) {
                autoLoadFromStorage = true
                alwaysAutoRefresh = true
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        initializeViews()
        setupListeners()

        // Start the intelligent loading process
        loadProfile()
    }

    private fun initializeViews() {
        tvHeaderName = findViewById(R.id.tvHeaderName)
        tvHeaderEmail = findViewById(R.id.tvHeaderEmail)
        etFirstName = findViewById(R.id.etProfileFirstName)
        etMiddleName = findViewById(R.id.etProfileMiddleName)
        etLastName = findViewById(R.id.etProfileLastName)
        tvEmail = findViewById(R.id.tvProfileEmail)
        etPhone = findViewById(R.id.etProfilePhone)
        etAddress = findViewById(R.id.etProfileAddress)
        btnEditSave = findViewById(R.id.btnEditProfile)
        btnLogout = findViewById(R.id.btnLogout)
    }

    private fun setupListeners() {
        btnEditSave.setOnClickListener {
            if (isEditMode) saveProfile() else toggleEditMode(true)
        }
        btnLogout.setOnClickListener { handleLogout() }
    }

    private fun loadProfile() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // STRATEGY: Instead of a fixed delay, wait for the Auth status to be Authenticated
                Log.d(TAG, "Waiting for Auth session to initialize...")

                // Wait until the session status is Authenticated or times out after 5 seconds
                withTimeoutOrNull(5000) {
                    supabaseClient.auth.sessionStatus.filter { it is SessionStatus.Authenticated }.first()
                }

                userId = supabaseClient.auth.currentUserOrNull()?.id

                if (userId == null) {
                    Log.e(TAG, "Auth failed: No user found after waiting.")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ProfileActivity, "Please log in again", Toast.LENGTH_LONG).show()
                        handleLogout()
                    }
                    return@launch
                }

                Log.d(TAG, "Auth success. Querying data for: $userId")

                // Query Database
                val response = supabaseClient.from("customers").select {
                    filter { eq("user_id", userId!!) }
                }

                Log.d(TAG, "Raw Response: ${response.data}")

                val json = Json { ignoreUnknownKeys = true }
                val customerList = json.decodeFromString<List<Customer>>(response.data)

                if (customerList.isNotEmpty()) {
                    val c = customerList[0]
                    withContext(Dispatchers.Main) {
                        updateUI(c)
                    }
                } else {
                    Log.e(TAG, "Data exists in DB but RLS is blocking the query.")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ProfileActivity, "Database Access Denied (Check RLS)", Toast.LENGTH_LONG).show()
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Loading Error: ${e.localizedMessage}")
            }
        }
    }

    private fun updateUI(c: Customer) {
        val fullName = "${c.f_name} ${c.l_name}"
        tvHeaderName.text = fullName
        tvHeaderEmail.text = c.email ?: "No Email"
        etFirstName.setText(c.f_name)
        etMiddleName.setText(c.m_name ?: "")
        etLastName.setText(c.l_name)
        tvEmail.text = c.email ?: "No Email"
        etPhone.setText(c.phone_num ?: "")
        etAddress.setText(c.address ?: "")
    }

    private fun toggleEditMode(enabled: Boolean) {
        isEditMode = enabled
        val fields = listOf(etFirstName, etMiddleName, etLastName, etPhone, etAddress)
        fields.forEach { it.isEnabled = enabled }

        if (enabled) {
            btnEditSave.text = "Save Changes"
            btnEditSave.setIconResource(android.R.drawable.ic_menu_save)
        } else {
            btnEditSave.text = "Edit Profile"
            btnEditSave.setIconResource(android.R.drawable.ic_menu_edit)
        }
    }

    private fun saveProfile() {
        val fName = etFirstName.text.toString().trim()
        val mName = etMiddleName.text.toString().trim()
        val lName = etLastName.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val address = etAddress.text.toString().trim()

        btnEditSave.isEnabled = false
        btnEditSave.text = "Saving..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                supabaseClient.from("customers").update(
                    mapOf(
                        "f_name" to fName,
                        "m_name" to if (mName.isEmpty()) null else mName,
                        "l_name" to lName,
                        "phone_num" to phone,
                        "address" to address
                    )
                ) { filter { eq("user_id", userId!!) } }

                withContext(Dispatchers.Main) {
                    toggleEditMode(false)
                    btnEditSave.isEnabled = true
                    tvHeaderName.text = "$fName $lName"
                    Toast.makeText(this@ProfileActivity, "Profile Updated!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Update failed: ${e.message}")
                withContext(Dispatchers.Main) {
                    btnEditSave.isEnabled = true
                    Toast.makeText(this@ProfileActivity, "Update Failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun handleLogout() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                supabaseClient.auth.signOut()
                withContext(Dispatchers.Main) {
                    val intent = Intent(this@ProfileActivity, LogInActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            } catch (e: Exception) { Log.e(TAG, "Logout error") }
        }
    }
}