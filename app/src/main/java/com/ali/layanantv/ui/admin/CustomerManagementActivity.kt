package com.ali.layanantv.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ali.layanantv.data.model.User
import com.ali.layanantv.data.repository.AdminRepository
import com.ali.layanantv.databinding.ActivityCustomerManagementBinding
import com.ali.layanantv.ui.admin.CustomerDetailActivity
import kotlinx.coroutines.launch

class CustomerManagementActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCustomerManagementBinding
    private lateinit var adminRepository: AdminRepository
    private lateinit var customerAdapter: CustomerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomerManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adminRepository = AdminRepository()
        setupUI()
        loadCustomers()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Kelola Pelanggan"

        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        // Setup RecyclerView
        customerAdapter = CustomerAdapter(
            onItemClick = { user ->
                val intent = Intent(this, CustomerDetailActivity::class.java)
                intent.putExtra("USER_ID", user.uid)
                startActivity(intent)
            },
            onToggleStatus = { user ->
                showToggleStatusDialog(user)
            }
        )

        binding.rvCustomers.apply {
            layoutManager = LinearLayoutManager(this@CustomerManagementActivity)
            adapter = customerAdapter
        }

        // Search functionality
        binding.etSearch.setOnEditorActionListener { _, _, _ ->
            val query = binding.etSearch.text.toString().trim()
            searchCustomers(query)
            true
        }
    }

    private fun loadCustomers() {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                binding.tvEmpty.visibility = View.GONE

                val users = adminRepository.getAllUsers()
                val customers = users.filter { it.role == "CUSTOMER" }

                if (customers.isEmpty()) {
                    binding.tvEmpty.visibility = View.VISIBLE
                } else {
                    customerAdapter.submitList(customers)
                }

                binding.progressBar.visibility = View.GONE
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@CustomerManagementActivity, "Error loading customers: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun searchCustomers(query: String) {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                binding.tvEmpty.visibility = View.GONE

                val users = adminRepository.getAllUsers()
                val filteredCustomers = users.filter { user ->
                    user.role == "CUSTOMER" && (
                            user.name.contains(query, ignoreCase = true) ||
                                    user.email.contains(query, ignoreCase = true) ||
                                    (user.phoneNumber?.contains(query, ignoreCase = true) == true)
                            )
                }

                if (filteredCustomers.isEmpty()) {
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.tvEmpty.text = if (query.isEmpty()) "Belum ada pelanggan" else "Tidak ada pelanggan yang cocok"
                } else {
                    customerAdapter.submitList(filteredCustomers)
                }

                binding.progressBar.visibility = View.GONE
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@CustomerManagementActivity, "Error searching customers: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showToggleStatusDialog(user: User) {
        val newStatus = !user.isActive
        val statusText = if (newStatus) "mengaktifkan" else "menonaktifkan"

        AlertDialog.Builder(this)
            .setTitle("Ubah Status Pelanggan")
            .setMessage("Apakah Anda yakin ingin $statusText pelanggan ${user.name}?")
            .setPositiveButton("Ya") { _, _ ->
                updateUserStatus(user, newStatus)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun updateUserStatus(user: User, isActive: Boolean) {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                adminRepository.updateUserStatus(user.uid, isActive)

                val statusText = if (isActive) "diaktifkan" else "dinonaktifkan"
                Toast.makeText(this@CustomerManagementActivity, "Pelanggan berhasil $statusText", Toast.LENGTH_SHORT).show()

                loadCustomers()
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@CustomerManagementActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadCustomers()
    }
}