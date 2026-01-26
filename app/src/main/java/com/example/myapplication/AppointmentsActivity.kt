package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class AppointmentsActivity : AppCompatActivity() {

    private lateinit var rvAppointments: RecyclerView
    private lateinit var appointmentsAdapter: AppointmentsAdapter
    private val appointments = mutableListOf<AppointmentWithDetails>()

    private var userId: String? = null
    private var customerId: Int? = null

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
        setContentView(R.layout.activity_appointments)

        Log.d("AppointmentsActivity", "=== APPOINTMENTS ACTIVITY CREATED ===")

        initializeViews()
        loadUserData()
    }

    private fun initializeViews() {
        rvAppointments = findViewById(R.id.rvAppointments)
        rvAppointments.layoutManager = LinearLayoutManager(this)

        appointmentsAdapter = AppointmentsAdapter(
            appointments = appointments,
            onCancelClick = { appointment -> showCancelDialog(appointment) },
            onViewDetailsClick = { appointment -> showAppointmentDetails(appointment) }
        )

        rvAppointments.adapter = appointmentsAdapter
    }

    private fun loadUserData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("AppointmentsActivity", "=== LOADING USER DATA ===")

                // Get userId from intent first (passed from MainActivity after booking)
                userId = intent.getStringExtra("USER_ID")
                Log.d("AppointmentsActivity", "USER_ID from intent: $userId")

                // If not in intent, try getting from current session
                if (userId == null) {
                    // Force initialization of supabaseClient
                    @Suppress("UNUSED_VARIABLE")
                    val client = supabaseClient
                    Log.d("AppointmentsActivity", "Supabase client initialized")

                    // Wait briefly for session to load (reduced from 2500ms)
                    delay(500)

                    val currentUser = supabaseClient.auth.currentUserOrNull()
                    Log.d("AppointmentsActivity", "Session check - User: ${currentUser?.id}")
                    userId = currentUser?.id
                }

                if (userId == null) {
                    Log.e("AppointmentsActivity", "ERROR: No user ID found!")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@AppointmentsActivity,
                            "Please log in first",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                    return@launch
                }

                Log.d("AppointmentsActivity", "Final userId: $userId")

                // Get customer_id - with retry logic since session might not be fully loaded
                Log.d("AppointmentsActivity", "Querying customers for user_id: $userId")

                var customers: List<Customer> = emptyList()
                var retryCount = 0
                val maxRetries = 3

                while (customers.isEmpty() && retryCount < maxRetries) {
                    val customerResponse = supabaseClient.from("customers")
                        .select {
                            filter {
                                eq("user_id", userId!!)
                            }
                        }

                    Log.d("AppointmentsActivity", "Attempt ${retryCount + 1}: Customer response: ${customerResponse.data}")

                    val json = Json { ignoreUnknownKeys = true }
                    customers = json.decodeFromString<List<Customer>>(customerResponse.data)

                    if (customers.isEmpty() && retryCount < maxRetries - 1) {
                        Log.d("AppointmentsActivity", "No customers found, waiting 500ms before retry...")
                        delay(500)
                        retryCount++
                    } else {
                        break
                    }
                }

                Log.d("AppointmentsActivity", "Customers decoded: ${customers.size} found after $retryCount retries")

                if (customers.isNotEmpty()) {
                    customerId = customers[0].customer_id
                    Log.d("AppointmentsActivity", "Customer ID: $customerId (${customers[0].f_name} ${customers[0].l_name})")

                    loadAppointments()
                } else {
                    Log.e("AppointmentsActivity", "ERROR: No customer found for user_id: $userId after $maxRetries attempts")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@AppointmentsActivity,
                            "Customer profile not found. Please try again.",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                }

            } catch (e: Exception) {
                Log.e("AppointmentsActivity", "Error loading user data: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@AppointmentsActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun loadAppointments() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("AppointmentsActivity", "=== LOADING APPOINTMENTS ===")

                // Get all appointments for this customer
                val appointmentsResponse = supabaseClient.from("appointments")
                    .select {
                        filter {
                            eq("customer_id", customerId!!)
                        }
                    }

                Log.d("AppointmentsActivity", "Appointments response: ${appointmentsResponse.data}")

                val json = Json { ignoreUnknownKeys = true }
                // Sort in Kotlin after fetching (simpler and works with all Supabase versions)
                val appointmentsList = json.decodeFromString<List<AppointmentResponse>>(appointmentsResponse.data)
                    .sortedWith(
                        compareByDescending<AppointmentResponse> { it.appointment_date }
                            .thenByDescending { it.appointment_time }
                    )

                if (appointmentsList.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@AppointmentsActivity,
                            "No appointments found",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                // Pre-load all services (one query instead of many)
                val allServicesResponse = supabaseClient.from("services").select()
                val allServices = json.decodeFromString<List<Service>>(allServicesResponse.data)
                val servicesMap = allServices.associateBy { it.service_id }
                Log.d("AppointmentsActivity", "Loaded ${allServices.size} services")

                // Pre-load all appointment_services for these appointments (one query)
                val appointmentIds = appointmentsList.map { it.appointment_id }
                val allAppointmentServicesResponse = supabaseClient.from("appointment_services")
                    .select()

                val allAppointmentServices = json.decodeFromString<List<AppointmentService>>(allAppointmentServicesResponse.data)
                val appointmentServicesMap = allAppointmentServices
                    .filter { it.appointment_id in appointmentIds }
                    .groupBy { it.appointment_id }
                Log.d("AppointmentsActivity", "Loaded appointment services")

                // Pre-load all payments for these appointments (one query)
                val allPaymentsResponse = supabaseClient.from("payments").select()
                val allPayments = json.decodeFromString<List<Payment>>(allPaymentsResponse.data)
                val paymentsMap = allPayments
                    .filter { it.appointment_id in appointmentIds }
                    .groupBy { it.appointment_id }
                Log.d("AppointmentsActivity", "Loaded payments")

                // For each appointment, build details from cached data
                val detailedAppointments = appointmentsList.map { appointment ->
                    // Get services for this appointment
                    val appointmentServices = appointmentServicesMap[appointment.appointment_id] ?: emptyList()
                    val serviceNames = appointmentServices.mapNotNull { appointmentService ->
                        servicesMap[appointmentService.service_id]?.service_name
                    }

                    // Get payment method
                    val paymentMethod = paymentsMap[appointment.appointment_id]?.firstOrNull()?.payment_method

                    AppointmentWithDetails(
                        appointment = appointment,
                        services = serviceNames,
                        paymentMethod = paymentMethod
                    )
                }

                Log.d("AppointmentsActivity", "Loaded ${detailedAppointments.size} appointments")

                withContext(Dispatchers.Main) {
                    appointments.clear()
                    appointments.addAll(detailedAppointments)
                    appointmentsAdapter.notifyDataSetChanged()
                }

            } catch (e: Exception) {
                Log.e("AppointmentsActivity", "Error loading appointments: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@AppointmentsActivity,
                        "Error loading appointments: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun showCancelDialog(appointment: AppointmentWithDetails) {
        AlertDialog.Builder(this)
            .setTitle("Cancel Appointment")
            .setMessage("Are you sure you want to cancel this appointment? This action cannot be undone.")
            .setPositiveButton("Yes, Cancel") { _, _ ->
                cancelAppointment(appointment)
            }
            .setNegativeButton("No, Keep It", null)
            .show()
    }

    private fun cancelAppointment(appointment: AppointmentWithDetails) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("AppointmentsActivity", "Cancelling appointment ${appointment.appointment.appointment_id}")

                // Update appointment status to 'cancelled'
                supabaseClient.from("appointments")
                    .update(mapOf("status" to "cancelled")) {
                        filter {
                            eq("appointment_id", appointment.appointment.appointment_id)
                        }
                    }

                Log.d("AppointmentsActivity", "Appointment cancelled successfully")

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@AppointmentsActivity,
                        "Appointment cancelled successfully",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Reload appointments
                    loadAppointments()
                }

            } catch (e: Exception) {
                Log.e("AppointmentsActivity", "Error cancelling appointment: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@AppointmentsActivity,
                        "Error cancelling appointment: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun showAppointmentDetails(appointment: AppointmentWithDetails) {
        val details = buildString {
            append("Appointment #${appointment.appointment.appointment_id}\n\n")
            append("📅 Date: ${appointment.appointment.appointment_date}\n")
            append("🕐 Time: ${appointment.appointment.appointment_time}\n")
            append("🏭 Machine: ${appointment.appointment.machine ?: "Not assigned"}\n")
            append("🧺 Services: ${appointment.services.joinToString(", ")}\n")
            append("🚚 Delivery: ${appointment.appointment.delivery_method ?: "N/A"}\n")
            append("💳 Payment: ${appointment.paymentMethod ?: "N/A"}\n")
            append("📊 Status: ${appointment.appointment.status}\n")

            if (appointment.appointment.detergent_option == "store") {
                append("🧴 Detergent: Store (₱${appointment.appointment.detergent_charge?.toInt() ?: 0})\n")
            }

            if (appointment.appointment.total_weight != null) {
                append("⚖️ Weight: ${appointment.appointment.total_weight} kg\n")
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Appointment Details")
            .setMessage(details)
            .setPositiveButton("OK", null)
            .show()
    }
}