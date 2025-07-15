package com.ali.layanantv.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ali.layanantv.data.model.Order
import com.ali.layanantv.data.repository.AdminRepository
import com.ali.layanantv.databinding.ActivityOrderManagementBinding
import kotlinx.coroutines.launch

class OrderManagementActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOrderManagementBinding
    private lateinit var adminRepository: AdminRepository
    private lateinit var orderAdapter: OrderAdapter

    // Current filter to maintain state
    private var currentFilter: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adminRepository = AdminRepository()
        setupUI()
        loadOrders()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Kelola Orders"

        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        // Setup RecyclerView
        orderAdapter = OrderAdapter(
            onVerifyPayment = { order ->
                showVerifyPaymentDialog(order)
            },
            onUpdateStatus = { order ->
                showUpdateStatusDialog(order)
            },
            onViewDetails = { order ->
                showOrderDetailsDialog(order)
            }
        )

        binding.rvOrders.apply {
            layoutManager = LinearLayoutManager(this@OrderManagementActivity)
            adapter = orderAdapter
        }

        // Setup pull-to-refresh
        binding.swipeRefresh.setOnRefreshListener {
            loadOrders(currentFilter)
        }

        // Filter chips
        binding.chipAll.setOnClickListener {
            currentFilter = null
            loadOrders(currentFilter)
            resetChipSelection()
            binding.chipAll.isChecked = true
        }

        binding.chipPending.setOnClickListener {
            currentFilter = "pending"
            loadOrders(currentFilter)
            resetChipSelection()
            binding.chipPending.isChecked = true
        }

        binding.chipConfirmed.setOnClickListener {
            currentFilter = "confirmed"
            loadOrders(currentFilter)
            resetChipSelection()
            binding.chipConfirmed.isChecked = true
        }

        binding.chipCompleted.setOnClickListener {
            currentFilter = "completed"
            loadOrders(currentFilter)
            resetChipSelection()
            binding.chipCompleted.isChecked = true
        }

        binding.chipCancelled.setOnClickListener {
            currentFilter = "cancelled"
            loadOrders(currentFilter)
            resetChipSelection()
            binding.chipCancelled.isChecked = true
        }
    }

    private fun resetChipSelection() {
        binding.chipAll.isChecked = false
        binding.chipPending.isChecked = false
        binding.chipConfirmed.isChecked = false
        binding.chipCompleted.isChecked = false
        binding.chipCancelled.isChecked = false
    }

    private fun loadOrders(statusFilter: String? = null) {
        lifecycleScope.launch {
            try {
                if (!binding.swipeRefresh.isRefreshing) {
                    binding.progressBar.visibility = View.VISIBLE
                }
                binding.layoutEmpty.visibility = View.GONE

                val allOrders = adminRepository.getAllOrders()
                val filteredOrders = if (statusFilter != null) {
                    allOrders.filter { it.status == statusFilter }
                } else {
                    allOrders
                }

                // Sort orders by creation date (newest first)
                val sortedOrders = filteredOrders.sortedByDescending { it.createdAt.seconds }

                if (sortedOrders.isEmpty()) {
                    binding.layoutEmpty.visibility = View.VISIBLE
                    binding.tvEmpty.text = if (statusFilter != null)
                        "Tidak ada order dengan status $statusFilter"
                    else
                        "Belum ada order"
                } else {
                    orderAdapter.submitList(sortedOrders)
                }

                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false

                // Update toolbar title with order count
                val statusText = statusFilter?.uppercase() ?: "SEMUA"
                supportActionBar?.title = "Kelola Orders ($statusText: ${sortedOrders.size})"

            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
                Toast.makeText(this@OrderManagementActivity, "Error loading orders: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showVerifyPaymentDialog(order: Order) {
        val options = arrayOf("Verifikasi Pembayaran", "Tolak Pembayaran")
        val colors = arrayOf("✅ Verifikasi", "❌ Tolak")

        AlertDialog.Builder(this)
            .setTitle("Verifikasi Pembayaran")
            .setMessage("Order ID: ${order.id}\n" +
                    "Channel: ${order.channelName}\n" +
                    "Pelanggan: ${order.userName}\n" +
                    "Email: ${order.userEmail}\n" +
                    "Jumlah: Rp ${String.format("%,.0f", order.totalAmount)}\n" +
                    "Metode: ${order.paymentMethod}")
            .setItems(colors) { _, which ->
                when (which) {
                    0 -> verifyPayment(order.id, true)
                    1 -> verifyPayment(order.id, false)
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showUpdateStatusDialog(order: Order) {
        val statusOptions = arrayOf("🕐 Pending", "✅ Confirmed", "🎉 Completed", "❌ Cancelled")
        val statusValues = arrayOf("pending", "confirmed", "completed", "cancelled")

        // Find current status index
        val currentStatusIndex = statusValues.indexOf(order.status)

        AlertDialog.Builder(this)
            .setTitle("Update Status Order")
            .setMessage("Order ID: ${order.id}\n" +
                    "Channel: ${order.channelName}\n" +
                    "Pelanggan: ${order.userName}\n" +
                    "Status Saat Ini: ${order.status.uppercase()}")
            .setSingleChoiceItems(statusOptions, currentStatusIndex) { dialog, which ->
                val newStatus = statusValues[which]
                if (newStatus != order.status) {
                    showStatusUpdateConfirmation(order, newStatus)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showStatusUpdateConfirmation(order: Order, newStatus: String) {
        val statusEmoji = when (newStatus) {
            "pending" -> "🕐"
            "confirmed" -> "✅"
            "completed" -> "🎉"
            "cancelled" -> "❌"
            else -> "📋"
        }

        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Perubahan Status")
            .setMessage("Apakah Anda yakin ingin mengubah status order?\n\n" +
                    "Dari: ${order.status.uppercase()}\n" +
                    "Ke: $statusEmoji ${newStatus.uppercase()}")
            .setPositiveButton("Ya, Ubah") { _, _ ->
                updateOrderStatus(order.id, newStatus)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showOrderDetailsDialog(order: Order) {
        val paymentStatus = if (order.paymentVerified) "✅ Verified" else "⏳ Pending"
        val notes = if (order.notes.isNotEmpty()) order.notes else "Tidak ada catatan"

        AlertDialog.Builder(this)
            .setTitle("Detail Order")
            .setMessage("Order ID: ${order.id}\n" +
                    "Status: ${order.status.uppercase()}\n" +
                    "Pelanggan: ${order.userName}\n" +
                    "Email: ${order.userEmail}\n" +
                    "Channel: ${order.channelName}\n" +
                    "Tipe: ${order.subscriptionType}\n" +
                    "Pembayaran: ${order.paymentMethod}\n" +
                    "Jumlah: Rp ${String.format("%,.0f", order.totalAmount)}\n" +
                    "Status Pembayaran: $paymentStatus\n" +
                    "Catatan: $notes")
            .setPositiveButton("Tutup", null)
            .show()
    }

    private fun verifyPayment(orderId: String, isVerified: Boolean) {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE

                // Update payment verification status
                adminRepository.verifyPayment(orderId, isVerified)

                // If payment is verified, automatically update status to confirmed
                if (isVerified) {
                    adminRepository.updateOrderStatus(orderId, "confirmed")
                    Toast.makeText(this@OrderManagementActivity,
                        "✅ Pembayaran berhasil diverifikasi dan status diubah ke CONFIRMED",
                        Toast.LENGTH_LONG).show()
                } else {
                    // If payment is rejected, update status to cancelled
                    adminRepository.updateOrderStatus(orderId, "cancelled")
                    Toast.makeText(this@OrderManagementActivity,
                        "❌ Pembayaran ditolak dan status diubah ke CANCELLED",
                        Toast.LENGTH_LONG).show()
                }

                loadOrders(currentFilter)
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@OrderManagementActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateOrderStatus(orderId: String, status: String) {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE

                // Update order status
                adminRepository.updateOrderStatus(orderId, status)

                val statusEmoji = when (status) {
                    "pending" -> "🕐"
                    "confirmed" -> "✅"
                    "completed" -> "🎉"
                    "cancelled" -> "❌"
                    else -> "📋"
                }

                Toast.makeText(this@OrderManagementActivity,
                    "$statusEmoji Status order berhasil diubah ke ${status.uppercase()}",
                    Toast.LENGTH_SHORT).show()

                loadOrders(currentFilter)
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@OrderManagementActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadOrders(currentFilter)
    }
}