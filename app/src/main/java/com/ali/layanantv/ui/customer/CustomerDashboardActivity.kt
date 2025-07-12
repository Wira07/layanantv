package com.ali.layanantv.ui.customer

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ali.layanantv.data.repository.AuthRepository
import com.ali.layanantv.databinding.ActivityCustomerDashboardBinding
import com.ali.layanantv.ui.auth.LoginActivity

class CustomerDashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCustomerDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomerDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {
        // Setup the welcome message
        binding.tvWelcome.text = "Welcome Customer!"

        // Setting up the logout button
        binding.btnLogout.setOnClickListener {
            AuthRepository().logout()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // You can add your logic for enabling/disabling buttons
        // If you want to enable the buttons, simply set `enabled = true` and adjust alpha if necessary
        binding.btnBrowseChannels.isEnabled = true
        binding.btnBrowseChannels.alpha = 1.0f

        binding.btnMySubscriptions.isEnabled = true
        binding.btnMySubscriptions.alpha = 1.0f

        binding.btnProfile.isEnabled = true
        binding.btnProfile.alpha = 1.0f
    }
}
