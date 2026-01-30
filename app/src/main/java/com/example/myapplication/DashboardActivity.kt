package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
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

        // Get user ID from intent or current session
        userId = intent.getStringExtra("USER_ID")
        Log.d("DashboardActivity", "Received user ID from intent: $userId")

        // Load user data
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
        btnBook.setOnClickListener {
            navigateToBooking()
        }

        btnAppointments.setOnClickListener {
            navigateToAppointments()
        }

        btnProfile.setOnClickListener {
            navigateToProfile()
        }
    }

    private fun setupClickListeners() {
        // See All Orders -> AppointmentsActivity
        tvSeeAllOrders.setOnClickListener {
            navigateToAppointments()
        }

        // Appointment Card -> AppointmentsActivity
        cardNearestAppointment.setOnClickListener {
            navigateToAppointments()
        }

        // See All Services -> MainActivity (Booking)
        tvSeeAllServices.setOnClickListener {
            navigateToBooking()
        }
    }

    private fun navigateToBooking() {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("USER_ID", userId)
        Log.d("DashboardActivity", "Passing USER_ID to MainActivity: $userId")
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

                // Query the customers table for this user
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

                if (response.data == "[]" || response.data.trim() == "[]") {
                    Log.e("DashboardActivity", "Empty response - no customer found for user_id: $userId")

                    withContext(Dispatchers.Main) {
                        tvGreeting.text = "Hey there! 👋"
                        tvLocation.text = "No address on file"
                    }
                    return@launch
                }

                val customers = json.decodeFromString<List<Customer>>(response.data)

                if (customers.isNotEmpty()) {
                    val customer = customers[0]
                    customerId = customer.customer_id
                    Log.d("DashboardActivity", "Customer found: ${customer.f_name} ${customer.l_name}, ID: $customerId")

                    withContext(Dispatchers.Main) {
                        // Update UI with user data
                        tvGreeting.text = "Hey, ${customer.l_name}! 👋"
                        tvLocation.text = customer.address ?: "No address on file"
                    }

                    // Load nearest appointment
                    loadNearestAppointment()
                } else {
                    Log.e("DashboardActivity", "No customer found for user_id: $userId")

                    withContext(Dispatchers.Main) {
                        tvGreeting.text = "Hey there! 👋"
                        tvLocation.text = "No address on file"
                    }
                }

            } catch (e: Exception) {
                Log.e("DashboardActivity", "Error loading user data: ${e.message}", e)

                withContext(Dispatchers.Main) {
                    tvGreeting.text = "Hey there! 👋"
                    tvLocation.text = "Unable to load address"
                }
            }
        }
    }

    private fun loadNearestAppointment() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (customerId == null) {
                    Log.e("DashboardActivity", "Cannot load appointment - no customer ID")
                    return@launch
                }

                Log.d("DashboardActivity", "=== LOADING NEAREST APPOINTMENT ===")

                // Get all appointments for this customer
                val appointmentsResponse = supabaseClient.from("appointments")
                    .select {
                        filter {
                            eq("customer_id", customerId!!)
                            neq("status", "cancelled")
                        }
                    }

                val json = Json { ignoreUnknownKeys = true }
                val appointments = json.decodeFromString<List<AppointmentResponse>>(appointmentsResponse.data)

                if (appointments.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        layoutEmptyAppointments.visibility = android.view.View.VISIBLE
                        layoutAppointmentDetails.visibility = android.view.View.GONE
                    }
                    return@launch
                }

                // Pre-load all services
                val allServicesResponse = supabaseClient.from("services").select()
                val allServices = json.decodeFromString<List<Service>>(allServicesResponse.data)
                val servicesMap = allServices.associateBy { it.service_id }

                // Pre-load appointment services
                val appointmentIds = appointments.map { it.appointment_id }
                val allAppointmentServicesResponse = supabaseClient.from("appointment_services").select()
                val allAppointmentServices = json.decodeFromString<List<AppointmentService>>(allAppointmentServicesResponse.data)
                val appointmentServicesMap = allAppointmentServices
                    .filter { it.appointment_id in appointmentIds }
                    .groupBy { it.appointment_id }

                val now = Calendar.getInstance()

                val futureAppointments = appointments.filter { appointment ->
                    try {
                        val appointmentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            .parse(appointment.appointment_date)
                        val appointmentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                            .parse(appointment.appointment_time)

                        if (appointmentDate == null || appointmentTime == null) return@filter false

                        val appointmentDateTime = Calendar.getInstance()
                        appointmentDateTime.time = appointmentDate
                        appointmentDateTime.set(Calendar.HOUR_OF_DAY, appointmentTime.hours)
                        appointmentDateTime.set(Calendar.MINUTE, appointmentTime.minutes)
                        appointmentDateTime.set(Calendar.SECOND, appointmentTime.seconds)

                        appointmentDateTime.timeInMillis > now.timeInMillis
                    } catch (e: Exception) {
                        false
                    }
                }.sortedWith(
                    compareBy<AppointmentResponse> { it.appointment_date }
                        .thenBy { it.appointment_time }
                )

                if (futureAppointments.isNotEmpty()) {
                    val nearest = futureAppointments[0]
                    val appointmentServices = appointmentServicesMap[nearest.appointment_id] ?: emptyList()
                    val serviceNames = appointmentServices.mapNotNull { servicesMap[it.service_id]?.service_name }

                    withContext(Dispatchers.Main) {
                        layoutEmptyAppointments.visibility = android.view.View.GONE
                        layoutAppointmentDetails.visibility = android.view.View.VISIBLE

                        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                        val displayTimeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

                        val date = dateFormat.format(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(nearest.appointment_date)!!)
                        val time = displayTimeFormat.format(timeFormat.parse(nearest.appointment_time)!!)

                        tvAppointmentDate.text = "$date at $time"
                        tvAppointmentServices.text = if (serviceNames.isNotEmpty()) serviceNames.joinToString(", ") else "No services"
                        tvAppointmentStatus.text = nearest.status.uppercase()

                        when (nearest.status.lowercase()) {
                            "pending" -> tvAppointmentStatus.setBackgroundColor(android.graphics.Color.parseColor("#FF9800"))
                            "completed" -> tvAppointmentStatus.setBackgroundColor(android.graphics.Color.parseColor("#4CAF50"))
                            "cancelled" -> tvAppointmentStatus.setBackgroundColor(android.graphics.Color.parseColor("#F44336"))
                            "in_progress" -> tvAppointmentStatus.setBackgroundColor(android.graphics.Color.parseColor("#2196F3"))
                            else -> tvAppointmentStatus.setBackgroundColor(android.graphics.Color.parseColor("#9E9E9E"))
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        layoutEmptyAppointments.visibility = android.view.View.VISIBLE
                        layoutAppointmentDetails.visibility = android.view.View.GONE
                    }
                }

            } catch (e: Exception) {
                Log.e("DashboardActivity", "Error loading nearest appointment: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    layoutEmptyAppointments.visibility = android.view.View.VISIBLE
                    layoutAppointmentDetails.visibility = android.view.View.GONE
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
        loadUserData()
    }
}
