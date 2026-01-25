package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.widget.CalendarView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    // Service Buttons
    private lateinit var btnWashFold: MaterialButton
    private lateinit var btnDryCleaning: MaterialButton
    private lateinit var btnIroning: MaterialButton
    private lateinit var btnExpress: MaterialButton

    // Machine Buttons
    private lateinit var btnMachine1: MaterialButton
    private lateinit var btnMachine2: MaterialButton
    private lateinit var btnMachine3: MaterialButton
    private lateinit var btnMachine4: MaterialButton
    private lateinit var btnMachine5: MaterialButton
    private lateinit var btnMachine6: MaterialButton

    // Calendar and Time
    private lateinit var calendarView: CalendarView
    private lateinit var btnTime9am: MaterialButton
    private lateinit var btnTime11am: MaterialButton
    private lateinit var btnTime1pm: MaterialButton
    private lateinit var btnTime3pm: MaterialButton
    private lateinit var btnTime5pm: MaterialButton
    private lateinit var btnTime7pm: MaterialButton

    // Delivery & Payment
    private lateinit var toggleGroupDelivery: MaterialButtonToggleGroup
    private lateinit var btnMethodDropOff: MaterialButton
    private lateinit var btnMethodPickup: MaterialButton

    private lateinit var toggleGroupPayment: MaterialButtonToggleGroup
    private lateinit var btnPaymentCash: MaterialButton
    private lateinit var btnPaymentEWallet: MaterialButton

    // Book Button
    private lateinit var btnBookAppointment: MaterialButton

    // Selected values
    private val selectedServices = mutableListOf<Int>() // Service IDs
    private var selectedMachine: String? = null
    private var selectedDate: String? = null
    private var selectedTime: String? = null
    private var selectedDeliveryMethod: String? = null
    private var selectedPaymentMethod: String? = null

    private var userId: String? = null
    private var customerId: Int? = null
    private var isCustomerDataLoaded = false

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
        setContentView(R.layout.activity_main)

        Log.d("MainActivity", "=== MAIN ACTIVITY CREATED ===")
        Log.d("MainActivity", "Received USER_ID from intent: ${intent.getStringExtra("USER_ID")}")

        initializeViews()
        setupListeners()
        loadUserData()
    }

    private fun initializeViews() {
        // Service buttons
        btnWashFold = findViewById(R.id.btnWashFold)
        btnDryCleaning = findViewById(R.id.btnDryCleaning)
        btnIroning = findViewById(R.id.btnIroning)
        btnExpress = findViewById(R.id.btnExpress)

        // Machine buttons
        btnMachine1 = findViewById(R.id.btnMachine1)
        btnMachine2 = findViewById(R.id.btnMachine2)
        btnMachine3 = findViewById(R.id.btnMachine3)
        btnMachine4 = findViewById(R.id.btnMachine4)
        btnMachine5 = findViewById(R.id.btnMachine5)
        btnMachine6 = findViewById(R.id.btnMachine6)

        // Calendar and time
        calendarView = findViewById(R.id.calendarView)
        btnTime9am = findViewById(R.id.btnTime9am)
        btnTime11am = findViewById(R.id.btnTime11am)
        btnTime1pm = findViewById(R.id.btnTime1pm)
        btnTime3pm = findViewById(R.id.btnTime3pm)
        btnTime5pm = findViewById(R.id.btnTime5pm)
        btnTime7pm = findViewById(R.id.btnTime7pm)

        // Delivery & Payment
        toggleGroupDelivery = findViewById(R.id.toggleGroupDelivery)
        btnMethodDropOff = findViewById(R.id.btnMethodDropOff)
        btnMethodPickup = findViewById(R.id.btnMethodPickup)

        toggleGroupPayment = findViewById(R.id.toggleGroupPayment)
        btnPaymentCash = findViewById(R.id.btnPaymentCash)
        btnPaymentEWallet = findViewById(R.id.btnPaymentEWallet)

        // Book button
        btnBookAppointment = findViewById(R.id.btnBookAppointment)

        // Disable book button until customer data is loaded
        btnBookAppointment.isEnabled = false
        btnBookAppointment.text = "Loading..."
    }

    private fun setupListeners() {
        Log.d("MainActivity", "=== SETTING UP LISTENERS ===")

        // Service buttons - multiple selection
        Log.d("MainActivity", "Setting up service buttons...")
        setupServiceButton(btnWashFold, 1) // Assuming service_id 1
        setupServiceButton(btnDryCleaning, 2) // Assuming service_id 2
        setupServiceButton(btnIroning, 3) // Assuming service_id 3
        setupServiceButton(btnExpress, 4) // Assuming service_id 4
        Log.d("MainActivity", "Service buttons set up complete")

        // Machine buttons - single selection
        val machineButtons = listOf(
            btnMachine1 to "Machine 1",
            btnMachine2 to "Machine 2",
            btnMachine3 to "Machine 3",
            btnMachine4 to "Machine 4",
            btnMachine5 to "Machine 5",
            btnMachine6 to "Machine 6"
        )

        machineButtons.forEach { (button, machineName) ->
            button.setOnClickListener {
                // Deselect all other machines
                machineButtons.forEach { (btn, _) ->
                    btn.isChecked = btn == button
                }
                selectedMachine = machineName
                Log.d("MainActivity", "Selected machine: $machineName")
            }
        }

        // Calendar
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)
            selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            Log.d("MainActivity", "Selected date: $selectedDate")
        }

        // Time buttons - single selection
        val timeButtons = listOf(
            btnTime9am to "09:00:00",
            btnTime11am to "11:00:00",
            btnTime1pm to "13:00:00",
            btnTime3pm to "15:00:00",
            btnTime5pm to "17:00:00",
            btnTime7pm to "19:00:00"
        )

        timeButtons.forEach { (button, time) ->
            button.setOnClickListener {
                // Deselect all other times
                timeButtons.forEach { (btn, _) ->
                    btn.isChecked = btn == button
                }
                selectedTime = time
                Log.d("MainActivity", "Selected time: $time")
            }
        }

        // Delivery method
        toggleGroupDelivery.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                selectedDeliveryMethod = when (checkedId) {
                    R.id.btnMethodDropOff -> "Drop Off"
                    R.id.btnMethodPickup -> "Pickup"
                    else -> null
                }
                Log.d("MainActivity", "Selected delivery: $selectedDeliveryMethod")
            }
        }

        // Payment method
        toggleGroupPayment.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                selectedPaymentMethod = when (checkedId) {
                    R.id.btnPaymentCash -> "Cash"
                    R.id.btnPaymentEWallet -> "E-Wallet"
                    else -> null
                }
                Log.d("MainActivity", "Selected payment: $selectedPaymentMethod")
            }
        }

        // Book appointment button
        btnBookAppointment.setOnClickListener {
            if (validateInputs()) {
                bookAppointment()
            }
        }
    }

    private fun setupServiceButton(button: MaterialButton, serviceId: Int) {
        // Make button checkable so it can toggle visual state
        button.isCheckable = true

        button.setOnClickListener {
            // MaterialButton handles toggle automatically when isCheckable = true
            // Just update the selected services list based on checked state
            if (button.isChecked) {
                if (!selectedServices.contains(serviceId)) {
                    selectedServices.add(serviceId)
                }
            } else {
                selectedServices.remove(serviceId)
            }

            Log.d("MainActivity", "Button ${button.text} clicked")
            Log.d("MainActivity", "Button checked state: ${button.isChecked}")
            Log.d("MainActivity", "Selected services: $selectedServices")
        }
    }

    private fun loadUserData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("MainActivity", "=== LOADING USER DATA ===")

                // CRITICAL FIX: Force initialization of supabaseClient first
                // This triggers the lazy property to create the client
                @Suppress("UNUSED_VARIABLE")
                val client = supabaseClient
                Log.d("MainActivity", "Supabase client initialized")

                // Now wait for the session to load from storage
                Log.d("MainActivity", "Waiting for session to load from storage...")
                delay(2500) // Wait for async session loading

                // Verify session is loaded
                val currentUser = supabaseClient.auth.currentUserOrNull()
                Log.d("MainActivity", "Session check - User from auth: ${currentUser?.id}")

                // Get user ID from intent or session
                userId = intent.getStringExtra("USER_ID")
                    ?: currentUser?.id

                Log.d("MainActivity", "Final userId: $userId")

                if (userId == null) {
                    withContext(Dispatchers.Main) {
                        btnBookAppointment.isEnabled = false
                        btnBookAppointment.text = "Login Required"
                        Toast.makeText(
                            this@MainActivity,
                            "Please log in first",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@launch
                }

                // Get customer_id from customers table
                Log.d("MainActivity", "Fetching customer data for user_id: $userId")
                val response = supabaseClient.from("customers")
                    .select() {
                        filter {
                            eq("user_id", userId!!)
                        }
                    }

                Log.d("MainActivity", "Customer query response: ${response.data}")

                val json = Json { ignoreUnknownKeys = true }
                val customers = json.decodeFromString<List<Customer>>(response.data)

                if (customers.isNotEmpty()) {
                    customerId = customers[0].customer_id
                    isCustomerDataLoaded = true

                    Log.d("MainActivity", "=== CUSTOMER DATA LOADED ===")
                    Log.d("MainActivity", "Customer ID: $customerId")
                    Log.d("MainActivity", "Customer Name: ${customers[0].f_name} ${customers[0].l_name}")

                    withContext(Dispatchers.Main) {
                        btnBookAppointment.isEnabled = true
                        btnBookAppointment.text = "Book Appointment"
                        Toast.makeText(
                            this@MainActivity,
                            "Ready to book!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Log.e("MainActivity", "No customer found for user_id: $userId")
                    Log.e("MainActivity", "This means RLS policy is blocking or session not loaded")

                    withContext(Dispatchers.Main) {
                        btnBookAppointment.isEnabled = false
                        btnBookAppointment.text = "Profile Not Found"
                        Toast.makeText(
                            this@MainActivity,
                            "Customer profile not found. Please contact support.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

            } catch (e: Exception) {
                Log.e("MainActivity", "=== ERROR LOADING USER DATA ===")
                Log.e("MainActivity", "Error message: ${e.message}", e)

                withContext(Dispatchers.Main) {
                    btnBookAppointment.isEnabled = false
                    btnBookAppointment.text = "Error Loading Data"
                    Toast.makeText(
                        this@MainActivity,
                        "Error loading customer data: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun validateInputs(): Boolean {
        Log.d("MainActivity", "=== VALIDATING INPUTS ===")
        Log.d("MainActivity", "Customer data loaded: $isCustomerDataLoaded")
        Log.d("MainActivity", "Customer ID: $customerId")
        Log.d("MainActivity", "Selected services: $selectedServices (size: ${selectedServices.size})")
        Log.d("MainActivity", "Selected machine: $selectedMachine")
        Log.d("MainActivity", "Selected date: $selectedDate")
        Log.d("MainActivity", "Selected time: $selectedTime")
        Log.d("MainActivity", "Selected delivery: $selectedDeliveryMethod")
        Log.d("MainActivity", "Selected payment: $selectedPaymentMethod")

        if (!isCustomerDataLoaded || customerId == null) {
            Toast.makeText(this, "Customer data not loaded. Please wait or restart the app.", Toast.LENGTH_LONG).show()
            return false
        }

        if (selectedServices.isEmpty()) {
            Toast.makeText(this, "Please select at least one service", Toast.LENGTH_SHORT).show()
            return false
        }

        if (selectedMachine == null) {
            Toast.makeText(this, "Please select a machine", Toast.LENGTH_SHORT).show()
            return false
        }

        if (selectedDate == null) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show()
            return false
        }

        if (selectedTime == null) {
            Toast.makeText(this, "Please select a time", Toast.LENGTH_SHORT).show()
            return false
        }

        if (selectedDeliveryMethod == null) {
            Toast.makeText(this, "Please select delivery method", Toast.LENGTH_SHORT).show()
            return false
        }

        if (selectedPaymentMethod == null) {
            Toast.makeText(this, "Please select payment method", Toast.LENGTH_SHORT).show()
            return false
        }

        Log.d("MainActivity", "All validations passed!")
        return true
    }

    private fun bookAppointment() {
        btnBookAppointment.isEnabled = false
        btnBookAppointment.text = "Booking..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("MainActivity", "=== STARTING APPOINTMENT BOOKING ===")

                // First, check if appointments table has delivery_method column
                // If not, we'll add it to the database

                // Create appointment
                val appointmentData = AppointmentInsert(
                    customer_id = customerId!!,
                    appointment_date = selectedDate!!,
                    appointment_time = selectedTime!!,
                    status = "pending",
                    delivery_method = selectedDeliveryMethod,
                    machine = selectedMachine
                )

                Log.d("MainActivity", "Inserting appointment: $appointmentData")

                val appointmentResponse = supabaseClient.from("appointments")
                    .insert(appointmentData) {
                        select()
                    }

                Log.d("MainActivity", "Appointment response: ${appointmentResponse.data}")

                // Parse the created appointment to get appointment_id
                val json = Json { ignoreUnknownKeys = true }
                val createdAppointments = json.decodeFromString<List<AppointmentResponse>>(appointmentResponse.data)

                if (createdAppointments.isEmpty()) {
                    throw Exception("Failed to create appointment")
                }

                val appointmentId = createdAppointments[0].appointment_id
                Log.d("MainActivity", "Created appointment ID: $appointmentId")

                // Get service prices and insert appointment_services
                for (serviceId in selectedServices) {
                    // Get service details
                    val serviceResponse = supabaseClient.from("services")
                        .select() {
                            filter {
                                eq("service_id", serviceId)
                            }
                        }

                    val services = json.decodeFromString<List<Service>>(serviceResponse.data)

                    if (services.isNotEmpty()) {
                        val service = services[0]

                        // Insert into appointment_services
                        val appointmentServiceData = AppointmentServiceInsert(
                            appointment_id = appointmentId,
                            service_id = serviceId,
                            applied_price = service.price_per_kilo.toDouble()
                        )

                        supabaseClient.from("appointment_services")
                            .insert(appointmentServiceData)

                        Log.d("MainActivity", "Added service: ${service.service_name}")
                    }
                }

                // Create payment record
                val paymentData = PaymentInsert(
                    appointment_id = appointmentId,
                    payment_method = selectedPaymentMethod!!,
                    payment_status = "pending",
                    amount = 0.0 // Will be updated after weighing
                )

                supabaseClient.from("payments")
                    .insert(paymentData)

                Log.d("MainActivity", "Payment record created")

                withContext(Dispatchers.Main) {
                    btnBookAppointment.isEnabled = true
                    btnBookAppointment.text = "Book Appointment"

                    Toast.makeText(
                        this@MainActivity,
                        "Appointment booked successfully!",
                        Toast.LENGTH_LONG
                    ).show()

                    // Reset form
                    resetForm()
                }

            } catch (e: Exception) {
                Log.e("MainActivity", "=== ERROR BOOKING APPOINTMENT ===")
                Log.e("MainActivity", "Error type: ${e::class.simpleName}")
                Log.e("MainActivity", "Error message: ${e.message}", e)

                withContext(Dispatchers.Main) {
                    btnBookAppointment.isEnabled = true
                    btnBookAppointment.text = "Book Appointment"

                    val errorMsg = when {
                        e.message?.contains("permission denied", ignoreCase = true) == true ->
                            "Database permission error. Missing RLS policies. Please run FIX_BOOKING_NOW.sql"
                        e.message?.contains("column") == true &&
                                e.message?.contains("does not exist") == true ->
                            "Database needs to be updated. Please contact support."
                        e.message?.contains("policy", ignoreCase = true) == true ->
                            "Missing database policies. Please run FIX_BOOKING_NOW.sql"
                        else -> "Booking error: ${e.message}"
                    }

                    Toast.makeText(
                        this@MainActivity,
                        errorMsg,
                        Toast.LENGTH_LONG
                    ).show()

                    Log.e("MainActivity", "User error message: $errorMsg")
                }
            }
        }
    }

    private fun resetForm() {
        // Reset services
        selectedServices.clear()
        btnWashFold.isChecked = false
        btnDryCleaning.isChecked = false
        btnIroning.isChecked = false
        btnExpress.isChecked = false

        // Reset machines
        selectedMachine = null
        btnMachine1.isChecked = false
        btnMachine2.isChecked = false
        btnMachine3.isChecked = false
        btnMachine4.isChecked = false
        btnMachine5.isChecked = false
        btnMachine6.isChecked = false

        // Reset time
        selectedTime = null
        btnTime9am.isChecked = false
        btnTime11am.isChecked = false
        btnTime1pm.isChecked = false
        btnTime3pm.isChecked = false
        btnTime5pm.isChecked = false
        btnTime7pm.isChecked = false

        // Reset delivery and payment
        selectedDeliveryMethod = null
        selectedPaymentMethod = null
        toggleGroupDelivery.clearChecked()
        toggleGroupPayment.clearChecked()
    }
}