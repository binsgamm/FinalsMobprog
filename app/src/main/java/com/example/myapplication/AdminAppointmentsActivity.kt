package com.example.myapplication

import android.app.Dialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json

class AdminAppointmentsActivity : AppCompatActivity() {

    private lateinit var rvAdmin: RecyclerView
    private val adminList = mutableListOf<AdminAppointmentDetails>()
    private lateinit var adapter: AdminAppointmentsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_appointments)

        // Initialize UI
        rvAdmin = findViewById(R.id.rvAdminAppointments)
        rvAdmin.layoutManager = LinearLayoutManager(this)

        adapter = AdminAppointmentsAdapter(
            adminList,
            onImageClick = { base64 -> showFullImage(base64) },
            onStatusUpdate = { id, newStatus -> updateAppointmentStatus(id, newStatus) }
        )
        rvAdmin.adapter = adapter

        // --- SIGN OUT BUTTON LOGIC ---
        findViewById<MaterialButton>(R.id.btnAdminBack).setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    SupabaseManager.client.auth.signOut()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@AdminAppointmentsActivity, "Logged out", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@AdminAppointmentsActivity, LogInActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                } catch (e: Exception) { Log.e("Admin", "Signout error") }
            }
        }

        fetchAllAppointments()
    }

    private fun fetchAllAppointments() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = Json { ignoreUnknownKeys = true }

                // 1. Fetch data from all tables
                val appRes = SupabaseManager.client.from("appointments").select()
                val appointments = json.decodeFromString<List<AppointmentResponse>>(appRes.data)

                val custRes = SupabaseManager.client.from("customers").select()
                val customersMap = json.decodeFromString<List<Customer>>(custRes.data).associateBy { it.customer_id }

                val servRes = SupabaseManager.client.from("services").select()
                val servicesMap = json.decodeFromString<List<Service>>(servRes.data).associateBy { it.service_id }

                val juncRes = SupabaseManager.client.from("appointment_services").select()
                val junctionMap = json.decodeFromString<List<AppointmentService>>(juncRes.data).groupBy { it.appointment_id }

                val payRes = SupabaseManager.client.from("payments").select()
                val paymentsMap = json.decodeFromString<List<Payment>>(payRes.data).associateBy { it.appointment_id }

                // 2. Map everything together
                val combined = appointments.map { app ->
                    val customer = customersMap[app.customer_id]
                    val cName = "${customer?.f_name ?: "Unknown"} ${customer?.l_name ?: ""}"
                    val cPhone = customer?.phone_num ?: "N/A"

                    val sNames = junctionMap[app.appointment_id]?.mapNotNull {
                        servicesMap[it.service_id]?.service_name
                    }?.joinToString(", ") ?: "No services"

                    AdminAppointmentDetails(app, cName, cPhone, sNames, paymentsMap[app.appointment_id])
                }.sortedByDescending { it.appointment.appointment_id }

                withContext(Dispatchers.Main) {
                    adminList.clear()
                    adminList.addAll(combined)
                    adapter.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                Log.e("Admin", "Fetch error: ${e.message}")
            }
        }
    }

    private fun updateAppointmentStatus(id: Int, status: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                SupabaseManager.client.from("appointments").update(mapOf("status" to status)) {
                    filter { eq("appointment_id", id) }
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AdminAppointmentsActivity, "Status updated", Toast.LENGTH_SHORT).show()
                    fetchAllAppointments()
                }
            } catch (e: Exception) { Log.e("Admin", "Update error") }
        }
    }

    private fun showFullImage(base64String: String) {
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_full_image)
        val ivFullImage = dialog.findViewById<ImageView>(R.id.ivFullImage)
        val btnClose = dialog.findViewById<MaterialButton>(R.id.btnCloseImage)
        try {
            val bytes = Base64.decode(base64String, Base64.DEFAULT)
            ivFullImage.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
        } catch (e: Exception) { Toast.makeText(this, "Image error", Toast.LENGTH_SHORT).show() }
        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
}