package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
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
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    // Service Buttons
    private lateinit var btnWashFold: MaterialButton
    private lateinit var btnDryCleaning: MaterialButton
    private lateinit var btnIroning: MaterialButton

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

    // E-Wallet Details (Recipient Info)
    private lateinit var layoutEWalletRecipientInfo: CardView

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
    private lateinit var cardImagePreview: CardView
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
    private var selectedDetergentBrand: String? = null
    private var selectedDetergentQuantity: Int = 1
    private val detergentPricePerUnit: Double = 30.0

    // Service pricing cache
    private val servicePrices = mutableMapOf<Int, Double>()

    // Time slot availability
    private val fullyBookedTimes = mutableSetOf<String>()

    // E-Wallet payment proof
    private var paymentProofUri: android.net.Uri? = null
    private var paymentProofBase64: String? = null
    private val PICK_IMAGE_REQUEST = 1001

    private var userId: String? = null
    private var customerId: Int? = null
    private var isCustomerDataLoaded = false

    private val MAX_BOOKINGS_PER_SLOT = 6

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

        initializeViews()
        setupListeners()
        loadUserData()
    }

    private fun initializeViews() {
        btnWashFold = findViewById(R.id.btnWashFold)
        btnDryCleaning = findViewById(R.id.btnDryCleaning)
        btnIroning = findViewById(R.id.btnIroning)

        btnSelectDate = findViewById(R.id.btnSelectDate)
        tvSelectedDate = findViewById(R.id.tvSelectedDate)
        btnTime9am = findViewById(R.id.btnTime9am)
        btnTime1130am = findViewById(R.id.btnTime1130am)
        btnTime2pm = findViewById(R.id.btnTime2pm)
        btnTime430pm = findViewById(R.id.btnTime430pm)
        btnTime7pm = findViewById(R.id.btnTime7pm)

        toggleGroupDelivery = findViewById(R.id.toggleGroupDelivery)
        btnMethodDropOff = findViewById(R.id.btnMethodDropOff)
        btnMethodPickup = findViewById(R.id.btnMethodPickup)

        toggleGroupPayment = findViewById(R.id.toggleGroupPayment)
        btnPaymentCash = findViewById(R.id.btnPaymentCash)
        btnPaymentEWallet = findViewById(R.id.btnPaymentEWallet)

        layoutEWalletRecipientInfo = findViewById(R.id.layoutEWalletRecipientInfo)

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

        tvServicesTotal = findViewById(R.id.tvServicesTotal)
        tvDetergentCost = findViewById(R.id.tvDetergentCost)
        layoutDetergentCost = findViewById(R.id.layoutDetergentCost)
        tvGrandTotal = findViewById(R.id.tvGrandTotal)

        layoutPaymentProof = findViewById(R.id.layoutPaymentProof)
        btnUploadProof = findViewById(R.id.btnUploadProof)
        cardImagePreview = findViewById(R.id.cardImagePreview)
        ivPaymentProof = findViewById(R.id.ivPaymentProof)
        btnRemoveProof = findViewById(R.id.btnRemoveProof)

        btnBookAppointment = findViewById(R.id.btnBookAppointment)
        btnBookAppointment.isEnabled = false
        btnBookAppointment.text = "Loading..."
    }

    private fun setupListeners() {
        setupServiceButton(btnWashFold, 1)
        setupServiceButton(btnDryCleaning, 2)
        setupServiceButton(btnIroning, 3)

        btnSelectDate.setOnClickListener {
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

                val displayDate = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault()).format(calendar.time)
                tvSelectedDate.text = displayDate
                tvSelectedDate.setTextColor(android.graphics.Color.parseColor("#2d58a9"))

                checkTimeSlotAvailability()
            }

            datePicker.show(supportFragmentManager, "DATE_PICKER")
        }

        val timeButtons = listOf(
            btnTime9am to "09:00:00",
            btnTime1130am to "11:30:00",
            btnTime2pm to "14:00:00",
            btnTime430pm to "16:30:00",
            btnTime7pm to "19:00:00"
        )

        timeButtons.forEach { (button, time) ->
            button.setOnClickListener {
                val isToday = selectedDate == SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(Calendar.getInstance().time)

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
                            val currentHour = currentTime.get(Calendar.HOUR_OF_DAY)
                            isPastTime = (slotHour < currentHour)
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Time check error: ${e.message}")
                    }
                }

                if (isPastTime) {
                    button.isChecked = false
                    Toast.makeText(this, "This time has passed", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (fullyBookedTimes.contains(time)) {
                    button.isChecked = false
                    Toast.makeText(this, "Fully booked", Toast.LENGTH_SHORT).show()
                } else {
                    timeButtons.forEach { (btn, _) -> btn.isChecked = btn == button }
                    selectedTime = time
                }
            }
        }

        toggleGroupDelivery.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                selectedDeliveryMethod = when (checkedId) {
                    R.id.btnMethodDropOff -> "Drop Off"
                    R.id.btnMethodPickup -> "Pickup"
                    else -> null
                }
            }
        }

        toggleGroupDetergent.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnDetergentOwn -> {
                        layoutBrandSelection.visibility = View.GONE
                        selectedDetergentBrand = null
                        selectedDetergentQuantity = 0
                        updateBookingSummary()
                    }
                    R.id.btnDetergentAddOn -> {
                        layoutBrandSelection.visibility = View.VISIBLE
                        selectedDetergentQuantity = 1
                        updateDetergentTotal()
                    }
                }
            }
        }

        toggleGroupBrand.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                selectedDetergentBrand = when (checkedId) {
                    R.id.btnBrandX -> "Brand X"
                    R.id.btnBrandY -> "Brand Y"
                    R.id.btnBrandZ -> "Brand Z"
                    else -> null
                }
                updateDetergentTotal()
            }
        }

        btnQuantityMinus.setOnClickListener {
            if (selectedDetergentQuantity > 1) {
                selectedDetergentQuantity--
                tvQuantity.text = selectedDetergentQuantity.toString()
                updateDetergentTotal()
            }
        }

        btnQuantityPlus.setOnClickListener {
            if (selectedDetergentQuantity < 10) {
                selectedDetergentQuantity++
                tvQuantity.text = selectedDetergentQuantity.toString()
                updateDetergentTotal()
            }
        }

        toggleGroupPayment.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                selectedPaymentMethod = when (checkedId) {
                    R.id.btnPaymentCash -> {
                        layoutPaymentProof.visibility = View.GONE
                        layoutEWalletRecipientInfo.visibility = View.GONE
                        paymentProofUri = null
                        paymentProofBase64 = null
                        "Cash"
                    }
                    R.id.btnPaymentEWallet -> {
                        layoutPaymentProof.visibility = View.VISIBLE
                        layoutEWalletRecipientInfo.visibility = View.VISIBLE
                        "E-Wallet"
                    }
                    else -> null
                }
            }
        }

        btnUploadProof.setOnClickListener { openImagePicker() }

        btnRemoveProof.setOnClickListener {
            paymentProofUri = null
            paymentProofBase64 = null
            cardImagePreview.visibility = View.GONE
        }

        btnBookAppointment.setOnClickListener {
            if (validateInputs()) {
                bookAppointment()
            }
        }
    }

    private fun setupServiceButton(button: MaterialButton, serviceId: Int) {
        button.isCheckable = true
        button.setOnClickListener {
            if (button.isChecked) {
                if (!selectedServices.contains(serviceId)) {
                    selectedServices.add(serviceId)
                    fetchServicePrice(serviceId)
                }
            } else {
                selectedServices.remove(serviceId)
            }
            updateBookingSummary()
        }
    }

    private fun fetchServicePrice(serviceId: Int) {
        if (servicePrices.containsKey(serviceId)) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = supabaseClient.from("services").select {
                    filter { eq("service_id", serviceId) }
                }
                val json = Json { ignoreUnknownKeys = true }
                val services = json.decodeFromString<List<Service>>(response.data)
                if (services.isNotEmpty()) {
                    servicePrices[serviceId] = services[0].price_services
                    withContext(Dispatchers.Main) { updateBookingSummary() }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Fetch service price error: ${e.message}")
            }
        }
    }

    private fun updateBookingSummary() {
        var servicesTotal = 0.0
        selectedServices.forEach { servicesTotal += servicePrices[it] ?: 0.0 }
        val detergentCost = if (selectedDetergentBrand != null) selectedDetergentQuantity * detergentPricePerUnit else 0.0
        val grandTotal = servicesTotal + detergentCost

        tvServicesTotal.text = String.format("₱%.0f", servicesTotal)
        if (detergentCost > 0) {
            layoutDetergentCost.visibility = View.VISIBLE
            tvDetergentCost.text = String.format("₱%.0f", detergentCost)
        } else {
            layoutDetergentCost.visibility = View.GONE
        }
        tvGrandTotal.text = String.format("₱%.0f", grandTotal)
    }

    private fun openImagePicker() {
        val intent = android.content.Intent(android.content.Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            paymentProofUri = data.data
            if (paymentProofUri != null) {
                try {
                    ivPaymentProof.setImageURI(paymentProofUri)
                    cardImagePreview.visibility = View.VISIBLE
                    val inputStream = contentResolver.openInputStream(paymentProofUri!!)
                    val bytes = inputStream?.readBytes()
                    inputStream?.close()
                    if (bytes != null) {
                        paymentProofBase64 = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Image process error: ${e.message}")
                }
            }
        }
    }

    private fun updateDetergentTotal() {
        val total = selectedDetergentQuantity * detergentPricePerUnit
        tvDetergentTotal.text = String.format("Total: ₱%.0f", total)
        updateBookingSummary()
    }

    private fun checkTimeSlotAvailability() {
        if (selectedDate == null) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = supabaseClient.from("appointments").select {
                    filter { eq("appointment_date", selectedDate!!) }
                }
                val json = Json { ignoreUnknownKeys = true }
                val appointments = json.decodeFromString<List<AppointmentResponse>>(response.data)
                val bookingsPerTime = mutableMapOf<String, Int>()
                appointments.forEach {
                    if (it.status.lowercase() != "cancelled") {
                        val count = bookingsPerTime.getOrDefault(it.appointment_time, 0)
                        bookingsPerTime[it.appointment_time] = count + 1
                    }
                }
                fullyBookedTimes.clear()
                bookingsPerTime.forEach { (time, count) ->
                    if (count >= MAX_BOOKINGS_PER_SLOT) fullyBookedTimes.add(time)
                }
                withContext(Dispatchers.Main) { updateTimeButtons() }
            } catch (e: Exception) {
                Log.e("MainActivity", "Time slot availability error: ${e.message}")
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
        val isToday = selectedDate == SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
        val currentHour = if (isToday) Calendar.getInstance().get(Calendar.HOUR_OF_DAY) else -1

        timeButtons.forEach { (button, time) ->
            val slotHour = time.split(":")[0].toInt()
            val isDisabled = fullyBookedTimes.contains(time) || (isToday && slotHour <= currentHour)
            button.isEnabled = !isDisabled
            button.alpha = if (isDisabled) 0.4f else 1.0f
            if (isDisabled && selectedTime == time) {
                button.isChecked = false
                selectedTime = null
            }
        }
    }

    private fun loadUserData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                delay(1000)
                userId = intent.getStringExtra("USER_ID") ?: supabaseClient.auth.currentUserOrNull()?.id
                if (userId == null) {
                    withContext(Dispatchers.Main) {
                        btnBookAppointment.text = "Login Required"
                    }
                    return@launch
                }
                val response = supabaseClient.from("customers").select {
                    filter { eq("user_id", userId!!) }
                }
                val json = Json { ignoreUnknownKeys = true }
                val customers = json.decodeFromString<List<Customer>>(response.data)
                if (customers.isNotEmpty()) {
                    customerId = customers[0].customer_id
                    isCustomerDataLoaded = true
                    withContext(Dispatchers.Main) {
                        btnBookAppointment.isEnabled = true
                        btnBookAppointment.text = "Book Appointment"
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Load user data error: ${e.message}")
            }
        }
    }

    private fun validateInputs(): Boolean {
        if (!isCustomerDataLoaded || customerId == null) return false
        if (selectedServices.isEmpty()) {
            Toast.makeText(this, "Please select at least one service", Toast.LENGTH_SHORT).show()
            return false
        }
        if (selectedDate == null || selectedTime == null) {
            Toast.makeText(this, "Please select date and time", Toast.LENGTH_SHORT).show()
            return false
        }
        if (selectedDeliveryMethod == null || selectedPaymentMethod == null) {
            Toast.makeText(this, "Please select delivery and payment method", Toast.LENGTH_SHORT).show()
            return false
        }

        if (selectedPaymentMethod == "E-Wallet" && paymentProofBase64 == null) {
            Toast.makeText(this, "Please upload payment proof", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun bookAppointment() {
        btnBookAppointment.isEnabled = false
        btnBookAppointment.text = "Booking..."
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val detergentOption = if (selectedDetergentBrand != null) "store" else "own"
                val detergentCharge = if (selectedDetergentBrand != null) selectedDetergentQuantity * detergentPricePerUnit else 0.0

                val appointmentData = AppointmentInsert(
                    customer_id = customerId!!,
                    appointment_date = selectedDate!!,
                    appointment_time = selectedTime!!,
                    status = "pending",
                    delivery_method = selectedDeliveryMethod,
                    machine = null,
                    detergent_option = detergentOption,
                    detergent_charge = detergentCharge
                )

                val appointmentResponse = supabaseClient.from("appointments").insert(appointmentData) { select() }
                val json = Json { ignoreUnknownKeys = true }
                val createdAppointments = json.decodeFromString<List<AppointmentResponse>>(appointmentResponse.data)
                if (createdAppointments.isEmpty()) throw Exception("Failed to create appointment")

                val appointmentId = createdAppointments[0].appointment_id

                for (serviceId in selectedServices) {
                    val price = servicePrices[serviceId] ?: 0.0
                    supabaseClient.from("appointment_services").insert(
                        AppointmentServiceInsert(appointmentId, serviceId, price)
                    )
                }

                var servicesTotal = 0.0
                selectedServices.forEach { servicesTotal += servicePrices[it] ?: 0.0 }
                val totalAmount = servicesTotal + detergentCharge

                val paymentData = PaymentInsert(
                    appointment_id = appointmentId,
                    payment_method = selectedPaymentMethod!!,
                    payment_status = "pending",
                    amount = totalAmount,
                    proof_image = paymentProofBase64,
                    account_name = null,
                    account_number = null
                )

                supabaseClient.from("payments").insert(paymentData)

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Booked!", Toast.LENGTH_SHORT).show()
                    val intent = android.content.Intent(this@MainActivity, AppointmentsActivity::class.java)
                    intent.putExtra("USER_ID", userId)
                    startActivity(intent)
                    finish()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Booking error: ${e.message}")
                withContext(Dispatchers.Main) {
                    btnBookAppointment.isEnabled = true
                    btnBookAppointment.text = "Book Appointment"
                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
