package com.example.myapplication

import kotlinx.serialization.Serializable

// Admin data class
@Serializable
data class Admin(
    val admin_id: Int,
    val user_id: String,
    val f_name: String,
    val l_name: String,
    val email: String? = null,
    val created_at: String? = null
)

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
    val machine: String? = null,
    val detergent_option: String = "own", // "own" or "store"
    val detergent_charge: Double = 0.0
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
    val total_weight: Double? = null,
    val detergent_option: String? = null,
    val detergent_charge: Double? = null
)

// Full appointment details with services
data class AppointmentWithDetails(
    val appointment: AppointmentResponse,
    val services: List<String>,
    val paymentMethod: String?
)

// Service data class
@Serializable
data class Service(
    val service_id: Int,
    val service_name: String,
    val price_services: Double,  // Flat rate price for service (up to 8kg)
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
    val amount: Double,
    val payment_method: String?,
    val proof_image: String? = null,
    val payment_status: String = "pending", // Correct: matches schema
    val remaining_balance: Double = 0.0     // Correct: matches schema
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

// Detergent pricing
@Serializable
data class DetergentPricing(
    val id: Int? = null,
    val detergent_name: String,
    val price_per_load: Double,
    val is_active: Boolean = true
)

// Pricing rules for weight-based charges
@Serializable
data class PricingRule(
    val id: Int? = null,
    val rule_name: String,
    val weight_threshold_kg: Double,
    val additional_price_per_kilo: Double,
    val is_active: Boolean = true
)

// Payment configuration
@Serializable
data class PaymentConfig(
    val id: Int? = null,
    val payment_type: String,
    val requires_down_payment: Boolean,
    val down_payment_percentage: Double,
    val min_down_payment_amount: Double,
    val is_active: Boolean = true
)

// Cost calculation result
@Serializable
data class CostCalculation(
    val base_cost: Double,
    val weight_surcharge: Double,
    val detergent_charge: Double,
    val total_cost: Double,
    val required_down_payment: Double
)