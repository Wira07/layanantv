// PaymentActivity.kt
package com.ali.layanantv.ui.customer

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ali.layanantv.databinding.ActivityPaymentBinding
import com.ali.layanantv.data.repository.CustomerRepository
import com.ali.layanantv.data.model.Channel
import com.ali.layanantv.data.model.Order
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

class PaymentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPaymentBinding
    private lateinit var customerRepository: CustomerRepository
    private var selectedChannel: Channel? = null
    private var originalPrice: Double = 0.0
    private var userPoints: Int = 0
    private var pointsToUse: Int = 0
    private var finalPrice: Double = 0.0
    private var subscriptionType: String = "1_month"

    companion object {
        const val EXTRA_CHANNEL_ID = "channel_id"
        const val EXTRA_SUBSCRIPTION_TYPE = "subscription_type"
        const val MIN_POINTS_USAGE = 100
        const val POINT_TO_RUPIAH = 1 // 1 point = Rp 1
        const val MAX_DISCOUNT_PERCENTAGE = 0.5 // 50%
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        customerRepository = CustomerRepository()

        // Get data dari intent
        val channelId = intent.getStringExtra(EXTRA_CHANNEL_ID) ?: ""
        subscriptionType = intent.getStringExtra(EXTRA_SUBSCRIPTION_TYPE) ?: "1_month"

        setupUI()
        loadChannelData(channelId)
        loadUserPoints()
    }

    private fun setupUI() {
        binding.apply {
            // Back button
            btnBack.setOnClickListener { finish() }

            // Setup subscription type selection
            setupSubscriptionTypeSelection()

            // Setup point usage input
            setupPointUsageInput()

            // Payment button
            btnPayment.setOnClickListener {
                processPayment()
            }

            // Toggle point usage
            switchUsePoints.setOnCheckedChangeListener { _, isChecked ->
                togglePointUsage(isChecked)
            }
        }
    }

    private fun setupSubscriptionTypeSelection() {
        binding.apply {
            // Default selection
            updateSubscriptionSelection("1_month")

            btnSubscription1Month.setOnClickListener {
                updateSubscriptionSelection("1_month")
            }

            btnSubscription3Month.setOnClickListener {
                updateSubscriptionSelection("3_month")
            }

            btnSubscription12Month.setOnClickListener {
                updateSubscriptionSelection("12_month")
            }
        }
    }

    private fun updateSubscriptionSelection(type: String) {
        subscriptionType = type

        // Update UI selection state
        binding.apply {
            btnSubscription1Month.isSelected = type == "1_month"
            btnSubscription3Month.isSelected = type == "3_month"
            btnSubscription12Month.isSelected = type == "12_month"
        }

        // Recalculate price
        calculatePrice()
    }

    private fun setupPointUsageInput() {
        binding.etPointsToUse.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val input = s.toString()
                if (input.isNotEmpty()) {
                    try {
                        val points = input.toInt()
                        validateAndUpdatePointUsage(points)
                    } catch (e: NumberFormatException) {
                        binding.tvPointsError.text = "Format angka tidak valid"
                        binding.tvPointsError.visibility = android.view.View.VISIBLE
                    }
                } else {
                    pointsToUse = 0
                    calculateFinalPrice()
                }
            }
        })
    }

    private fun validateAndUpdatePointUsage(points: Int) {
        binding.apply {
            tvPointsError.visibility = android.view.View.GONE

            when {
                points < MIN_POINTS_USAGE -> {
                    tvPointsError.text = "Minimal penggunaan $MIN_POINTS_USAGE point"
                    tvPointsError.visibility = android.view.View.VISIBLE
                    pointsToUse = 0
                }
                points > userPoints -> {
                    tvPointsError.text = "Point tidak mencukupi. Maksimal: $userPoints point"
                    tvPointsError.visibility = android.view.View.VISIBLE
                    pointsToUse = userPoints
                }
                points > (originalPrice * MAX_DISCOUNT_PERCENTAGE).toInt() -> {
                    val maxPoints = (originalPrice * MAX_DISCOUNT_PERCENTAGE).toInt()
                    tvPointsError.text = "Maksimal penggunaan: $maxPoints point (50% dari harga)"
                    tvPointsError.visibility = android.view.View.VISIBLE
                    pointsToUse = maxPoints
                }
                else -> {
                    pointsToUse = points
                }
            }

            calculateFinalPrice()
        }
    }

    private fun togglePointUsage(isEnabled: Boolean) {
        binding.apply {
            layoutPointUsage.visibility = if (isEnabled) android.view.View.VISIBLE else android.view.View.GONE

            if (!isEnabled) {
                pointsToUse = 0
                etPointsToUse.text?.clear()
                calculateFinalPrice()
            }
        }
    }

    private fun loadChannelData(channelId: String) {
        lifecycleScope.launch {
            try {
                selectedChannel = customerRepository.getChannelById(channelId)
                selectedChannel?.let { channel ->
                    binding.apply {
                        tvChannelName.text = channel.name
                        tvChannelDescription.text = channel.description
                        // Load channel image if available
                        // Glide.with(this@PaymentActivity).load(channel.imageUrl).into(ivChannelImage)
                    }

                    calculatePrice()
                } ?: run {
                    Toast.makeText(this@PaymentActivity, "Channel tidak ditemukan", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this@PaymentActivity, "Error loading channel: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun loadUserPoints() {
        lifecycleScope.launch {
            try {
                userPoints = customerRepository.getUserPoints()
                binding.apply {
                    tvAvailablePoints.text = "Point tersedia: ${formatNumber(userPoints)}"
                    tvPointsInfo.text = "1 Point = Rp 1 • Min: $MIN_POINTS_USAGE • Max: 50% dari harga"
                }
            } catch (e: Exception) {
                userPoints = 0
                binding.tvAvailablePoints.text = "Point tersedia: 0"
            }
        }
    }

    private fun calculatePrice() {
        selectedChannel?.let { channel ->
            originalPrice = when (subscriptionType) {
                "1_month" -> channel.price
                "3_month" -> channel.price * 3 * 0.95 // 5% discount
                "12_month" -> channel.price * 12 * 0.85 // 15% discount
                else -> channel.price
            }

            calculateFinalPrice()
        }
    }

    private fun calculateFinalPrice() {
        val pointDiscount = pointsToUse * POINT_TO_RUPIAH
        finalPrice = originalPrice - pointDiscount

        binding.apply {
            tvOriginalPrice.text = "Harga Asli: ${formatCurrency(originalPrice)}"

            if (pointsToUse > 0) {
                tvPointDiscount.text = "Diskon Point: -${formatCurrency(pointDiscount.toDouble())}"
                tvPointDiscount.visibility = android.view.View.VISIBLE
                tvPointsUsed.text = "Menggunakan $pointsToUse point"
                tvPointsUsed.visibility = android.view.View.VISIBLE
            } else {
                tvPointDiscount.visibility = android.view.View.GONE
                tvPointsUsed.visibility = android.view.View.GONE
            }

            tvFinalPrice.text = formatCurrency(finalPrice)
            btnPayment.text = "Bayar ${formatCurrency(finalPrice)}"
        }
    }

    private fun processPayment() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Silakan login terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        selectedChannel?.let { channel ->
            lifecycleScope.launch {
                try {
                    // Disable payment button
                    binding.btnPayment.isEnabled = false
                    binding.btnPayment.text = "Memproses..."

                    // Create order
                    val order = Order(
                        userId = currentUser.uid,
                        channelId = channel.id,
                        channelName = channel.name,
                        subscriptionType = subscriptionType,
                        originalAmount = originalPrice,
                        pointsUsed = pointsToUse,
                        pointDiscount = (pointsToUse * POINT_TO_RUPIAH).toDouble(),
                        totalAmount = finalPrice,
                        status = "pending",
                        createdAt = Timestamp.now(),
                        updatedAt = Timestamp.now()
                    )

                    val orderId = customerRepository.createOrder(order)

                    // Simulate payment process
                    kotlinx.coroutines.delay(2000)

                    // Update order status to completed
                    val completedOrder = order.copy(
                        id = orderId,
                        status = "completed",
                        paymentVerified = true,
                        updatedAt = Timestamp.now()
                    )

                    // Create subscription
                    customerRepository.createSubscription(completedOrder)

                    // Deduct points from user if points were used
                    if (pointsToUse > 0) {
                        customerRepository.deductUserPoints(pointsToUse)
                    }

                    Toast.makeText(this@PaymentActivity, "Pembayaran berhasil! Langganan aktif.", Toast.LENGTH_LONG).show()

                    // Return to previous screen
                    finish()

                } catch (e: Exception) {
                    Toast.makeText(this@PaymentActivity, "Pembayaran gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                    binding.btnPayment.isEnabled = true
                    binding.btnPayment.text = "Bayar ${formatCurrency(finalPrice)}"
                }
            }
        }
    }

    private fun formatCurrency(amount: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        return formatter.format(amount)
    }

    private fun formatNumber(number: Int): String {
        return NumberFormat.getNumberInstance(Locale("id", "ID")).format(number)
    }
}