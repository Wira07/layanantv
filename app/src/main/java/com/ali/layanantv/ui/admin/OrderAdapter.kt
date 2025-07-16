package com.ali.layanantv.ui.admin

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ali.layanantv.R
import com.ali.layanantv.data.model.Order
import com.ali.layanantv.databinding.ItemOrderAdminBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderAdapter(
    private val onVerifyPayment: (Order) -> Unit,
    private val onUpdateStatus: (Order) -> Unit,
    private val onViewDetails: (Order) -> Unit
) : ListAdapter<Order, OrderAdapter.OrderViewHolder>(OrderDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemOrderAdminBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class OrderViewHolder(
        private val binding: ItemOrderAdminBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(order: Order) {
            binding.apply {
                // Order ID with better formatting
                tvOrderId.text = "#${order.id.take(8).uppercase()}"

                // Customer info
                tvCustomerName.text = order.userName
                tvCustomerEmail.text = order.userEmail

                // Channel info
                tvChannelName.text = order.channelName
                tvSubscriptionType.text = order.subscriptionType.uppercase()

                // Payment info
                tvPaymentMethod.text = order.paymentMethod
                tvTotalAmount.text = formatCurrency(order.totalAmount)

                // Payment verification status with emoji
                val paymentStatus = if (order.paymentVerified) "✅ Verified" else "⏳ Pending"
                tvPaymentVerified.text = paymentStatus

                // Order date with better formatting
                val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                tvOrderDate.text = dateFormat.format(Date(order.createdAt.seconds * 1000))

                // Order status with emoji
                updateStatusUI(order.status)

                // Notes
                if (order.notes.isNotEmpty()) {
                    tvNotes.visibility = View.VISIBLE
                    tvNotes.text = "📝 ${order.notes}"
                    layoutNotes.visibility = View.VISIBLE
                } else {
                    tvNotes.visibility = View.GONE
                    layoutNotes.visibility = View.GONE
                }

                // Button actions
                btnViewDetails.setOnClickListener {
                    navigateToOrderStatusUpdate(order)
                }

                btnVerifyPayment.setOnClickListener {
                    onVerifyPayment(order)
                }

                btnUpdateStatus.setOnClickListener {
                    navigateToOrderStatusUpdate(order)
                }

                // Button visibility and state
                updateButtonVisibility(order)

                // Add click ripple effect to entire card - Navigate to OrderStatusUpdateActivity
                root.setOnClickListener {
                    navigateToOrderStatusUpdate(order)
                }

                // Add subtle animation for better UX
                root.alpha = 0f
                root.animate().alpha(1f).setDuration(300).start()
            }
        }

        private fun navigateToOrderStatusUpdate(order: Order) {
            val context = binding.root.context
            val intent = Intent(context, OrderStatusUpdateActivity::class.java).apply {
                putExtra("ORDER_ID", order.id)
                putExtra("ORDER_STATUS", order.status)
                putExtra("CUSTOMER_NAME", order.userName)
                putExtra("CUSTOMER_EMAIL", order.userEmail)
                putExtra("CHANNEL_NAME", order.channelName)
                putExtra("SUBSCRIPTION_TYPE", order.subscriptionType)
                putExtra("PAYMENT_METHOD", order.paymentMethod)
                putExtra("TOTAL_AMOUNT", order.totalAmount)
                putExtra("PAYMENT_VERIFIED", order.paymentVerified)
                putExtra("NOTES", order.notes)

                // Optional: Add more data if needed
                putExtra("ORDER_DATE", order.createdAt.seconds * 1000)
                putExtra("USER_ID", order.userId)
                putExtra("CHANNEL_ID", order.channelId)
            }
            context.startActivity(intent)
        }

        private fun updateStatusUI(status: String) {
            val (statusEmoji, statusText) = when (status.lowercase()) {
                "pending" -> Pair("🕐", "PENDING")
                "confirmed" -> Pair("✅", "CONFIRMED")
                "completed" -> Pair("🎉", "COMPLETED")
                "cancelled" -> Pair("❌", "CANCELLED")
                else -> Pair("📋", status.uppercase())
            }

            binding.tvOrderStatus.text = "$statusEmoji $statusText"

            val context = binding.root.context
            val (backgroundColor, textColor) = when (status.lowercase()) {
                "pending" -> Pair(
                    ContextCompat.getColor(context, R.color.status_pending),
                    ContextCompat.getColor(context, android.R.color.white)
                )
                "confirmed" -> Pair(
                    ContextCompat.getColor(context, R.color.status_confirmed),
                    ContextCompat.getColor(context, android.R.color.white)
                )
                "completed" -> Pair(
                    ContextCompat.getColor(context, R.color.status_completed),
                    ContextCompat.getColor(context, android.R.color.white)
                )
                "cancelled" -> Pair(
                    ContextCompat.getColor(context, R.color.status_cancelled),
                    ContextCompat.getColor(context, android.R.color.white)
                )
                else -> Pair(
                    ContextCompat.getColor(context, R.color.status_pending),
                    ContextCompat.getColor(context, android.R.color.white)
                )
            }

            binding.tvOrderStatus.setBackgroundColor(backgroundColor)
            binding.tvOrderStatus.setTextColor(textColor)
        }

        private fun updateButtonVisibility(order: Order) {
            binding.apply {
                // Detail button is always visible
                btnViewDetails.visibility = View.VISIBLE
                btnViewDetails.isEnabled = true

                when (order.status.lowercase()) {
                    "pending" -> {
                        // Show verify payment button only if payment is not verified
                        btnVerifyPayment.visibility = if (!order.paymentVerified) View.VISIBLE else View.GONE
                        btnUpdateStatus.visibility = View.VISIBLE

                        // Update button text and functionality
                        if (order.paymentVerified) {
                            btnVerifyPayment.text = "✅ Verified"
                            btnVerifyPayment.isEnabled = false
                        } else {
                            btnVerifyPayment.text = "💰 Verifikasi"
                            btnVerifyPayment.isEnabled = true
                        }

                        // Status button should show next logical status
                        btnUpdateStatus.text = "✅ Confirm"
                        btnUpdateStatus.isEnabled = true
                    }
                    "confirmed" -> {
                        btnVerifyPayment.visibility = View.GONE
                        btnUpdateStatus.visibility = View.VISIBLE
                        btnUpdateStatus.isEnabled = true
                        btnUpdateStatus.text = "🎉 Complete"
                    }
                    "completed" -> {
                        btnVerifyPayment.visibility = View.GONE
                        btnUpdateStatus.visibility = View.VISIBLE
                        btnUpdateStatus.isEnabled = true
                        btnUpdateStatus.text = "🔄 Change"
                    }
                    "cancelled" -> {
                        btnVerifyPayment.visibility = View.GONE
                        btnUpdateStatus.visibility = View.VISIBLE
                        btnUpdateStatus.isEnabled = true
                        btnUpdateStatus.text = "🔄 Restore"
                    }
                    else -> {
                        btnVerifyPayment.visibility = View.VISIBLE
                        btnUpdateStatus.visibility = View.VISIBLE
                        btnVerifyPayment.isEnabled = true
                        btnUpdateStatus.isEnabled = true
                        btnVerifyPayment.text = "💰 Verifikasi"
                        btnUpdateStatus.text = "🔄 Status"
                    }
                }
            }
        }

        private fun formatCurrency(amount: Double): String {
            val formatter = NumberFormat.getInstance(Locale("id", "ID"))
            return "Rp ${formatter.format(amount)}"
        }
    }

    class OrderDiffCallback : DiffUtil.ItemCallback<Order>() {
        override fun areItemsTheSame(oldItem: Order, newItem: Order): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Order, newItem: Order): Boolean {
            return oldItem == newItem
        }
    }
}