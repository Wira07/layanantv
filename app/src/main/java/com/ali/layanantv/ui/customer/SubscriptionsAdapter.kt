package com.ali.layanantv.ui.customer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ali.layanantv.R
import com.ali.layanantv.data.model.Order
import com.ali.layanantv.databinding.ItemSubscriptionBinding
import com.bumptech.glide.Glide
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class SubscriptionsAdapter(
    private val onRenewClick: (Order) -> Unit,
    private val onCancelClick: (Order) -> Unit
) : ListAdapter<Order, SubscriptionsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSubscriptionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemSubscriptionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(order: Order) {
            binding.apply {
                // Basic info
                tvChannelName.text = order.channelName
                tvSubscriptionType.text = when (order.subscriptionType) {
                    "monthly" -> "Bulanan"
                    "yearly" -> "Tahunan"
                    else -> order.subscriptionType
                }

                // Format price dengan safety check
                try {
                    val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
                    tvPrice.text = formatter.format(order.totalAmount)
                } catch (e: Exception) {
                    tvPrice.text = "Rp ${order.totalAmount}"
                }

                // Format date dengan safety check
                try {
                    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
                    // Pastikan createdAt tidak null
                    val createdDate = order.createdAt?.toDate()
                    if (createdDate != null) {
                        tvCreatedDate.text = "Berlangganan sejak: ${dateFormat.format(createdDate)}"
                    } else {
                        tvCreatedDate.text = "Berlangganan sejak: -"
                    }
                } catch (e: Exception) {
                    tvCreatedDate.text = "Berlangganan sejak: -"
                }

                // Status
                tvStatus.text = when (order.status) {
                    "pending" -> "Menunggu"
                    "confirmed" -> "Dikonfirmasi"
                    "completed" -> "Aktif"
                    "cancelled" -> "Dibatalkan"
                    else -> order.status
                }

                // Set status color dengan resource color
                val statusColor = when (order.status) {
                    "completed" -> ContextCompat.getColor(binding.root.context, android.R.color.holo_green_dark)
                    "pending" -> ContextCompat.getColor(binding.root.context, android.R.color.holo_orange_dark)
                    "cancelled" -> ContextCompat.getColor(binding.root.context, android.R.color.holo_red_dark)
                    else -> ContextCompat.getColor(binding.root.context, android.R.color.darker_gray)
                }
                tvStatus.setTextColor(statusColor)

                // Button listeners dengan safety check
                btnRenew.setOnClickListener {
                    try {
                        onRenewClick(order)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                btnCancel.setOnClickListener {
                    try {
                        onCancelClick(order)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                // Enable/disable buttons based on status
                btnRenew.isEnabled = order.status == "completed"
                btnCancel.isEnabled = order.status != "cancelled"

                // Optional: Set button alpha untuk visual feedback
                btnRenew.alpha = if (btnRenew.isEnabled) 1.0f else 0.5f
                btnCancel.alpha = if (btnCancel.isEnabled) 1.0f else 0.5f
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Order>() {
        override fun areItemsTheSame(oldItem: Order, newItem: Order): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Order, newItem: Order): Boolean {
            return oldItem == newItem
        }
    }
}