package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class ProfileActivity : AppCompatActivity() {

    private lateinit var tvHeaderName: TextView
    private lateinit var tvHeaderEmail: TextView
    private lateinit var tvProfileFullName: TextView
    private lateinit var tvProfileEmail: TextView
    private lateinit var tvProfilePhone: TextView
    private lateinit var tvProfileAddress: TextView
    private lateinit var btnLogout: MaterialButton
    private lateinit var btnEditProfile: MaterialButton

    private var userId: String? = null

    private val supabaseClient: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = "https://mxxyzcoevcsniinvleos.supabase.co",
            supabaseKey = "sb_publishable_pdEutnY70rVI_FVG6Casaw_03co6UQR"
        ) {
            install(io.github.jan.supabase.postgrest.Postgrest)
            install(io.github.jan.supabase.auth.Auth) {
                alwaysAutoRefresh = true
                autoLoadFromStorage = true
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Get user ID from intent
        userId = intent.getStringExtra("USER_ID")
        
        // If userId is null, try to get it from current session
        if (userId == null) {
            userId = supabaseClient.auth.currentUserOrNull()?.id
        }

        initializeViews()
        setupListeners()
        loadProfileData()
    }

    private fun initializeViews() {
        tvHeaderName = findViewById(R.id.tvHeaderName)
        tvHeaderEmail = findViewById(R.id.tvHeaderEmail)
        tvProfileFullName = findViewById(R.id.tvProfileFullName)
        tvProfileEmail = findViewById(R.id.tvProfileEmail)
        tvProfilePhone = findViewById(R.id.tvProfilePhone)
        tvProfileAddress = findViewById(R.id.tvProfileAddress)
        btnLogout = findViewById(R.id.btnLogout)
        btnEditProfile = findViewById(R.id.btnEditProfile)
    }

    private fun setupListeners() {
        btnLogout.setOnClickListener {
            handleLogout()
        }

        btnEditProfile.setOnClickListener {
            Toast.makeText(this, "Edit Profile feature coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadProfileData() {
        if (userId == null) {
            Toast.makeText(this, "User session not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("ProfileActivity", "Loading profile for user_id: $userId")
                
                val response = supabaseClient.from("customers")
                    .select {
                        filter {
                            eq("user_id", userId!!)
                        }
                    }

                val json = Json { ignoreUnknownKeys = true }
                val customers = json.decodeFromString<List<Customer>>(response.data)

                if (customers.isNotEmpty()) {
                    val customer = customers[0]
                    val fullName = "${customer.f_name} ${customer.m_name ?: ""} ${customer.l_name}".replace("  ", " ")

                    withContext(Dispatchers.Main) {
                        tvHeaderName.text = fullName
                        tvHeaderEmail.text = customer.email ?: "N/A"
                        tvProfileFullName.text = fullName
                        tvProfileEmail.text = customer.email ?: "N/A"
                        tvProfilePhone.text = customer.phone_num ?: "N/A"
                        tvProfileAddress.text = customer.address ?: "N/A"
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ProfileActivity, "Profile data not found", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("ProfileActivity", "Error loading profile: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ProfileActivity, "Error loading profile data", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun handleLogout() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                supabaseClient.auth.signOut()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ProfileActivity, "Logged out successfully", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@ProfileActivity, LogInActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            } catch (e: Exception) {
                Log.e("ProfileActivity", "Logout error: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ProfileActivity, "Error during logout", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
