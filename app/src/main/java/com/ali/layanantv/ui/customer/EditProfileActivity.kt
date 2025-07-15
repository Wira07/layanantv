package com.ali.layanantv.ui.customer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.ali.layanantv.R
import com.ali.layanantv.data.repository.CustomerRepository
import com.ali.layanantv.databinding.ActivityEditProfileBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class EditProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var customerRepository: CustomerRepository
    private var updateJob: Job? = null
    private var selectedImageUri: Uri? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            selectedImageUri = result.data?.data
            selectedImageUri?.let { uri ->
                binding.ivProfilePhoto.setImageURI(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        customerRepository = CustomerRepository()

        setupWindowInsets()
        setupToolbar()
        setupUI()
        loadUserData()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.toolbar.title = "Edit Profile"
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupUI() {
        // Profile photo click listener
        binding.ivProfilePhoto.setOnClickListener {
            openImagePicker()
        }

        binding.btnChangePhoto.setOnClickListener {
            openImagePicker()
        }

        // Save button click listener
        binding.btnSave.setOnClickListener {
            saveProfile()
        }

        // Cancel button click listener
        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun loadUserData() {
        lifecycleScope.launch {
            try {
                val user = customerRepository.getCurrentUser()
                user?.let { userData ->
                    binding.etFullName.setText(userData.name)
                    binding.etEmail.setText(userData.email)
                    binding.etPhone.setText(userData.phoneNumber ?: "")

                    // Load profile photo if available
                    // Note: User model doesn't have profilePhoto field
                    // You might need to handle this separately if needed
                    // binding.ivProfilePhoto.load(userData.profilePhoto)
                }
            } catch (e: Exception) {
                Toast.makeText(this@EditProfileActivity, "Error loading user data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveProfile() {
        val fullName = binding.etFullName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()

        // Validation
        if (fullName.isEmpty()) {
            binding.etFullName.error = "Nama lengkap harus diisi"
            return
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Email tidak valid"
            return
        }

        if (phone.isEmpty()) {
            binding.etPhone.error = "Nomor telepon harus diisi"
            return
        }

        // Show loading - Fixed to use the LinearLayout instead of Button
        binding.btnSave.isEnabled = false
        // Find the TextView inside the LinearLayout and update its text
        val saveButtonText = binding.btnSave.findViewById<android.widget.TextView>(android.R.id.text1)
        saveButtonText?.text = "Menyimpan..."

        updateJob?.cancel()
        updateJob = lifecycleScope.launch {
            try {
                // Update user profile - matching the updated method signature
                val success = customerRepository.updateUserProfile(
                    name = fullName,
                    email = email,
                    phoneNumber = phone,
                    profilePhoto = selectedImageUri?.toString()
                )

                if (success) {
                    Toast.makeText(this@EditProfileActivity, "Profile berhasil diperbarui", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this@EditProfileActivity, "Gagal memperbarui profile", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@EditProfileActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.btnSave.isEnabled = true
                saveButtonText?.text = "Simpan"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        updateJob?.cancel()
    }
}