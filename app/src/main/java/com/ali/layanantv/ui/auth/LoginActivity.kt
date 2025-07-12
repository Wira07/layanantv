package com.ali.layanantv.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ali.layanantv.data.model.Role
import com.ali.layanantv.data.model.User
import com.ali.layanantv.ui.admin.AdminDashboardActivity
import com.ali.layanantv.ui.customer.CustomerDashboardActivity
import kotlinx.coroutines.launch
import com.ali.layanantv.R

class LoginActivity : AppCompatActivity() {
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                // Update UI berdasarkan state
                updateLoadingState(state.isLoading)

                state.error?.let { error ->
                    showError(error)
                    viewModel.clearError()
                }

                if (state.isLoginSuccessful) {
                    handleLoginSuccess(state.user!!)
                }
            }
        }
    }

    private fun setupClickListeners() {
        findViewById<Button>(R.id.btn_login).setOnClickListener {
            val email = findViewById<EditText>(R.id.et_email).text.toString().trim()
            val password = findViewById<EditText>(R.id.et_password).text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                showError("Email dan Password tidak boleh kosong.")
                return@setOnClickListener
            }

            viewModel.login(email, password)
        }

        findViewById<TextView>(R.id.tv_register).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun updateLoadingState(isLoading: Boolean) {
        findViewById<Button>(R.id.btn_login).isEnabled = !isLoading
        findViewById<ProgressBar>(R.id.progress_bar).visibility =
            if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun handleLoginSuccess(user: User) {
        Toast.makeText(this, "Login berhasil", Toast.LENGTH_SHORT).show()

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
