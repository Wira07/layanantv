package com.ali.layanantv.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ali.layanantv.data.model.Role
import com.ali.layanantv.data.model.User
import com.ali.layanantv.databinding.ActivityRegisterBinding
import com.ali.layanantv.ui.admin.AdminDashboardActivity
import com.ali.layanantv.ui.customer.CustomerDashboardActivity
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                updateLoadingState(state.isLoading)

                state.error?.let { error ->
                    showError(error)
                    viewModel.clearError()
                }

                if (state.isRegisterSuccessful) {
                    handleRegisterSuccess(state.user!!)
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnRegister.setOnClickListener {
            val name = binding.etName.text.toString()
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()

            viewModel.register(name, email, password, confirmPassword)
        }

        binding.tvLogin.setOnClickListener {
            finish()
        }
    }

    private fun updateLoadingState(isLoading: Boolean) {
        binding.btnRegister.isEnabled = !isLoading
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun handleRegisterSuccess(user: User) {
        Toast.makeText(this, "Registrasi berhasil", Toast.LENGTH_SHORT).show()

        when (user.role) {
            Role.ADMIN.name -> {
                startActivity(Intent(this, AdminDashboardActivity::class.java))
            }
            Role.CUSTOMER.name -> {
                startActivity(Intent(this, CustomerDashboardActivity::class.java))
            }
        }
        finish()
    }
}
