package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

class ProfileActivity : AppCompatActivity() {

    private lateinit var tvHeaderName: TextView
    private lateinit var tvHeaderEmail: TextView
    private lateinit var etFirstName: EditText
    private lateinit var etMiddleName: EditText
    private lateinit var etLastName: EditText
    private lateinit var tvEmail: TextView
    private lateinit var etPhone: EditText
    private lateinit var etAddress: EditText
    private lateinit var btnEditSave: MaterialButton
    private lateinit var btnLogout: MaterialButton

    private var isEditMode = false
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        initializeViews()
        setupListeners()
        loadProfile()
    }

    private fun initializeViews() {
        tvHeaderName = findViewById(R.id.tvHeaderName)
        tvHeaderEmail = findViewById(R.id.tvHeaderEmail)
        etFirstName = findViewById(R.id.etProfileFirstName)
        etMiddleName = findViewById(R.id.etProfileMiddleName)
        etLastName = findViewById(R.id.etProfileLastName)
        tvEmail = findViewById(R.id.tvProfileEmail)
        etPhone = findViewById(R.id.etProfilePhone)
        etAddress = findViewById(R.id.etProfileAddress)
        btnEditSave = findViewById(R.id.btnEditProfile)
        btnLogout = findViewById(R.id.btnLogout)
    }

    private fun setupListeners() {
        btnEditSave.setOnClickListener { if (isEditMode) saveProfile() else toggleEditMode(true) }
        btnLogout.setOnClickListener { handleLogout() }
    }

    private fun loadProfile() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Wait for Auth
                if (SupabaseManager.client.auth.sessionStatus.value !is SessionStatus.Authenticated) {
                    SupabaseManager.client.auth.sessionStatus.filter { it is SessionStatus.Authenticated }.first()
                }

                userId = SupabaseManager.client.auth.currentUserOrNull()?.id
                if (userId != null) {
                    val res = SupabaseManager.client.from("customers").select { filter { eq("user_id", userId!!) } }
                    val data = Json { ignoreUnknownKeys = true }.decodeFromString<List<Customer>>(res.data)
                    if (data.isNotEmpty()) {
                        val c = data[0]
                        withContext(Dispatchers.Main) { updateUI(c) }
                    }
                }
            } catch (e: Exception) { Log.e("Profile", "Load error") }
        }
    }

    private fun updateUI(c: Customer) {
        tvHeaderName.text = "${c.f_name} ${c.l_name}"
        tvHeaderEmail.text = c.email ?: "N/A"
        etFirstName.setText(c.f_name)
        etMiddleName.setText(c.m_name ?: "")
        etLastName.setText(c.l_name)
        tvEmail.text = c.email ?: "N/A"
        etPhone.setText(c.phone_num ?: "")
        etAddress.setText(c.address ?: "")
    }

    private fun toggleEditMode(enabled: Boolean) {
        isEditMode = enabled
        listOf(etFirstName, etMiddleName, etLastName, etPhone, etAddress).forEach { it.isEnabled = enabled }
        btnEditSave.text = if (enabled) "Save Changes" else "Edit Profile"
        btnEditSave.setIconResource(if (enabled) android.R.drawable.ic_menu_save else android.R.drawable.ic_menu_edit)
    }

    private fun saveProfile() {
        val updates = mapOf(
            "f_name" to etFirstName.text.toString().trim(),
            "m_name" to etMiddleName.text.toString().trim().ifEmpty { null },
            "l_name" to etLastName.text.toString().trim(),
            "phone_num" to etPhone.text.toString().trim(),
            "address" to etAddress.text.toString().trim()
        )
        if (updates["f_name"].isNullOrEmpty() || updates["l_name"].isNullOrEmpty()) {
            Toast.makeText(this, "First/Last name required", Toast.LENGTH_SHORT).show()
            return
        }

        btnEditSave.isEnabled = false
        CoroutineScope(Dispatchers.IO).launch {
            try {
                SupabaseManager.client.from("customers").update(updates) { filter { eq("user_id", userId!!) } }
                withContext(Dispatchers.Main) {
                    toggleEditMode(false)
                    btnEditSave.isEnabled = true
                    tvHeaderName.text = "${updates["f_name"]} ${updates["l_name"]}"
                    Toast.makeText(this@ProfileActivity, "Saved!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) { withContext(Dispatchers.Main) { btnEditSave.isEnabled = true } }
        }
    }

    private fun handleLogout() {
        CoroutineScope(Dispatchers.IO).launch {
            SupabaseManager.client.auth.signOut()
            startActivity(Intent(this@ProfileActivity, LogInActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK })
            finish()
        }
    }
}