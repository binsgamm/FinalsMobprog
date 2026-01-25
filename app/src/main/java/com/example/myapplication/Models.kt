package com.example.myapplication

import kotlinx.serialization.Serializable

// Customer data class with all fields
@Serializable
data class Customer(
    val customer_id: Int? = null,
    val f_name: String,
    val m_name: String? = null,
    val l_name: String,
    val email: String? = null,
    val phone_num: String? = null,
    val address: String? = null,
    val user_id: String? = null,
    val created_at: String? = null
)

// For inserting new customers during signup
@Serializable
data class CustomerInsert(
    val user_id: String,
    val f_name: String,
    val m_name: String? = null,
    val l_name: String,
    val email: String,
    val phone_num: String,
    val address: String
)

// Appointment data classes
@Serializable
data class AppointmentInsert(
    val customer_id: Int,
    val appointment_date: String,
    val appointment_time: String,
    val status: String = "pending",
    val delivery_method: String? = null,
    val machine: String? = null
)

@Serializable
data class AppointmentResponse(
    val appointment_id: Int,
    val customer_id: Int,
    val appointment_date: String,
    val appointment_time: String,
    val status: String,
    val delivery_method: String? = null,
    val machine: String? = null,
    val total_weight: Double? = null
)

// Service data class
@Serializable
data class Service(
    val service_id: Int,
    val service_name: String,
    val price_per_kilo: Double,  // Changed from String to Double
    val description: String? = null
)

// Appointment services junction table
@Serializable
data class AppointmentServiceInsert(
    val appointment_id: Int,
    val service_id: Int,
    val applied_price: Double
)

@Serializable
data class AppointmentService(
    val id: Int? = null,
    val appointment_id: Int,
    val service_id: Int,
    val applied_price: Double,
    val subtotal: Double? = null
)

// Payment data classes
@Serializable
data class PaymentInsert(
    val appointment_id: Int,
    val payment_method: String,
    val payment_status: String = "pending",
    val amount: Double
)

@Serializable
data class Payment(
    val payment_id: Int? = null,
    val appointment_id: Int,
    val payment_type: String? = null,
    val amount: Double,
    val payment_method: String? = null,
    val proof_image: String? = null,
    val payment_status: String? = null,
    val verification_date: String? = null
)

// User data for signup
data class UserData(
    val firstName: String,
    val middleName: String,
    val lastName: String,
    val address: String,
    val phone: String,
    val email: String,
    val password: String
)