package com.example.myapplication

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

class AppointmentsAdapter(
    private val appointments: List<AppointmentWithDetails>,
    private val onCancelClick: (AppointmentWithDetails) -> Unit,
    private val onViewDetailsClick: (AppointmentWithDetails) -> Unit
) : RecyclerView.Adapter<AppointmentsAdapter.AppointmentViewHolder>() {

    class AppointmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvAppointmentId: TextView = itemView.findViewById(R.id.tvAppointmentId)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvDateTime: TextView = itemView.findViewById(R.id.tvDateTime)
        val tvMachine: TextView = itemView.findViewById(R.id.tvMachine)
        val tvServices: TextView = itemView.findViewById(R.id.tvServices)
        val tvDelivery: TextView = itemView.findViewById(R.id.tvDelivery)
        val btnViewDetails: MaterialButton = itemView.findViewById(R.id.btnViewDetails)
        val btnCancel: MaterialButton = itemView.findViewById(R.id.btnCancel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_appointment, parent, false)
        return AppointmentViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
        val appointmentWithDetails = appointments[position]
        val appointment = appointmentWithDetails.appointment

        // Appointment ID
        holder.tvAppointmentId.text = "Appointment #${appointment.appointment_id}"

        // Status with color coding
        holder.tvStatus.text = appointment.status.uppercase()
        when (appointment.status.lowercase()) {
            "pending" -> {
                holder.tvStatus.setBackgroundColor(Color.parseColor("#FF9800")) // Orange
            }
            "completed" -> {
                holder.tvStatus.setBackgroundColor(Color.parseColor("#4CAF50")) // Green
            }
            "cancelled" -> {
                holder.tvStatus.setBackgroundColor(Color.parseColor("#F44336")) // Red
            }
            "in_progress" -> {
                holder.tvStatus.setBackgroundColor(Color.parseColor("#2196F3")) // Blue
            }
            else -> {
                holder.tvStatus.setBackgroundColor(Color.parseColor("#9E9E9E")) // Gray
            }
        }

        // Date & Time - format nicely
        holder.tvDateTime.text = formatDateTime(
            appointment.appointment_date,
            appointment.appointment_time
        )

        // Machine
        holder.tvMachine.text = appointment.machine ?: "Not assigned"

        // Services
        holder.tvServices.text = if (appointmentWithDetails.services.isNotEmpty()) {
            appointmentWithDetails.services.joinToString(", ")
        } else {
            "No services"
        }

        // Delivery
        holder.tvDelivery.text = appointment.delivery_method ?: "N/A"

        // Cancel button visibility
        // Only show cancel button for pending appointments
        if (appointment.status.lowercase() == "pending") {
            holder.btnCancel.visibility = View.VISIBLE
            holder.btnCancel.setOnClickListener {
                onCancelClick(appointmentWithDetails)
            }
        } else {
            holder.btnCancel.visibility = View.GONE
        }

        // View details button
        holder.btnViewDetails.setOnClickListener {
            onViewDetailsClick(appointmentWithDetails)
        }
    }

    override fun getItemCount(): Int = appointments.size

    private fun formatDateTime(date: String, time: String): String {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val outputDateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
            val outputTimeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

            val dateObj = dateFormat.parse(date)
            val timeObj = timeFormat.parse(time)

            val formattedDate = if (dateObj != null) outputDateFormat.format(dateObj) else date
            val formattedTime = if (timeObj != null) outputTimeFormat.format(timeObj) else time

            "$formattedDate at $formattedTime"
        } catch (e: Exception) {
            "$date at $time"
        }
    }
}