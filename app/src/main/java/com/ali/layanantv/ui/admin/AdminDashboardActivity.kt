package com.ali.layanantv.ui.admin

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ali.layanantv.data.repository.AuthRepository
import com.ali.layanantv.data.repository.AdminRepository
import com.ali.layanantv.databinding.ActivityAdminDashboardBinding
import com.ali.layanantv.ui.auth.LoginActivity
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class AdminDashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminDashboardBinding
    private lateinit var adminRepository: AdminRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adminRepository = AdminRepository()
        setupUI()
        loadDashboardData()
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

        // Enable all buttons
        binding.btnManageChannels.isEnabled = true
        binding.btnManageChannels.alpha = 1.0f
        binding.btnManageChannels.setOnClickListener {
            startActivity(Intent(this, ChannelManagementActivity::class.java))
        }

        binding.btnManageCustomers.isEnabled = true
        binding.btnManageCustomers.alpha = 1.0f
        binding.btnManageCustomers.setOnClickListener {
            startActivity(Intent(this, CustomerManagementActivity::class.java))
        }

        binding.btnReports.isEnabled = true
        binding.btnReports.alpha = 1.0f
        binding.btnReports.setOnClickListener {
            startActivity(Intent(this, OrderManagementActivity::class.java))
        }
    }

    private fun loadDashboardData() {
        lifecycleScope.launch {
            try {
                // Load dashboard statistics
                val stats = adminRepository.getDashboardStats()

                // Format currency properly for Indonesian Rupiah
                val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
                binding.tvTotalRevenue.text = formatter.format(stats.totalRevenue)

                binding.tvTotalUsers.text = stats.totalUsers.toString()
                binding.tvTotalOrders.text = stats.totalOrders.toString()
                binding.tvActiveChannels.text = stats.activeChannels.toString()

            } catch (e: Exception) {
                // Handle error - set default values
                binding.tvTotalRevenue.text = "Rp 0"
                binding.tvTotalUsers.text = "0"
                binding.tvTotalOrders.text = "0"
                binding.tvActiveChannels.text = "0"
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadDashboardData() // Refresh data when returning from other activities
    }
}