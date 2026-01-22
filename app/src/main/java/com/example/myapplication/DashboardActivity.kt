package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class DashboardActivity : AppCompatActivity() {

    private lateinit var tvGreeting: TextView
    private lateinit var tvLocation: TextView

    private var userId: String? = null

    // Supabase client
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
        setContentView(R.layout.activity_dashboard)

        initializeViews()

        // Get user ID from intent or current session
        userId = intent.getStringExtra("USER_ID")
        Log.d("DashboardActivity", "Received user ID from intent: $userId")

        // Load user data
        loadUserData()
    }

    private fun initializeViews() {
        tvGreeting = findViewById(R.id.tvGreeting)
        tvLocation = findViewById(R.id.tvLocation)
    }

    private fun loadUserData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // If userId is null, try to get it from current session
                if (userId == null) {
                    val currentUser = supabaseClient.auth.currentUserOrNull()
                    userId = currentUser?.id
                    Log.d("DashboardActivity", "Got user ID from session: $userId")
                }

                Log.d("DashboardActivity", "Loading data for user_id: $userId")

                if (userId == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@DashboardActivity,
                            "No user session found. Please log in again.",
                            Toast.LENGTH_LONG
                        ).show()
                        navigateToLogin()
                    }
                    return@launch
                }

                // First, let's try to get ALL customers to see what's in the table
                Log.d("DashboardActivity", "Fetching all customers to debug...")
                val allCustomersResponse = supabaseClient.from("customers")
                    .select()
                Log.d("DashboardActivity", "All customers: ${allCustomersResponse.data}")

                // Query the customers table for this user - simplified query
                Log.d("DashboardActivity", "Querying for user_id: $userId")
                val response = supabaseClient.from("customers")
                    .select() {
                        filter {
                            eq("user_id", userId!!)
                        }
                    }

                Log.d("DashboardActivity", "Database response: ${response.data}")

                // Parse the response
                val json = Json { ignoreUnknownKeys = true }

                // Check if response.data is empty
                if (response.data == "[]" || response.data.trim() == "[]") {
                    Log.e("DashboardActivity", "Empty response - no customer found for user_id: $userId")

                    withContext(Dispatchers.Main) {
                        tvGreeting.text = "Hey there! 👋"
                        tvLocation.text = "No address on file"

                        Toast.makeText(
                            this@DashboardActivity,
                            "Customer profile not found. User ID: ${userId?.take(8)}...",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@launch
                }

                val customers = json.decodeFromString<List<Customer>>(response.data)

                if (customers.isNotEmpty()) {
                    val customer = customers[0]
                    Log.d("DashboardActivity", "Customer found: ${customer.f_name} ${customer.l_name}")

                    withContext(Dispatchers.Main) {
                        // Update UI with user data
                        tvGreeting.text = "Hey, ${customer.l_name}! 👋"
                        tvLocation.text = customer.address ?: "No address on file"
                    }
                } else {
                    Log.e("DashboardActivity", "No customer found for user_id: $userId")

                    withContext(Dispatchers.Main) {
                        tvGreeting.text = "Hey there! 👋"
                        tvLocation.text = "No address on file"

                        Toast.makeText(
                            this@DashboardActivity,
                            "Customer profile not found",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            } catch (e: Exception) {
                Log.e("DashboardActivity", "Error loading user data: ${e.message}", e)

                withContext(Dispatchers.Main) {
                    tvGreeting.text = "Hey there! 👋"
                    tvLocation.text = "Unable to load address"

                    Toast.makeText(
                        this@DashboardActivity,
                        "Error loading profile: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LogInActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()
        // Reload user data when returning to this activity
        loadUserData()
    }
}

// Data class for deserializing customer data from Supabase
@Serializable
data class Customer(
    val f_name: String,
    val l_name: String,
    val address: String? = null,
    val user_id: String? = null
)