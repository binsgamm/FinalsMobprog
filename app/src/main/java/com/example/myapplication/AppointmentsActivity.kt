package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*

class AppointmentsActivity : AppCompatActivity() {

    private lateinit var rvAppointments: RecyclerView
    private lateinit var appointmentsAdapter: AppointmentsAdapter
    private val appointments = mutableListOf<AppointmentWithDetails>()
    private var userId: String? = null
    private var customerId: Int? = null

    // Define the custom status order: In Progress -> Pending -> Completed -> Cancelled
    private val statusPriorityMap = mapOf(
        "in progress" to 1,
        "pending" to 2,
        "completed" to 3,
        "cancelled" to 4
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_appointments)
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
                userId = intent.getStringExtra("USER_ID") ?: SupabaseManager.client.auth.currentUserOrNull()?.id
                if (userId == null) {
                    delay(1500) // Wait for session retry
                    userId = SupabaseManager.client.auth.currentUserOrNull()?.id
                }

                if (userId != null) {
                    val res = SupabaseManager.client.from("customers").select { filter { eq("user_id", userId!!) } }
                    val customers = Json { ignoreUnknownKeys = true }.decodeFromString<List<Customer>>(res.data)
                    if (customers.isNotEmpty()) {
                        customerId = customers[0].customer_id
                        loadAppointments()
                    }
                }
            } catch (e: Exception) { Log.e("AA", "User load error") }
        }
    }

    private fun loadAppointments() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = SupabaseManager.client.from("appointments").select { filter { eq("customer_id", customerId!!) } }
                val appointmentsList = Json { ignoreUnknownKeys = true }.decodeFromString<List<AppointmentResponse>>(response.data)

                // --- AUTO-CANCEL LOGIC ---
                val now = Calendar.getInstance()
                val pastPending = appointmentsList.filter { appointment ->
                    if (appointment.status.lowercase() != "pending") return@filter false
                    try {
                        val appDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(appointment.appointment_date)
                        val appTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).parse(appointment.appointment_time)
                        if (appDate != null && appTime != null) {
                            val appDateTime = Calendar.getInstance()
                            appDateTime.time = appDate
                            appDateTime.set(Calendar.HOUR_OF_DAY, appTime.hours)
                            appDateTime.set(Calendar.MINUTE, appTime.minutes)
                            appDateTime.timeInMillis < now.timeInMillis
                        } else false
                    } catch (e: Exception) { false }
                }

                if (pastPending.isNotEmpty()) {
                    pastPending.forEach {
                        SupabaseManager.client.from("appointments").update(mapOf("status" to "cancelled")) { filter { eq("appointment_id", it.appointment_id) } }
                    }
                }

                // --- FETCH FRESH DATA AFTER AUTO-CANCEL ---
                val freshRes = SupabaseManager.client.from("appointments").select { filter { eq("customer_id", customerId!!) } }
                val freshList = Json { ignoreUnknownKeys = true }.decodeFromString<List<AppointmentResponse>>(freshRes.data)

                // --- IMPROVED SORTING LOGIC ---
                // 1st: Sort by Status Priority Map
                // 2nd: Sort by Date (Closest first)
                // 3rd: Sort by Time (Closest first)
                val sorted = freshList.sortedWith(
                    compareBy<AppointmentResponse> {
                        statusPriorityMap[it.status.lowercase().trim()] ?: 5
                    }
                        .thenBy { it.appointment_date }
                        .thenBy { it.appointment_time }
                )

                // --- OPTIMIZED PRE-LOADING ---
                val servicesRes = SupabaseManager.client.from("services").select()
                val servicesMap = Json { ignoreUnknownKeys = true }.decodeFromString<List<Service>>(servicesRes.data).associateBy { it.service_id }

                val appServicesRes = SupabaseManager.client.from("appointment_services").select()
                val appServicesMap = Json { ignoreUnknownKeys = true }.decodeFromString<List<AppointmentService>>(appServicesRes.data).groupBy { it.appointment_id }

                val paymentsRes = SupabaseManager.client.from("payments").select()
                val paymentsMap = Json { ignoreUnknownKeys = true }.decodeFromString<List<Payment>>(paymentsRes.data).associateBy { it.appointment_id }

                val detailed = sorted.map { app ->
                    val sNames = appServicesMap[app.appointment_id]?.mapNotNull { servicesMap[it.service_id]?.service_name } ?: emptyList()
                    AppointmentWithDetails(app, sNames, paymentsMap[app.appointment_id]?.payment_method)
                }

                withContext(Dispatchers.Main) {
                    appointments.clear()
                    appointments.addAll(detailed)
                    appointmentsAdapter.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                Log.e("AA", "Load Error: ${e.message}")
            }
        }
    }

    private fun showCancelDialog(appointment: AppointmentWithDetails) {
        AlertDialog.Builder(this)
            .setTitle("Cancel Appointment")
            .setMessage("Are you sure you want to cancel this appointment?")
            .setPositiveButton("Yes") { _, _ -> cancelAppointment(appointment) }
            .setNegativeButton("No", null)
            .show()
    }

    private fun cancelAppointment(appointment: AppointmentWithDetails) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                SupabaseManager.client.from("appointments").update(mapOf("status" to "cancelled")) {
                    filter { eq("appointment_id", appointment.appointment.appointment_id) }
                }
                loadAppointments() // Refresh list
            } catch (e: Exception) {
                Log.e("AA", "Cancel error")
            }
        }
    }

    private fun showAppointmentDetails(appointment: AppointmentWithDetails) {
        val details = buildString {
            append("Date: ${appointment.appointment.appointment_date}\n")
            append("Time: ${appointment.appointment.appointment_time}\n")
            append("Status: ${appointment.appointment.status.uppercase()}\n")
            append("Services: ${appointment.services.joinToString(", ")}\n")
            append("Payment: ${appointment.paymentMethod ?: "N/A"}")
        }
        AlertDialog.Builder(this)
            .setTitle("Appointment Details")
            .setMessage(details)
            .setPositiveButton("OK", null)
            .show()
    }
}