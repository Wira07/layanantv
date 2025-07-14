package com.ali.layanantv.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ali.layanantv.R
import com.ali.layanantv.data.model.Channel
import com.ali.layanantv.databinding.ItemChannelAdminBinding
import com.bumptech.glide.Glide
import java.text.NumberFormat
import java.util.Locale

class ChannelAdapter(
    private val onEditClick: (Channel) -> Unit,
    private val onDeleteClick: (Channel) -> Unit
) : ListAdapter<Channel, ChannelAdapter.ChannelViewHolder>(ChannelDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val binding = ItemChannelAdminBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ChannelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ChannelViewHolder(
        private val binding: ItemChannelAdminBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(channel: Channel) {
            binding.apply {
                tvChannelName.text = channel.name
                tvChannelDescription.text = channel.description
                tvChannelCategory.text = channel.category

                // Format price dengan safety check
                try {
                    val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
                    tvChannelPrice.text = formatter.format(channel.price)
                } catch (e: Exception) {
                    tvChannelPrice.text = "Rp ${channel.price}"
                }

                // Status indicator
                val statusText = if (channel.isActive) "Aktif" else "Nonaktif"
                val statusColor = if (channel.isActive)
                    ContextCompat.getColor(itemView.context, android.R.color.holo_green_dark)
                else
                    ContextCompat.getColor(itemView.context, android.R.color.holo_red_dark)

                tvChannelStatus.text = statusText
                tvChannelStatus.setTextColor(statusColor)

                // Load channel logo dengan prioritas Base64 -> URL -> placeholder
                try {
                    when {
                        channel.logoBase64.isNotEmpty() -> {
                            val base64String = if (channel.logoBase64.startsWith("data:image")) {
                                channel.logoBase64
                            } else {
                                "data:image/jpeg;base64,${channel.logoBase64}"
                            }
                            Glide.with(itemView.context)
                                .load(base64String)
                                .placeholder(R.drawable.ic_tv)
                                .error(R.drawable.ic_tv)
                                .into(ivChannelLogo)
                        }
                        channel.logoUrl.isNotEmpty() -> {
                            Glide.with(itemView.context)
                                .load(channel.logoUrl)
                                .placeholder(R.drawable.ic_tv)
                                .error(R.drawable.ic_tv)
                                .into(ivChannelLogo)
                        }
                        else -> {
                            ivChannelLogo.setImageResource(R.drawable.ic_tv)
                        }
                    }
                } catch (e: Exception) {
                    // Fallback jika ada error loading gambar
                    ivChannelLogo.setImageResource(R.drawable.ic_tv)
                }

                // Button listeners dengan safety check
                btnEditChannel.setOnClickListener {
                    try {
                        onEditClick(channel)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                btnDeleteChannel.setOnClickListener {
                    try {
                        onDeleteClick(channel)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    class ChannelDiffCallback : DiffUtil.ItemCallback<Channel>() {
        override fun areItemsTheSame(oldItem: Channel, newItem: Channel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Channel, newItem: Channel): Boolean {
            return oldItem == newItem
        }
    }
}