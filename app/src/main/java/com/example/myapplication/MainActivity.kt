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
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
data class Customer(
    val customer_id: Int? = null,
    val f_name: String? = null,
    val m_name: String? = null,
    val l_name: String? = null,
    val email: String? = null,
    val phone_num: String? = null,
    val created_at: String? = null,
    val user_id: String? = null,
    val address: String? = null
)

class MainActivity : AppCompatActivity() {

    private lateinit var tvGreeting: TextView
    private lateinit var tvLocation: TextView

    private val supabaseClient: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = "https://mxxyzcoevcsniinvleos.supabase.co",
            supabaseKey = "sb_publishable_pdEutnY70rVI_FVG6Casaw_03co6UQR"
        ) {
            install(io.github.jan.supabase.postgrest.Postgrest)
            install(io.github.jan.supabase.auth.Auth)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        tvGreeting = findViewById(R.id.tvGreeting)
        tvLocation = findViewById(R.id.tvLocation)

        tvGreeting.text = "Loading..."
        tvLocation.text = "Please wait"

        loadCustomerData()
    }

    private fun loadCustomerData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Get current logged-in user
                val currentUser = supabaseClient.auth.currentUserOrNull()
                val userId = currentUser?.id
                val userEmail = currentUser?.email?.lowercase()

                if (userId == null) {
                    showOnMain("Hey, there! 👋", "Please log in")
                    return@launch
                }

                Log.d("MainActivity", "Logged in as: $userEmail, ID: $userId")

                // 2. TRY 1: Get customer by user_id FROM CUSTOMERS TABLE
                var customer: Customer? = null

                try {
                    val result = supabaseClient.postgrest
                        .from("customers")  // ← CORRECT TABLE NAME
                        .select {
                            filter {
                                eq("user_id", userId)
                            }
                        }
                        .decodeList<Customer>()

                    Log.d("MainActivity", "Query returned ${result.size} customer(s)")

                    if (result.isNotEmpty()) {
                        customer = result[0]
                        Log.d("MainActivity", "✅ Found customer: ${customer.l_name}")
                    } else {
                        Log.d("MainActivity", "❌ No customer found with user_id: $userId")
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error querying customers: ${e.message}")
                }

                // 3. TRY 2: If not found, try by email
                if (customer == null && userEmail != null) {
                    try {
                        val result = supabaseClient.postgrest
                            .from("customers")  // ← CORRECT TABLE NAME
                            .select {
                                filter {
                                    eq("email", userEmail)
                                }
                            }
                            .decodeList<Customer>()

                        if (result.isNotEmpty()) {
                            customer = result[0]
                            Log.d("MainActivity", "✅ Found customer by email: ${customer.l_name}")
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error querying by email: ${e.message}")
                    }
                }

                // 4. DISPLAY THE DATA
                withContext(Dispatchers.Main) {
                    if (customer != null) {
                        // Display last name from customers table
                        val lastName = customer.l_name?.trim()
                        tvGreeting.text = if (!lastName.isNullOrEmpty()) {
                            "Hey, $lastName! 👋"
                        } else {
                            "Hey, there! 👋"
                        }

                        // Display address from customers table
                        val address = customer.address?.trim()
                        tvLocation.text = address ?: "Address not set"

                        Toast.makeText(
                            this@MainActivity,
                            "Welcome back!",
                            Toast.LENGTH_SHORT
                        ).show()

                        Log.d("MainActivity", "📱 Displaying: Last Name='$lastName', Address='$address'")

                    } else {
                        tvGreeting.text = "Hey, there! 👋"
                        tvLocation.text = "Complete your profile"

                        Toast.makeText(
                            this@MainActivity,
                            "Welcome! Please complete your profile",
                            Toast.LENGTH_LONG
                        ).show()

                        Log.e("MainActivity", "❌ Customer not found in 'customers' table")
                        Log.e("MainActivity", "   User ID: $userId")
                        Log.e("MainActivity", "   Email: $userEmail")
                    }
                }

            } catch (e: Exception) {
                Log.e("MainActivity", "Error: ${e.message}", e)
                showOnMain("Hey, there! 👋", "Welcome to Labada")
            }
        }
    }

    private fun showOnMain(greeting: String, location: String) {
        CoroutineScope(Dispatchers.Main).launch {
            tvGreeting.text = greeting
            tvLocation.text = location
        }
    }
}