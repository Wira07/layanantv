package com.ali.layanantv.ui.admin

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
                } else {
                    tvNotes.visibility = View.GONE
                }

                // Button actions
                btnVerifyPayment.setOnClickListener {
                    onVerifyPayment(order)
                }

                btnUpdateStatus.setOnClickListener {
                    onUpdateStatus(order)
                }

                // Card click for details
                root.setOnClickListener {
                    onViewDetails(order)
                }

                // Button visibility and state
                updateButtonVisibility(order)

                // Add subtle animation for better UX
                root.alpha = 0f
                root.animate().alpha(1f).setDuration(300).start()
            }
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
            when (order.status.lowercase()) {
                "pending" -> {
                    // Show verify payment button only if payment is not verified
                    binding.btnVerifyPayment.visibility = if (!order.paymentVerified) View.VISIBLE else View.GONE
                    binding.btnUpdateStatus.visibility = View.VISIBLE
                    binding.btnVerifyPayment.isEnabled = true
                    binding.btnUpdateStatus.isEnabled = true

                    // Update button text based on payment status
                    binding.btnVerifyPayment.text = if (order.paymentVerified) "✅ Verified" else "⏳ Verify Payment"
                }
                "confirmed" -> {
                    binding.btnVerifyPayment.visibility = View.GONE
                    binding.btnUpdateStatus.visibility = View.VISIBLE
                    binding.btnUpdateStatus.isEnabled = true
                    binding.btnUpdateStatus.text = "Update Status"
                }
                "completed" -> {
                    binding.btnVerifyPayment.visibility = View.GONE
                    binding.btnUpdateStatus.visibility = View.VISIBLE
                    binding.btnUpdateStatus.isEnabled = true
                    binding.btnUpdateStatus.text = "Change Status"
                }
                "cancelled" -> {
                    binding.btnVerifyPayment.visibility = View.GONE
                    binding.btnUpdateStatus.visibility = View.VISIBLE
                    binding.btnUpdateStatus.isEnabled = true
                    binding.btnUpdateStatus.text = "Change Status"
                }
                else -> {
                    binding.btnVerifyPayment.visibility = View.VISIBLE
                    binding.btnUpdateStatus.visibility = View.VISIBLE
                    binding.btnVerifyPayment.isEnabled = true
                    binding.btnUpdateStatus.isEnabled = true
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