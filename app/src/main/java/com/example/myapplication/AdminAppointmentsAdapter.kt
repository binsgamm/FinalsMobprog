package com.example.myapplication

import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class AdminAppointmentsAdapter(
    private val items: MutableList<AdminAppointmentDetails>,
    private val onImageClick: (String) -> Unit,
    private val onStatusUpdate: (Int, String) -> Unit
) : RecyclerView.Adapter<AdminAppointmentsAdapter.ViewHolder>() {

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tvStatus: TextView = v.findViewById(R.id.tvAdminStatus)
        val tvPaymentMethod: TextView = v.findViewById(R.id.tvAdminPaymentMethod)
        val tvName: TextView = v.findViewById(R.id.tvAdminCustomerName)
        val tvPhone: TextView = v.findViewById(R.id.tvAdminCustomerPhone)
        val tvDate: TextView = v.findViewById(R.id.tvAdminAppointmentDate)
        val tvServices: TextView = v.findViewById(R.id.tvAdminServices)
        val ivPayment: ImageView = v.findViewById(R.id.ivProofOfPayment)

        val btnPending: MaterialButton = v.findViewById(R.id.btnSetPending)
        val btnInProgress: MaterialButton = v.findViewById(R.id.btnSetInProgress)
        val btnCompleted: MaterialButton = v.findViewById(R.id.btnSetCompleted)
        val btnCancelled: MaterialButton = v.findViewById(R.id.btnSetCancelled)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_appointment, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        // Basic Info
        holder.tvName.text = item.customerName
        holder.tvPhone.text = item.customerPhone
        holder.tvServices.text = item.services
        holder.tvStatus.text = item.appointment.status.uppercase()
        holder.tvDate.text = "${item.appointment.appointment_date} | ${item.appointment.appointment_time}"

        // --- PAYMENT METHOD LOGIC ---
        val method = item.payment?.payment_method ?: "Not Specified"
        holder.tvPaymentMethod.text = method

        // Color coding for Payment Method
        if (method.contains("Cash", ignoreCase = true)) {
            holder.tvPaymentMethod.setTextColor(Color.parseColor("#4CAF50")) // Green for Cash
        } else {
            holder.tvPaymentMethod.setTextColor(Color.parseColor("#2d58a9")) // Blue for E-Wallet
        }

        // Proof of Payment Image
        val proof = item.payment?.proof_image
        if (!proof.isNullOrEmpty()) {
            try {
                val bytes = Base64.decode(proof, Base64.DEFAULT)
                holder.ivPayment.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
                holder.ivPayment.setOnClickListener { onImageClick(proof) }
            } catch (e: Exception) {
                holder.ivPayment.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        } else {
            holder.ivPayment.setImageResource(android.R.drawable.ic_menu_gallery)
            holder.ivPayment.setOnClickListener(null)
        }

        // Status Buttons
        holder.btnPending.setOnClickListener { onStatusUpdate(item.appointment.appointment_id, "pending") }
        holder.btnInProgress.setOnClickListener { onStatusUpdate(item.appointment.appointment_id, "in progress") }
        holder.btnCompleted.setOnClickListener { onStatusUpdate(item.appointment.appointment_id, "completed") }
        holder.btnCancelled.setOnClickListener { onStatusUpdate(item.appointment.appointment_id, "cancelled") }
    }

    override fun getItemCount() = items.size
}