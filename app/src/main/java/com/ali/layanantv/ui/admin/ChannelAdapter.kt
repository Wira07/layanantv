package com.ali.layanantv.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
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

                val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
                tvChannelPrice.text = formatter.format(channel.price)

                // Status indicator
                val statusText = if (channel.isActive) "Aktif" else "Nonaktif"
                val statusColor = if (channel.isActive)
                    itemView.context.getColor(R.color.success_color)
                else
                    itemView.context.getColor(R.color.error_color)

                tvChannelStatus.text = statusText
                tvChannelStatus.setTextColor(statusColor)

                // Load channel logo
                if (channel.logoUrl.isNotEmpty()) {
                    Glide.with(itemView.context)
                        .load(channel.logoUrl)
                        .placeholder(R.drawable.ic_tv)
                        .error(R.drawable.ic_tv)
                        .into(ivChannelLogo)
                } else {
                    ivChannelLogo.setImageResource(R.drawable.ic_tv)
                }

                // Button listeners
                btnEditChannel.setOnClickListener {
                    onEditClick(channel)
                }

                btnDeleteChannel.setOnClickListener {
                    onDeleteClick(channel)
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