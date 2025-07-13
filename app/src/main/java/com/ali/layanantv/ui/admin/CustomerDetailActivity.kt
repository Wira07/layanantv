package com.ali.layanantv.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ali.layanantv.data.model.User
import com.ali.layanantv.data.repository.AdminRepository
import com.ali.layanantv.databinding.ActivityCustomerDetailBinding
import kotlinx.coroutines.launch

class CustomerDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCustomerDetailBinding
    private lateinit var adminRepository: AdminRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomerDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adminRepository = AdminRepository()
        setupUI()
        loadCustomerDetail()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Detail Pelanggan"

        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun loadCustomerDetail() {
        val userId = intent.getStringExtra("USER_ID")
        if (userId != null) {
            lifecycleScope.launch {
                try {
                    binding.progressBar.visibility = View.VISIBLE

                    val user = adminRepository.getUserById(userId)
                    if (user != null) {
                        displayUserInfo(user)
                    } else {
                        Toast.makeText(this@CustomerDetailActivity, "Pelanggan tidak ditemukan", Toast.LENGTH_SHORT).show()
                        finish()
                    }

                    binding.progressBar.visibility = View.GONE
                } catch (e: Exception) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this@CustomerDetailActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun displayUserInfo(user: User) {
        binding.apply {
            tvCustomerName.text = user.name
            tvCustomerEmail.text = user.email
            tvCustomerPhone.text = user.phoneNumber ?: "-"
            tvCustomerStatus.text = if (user.isActive) "Aktif" else "Tidak Aktif"
        }
    }
}