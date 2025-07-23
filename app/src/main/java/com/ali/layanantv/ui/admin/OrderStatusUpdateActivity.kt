package com.ali.layanantv.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ali.layanantv.R
import com.ali.layanantv.data.repository.AdminRepository
import com.ali.layanantv.databinding.ActivityOrderStatusUpdateBinding
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch

class OrderStatusUpdateActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOrderStatusUpdateBinding
    private lateinit var adminRepository: AdminRepository

    private var orderId: String = ""
    private var currentStatus: String = ""
    private var customerName: String = ""
    private var customerEmail: String = ""
    private var channelName: String = ""
    private var subscriptionType: String = ""
    private var paymentMethod: String = ""
    private var totalAmount: Double = 0.0
    private var paymentVerified: Boolean = false
    private var notes: String = ""

    private var proofImageUrl: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderStatusUpdateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adminRepository = AdminRepository()

        // Get data from intent
        getIntentData()

        setupUI()
        displayOrderInfo()
    }

    private fun getIntentData() {
        orderId = intent.getStringExtra("ORDER_ID") ?: ""
        currentStatus = intent.getStringExtra("ORDER_STATUS") ?: ""
        customerName = intent.getStringExtra("CUSTOMER_NAME") ?: ""
        customerEmail = intent.getStringExtra("CUSTOMER_EMAIL") ?: ""
        channelName = intent.getStringExtra("CHANNEL_NAME") ?: ""
        subscriptionType = intent.getStringExtra("SUBSCRIPTION_TYPE") ?: ""
        paymentMethod = intent.getStringExtra("PAYMENT_METHOD") ?: ""
        totalAmount = intent.getDoubleExtra("TOTAL_AMOUNT", 0.0)
        paymentVerified = intent.getBooleanExtra("PAYMENT_VERIFIED", false)
        proofImageUrl = intent.getStringExtra("PAYMENT_IMAGE")
        notes = intent.getStringExtra("NOTES") ?: ""
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Update Status Order"

        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        // Setup status buttons
        binding.btnPending.setOnClickListener {
            if (currentStatus != "pending") {
                showConfirmationDialog("pending", "🕐 PENDING")
            } else {
                Toast.makeText(this, "Status sudah PENDING", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnConfirmed.setOnClickListener {
            if (currentStatus != "confirmed") {
                showConfirmationDialog("confirmed", "✅ CONFIRMED")
            } else {
                Toast.makeText(this, "Status sudah CONFIRMED", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnCompleted.setOnClickListener {
            if (currentStatus != "completed") {
                showConfirmationDialog("completed", "🎉 COMPLETED")
            } else {
                Toast.makeText(this, "Status sudah COMPLETED", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnCancelled.setOnClickListener {
            if (currentStatus != "cancelled") {
                showConfirmationDialog("cancelled", "❌ CANCELLED")
            } else {
                Toast.makeText(this, "Status sudah CANCELLED", Toast.LENGTH_SHORT).show()
            }
        }

        // Verify payment button
        binding.btnVerifyPayment.setOnClickListener {
            if (!paymentVerified) {
                showVerifyPaymentDialog()
            } else {
                Toast.makeText(this, "Pembayaran sudah diverifikasi", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displayOrderInfo() {
        binding.apply {
            // Order info
            tvOrderId.text = "#${orderId.take(8).uppercase()}"
            tvCustomerName.text = customerName
            tvCustomerEmail.text = customerEmail
            tvChannelName.text = channelName
            tvSubscriptionType.text = subscriptionType.uppercase()
            tvPaymentMethod.text = paymentMethod
            tvTotalAmount.text = "Rp ${String.format("%,.0f", totalAmount)}"

            // Current status
            updateCurrentStatusDisplay()

            // Payment verification status
            updatePaymentStatusDisplay()

            if (!proofImageUrl.isNullOrEmpty()) {
                binding.imgPaymentProof.visibility = View.VISIBLE

                // Jika pakai Glide
                Glide.with(binding.root.context)
                    .load(proofImageUrl)
                    .centerCrop() // <- Tambahan
                    .into(binding.imgPaymentProof)

            } else {
                binding.imgPaymentProof.visibility = View.GONE
            }


            // Notes
            if (notes.isNotEmpty()) {
                tvNotes.text = notes
                tvNotes.visibility = View.VISIBLE
                labelNotes.visibility = View.VISIBLE
            } else {
                tvNotes.visibility = View.GONE
                labelNotes.visibility = View.GONE
            }

            // Update button states
            updateButtonStates()
        }
    }

    private fun updateCurrentStatusDisplay() {
        val (statusEmoji, statusText) = when (currentStatus.lowercase()) {
            "pending" -> Pair("🕐", "PENDING")
            "confirmed" -> Pair("✅", "CONFIRMED")
            "completed" -> Pair("🎉", "COMPLETED")
            "cancelled" -> Pair("❌", "CANCELLED")
            else -> Pair("📋", currentStatus.uppercase())
        }

        binding.tvCurrentStatus.text = "$statusEmoji $statusText"

        // Set background color based on status
        val backgroundColor = when (currentStatus.lowercase()) {
            "pending" -> R.color.status_pending
            "confirmed" -> R.color.status_confirmed
            "completed" -> R.color.status_completed
            "cancelled" -> R.color.status_cancelled
            else -> R.color.status_pending
        }

        binding.tvCurrentStatus.setBackgroundResource(backgroundColor)
    }

    private fun updatePaymentStatusDisplay() {
        val paymentStatus = if (paymentVerified) "✅ Verified" else "⏳ Pending"
        binding.tvPaymentStatus.text = paymentStatus

        // Update verify payment button
        if (paymentVerified) {
            binding.btnVerifyPayment.text = "✅ Sudah Terverifikasi"
            binding.btnVerifyPayment.isEnabled = false
        } else {
            binding.btnVerifyPayment.text = "💰 Verifikasi Pembayaran"
            binding.btnVerifyPayment.isEnabled = true
        }
    }

    private fun updateButtonStates() {
        binding.apply {
            // Disable current status button
            when (currentStatus.lowercase()) {
                "pending" -> {
                    btnPending.isEnabled = false
                    btnPending.alpha = 0.5f
                }
                "confirmed" -> {
                    btnConfirmed.isEnabled = false
                    btnConfirmed.alpha = 0.5f
                }
                "completed" -> {
                    btnCompleted.isEnabled = false
                    btnCompleted.alpha = 0.5f
                }
                "cancelled" -> {
                    btnCancelled.isEnabled = false
                    btnCancelled.alpha = 0.5f
                }
            }
        }
    }

    private fun showConfirmationDialog(newStatus: String, statusDisplay: String) {
        val currentDisplay = when (currentStatus.lowercase()) {
            "pending" -> "🕐 PENDING"
            "confirmed" -> "✅ CONFIRMED"
            "completed" -> "🎉 COMPLETED"
            "cancelled" -> "❌ CANCELLED"
            else -> currentStatus.uppercase()
        }

        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Update Status")
            .setMessage("Apakah Anda yakin ingin mengubah status order ini?\n\n" +
                    "Order ID: #${orderId.take(8).uppercase()}\n" +
                    "Customer: $customerName\n" +
                    "Channel: $channelName\n\n" +
                    "Status saat ini: $currentDisplay\n" +
                    "Status baru: $statusDisplay")
            .setPositiveButton("Ya, Update") { _, _ ->
                updateOrderStatus(newStatus)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showVerifyPaymentDialog() {
        AlertDialog.Builder(this)
            .setTitle("Verifikasi Pembayaran")
            .setMessage("Apakah Anda yakin ingin memverifikasi pembayaran untuk order ini?\n\n" +
                    "Order ID: #${orderId.take(8).uppercase()}\n" +
                    "Customer: $customerName\n" +
                    "Channel: $channelName\n" +
                    "Total: Rp ${String.format("%,.0f", totalAmount)}\n" +
                    "Metode: $paymentMethod")
            .setPositiveButton("✅ Verifikasi") { _, _ ->
                verifyPayment()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun updateOrderStatus(newStatus: String) {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                binding.layoutButtons.visibility = View.GONE

                // Update order status
                adminRepository.updateOrderStatus(orderId, newStatus)

                // Update current status
                currentStatus = newStatus

                // Update UI
                updateCurrentStatusDisplay()
                updateButtonStates()

                val statusEmoji = when (newStatus.lowercase()) {
                    "pending" -> "🕐"
                    "confirmed" -> "✅"
                    "completed" -> "🎉"
                    "cancelled" -> "❌"
                    else -> "📋"
                }

                Toast.makeText(this@OrderStatusUpdateActivity,
                    "$statusEmoji Status berhasil diubah ke ${newStatus.uppercase()}",
                    Toast.LENGTH_SHORT).show()

                // Set result to notify parent activity
                setResult(RESULT_OK)

            } catch (e: Exception) {
                Toast.makeText(this@OrderStatusUpdateActivity,
                    "Error updating status: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.layoutButtons.visibility = View.VISIBLE
            }
        }
    }

    private fun verifyPayment() {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                binding.layoutButtons.visibility = View.GONE

                // Verify payment
                adminRepository.verifyPayment(orderId, true)

                // Update payment status
                paymentVerified = true

                // If payment is verified and status is pending, auto-update to confirmed
                if (currentStatus == "pending") {
                    adminRepository.updateOrderStatus(orderId, "confirmed")
                    currentStatus = "confirmed"
                    updateCurrentStatusDisplay()
                    updateButtonStates()
                }

                // Update payment status display
                updatePaymentStatusDisplay()

                Toast.makeText(this@OrderStatusUpdateActivity,
                    "✅ Pembayaran berhasil diverifikasi",
                    Toast.LENGTH_SHORT).show()

                // Set result to notify parent activity
                setResult(RESULT_OK)

            } catch (e: Exception) {
                Toast.makeText(this@OrderStatusUpdateActivity,
                    "Error verifying payment: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.layoutButtons.visibility = View.VISIBLE
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // Return to parent activity
        finish()
    }
}