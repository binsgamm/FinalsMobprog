package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.CalendarView
import android.widget.LinearLayout
import android.widget.TextView
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

    // Service Buttons (Express removed)
    private lateinit var btnWashFold: MaterialButton
    private lateinit var btnDryCleaning: MaterialButton
    private lateinit var btnIroning: MaterialButton

    // Calendar and Time
    // Date selection
    private lateinit var btnSelectDate: MaterialButton
    private lateinit var tvSelectedDate: TextView
    private lateinit var btnTime9am: MaterialButton
    private lateinit var btnTime1130am: MaterialButton
    private lateinit var btnTime2pm: MaterialButton
    private lateinit var btnTime430pm: MaterialButton
    private lateinit var btnTime7pm: MaterialButton

    // Delivery & Payment
    private lateinit var toggleGroupDelivery: MaterialButtonToggleGroup
    private lateinit var btnMethodDropOff: MaterialButton
    private lateinit var btnMethodPickup: MaterialButton

    private lateinit var toggleGroupPayment: MaterialButtonToggleGroup
    private lateinit var btnPaymentCash: MaterialButton
    private lateinit var btnPaymentEWallet: MaterialButton

    // Detergent selection
    private lateinit var toggleGroupDetergent: MaterialButtonToggleGroup
    private lateinit var btnDetergentOwn: MaterialButton
    private lateinit var btnDetergentAddOn: MaterialButton
    private lateinit var layoutBrandSelection: LinearLayout
    private lateinit var toggleGroupBrand: MaterialButtonToggleGroup
    private lateinit var btnBrandX: MaterialButton
    private lateinit var btnBrandY: MaterialButton
    private lateinit var btnBrandZ: MaterialButton
    private lateinit var btnQuantityMinus: MaterialButton
    private lateinit var btnQuantityPlus: MaterialButton
    private lateinit var tvQuantity: TextView
    private lateinit var tvDetergentTotal: TextView

    // Booking summary
    private lateinit var tvServicesTotal: TextView
    private lateinit var tvDetergentCost: TextView
    private lateinit var layoutDetergentCost: LinearLayout
    private lateinit var tvGrandTotal: TextView

    // Payment proof (E-Wallet)
    private lateinit var layoutPaymentProof: LinearLayout
    private lateinit var btnUploadProof: MaterialButton
    private lateinit var cardImagePreview: androidx.cardview.widget.CardView
    private lateinit var ivPaymentProof: android.widget.ImageView
    private lateinit var btnRemoveProof: MaterialButton

    // Book Button
    private lateinit var btnBookAppointment: MaterialButton

    // Selected values
    private val selectedServices = mutableListOf<Int>() // Service IDs
    private var selectedDate: String? = null
    private var selectedTime: String? = null
    private var selectedDeliveryMethod: String? = null
    private var selectedPaymentMethod: String? = null

    // Detergent add-ons
    private var selectedDetergentBrand: String? = null  // "Brand X", "Brand Y", or "Brand Z"
    private var selectedDetergentQuantity: Int = 1  // Quantity of detergent
    private val detergentPricePerUnit: Double = 30.0  // ₱30 per unit

    // Service pricing cache
    private val servicePrices = mutableMapOf<Int, Double>()  // service_id to price mapping

    // Time slot availability (for selected date)
    private val fullyBookedTimes = mutableSetOf<String>()  // Time slots where all 6 machines are booked

    // E-Wallet payment proof
    private var paymentProofUri: android.net.Uri? = null
    private var paymentProofBase64: String? = null
    private val PICK_IMAGE_REQUEST = 1001

    private var userId: String? = null
    private var customerId: Int? = null
    private var isCustomerDataLoaded = false

    // Maximum bookings per time slot (6 machines available)
    private val MAX_BOOKINGS_PER_SLOT = 6

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
        // Service buttons (Express removed)
        btnWashFold = findViewById(R.id.btnWashFold)
        btnDryCleaning = findViewById(R.id.btnDryCleaning)
        btnIroning = findViewById(R.id.btnIroning)

        // Date selection and time
        btnSelectDate = findViewById(R.id.btnSelectDate)
        tvSelectedDate = findViewById(R.id.tvSelectedDate)
        btnTime9am = findViewById(R.id.btnTime9am)
        btnTime1130am = findViewById(R.id.btnTime1130am)
        btnTime2pm = findViewById(R.id.btnTime2pm)
        btnTime430pm = findViewById(R.id.btnTime430pm)
        btnTime7pm = findViewById(R.id.btnTime7pm)

        // Delivery & Payment
        toggleGroupDelivery = findViewById(R.id.toggleGroupDelivery)
        btnMethodDropOff = findViewById(R.id.btnMethodDropOff)
        btnMethodPickup = findViewById(R.id.btnMethodPickup)

        toggleGroupPayment = findViewById(R.id.toggleGroupPayment)
        btnPaymentCash = findViewById(R.id.btnPaymentCash)
        btnPaymentEWallet = findViewById(R.id.btnPaymentEWallet)

        // Detergent selection
        toggleGroupDetergent = findViewById(R.id.toggleGroupDetergent)
        btnDetergentOwn = findViewById(R.id.btnDetergentOwn)
        btnDetergentAddOn = findViewById(R.id.btnDetergentAddOn)
        layoutBrandSelection = findViewById(R.id.layoutBrandSelection)
        toggleGroupBrand = findViewById(R.id.toggleGroupBrand)
        btnBrandX = findViewById(R.id.btnBrandX)
        btnBrandY = findViewById(R.id.btnBrandY)
        btnBrandZ = findViewById(R.id.btnBrandZ)
        btnQuantityMinus = findViewById(R.id.btnQuantityMinus)
        btnQuantityPlus = findViewById(R.id.btnQuantityPlus)
        tvQuantity = findViewById(R.id.tvQuantity)
        tvDetergentTotal = findViewById(R.id.tvDetergentTotal)

        // Booking summary
        tvServicesTotal = findViewById(R.id.tvServicesTotal)
        tvDetergentCost = findViewById(R.id.tvDetergentCost)
        layoutDetergentCost = findViewById(R.id.layoutDetergentCost)
        tvGrandTotal = findViewById(R.id.tvGrandTotal)

        // Payment proof
        layoutPaymentProof = findViewById(R.id.layoutPaymentProof)
        btnUploadProof = findViewById(R.id.btnUploadProof)
        cardImagePreview = findViewById(R.id.cardImagePreview)
        ivPaymentProof = findViewById(R.id.ivPaymentProof)
        btnRemoveProof = findViewById(R.id.btnRemoveProof)

        // Book button
        btnBookAppointment = findViewById(R.id.btnBookAppointment)

        // Disable book button until customer data is loaded
        btnBookAppointment.isEnabled = false
        btnBookAppointment.text = "Loading..."
    }

    private fun setupListeners() {
        Log.d("MainActivity", "=== SETTING UP LISTENERS ===")

        // Service buttons - multiple selection (Express removed, only 3 services now)
        Log.d("MainActivity", "Setting up service buttons...")
        setupServiceButton(btnWashFold, 1) // service_id 1
        setupServiceButton(btnDryCleaning, 2) // service_id 2
        setupServiceButton(btnIroning, 3) // service_id 3
        Log.d("MainActivity", "Service buttons set up complete (Express removed)")

        // Date Picker Button
        btnSelectDate.setOnClickListener {
            // Create MaterialDatePicker
            val today = Calendar.getInstance()
            today.set(Calendar.HOUR_OF_DAY, 0)
            today.set(Calendar.MINUTE, 0)
            today.set(Calendar.SECOND, 0)
            today.set(Calendar.MILLISECOND, 0)

            val maxDate = Calendar.getInstance()
            maxDate.add(Calendar.MONTH, 3)

            val datePicker = com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Date")
                .setSelection(com.google.android.material.datepicker.MaterialDatePicker.todayInUtcMilliseconds())
                .setCalendarConstraints(
                    com.google.android.material.datepicker.CalendarConstraints.Builder()
                        .setStart(today.timeInMillis)
                        .setEnd(maxDate.timeInMillis)
                        .setValidator(com.google.android.material.datepicker.DateValidatorPointForward.from(today.timeInMillis))
                        .build()
                )
                .setTheme(com.google.android.material.R.style.ThemeOverlay_Material3_MaterialCalendar)
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = selection
                selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

                // Update UI
                val displayDate = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault()).format(calendar.time)
                tvSelectedDate.text = displayDate
                tvSelectedDate.setTextColor(android.graphics.Color.parseColor("#2d58a9"))

                Log.d("MainActivity", "Selected date: $selectedDate")

                // Check which time slots are fully booked for this date
                checkTimeSlotAvailability()
            }

            datePicker.show(supportFragmentManager, "DATE_PICKER")
        }

        // Time buttons - single selection with 2.5-hour intervals
        val timeButtons = listOf(
            btnTime9am to "09:00:00",
            btnTime1130am to "11:30:00",
            btnTime2pm to "14:00:00",
            btnTime430pm to "16:30:00",
            btnTime7pm to "19:00:00"
        )

        timeButtons.forEach { (button, time) ->
            button.setOnClickListener {
                // Check if selected date is today
                val isToday = selectedDate == SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(Calendar.getInstance().time)

                // Check if this time has passed (if today)
                var isPastTime = false
                if (isToday) {
                    try {
                        val currentTime = Calendar.getInstance()
                        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                        val slotTime = timeFormat.parse(time)

                        if (slotTime != null) {
                            val slotCalendar = Calendar.getInstance()
                            slotCalendar.time = slotTime

                            val slotHour = slotCalendar.get(Calendar.HOUR_OF_DAY)
                            val slotMinute = slotCalendar.get(Calendar.MINUTE)
                            val currentHour = currentTime.get(Calendar.HOUR_OF_DAY)
                            val currentMinute = currentTime.get(Calendar.MINUTE)

                            isPastTime = (slotHour < currentHour) ||
                                    (slotHour == currentHour && slotMinute <= currentMinute)
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error checking time: ${e.message}")
                    }
                }

                // Prevent selection if time has passed
                if (isPastTime) {
                    button.isChecked = false
                    Toast.makeText(
                        this@MainActivity,
                        "This time has already passed. Please choose a future time.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@setOnClickListener
                }

                // Check if this time slot is fully booked
                if (fullyBookedTimes.contains(time)) {
                    button.isChecked = false
                    Toast.makeText(
                        this@MainActivity,
                        "This time slot is fully booked (6/6 machines occupied). Please choose another time.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    // Deselect all other times
                    timeButtons.forEach { (btn, _) ->
                        btn.isChecked = btn == button
                    }
                    selectedTime = time
                    Log.d("MainActivity", "Selected time: $time")
                }
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

        // Detergent selection
        toggleGroupDetergent.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnDetergentOwn -> {
                        // Hide brand selection
                        layoutBrandSelection.visibility = View.GONE
                        selectedDetergentBrand = null
                        selectedDetergentQuantity = 0
                        Log.d("MainActivity", "Selected: Own detergent")
                        updateBookingSummary()
                    }
                    R.id.btnDetergentAddOn -> {
                        // Show brand selection
                        layoutBrandSelection.visibility = View.VISIBLE
                        selectedDetergentQuantity = 1
                        updateDetergentTotal()
                        Log.d("MainActivity", "Selected: Add-on detergent")
                    }
                }
            }
        }

        // Brand selection
        toggleGroupBrand.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                selectedDetergentBrand = when (checkedId) {
                    R.id.btnBrandX -> "Brand X"
                    R.id.btnBrandY -> "Brand Y"
                    R.id.btnBrandZ -> "Brand Z"
                    else -> null
                }
                Log.d("MainActivity", "Selected brand: $selectedDetergentBrand")
                updateDetergentTotal()
            }
        }

        // Quantity controls
        btnQuantityMinus.setOnClickListener {
            if (selectedDetergentQuantity > 1) {
                selectedDetergentQuantity--
                tvQuantity.text = selectedDetergentQuantity.toString()
                updateDetergentTotal()
            }
        }

        btnQuantityPlus.setOnClickListener {
            if (selectedDetergentQuantity < 10) { // Max 10 units
                selectedDetergentQuantity++
                tvQuantity.text = selectedDetergentQuantity.toString()
                updateDetergentTotal()
            }
        }

        // Payment method
        toggleGroupPayment.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                selectedPaymentMethod = when (checkedId) {
                    R.id.btnPaymentCash -> {
                        layoutPaymentProof.visibility = View.GONE
                        paymentProofUri = null
                        paymentProofBase64 = null
                        "Cash"
                    }
                    R.id.btnPaymentEWallet -> {
                        layoutPaymentProof.visibility = View.VISIBLE
                        "E-Wallet"
                    }
                    else -> null
                }
                Log.d("MainActivity", "Selected payment: $selectedPaymentMethod")
            }
        }

        // Upload payment proof button
        btnUploadProof.setOnClickListener {
            openImagePicker()
        }

        // Remove payment proof button
        btnRemoveProof.setOnClickListener {
            paymentProofUri = null
            paymentProofBase64 = null
            cardImagePreview.visibility = View.GONE
            Log.d("MainActivity", "Payment proof removed")
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
                    // Fetch price for this service if not cached
                    fetchServicePrice(serviceId)
                }
            } else {
                selectedServices.remove(serviceId)
            }

            Log.d("MainActivity", "Button ${button.text} clicked")
            Log.d("MainActivity", "Button checked state: ${button.isChecked}")
            Log.d("MainActivity", "Selected services: $selectedServices")

            // Update subtotal display
            updateBookingSummary()
        }
    }

    private fun fetchServicePrice(serviceId: Int) {
        // If price already cached, skip fetch
        if (servicePrices.containsKey(serviceId)) {
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = supabaseClient.from("services")
                    .select() {
                        filter {
                            eq("service_id", serviceId)
                        }
                    }

                val json = Json { ignoreUnknownKeys = true }
                val services = json.decodeFromString<List<Service>>(response.data)

                if (services.isNotEmpty()) {
                    servicePrices[serviceId] = services[0].price_services
                    Log.d("MainActivity", "Cached price for service $serviceId: ₱${services[0].price_services}")

                    withContext(Dispatchers.Main) {
                        updateBookingSummary()
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error fetching service price: ${e.message}", e)
            }
        }
    }

    private fun updateBookingSummary() {
        // Calculate services total
        var servicesTotal = 0.0
        selectedServices.forEach { serviceId ->
            servicesTotal += servicePrices[serviceId] ?: 0.0
        }

        // Calculate detergent cost
        val detergentCost = if (selectedDetergentBrand != null) {
            selectedDetergentQuantity * detergentPricePerUnit
        } else {
            0.0
        }

        // Calculate grand total
        val grandTotal = servicesTotal + detergentCost

        // Update UI
        tvServicesTotal.text = "₱${servicesTotal.toInt()}"

        if (detergentCost > 0) {
            layoutDetergentCost.visibility = View.VISIBLE
            tvDetergentCost.text = "₱${detergentCost.toInt()}"
        } else {
            layoutDetergentCost.visibility = View.GONE
        }

        tvGrandTotal.text = "₱${grandTotal.toInt()}"

        Log.d("MainActivity", "Booking Summary - Services: ₱$servicesTotal, Detergent: ₱$detergentCost, Total: ₱$grandTotal")
    }

    private fun openImagePicker() {
        val intent = android.content.Intent(android.content.Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            paymentProofUri = data.data

            if (paymentProofUri != null) {
                try {
                    // Show image preview
                    ivPaymentProof.setImageURI(paymentProofUri)
                    cardImagePreview.visibility = View.VISIBLE

                    // Convert to Base64 for storage
                    val inputStream = contentResolver.openInputStream(paymentProofUri!!)
                    val bytes = inputStream?.readBytes()
                    inputStream?.close()

                    if (bytes != null) {
                        paymentProofBase64 = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
                        Log.d("MainActivity", "Payment proof converted to Base64 (${bytes.size} bytes)")

                        Toast.makeText(this, "Payment proof uploaded successfully", Toast.LENGTH_SHORT).show()
                    }

                } catch (e: Exception) {
                    Log.e("MainActivity", "Error loading image: ${e.message}", e)
                    Toast.makeText(this, "Error loading image: ${e.message}", Toast.LENGTH_LONG).show()
                    paymentProofUri = null
                    paymentProofBase64 = null
                }
            }
        }
    }

    private fun updateDetergentTotal() {
        val total = selectedDetergentQuantity * detergentPricePerUnit
        tvDetergentTotal.text = "Total: ₱${total.toInt()}"
        Log.d("MainActivity", "Detergent total updated: ₱$total (${selectedDetergentBrand} × $selectedDetergentQuantity)")

        // Update booking summary
        updateBookingSummary()
    }

    private fun checkTimeSlotAvailability() {
        if (selectedDate == null) {
            Log.d("MainActivity", "Date not selected, skipping time slot check")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("MainActivity", "Checking time slot availability for $selectedDate")

                // Query all appointments for the selected date
                val response = supabaseClient.from("appointments")
                    .select() {
                        filter {
                            eq("appointment_date", selectedDate!!)
                        }
                    }

                val json = Json { ignoreUnknownKeys = true }
                val appointments = json.decodeFromString<List<AppointmentResponse>>(response.data)

                // Count bookings per time slot
                val bookingsPerTime = mutableMapOf<String, Int>()
                appointments.forEach { appointment ->
                    // Count all non-cancelled appointments
                    if (appointment.status.lowercase() != "cancelled") {
                        val count = bookingsPerTime.getOrDefault(appointment.appointment_time, 0)
                        bookingsPerTime[appointment.appointment_time] = count + 1
                    }
                }

                // Find time slots where all 6 machines are booked
                fullyBookedTimes.clear()
                bookingsPerTime.forEach { (time, count) ->
                    if (count >= MAX_BOOKINGS_PER_SLOT) {
                        fullyBookedTimes.add(time)
                        Log.d("MainActivity", "Time slot fully booked: $time ($count/$MAX_BOOKINGS_PER_SLOT slots)")
                    } else {
                        Log.d("MainActivity", "Time slot available: $time ($count/$MAX_BOOKINGS_PER_SLOT slots)")
                    }
                }

                withContext(Dispatchers.Main) {
                    updateTimeButtons()
                }

            } catch (e: Exception) {
                Log.e("MainActivity", "Error checking time slot availability: ${e.message}", e)
            }
        }
    }

    private fun updateTimeButtons() {
        val timeButtons = listOf(
            btnTime9am to "09:00:00",
            btnTime1130am to "11:30:00",
            btnTime2pm to "14:00:00",
            btnTime430pm to "16:30:00",
            btnTime7pm to "19:00:00"
        )

        // Check if selected date is today
        val isToday = selectedDate == SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .format(Calendar.getInstance().time)

        val currentTime = if (isToday) {
            Calendar.getInstance()
        } else null

        timeButtons.forEach { (button, time) ->
            val isFullyBooked = fullyBookedTimes.contains(time)

            // Check if this time has passed today
            var isPastTime = false
            if (isToday && currentTime != null) {
                try {
                    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    val slotTime = timeFormat.parse(time)

                    if (slotTime != null) {
                        val slotCalendar = Calendar.getInstance()
                        slotCalendar.time = slotTime

                        // Compare hours and minutes
                        val slotHour = slotCalendar.get(Calendar.HOUR_OF_DAY)
                        val slotMinute = slotCalendar.get(Calendar.MINUTE)
                        val currentHour = currentTime.get(Calendar.HOUR_OF_DAY)
                        val currentMinute = currentTime.get(Calendar.MINUTE)

                        isPastTime = (slotHour < currentHour) ||
                                (slotHour == currentHour && slotMinute <= currentMinute)

                        if (isPastTime) {
                            Log.d("MainActivity", "Time $time is in the past (current: $currentHour:$currentMinute, slot: $slotHour:$slotMinute)")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error parsing time: ${e.message}")
                }
            }

            // Disable if either fully booked OR past time
            val isDisabled = isFullyBooked || isPastTime

            // Visual state - gray out disabled times
            button.isEnabled = !isDisabled
            button.alpha = if (isDisabled) 0.4f else 1.0f

            // Handle selection state
            if (isDisabled && selectedTime == time) {
                // This time is disabled and was selected - clear it
                button.isChecked = false
                selectedTime = null

                if (isPastTime) {
                    Log.d("MainActivity", "Time $time has passed - cleared selection")
                } else {
                    Log.d("MainActivity", "Time $time is fully booked - cleared selection")
                }
            } else if (!isDisabled && selectedTime == time) {
                // This time is available and is selected - keep it checked
                button.isChecked = true
                Log.d("MainActivity", "Time $time still selected and available")
            }

            Log.d("MainActivity", "Time $time - Fully booked: $isFullyBooked, Past: $isPastTime, Selected: ${selectedTime == time}")
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
        Log.d("MainActivity", "Selected date: $selectedDate")
        Log.d("MainActivity", "Selected time: $selectedTime")
        Log.d("MainActivity", "Selected delivery: $selectedDeliveryMethod")
        Log.d("MainActivity", "Selected payment: $selectedPaymentMethod")
        Log.d("MainActivity", "Detergent quantity: $selectedDetergentQuantity")

        if (!isCustomerDataLoaded || customerId == null) {
            Toast.makeText(this, "Customer data not loaded. Please wait or restart the app.", Toast.LENGTH_LONG).show()
            return false
        }

        if (selectedServices.isEmpty()) {
            Toast.makeText(this, "Please select at least one service", Toast.LENGTH_SHORT).show()
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

        // Validate E-Wallet payment proof
        if (selectedPaymentMethod == "E-Wallet" && paymentProofBase64 == null) {
            Toast.makeText(this, "Please upload payment proof for E-Wallet payment", Toast.LENGTH_LONG).show()
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

                // Create appointment with detergent add-on
                // Machine will be assigned by admin when customer arrives
                val detergentOption = if (selectedDetergentBrand != null) "store" else "own"
                val detergentCharge = if (selectedDetergentBrand != null) {
                    selectedDetergentQuantity * detergentPricePerUnit
                } else {
                    0.0
                }

                val appointmentData = AppointmentInsert(
                    customer_id = customerId!!,
                    appointment_date = selectedDate!!,
                    appointment_time = selectedTime!!,
                    status = "pending",
                    delivery_method = selectedDeliveryMethod,
                    machine = null, // Machine will be assigned by admin later
                    detergent_option = detergentOption,
                    detergent_charge = detergentCharge
                )

                Log.d("MainActivity", "Inserting appointment: $appointmentData")
                Log.d("MainActivity", "Detergent: $detergentOption, Brand: $selectedDetergentBrand, Quantity: $selectedDetergentQuantity, Charge: ₱$detergentCharge")
                Log.d("MainActivity", "Machine: Not assigned (admin will assign on arrival)")

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
                        // FLAT RATE PRICING: Each service costs a fixed amount (up to 8kg max)
                        // price_services is the flat rate per service
                        val appointmentServiceData = AppointmentServiceInsert(
                            appointment_id = appointmentId,
                            service_id = serviceId,
                            applied_price = service.price_services  // Flat rate (e.g., ₱50 for up to 8kg)
                        )

                        supabaseClient.from("appointment_services")
                            .insert(appointmentServiceData)

                        Log.d("MainActivity", "Added service: ${service.service_name} at flat rate ₱${service.price_services}")
                    }
                }

                // Create payment record
                val paymentData = PaymentInsert(
                    appointment_id = appointmentId,
                    payment_method = selectedPaymentMethod!!,
                    payment_status = "pending",
                    amount = 0.0, // Will be updated after weighing
                    proof_image = paymentProofBase64  // Include payment proof for E-Wallet
                )

                supabaseClient.from("payments")
                    .insert(paymentData)

                Log.d("MainActivity", "Payment record created")
                Log.d("MainActivity", "Payment proof included: ${paymentProofBase64 != null}")

                withContext(Dispatchers.Main) {
                    btnBookAppointment.isEnabled = true
                    btnBookAppointment.text = "Book Appointment"

                    Toast.makeText(
                        this@MainActivity,
                        "Appointment booked successfully! Machine will be assigned on arrival.",
                        Toast.LENGTH_LONG
                    ).show()

                    // Redirect to AppointmentsActivity to view booking
                    val intent = android.content.Intent(this@MainActivity, AppointmentsActivity::class.java)
                    intent.putExtra("USER_ID", userId)
                    startActivity(intent)

                    // Optionally finish this activity so user can't go back
                    finish()
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

        // Reset time
        selectedTime = null
        btnTime9am.isChecked = false
        btnTime1130am.isChecked = false
        btnTime2pm.isChecked = false
        btnTime430pm.isChecked = false
        btnTime7pm.isChecked = false

        // Reset delivery and payment
        selectedDeliveryMethod = null
        selectedPaymentMethod = null
        toggleGroupDelivery.clearChecked()
        toggleGroupPayment.clearChecked()

        // Reset payment proof
        paymentProofUri = null
        paymentProofBase64 = null
        layoutPaymentProof.visibility = View.GONE
        cardImagePreview.visibility = View.GONE

        // Reset detergent add-on
        selectedDetergentBrand = null
        selectedDetergentQuantity = 1
        toggleGroupDetergent.clearChecked()
        toggleGroupBrand.clearChecked()
        layoutBrandSelection.visibility = View.GONE
        tvQuantity.text = "1"
        tvDetergentTotal.text = "Total: ₱30"

        // Reset booking summary
        updateBookingSummary()
    }
}