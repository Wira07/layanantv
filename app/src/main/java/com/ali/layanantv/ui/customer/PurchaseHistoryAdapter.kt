package com.ali.layanantv.ui.customer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ali.layanantv.data.model.Order
import com.ali.layanantv.databinding.ItemPurchaseHistoryBinding
import java.text.SimpleDateFormat
import java.util.*

class PurchaseHistoryAdapter(
    private val onItemClick: (Order) -> Unit,
    private val onReorderClick: (Order) -> Unit
) : ListAdapter<Order, PurchaseHistoryAdapter.PurchaseHistoryViewHolder>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<Order>() {
        override fun areItemsTheSame(oldItem: Order, newItem: Order): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Order, newItem: Order): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PurchaseHistoryViewHolder {
        val binding = ItemPurchaseHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PurchaseHistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PurchaseHistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PurchaseHistoryViewHolder(
        private val binding: ItemPurchaseHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(order: Order) {
            binding.apply {
                tvChannelName.text = order.channelName
                tvSubscriptionType.text = when (order.subscriptionType) {
                    "monthly" -> "Bulanan"
                    "yearly" -> "Tahunan"
                    else -> order.subscriptionType
                }

                // Format price dengan safety check
                try {
                    tvAmount.text = "Rp ${String.format("%,d", order.totalAmount.toInt())}"
                } catch (e: Exception) {
                    tvAmount.text = "Rp ${order.totalAmount}"
                }

                // Format status
                tvStatus.text = when (order.status) {
                    "pending" -> "Menunggu"
                    "confirmed" -> "Dikonfirmasi"
                    "completed" -> "Selesai"
                    "cancelled" -> "Dibatalkan"
                    else -> order.status.uppercase()
                }

                // Format date dengan safety check
                try {
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val createdDate = order.createdAt?.toDate()
                    if (createdDate != null) {
                        tvDate.text = dateFormat.format(createdDate)
                    } else {
                        tvDate.text = "-"
                    }
                } catch (e: Exception) {
                    tvDate.text = "-"
                }

                // Set status color dengan safety check
                val statusColor = when (order.status) {
                    "completed" -> ContextCompat.getColor(binding.root.context, android.R.color.holo_green_dark)
                    "pending" -> ContextCompat.getColor(binding.root.context, android.R.color.holo_orange_dark)
                    "cancelled" -> ContextCompat.getColor(binding.root.context, android.R.color.holo_red_dark)
                    else -> ContextCompat.getColor(binding.root.context, android.R.color.darker_gray)
                }
                tvStatus.setTextColor(statusColor)

                // Set click listeners dengan safety check
                root.setOnClickListener {
                    try {
                        onItemClick(order)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                btnReorder.setOnClickListener {
                    try {
                        onReorderClick(order)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                // Enable/disable reorder button based on status
                btnReorder.isEnabled = order.status == "completed" || order.status == "cancelled"
                btnReorder.alpha = if (btnReorder.isEnabled) 1.0f else 0.5f
            }
        }
    }
}