package com.ali.layanantv.ui.admin

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ali.layanantv.data.repository.AuthRepository
import com.ali.layanantv.databinding.ActivityAdminDashboardBinding
import com.ali.layanantv.ui.auth.LoginActivity

class AdminDashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {
        // Setup the welcome message
        binding.tvWelcome.text = "Welcome Admin!"

        // Setting up the logout button
        binding.btnLogout.setOnClickListener {
            AuthRepository().logout()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // You can add your logic for enabling/disabling buttons
        // If you want to enable the buttons, simply set `enabled = true` and adjust alpha if necessary
        binding.btnManageChannels.isEnabled = true
        binding.btnManageChannels.alpha = 1.0f

        binding.btnManageCustomers.isEnabled = true
        binding.btnManageCustomers.alpha = 1.0f

        binding.btnReports.isEnabled = true
        binding.btnReports.alpha = 1.0f
    }
}
