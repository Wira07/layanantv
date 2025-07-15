package com.ali.layanantv.ui.customer

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.ali.layanantv.R
import com.ali.layanantv.databinding.ActivityChangePasswordBinding
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChangePasswordBinding
    private lateinit var auth: FirebaseAuth
    private var currentUser: FirebaseUser? = null

    companion object {
        private const val TAG = "ChangePasswordActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initializeFirebase()
        setupUI()
    }

    private fun initializeFirebase() {
        auth = FirebaseAuth.getInstance()
        currentUser = auth.currentUser

        if (currentUser == null) {
            Log.e(TAG, "No user is currently signed in")
            Toast.makeText(this, "Error: No user signed in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Log.d(TAG, "Current user: ${currentUser?.email}")
    }

    private fun setupUI() {
        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Change Password"
        }

        // Setup click listeners
        binding.btnChangePassword.setOnClickListener {
            changePassword()
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun changePassword() {
        val currentPassword = binding.etCurrentPassword.text.toString().trim()
        val newPassword = binding.etNewPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        // Validate input
        if (!validateInput(currentPassword, newPassword, confirmPassword)) {
            return
        }

        // Show loading
        setLoadingState(true)

        // Re-authenticate user first
        reauthenticateUser(currentPassword) { success ->
            if (success) {
                // Update password
                updatePassword(newPassword)
            } else {
                setLoadingState(false)
            }
        }
    }

    private fun validateInput(currentPassword: String, newPassword: String, confirmPassword: String): Boolean {
        // Clear previous errors
        binding.tilCurrentPassword.error = null
        binding.tilNewPassword.error = null
        binding.tilConfirmPassword.error = null

        var isValid = true

        // Check current password
        if (TextUtils.isEmpty(currentPassword)) {
            binding.tilCurrentPassword.error = "Current password is required"
            isValid = false
        }

        // Check new password
        if (TextUtils.isEmpty(newPassword)) {
            binding.tilNewPassword.error = "New password is required"
            isValid = false
        } else if (newPassword.length < 6) {
            binding.tilNewPassword.error = "Password must be at least 6 characters"
            isValid = false
        }

        // Check confirm password
        if (TextUtils.isEmpty(confirmPassword)) {
            binding.tilConfirmPassword.error = "Please confirm your password"
            isValid = false
        } else if (newPassword != confirmPassword) {
            binding.tilConfirmPassword.error = "Passwords do not match"
            isValid = false
        }

        // Check if new password is same as current
        if (isValid && currentPassword == newPassword) {
            binding.tilNewPassword.error = "New password must be different from current password"
            isValid = false
        }

        return isValid
    }

    private fun reauthenticateUser(currentPassword: String, callback: (Boolean) -> Unit) {
        val user = currentUser
        if (user?.email == null) {
            Log.e(TAG, "User email is null")
            showToast("Error: Unable to verify current user")
            callback(false)
            return
        }

        val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)

        user.reauthenticate(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "User re-authenticated successfully")
                    callback(true)
                } else {
                    Log.e(TAG, "Re-authentication failed: ${task.exception?.message}")
                    binding.tilCurrentPassword.error = "Current password is incorrect"
                    showToast("Current password is incorrect")
                    callback(false)
                }
            }
    }

    private fun updatePassword(newPassword: String) {
        currentUser?.updatePassword(newPassword)
            ?.addOnCompleteListener { task ->
                setLoadingState(false)

                if (task.isSuccessful) {
                    Log.d(TAG, "Password updated successfully")
                    showToast("Password changed successfully")

                    // Clear the form
                    clearForm()

                    // Close activity after short delay
                    binding.root.postDelayed({
                        finish()
                    }, 1500)

                } else {
                    Log.e(TAG, "Password update failed: ${task.exception?.message}")
                    showToast("Failed to change password: ${task.exception?.message}")
                }
            }
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.btnChangePassword.isEnabled = !isLoading
        binding.btnCancel.isEnabled = !isLoading
        binding.etCurrentPassword.isEnabled = !isLoading
        binding.etNewPassword.isEnabled = !isLoading
        binding.etConfirmPassword.isEnabled = !isLoading

        if (isLoading) {
            binding.btnChangePassword.text = "Changing..."
        } else {
            binding.btnChangePassword.text = "Change Password"
        }
    }

    private fun clearForm() {
        binding.etCurrentPassword.setText("")
        binding.etNewPassword.setText("")
        binding.etConfirmPassword.setText("")

        // Clear errors
        binding.tilCurrentPassword.error = null
        binding.tilNewPassword.error = null
        binding.tilConfirmPassword.error = null
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}