package com.example.myapplication

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Base64
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

    // Logic: 1 appointment means the slot is fully allotted
    private val MAX_BOOKINGS_PER_SLOT = 1

    // Theme Colors for Visuals
    private val themeBlue = Color.parseColor("#2d58a9")
    private val themeWhite = Color.WHITE

    private lateinit var timeButtonsList: List<Pair<MaterialButton, String>>

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

        timeButtonsList = listOf(
            btnTime9am to "09:00:00", btnTime1130am to "11:30:00",
            btnTime2pm to "14:00:00", btnTime430pm to "16:30:00", btnTime7pm to "19:00:00"
        )

        btnBookAppointment.isEnabled = false
        btnBookAppointment.text = "Loading..."
    }

    private fun setupListeners() {
        Log.d("MainActivity", "=== SETTING UP LISTENERS ===")

        setupServiceButton(btnWashFold, 1)
        setupServiceButton(btnDryCleaning, 2)
        setupServiceButton(btnIroning, 3)

        btnSelectDate.setOnClickListener {
            val today = Calendar.getInstance()
            val datePicker = com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Date")
                .setCalendarConstraints(com.google.android.material.datepicker.CalendarConstraints.Builder()
                    .setValidator(com.google.android.material.datepicker.DateValidatorPointForward.now()).build())
                .setTheme(com.google.android.material.R.style.ThemeOverlay_Material3_MaterialCalendar)
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

        timeButtonsList.forEach { (button, time) ->
            button.isCheckable = true
            button.setOnClickListener {
                if (selectedDate == null) {
                    button.isChecked = false
                    Toast.makeText(this, "Please select a date first", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                timeButtonsList.forEach { it.first.isChecked = false; updateButtonStyle(it.first) }
                button.isChecked = true
                selectedTime = time
                updateButtonStyle(button)
            }
        }

        toggleGroupDetergent.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
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
            }
            findViewById<MaterialButton>(checkedId)?.let { updateButtonStyle(it) }
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
            findViewById<MaterialButton>(checkedId)?.let { updateButtonStyle(it) }
        }

        btnQuantityPlus.setOnClickListener { if (selectedDetergentQuantity < 10) selectedDetergentQuantity++; tvQuantity.text = selectedDetergentQuantity.toString(); updateDetergentTotal() }
        btnQuantityMinus.setOnClickListener { if (selectedDetergentQuantity > 1) selectedDetergentQuantity--; tvQuantity.text = selectedDetergentQuantity.toString(); updateDetergentTotal() }

        toggleGroupDelivery.addOnButtonCheckedListener { _, id, isChecked ->
            if (isChecked) selectedDeliveryMethod = if (id == R.id.btnMethodDropOff) "Drop Off" else "Pickup"
            findViewById<MaterialButton>(id)?.let { updateButtonStyle(it) }
        }

        toggleGroupPayment.addOnButtonCheckedListener { _, id, isChecked ->
            if (isChecked) {
                selectedPaymentMethod = if (id == R.id.btnPaymentCash) "Cash" else "E-Wallet"
                layoutPaymentProof.visibility = if (selectedPaymentMethod == "E-Wallet") View.VISIBLE else View.GONE
            }
            findViewById<MaterialButton>(id)?.let { updateButtonStyle(it) }
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
        if (!button.isEnabled) {
            button.alpha = 0.3f
            button.backgroundTintList = ColorStateList.valueOf(Color.LTGRAY)
            button.setTextColor(Color.GRAY)
            button.strokeWidth = 0
            return
        }
        button.alpha = 1.0f
        if (button.isChecked) {
            button.backgroundTintList = ColorStateList.valueOf(themeBlue)
            button.setTextColor(themeWhite)
            button.strokeColor = ColorStateList.valueOf(themeBlue)
        } else {
            button.backgroundTintList = ColorStateList.valueOf(themeWhite)
            button.setTextColor(Color.BLACK)
            button.strokeColor = ColorStateList.valueOf(themeBlue)
            button.strokeWidth = 2
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
    }

    private fun checkTimeSlotAvailability() {
        if (selectedDate == null) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Fetch all non-cancelled appointments for this date
                val response = SupabaseManager.client.from("appointments").select {
                    filter { eq("appointment_date", selectedDate!!) }
                }
                val appointments = Json { ignoreUnknownKeys = true }.decodeFromString<List<AppointmentResponse>>(response.data)

                fullyBookedTimes.clear()

                // Group by time. If slot has >= 1 active record, block it.
                appointments.groupBy { it.appointment_time }.forEach { (time, list) ->
                    val activeCount = list.count { it.status.lowercase() != "cancelled" }
                    if (activeCount >= MAX_BOOKINGS_PER_SLOT) {
                        fullyBookedTimes.add(time)
                    }
                }

                withContext(Dispatchers.Main) { updateTimeButtons() }
            } catch (e: Exception) { Log.e("MainActivity", "Availability check error") }
        }
    }

    private fun updateTimeButtons() {
        val now = Calendar.getInstance()
        val isToday = selectedDate == SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now.time)

        timeButtonsList.forEach { (button, time) ->
            var isPast = false
            if (isToday) {
                val p = time.split(":")
                val slotCal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, p[0].toInt()); set(Calendar.MINUTE, p[1].toInt()) }
                isPast = slotCal.before(now)
            }

            // Slot is disabled if it's already in the past OR if it exists in DB (Allotted)
            val isDisabled = isPast || fullyBookedTimes.contains(time)

            button.isEnabled = !isDisabled
            if (isDisabled) button.isChecked = false

            updateButtonStyle(button)
        }
    }

    private fun loadUserData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                delay(1500)
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
            Toast.makeText(this, "Complete all fields", Toast.LENGTH_SHORT).show()
            return false
        }
        if (selectedPaymentMethod == "E-Wallet" && paymentProofBase64 == null) {
            Toast.makeText(this, "Proof required for E-Wallet", Toast.LENGTH_SHORT).show()
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
                val dCharge = if (detergentOption == "store") selectedDetergentQuantity * detergentPricePerUnit else 0.0

                val appData = AppointmentInsert(customerId!!, selectedDate!!, selectedTime!!, detergent_option = detergentOption, detergent_charge = dCharge, delivery_method = selectedDeliveryMethod)
                val res = SupabaseManager.client.from("appointments").insert(appData) { select() }
                val appId = Json { ignoreUnknownKeys = true }.decodeFromString<List<AppointmentResponse>>(res.data)[0].appointment_id

                selectedServices.forEach { sId ->
                    SupabaseManager.client.from("appointment_services").insert(AppointmentServiceInsert(appId, sId, servicePrices[sId] ?: 0.0))
                }

                SupabaseManager.client.from("payments").insert(PaymentInsert(appId, 0.0, selectedPaymentMethod!!, paymentProofBase64))

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Successfully Booked!", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this@MainActivity, AppointmentsActivity::class.java).apply { putExtra("USER_ID", userId) })
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    btnBookAppointment.isEnabled = true
                    Toast.makeText(this@MainActivity, "Booking error", Toast.LENGTH_LONG).show()
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