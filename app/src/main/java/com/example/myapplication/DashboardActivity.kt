package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*

class DashboardActivity : AppCompatActivity() {

    private lateinit var tvGreeting: TextView
    private lateinit var tvLocation: TextView

    // Order section
    private lateinit var tvSeeAllOrders: TextView
    private lateinit var cardNearestAppointment: CardView
    private lateinit var layoutEmptyAppointments: LinearLayout
    private lateinit var layoutAppointmentDetails: LinearLayout
    private lateinit var tvAppointmentStatus: TextView
    private lateinit var tvAppointmentDate: TextView
    private lateinit var tvAppointmentServices: TextView

    // Services section
    private lateinit var tvSeeAllServices: TextView

    // Navigation buttons
    private lateinit var btnBook: MaterialButton
    private lateinit var btnAppointments: MaterialButton
    private lateinit var btnProfile: MaterialButton

    private var userId: String? = null
    private var customerId: Int? = null

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

        // Get user ID from intent
        userId = intent.getStringExtra("USER_ID")
        Log.d("DashboardActivity", "User ID from intent: $userId")

        // Initial data load
        loadUserData()
    }

    private fun initializeViews() {
        tvGreeting = findViewById(R.id.tvGreeting)
        tvLocation = findViewById(R.id.tvLocation)

        // Order section
        tvSeeAllOrders = findViewById(R.id.tvSeeAllOrders)
        cardNearestAppointment = findViewById(R.id.cardNearestAppointment)
        layoutEmptyAppointments = findViewById(R.id.layoutEmptyAppointments)
        layoutAppointmentDetails = findViewById(R.id.layoutAppointmentDetails)
        tvAppointmentStatus = findViewById(R.id.tvAppointmentStatus)
        tvAppointmentDate = findViewById(R.id.tvAppointmentDate)
        tvAppointmentServices = findViewById(R.id.tvAppointmentServices)

        // Services section
        tvSeeAllServices = findViewById(R.id.tvSeeAllServices)

        // Navigation buttons
        btnBook = findViewById(R.id.btnBook)
        btnAppointments = findViewById(R.id.btnAppointments)
        btnProfile = findViewById(R.id.btnProfile)

        setupNavigationListeners()
        setupClickListeners()
    }

    private fun setupNavigationListeners() {
        btnBook.setOnClickListener { navigateToBooking() }
        btnAppointments.setOnClickListener { navigateToAppointments() }
        btnProfile.setOnClickListener { navigateToProfile() }
    }

    private fun setupClickListeners() {
        tvSeeAllOrders.setOnClickListener { navigateToAppointments() }
        cardNearestAppointment.setOnClickListener { navigateToAppointments() }
        tvSeeAllServices.setOnClickListener { navigateToBooking() }
    }

    private fun navigateToBooking() {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("USER_ID", userId)
        startActivity(intent)
    }

    private fun navigateToAppointments() {
        val intent = Intent(this, AppointmentsActivity::class.java)
        intent.putExtra("USER_ID", userId)
        startActivity(intent)
    }

    private fun navigateToProfile() {
        val intent = Intent(this, ProfileActivity::class.java)
        intent.putExtra("USER_ID", userId)
        startActivity(intent)
    }

    private fun loadUserData() {
        lifecycleScope.launch {
            try {
                // If userId is missing, try session with a small retry
                if (userId == null) {
                    var attempts = 0
                    while (userId == null && attempts < 5) {
                        userId = supabaseClient.auth.currentUserOrNull()?.id
                        if (userId == null) {
                            delay(500)
                            attempts++
                        }
                    }
                }

                if (userId == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@DashboardActivity, "Session lost. Please log in again.", Toast.LENGTH_LONG).show()
                        navigateToLogin()
                    }
                    return@launch
                }

                Log.d("DashboardActivity", "Fetching customer data for user: $userId")
                
                // Fetch customer details
                val response = supabaseClient.from("customers")
                    .select {
                        filter { eq("user_id", userId!!) }
                    }

                val json = Json { ignoreUnknownKeys = true }
                val customers = json.decodeFromString<List<Customer>>(response.data)

                if (customers.isNotEmpty()) {
                    val customer = customers[0]
                    customerId = customer.customer_id
                    
                    withContext(Dispatchers.Main) {
                        // Display actual user name and address from database
                        tvGreeting.text = "Hey, ${customer.f_name}! 👋"
                        tvLocation.text = customer.address ?: "Address not set"
                    }

                    // Load the user's nearest appointment
                    loadNearestAppointment()
                } else {
                    Log.e("DashboardActivity", "No customer found for user_id: $userId")
                    withContext(Dispatchers.Main) {
                        tvGreeting.text = "Welcome! 👋"
                        tvLocation.text = "Profile incomplete"
                    }
                }

            } catch (e: Exception) {
                Log.e("DashboardActivity", "Error loading user data: ${e.message}")
                withContext(Dispatchers.Main) {
                    tvGreeting.text = "Error Loading Data"
                }
            }
        }
    }

    private fun loadNearestAppointment() {
        lifecycleScope.launch {
            try {
                if (customerId == null) return@launch

                // Get active appointments
                val response = supabaseClient.from("appointments")
                    .select {
                        filter {
                            eq("customer_id", customerId!!)
                            neq("status", "cancelled")
                        }
                    }

                val json = Json { ignoreUnknownKeys = true }
                val appointments = json.decodeFromString<List<AppointmentResponse>>(response.data)

                if (appointments.isEmpty()) {
                    showEmptyAppointmentState()
                    return@launch
                }

                // Find the soonest future appointment
                val now = Calendar.getInstance()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

                val nextAppointment = appointments.mapNotNull { appointment ->
                    try {
                        val date = dateFormat.parse(appointment.appointment_date)
                        val time = timeFormat.parse(appointment.appointment_time)
                        if (date == null || time == null) return@mapNotNull null

                        val dateTime = Calendar.getInstance().apply {
                            this.time = date
                            set(Calendar.HOUR_OF_DAY, time.hours)
                            set(Calendar.MINUTE, time.minutes)
                            set(Calendar.SECOND, time.seconds)
                        }

                        if (dateTime.after(now)) appointment to dateTime else null
                    } catch (e: Exception) { null }
                }.minByOrNull { it.second.timeInMillis }?.first

                if (nextAppointment != null) {
                    // Fetch services for this specific appointment
                    val servicesResponse = supabaseClient.from("appointment_services")
                        .select { filter { eq("appointment_id", nextAppointment.appointment_id) } }
                    val appServices = json.decodeFromString<List<AppointmentService>>(servicesResponse.data)
                    
                    // Fetch service names
                    val allServicesResponse = supabaseClient.from("services").select()
                    val allServices = json.decodeFromString<List<Service>>(allServicesResponse.data)
                    val serviceNamesMap = allServices.associate { it.service_id to it.service_name }
                    
                    val selectedServiceNames = appServices.mapNotNull { serviceNamesMap[it.service_id] }

                    updateAppointmentUI(nextAppointment, selectedServiceNames)
                } else {
                    showEmptyAppointmentState()
                }

            } catch (e: Exception) {
                Log.e("DashboardActivity", "Error loading appointment: ${e.message}")
                showEmptyAppointmentState()
            }
        }
    }

    private fun updateAppointmentUI(appointment: AppointmentResponse, serviceNames: List<String>) {
        lifecycleScope.launch(Dispatchers.Main) {
            layoutEmptyAppointments.visibility = View.GONE
            layoutAppointmentDetails.visibility = View.VISIBLE

            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
            val timeInputFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val timeOutputFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

            val formattedDate = try {
                outputFormat.format(inputFormat.parse(appointment.appointment_date)!!)
            } catch (e: Exception) { appointment.appointment_date }

            val formattedTime = try {
                timeOutputFormat.format(timeInputFormat.parse(appointment.appointment_time)!!)
            } catch (e: Exception) { appointment.appointment_time }

            tvAppointmentDate.text = "$formattedDate at $formattedTime"
            tvAppointmentServices.text = if (serviceNames.isNotEmpty()) serviceNames.joinToString(", ") else "Standard Service"
            tvAppointmentStatus.text = appointment.status.uppercase()

            // Update status background color
            val statusColor = when (appointment.status.lowercase()) {
                "pending" -> "#FF9800"
                "in_progress", "in progress" -> "#2196F3"
                "completed" -> "#4CAF50"
                else -> "#9E9E9E"
            }
            tvAppointmentStatus.setBackgroundColor(android.graphics.Color.parseColor(statusColor))
        }
    }

    private fun showEmptyAppointmentState() {
        lifecycleScope.launch(Dispatchers.Main) {
            layoutEmptyAppointments.visibility = View.VISIBLE
            layoutAppointmentDetails.visibility = View.GONE
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
        loadUserData()
    }
}
