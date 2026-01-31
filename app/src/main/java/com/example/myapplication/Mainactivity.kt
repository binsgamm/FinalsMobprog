package com.example.myapplication

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    // Service Buttons
    private lateinit var btnWashFold: MaterialButton
    private lateinit var btnDryCleaning: MaterialButton
    private lateinit var btnIroning: MaterialButton

    // Calendar and Time
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

    // Payment proof
    private lateinit var layoutPaymentProof: LinearLayout
    private lateinit var btnUploadProof: MaterialButton
    private lateinit var cardImagePreview: androidx.cardview.widget.CardView
    private lateinit var ivPaymentProof: android.widget.ImageView
    private lateinit var btnRemoveProof: MaterialButton

    // Book Button
    private lateinit var btnBookAppointment: MaterialButton

    // State Variables
    private val selectedServices = mutableListOf<Int>()
    private var selectedDate: String? = null
    private var selectedTime: String? = null
    private var selectedDeliveryMethod: String? = null
    private var selectedPaymentMethod: String? = null
    private var selectedDetergentBrand: String? = null
    private var selectedDetergentQuantity: Int = 1
    private val detergentPricePerUnit: Double = 30.0
    private val servicePrices = mutableMapOf<Int, Double>()
    private val fullyBookedTimes = mutableSetOf<String>()
    private var paymentProofBase64: String? = null
    private val PICK_IMAGE_REQUEST = 1001

    private var userId: String? = null
    private var customerId: Int? = null
    private var isCustomerDataLoaded = false
    private val MAX_BOOKINGS_PER_SLOT = 6

    // Theme Colors for Visuals
    private val themeBlue = Color.parseColor("#2d58a9")
    private val themeWhite = Color.WHITE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d("MainActivity", "=== MAIN ACTIVITY CREATED ===")
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
        Log.d("MainActivity", "=== SETTING UP LISTENERS ===")

        // Services
        setupServiceButton(btnWashFold, 1)
        setupServiceButton(btnDryCleaning, 2)
        setupServiceButton(btnIroning, 3)

        // Date Picker
        btnSelectDate.setOnClickListener {
            val datePicker = com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Date")
                .setCalendarConstraints(com.google.android.material.datepicker.CalendarConstraints.Builder()
                    .setValidator(com.google.android.material.datepicker.DateValidatorPointForward.now()).build())
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                calendar.timeInMillis = selection
                selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                tvSelectedDate.text = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault()).format(calendar.time)
                tvSelectedDate.setTextColor(themeBlue)
                checkTimeSlotAvailability()
            }
            datePicker.show(supportFragmentManager, "DATE_PICKER")
        }

        // Time Buttons logic (Preserving your exact original logic)
        val timeButtons = listOf(
            btnTime9am to "09:00:00", btnTime1130am to "11:30:00",
            btnTime2pm to "14:00:00", btnTime430pm to "16:30:00", btnTime7pm to "19:00:00"
        )

        timeButtons.forEach { (button, time) ->
            button.isCheckable = true
            button.setOnClickListener {
                val isToday = selectedDate == SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
                var isPastTime = false
                if (isToday) {
                    val currentTime = Calendar.getInstance()
                    val parts = time.split(":")
                    if (parts[0].toInt() < currentTime.get(Calendar.HOUR_OF_DAY) || (parts[0].toInt() == currentTime.get(Calendar.HOUR_OF_DAY) && parts[1].toInt() <= currentTime.get(Calendar.MINUTE))) {
                        isPastTime = true
                    }
                }

                if (isPastTime) {
                    button.isChecked = false
                    Toast.makeText(this, "This time has passed.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (fullyBookedTimes.contains(time)) {
                    button.isChecked = false
                    Toast.makeText(this, "Slot fully booked.", Toast.LENGTH_SHORT).show()
                } else {
                    timeButtons.forEach {
                        it.first.isChecked = it.first == button
                        updateButtonStyle(it.first)
                    }
                    selectedTime = time
                }
            }
        }

        // Detergent
        toggleGroupDetergent.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                val btn = findViewById<MaterialButton>(checkedId)
                updateButtonStyle(btn)
                if (checkedId == R.id.btnDetergentOwn) {
                    layoutBrandSelection.visibility = View.GONE
                    selectedDetergentBrand = null
                    selectedDetergentQuantity = 0
                } else {
                    layoutBrandSelection.visibility = View.VISIBLE
                    selectedDetergentQuantity = 1
                    tvQuantity.text = "1"
                }
                updateDetergentTotal()
            } else {
                findViewById<MaterialButton>(checkedId)?.let { updateButtonStyle(it) }
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
                findViewById<MaterialButton>(checkedId)?.let { updateButtonStyle(it) }
            } else {
                findViewById<MaterialButton>(checkedId)?.let { updateButtonStyle(it) }
            }
        }

        btnQuantityPlus.setOnClickListener { if (selectedDetergentQuantity < 10) selectedDetergentQuantity++; tvQuantity.text = selectedDetergentQuantity.toString(); updateDetergentTotal() }
        btnQuantityMinus.setOnClickListener { if (selectedDetergentQuantity > 1) selectedDetergentQuantity--; tvQuantity.text = selectedDetergentQuantity.toString(); updateDetergentTotal() }

        // Delivery/Payment
        toggleGroupDelivery.addOnButtonCheckedListener { _, id, isChecked ->
            if (isChecked) {
                selectedDeliveryMethod = if (id == R.id.btnMethodDropOff) "Drop Off" else "Pickup"
                findViewById<MaterialButton>(id)?.let { updateButtonStyle(it) }
            } else findViewById<MaterialButton>(id)?.let { updateButtonStyle(it) }
        }

        toggleGroupPayment.addOnButtonCheckedListener { _, id, isChecked ->
            if (isChecked) {
                selectedPaymentMethod = if (id == R.id.btnPaymentCash) "Cash" else "E-Wallet"
                layoutPaymentProof.visibility = if (selectedPaymentMethod == "E-Wallet") View.VISIBLE else View.GONE
                findViewById<MaterialButton>(id)?.let { updateButtonStyle(it) }
            } else findViewById<MaterialButton>(id)?.let { updateButtonStyle(it) }
        }

        btnUploadProof.setOnClickListener { startActivityForResult(Intent(Intent.ACTION_PICK).apply { type = "image/*" }, PICK_IMAGE_REQUEST) }
        btnRemoveProof.setOnClickListener { paymentProofBase64 = null; cardImagePreview.visibility = View.GONE }

        btnBookAppointment.setOnClickListener { if (validateInputs()) bookAppointment() }
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
            updateButtonStyle(button)
            updateBookingSummary()
        }
    }

    private fun updateButtonStyle(button: MaterialButton) {
        if (button.isChecked) {
            button.backgroundTintList = ColorStateList.valueOf(themeBlue)
            button.setTextColor(themeWhite)
            button.strokeColor = ColorStateList.valueOf(themeBlue)
        } else {
            button.backgroundTintList = ColorStateList.valueOf(themeWhite)
            button.setTextColor(Color.BLACK)
            button.strokeColor = ColorStateList.valueOf(Color.LTGRAY)
        }
    }

    private fun fetchServicePrice(serviceId: Int) {
        if (servicePrices.containsKey(serviceId)) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = SupabaseManager.client.from("services").select { filter { eq("service_id", serviceId) } }
                val services = Json { ignoreUnknownKeys = true }.decodeFromString<List<Service>>(response.data)
                if (services.isNotEmpty()) {
                    servicePrices[serviceId] = services[0].price_services
                    withContext(Dispatchers.Main) { updateBookingSummary() }
                }
            } catch (e: Exception) { Log.e("MainActivity", "Error fetching price") }
        }
    }

    private fun updateDetergentTotal() {
        val total = selectedDetergentQuantity * detergentPricePerUnit
        tvDetergentTotal.text = "Total: ₱${total.toInt()}"
        updateBookingSummary()
    }

    private fun updateBookingSummary() {
        var servicesTotal = selectedServices.sumOf { servicePrices[it] ?: 0.0 }
        val detergentCost = if (selectedDetergentQuantity > 0) selectedDetergentQuantity * detergentPricePerUnit else 0.0
        val grandTotal = servicesTotal + detergentCost

        tvServicesTotal.text = "₱${servicesTotal.toInt()}"
        tvDetergentCost.text = "₱${detergentCost.toInt()}"
        layoutDetergentCost.visibility = if (detergentCost > 0) View.VISIBLE else View.GONE
        tvGrandTotal.text = "₱${grandTotal.toInt()}"
        Log.d("MainActivity", "Summary Updated: Total ₱$grandTotal")
    }

    private fun checkTimeSlotAvailability() {
        if (selectedDate == null) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = SupabaseManager.client.from("appointments").select { filter { eq("appointment_date", selectedDate!!) } }
                val appointments = Json { ignoreUnknownKeys = true }.decodeFromString<List<AppointmentResponse>>(response.data)
                fullyBookedTimes.clear()
                appointments.groupBy { it.appointment_time }.forEach { (time, list) ->
                    if (list.count { it.status.lowercase() != "cancelled" } >= MAX_BOOKINGS_PER_SLOT) fullyBookedTimes.add(time)
                }
                withContext(Dispatchers.Main) { updateTimeButtons() }
            } catch (e: Exception) { Log.e("MainActivity", "Error checking slots") }
        }
    }

    private fun updateTimeButtons() {
        val timeButtons = listOf(btnTime9am to "09:00:00", btnTime1130am to "11:30:00", btnTime2pm to "14:00:00", btnTime430pm to "16:30:00", btnTime7pm to "19:00:00")
        val isToday = selectedDate == SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
        val now = Calendar.getInstance()

        timeButtons.forEach { (button, time) ->
            var isPast = false
            if (isToday) {
                val slotCal = Calendar.getInstance().apply {
                    val p = time.split(":")
                    set(Calendar.HOUR_OF_DAY, p[0].toInt()); set(Calendar.MINUTE, p[1].toInt())
                }
                isPast = slotCal.before(now)
            }
            val isDisabled = isPast || fullyBookedTimes.contains(time)
            button.isEnabled = !isDisabled
            button.alpha = if (isDisabled) 0.4f else 1.0f
            updateButtonStyle(button)
        }
    }

    private fun loadUserData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                delay(2000)
                userId = intent.getStringExtra("USER_ID") ?: SupabaseManager.client.auth.currentUserOrNull()?.id
                if (userId != null) {
                    val response = SupabaseManager.client.from("customers").select { filter { eq("user_id", userId!!) } }
                    val customers = Json { ignoreUnknownKeys = true }.decodeFromString<List<Customer>>(response.data)
                    if (customers.isNotEmpty()) {
                        customerId = customers[0].customer_id
                        isCustomerDataLoaded = true
                        withContext(Dispatchers.Main) {
                            btnBookAppointment.isEnabled = true
                            btnBookAppointment.text = "Book Appointment"
                        }
                    }
                }
            } catch (e: Exception) { Log.e("MainActivity", "User Load Fail") }
        }
    }

    private fun validateInputs(): Boolean {
        if (!isCustomerDataLoaded || customerId == null) return false
        if (selectedServices.isEmpty() || selectedDate == null || selectedTime == null || selectedPaymentMethod == null) {
            Toast.makeText(this, "Please complete all selections", Toast.LENGTH_SHORT).show()
            return false
        }
        if (selectedPaymentMethod == "E-Wallet" && paymentProofBase64 == null) {
            Toast.makeText(this, "Proof image required for E-Wallet", Toast.LENGTH_SHORT).show()
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
                val detergentCharge = if (detergentOption == "store") selectedDetergentQuantity * detergentPricePerUnit else 0.0

                // 1. Create Appointment
                val appData = AppointmentInsert(customerId!!, selectedDate!!, selectedTime!!, detergent_option = detergentOption, detergent_charge = detergentCharge, delivery_method = selectedDeliveryMethod)
                val appRes = SupabaseManager.client.from("appointments").insert(appData) { select() }
                val appId = Json { ignoreUnknownKeys = true }.decodeFromString<List<AppointmentResponse>>(appRes.data)[0].appointment_id

                // 2. Junction Services
                selectedServices.forEach { sId ->
                    val sInsert = AppointmentServiceInsert(appId, sId, servicePrices[sId] ?: 0.0)
                    SupabaseManager.client.from("appointment_services").insert(sInsert)
                }

                // 3. Payment
                val payData = PaymentInsert(appId, 0.0, selectedPaymentMethod!!, paymentProofBase64)
                SupabaseManager.client.from("payments").insert(payData)

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Successfully Booked!", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this@MainActivity, AppointmentsActivity::class.java).apply { putExtra("USER_ID", userId) })
                    finish()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error: ${e.message}")
                withContext(Dispatchers.Main) {
                    btnBookAppointment.isEnabled = true
                    Toast.makeText(this@MainActivity, "Booking error. Remove DB triggers.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            ivPaymentProof.setImageURI(uri)
            cardImagePreview.visibility = View.VISIBLE
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val bytes = contentResolver.openInputStream(uri)?.readBytes()
                    if (bytes != null) paymentProofBase64 = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
                } catch (e: Exception) { Log.e("MA", "Img error") }
            }
        }
    }
}