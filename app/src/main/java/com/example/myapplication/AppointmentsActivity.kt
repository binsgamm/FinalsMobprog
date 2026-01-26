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
import io.github.jan.supabase.postgrest.query.Order
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

                // Force initialization of supabaseClient
                @Suppress("UNUSED_VARIABLE")
                val client = supabaseClient
                Log.d("AppointmentsActivity", "Supabase client initialized")

                // Wait for session to load
                delay(2500)

                val currentUser = supabaseClient.auth.currentUserOrNull()
                Log.d("AppointmentsActivity", "Session check - User: ${currentUser?.id}")

                userId = intent.getStringExtra("USER_ID") ?: currentUser?.id

                if (userId == null) {
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

                // Get customer_id
                val customerResponse = supabaseClient.from("customers")
                    .select() {
                        filter {
                            eq("user_id", userId!!)
                        }
                    }

                val json = Json { ignoreUnknownKeys = true }
                val customers = json.decodeFromString<List<Customer>>(customerResponse.data)

                if (customers.isNotEmpty()) {
                    customerId = customers[0].customer_id
                    Log.d("AppointmentsActivity", "Customer ID: $customerId")

                    loadAppointments()
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@AppointmentsActivity,
                            "Customer profile not found",
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
                    .select() {
                        filter {
                            eq("customer_id", customerId!!)
                        }
                        order("appointment_date", Order.DESCENDING)
                        order("appointment_time", Order.DESCENDING)
                    }

                Log.d("AppointmentsActivity", "Appointments response: ${appointmentsResponse.data}")

                val json = Json { ignoreUnknownKeys = true }
                val appointmentsList = json.decodeFromString<List<AppointmentResponse>>(appointmentsResponse.data)

                // For each appointment, load services and payment info
                val detailedAppointments = mutableListOf<AppointmentWithDetails>()

                for (appointment in appointmentsList) {
                    // Get services for this appointment
                    val servicesResponse = supabaseClient.from("appointment_services")
                        .select() {
                            filter {
                                eq("appointment_id", appointment.appointment_id)
                            }
                        }

                    val appointmentServices = json.decodeFromString<List<AppointmentService>>(servicesResponse.data)

                    // Get service names
                    val serviceNames = mutableListOf<String>()
                    for (appointmentService in appointmentServices) {
                        val serviceResponse = supabaseClient.from("services")
                            .select() {
                                filter {
                                    eq("service_id", appointmentService.service_id)
                                }
                            }

                        val services = json.decodeFromString<List<Service>>(serviceResponse.data)
                        if (services.isNotEmpty()) {
                            serviceNames.add(services[0].service_name)
                        }
                    }

                    // Get payment method
                    val paymentResponse = supabaseClient.from("payments")
                        .select() {
                            filter {
                                eq("appointment_id", appointment.appointment_id)
                            }
                        }

                    val payments = json.decodeFromString<List<Payment>>(paymentResponse.data)
                    val paymentMethod = payments.firstOrNull()?.payment_method

                    detailedAppointments.add(
                        AppointmentWithDetails(
                            appointment = appointment,
                            services = serviceNames,
                            paymentMethod = paymentMethod
                        )
                    )
                }

                Log.d("AppointmentsActivity", "Loaded ${detailedAppointments.size} appointments")

                withContext(Dispatchers.Main) {
                    appointments.clear()
                    appointments.addAll(detailedAppointments)
                    appointmentsAdapter.notifyDataSetChanged()

                    if (appointments.isEmpty()) {
                        Toast.makeText(
                            this@AppointmentsActivity,
                            "No appointments found",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
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