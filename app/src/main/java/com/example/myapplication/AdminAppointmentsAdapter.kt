package com.example.myapplication

import android.graphics.BitmapFactory
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

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvServiceType: TextView = view.findViewById(R.id.tvAdminServiceType)
        val tvStatus: TextView = view.findViewById(R.id.tvAdminStatus)
        val tvCustomerName: TextView = view.findViewById(R.id.tvAdminCustomerName)
        val tvDate: TextView = view.findViewById(R.id.tvAdminAppointmentDate)
        val tvMachine: TextView = view.findViewById(R.id.tvAdminMachineInfo)
        val ivPayment: ImageView = view.findViewById(R.id.ivProofOfPayment)
        val btnPending: MaterialButton = view.findViewById(R.id.btnSetPending)
        val btnInProgress: MaterialButton = view.findViewById(R.id.btnSetInProgress)
        val btnCompleted: MaterialButton = view.findViewById(R.id.btnSetCompleted)
        val btnCancelled: MaterialButton = view.findViewById(R.id.btnSetCancelled)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_appointment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.tvServiceType.text = item.services
        holder.tvCustomerName.text = item.customerName
        holder.tvStatus.text = item.appointment.status.uppercase()
        holder.tvDate.text = "${item.appointment.appointment_date} | ${item.appointment.appointment_time}"
        holder.tvMachine.text = "Machine: ${item.appointment.machine ?: "TBD"}"

        // Handle Image Thumbnail
        val proofBase64 = item.payment?.proof_image
        if (!proofBase64.isNullOrEmpty()) {
            try {
                val imageBytes = Base64.decode(proofBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                holder.ivPayment.setImageBitmap(bitmap)
                holder.ivPayment.scaleType = ImageView.ScaleType.CENTER_CROP

                // IMAGE CLICK LOGIC
                holder.ivPayment.setOnClickListener { onImageClick(proofBase64) }
            } catch (e: Exception) {
                holder.ivPayment.setImageResource(android.R.drawable.ic_menu_report_image)
            }
        } else {
            holder.ivPayment.setImageResource(android.R.drawable.ic_menu_gallery)
            holder.ivPayment.scaleType = ImageView.ScaleType.CENTER_INSIDE
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