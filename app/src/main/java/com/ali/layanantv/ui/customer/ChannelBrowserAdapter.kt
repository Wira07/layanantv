package com.ali.layanantv.ui.customer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ali.layanantv.data.model.Channel
import com.ali.layanantv.databinding.ItemChannelBrowserBinding
import com.bumptech.glide.Glide
import java.text.NumberFormat
import java.util.*

class ChannelBrowserAdapter(
    private val onChannelClick: (Channel) -> Unit
) : ListAdapter<Channel, ChannelBrowserAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemChannelBrowserBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemChannelBrowserBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(channel: Channel) {
            binding.apply {
                tvChannelName.text = channel.name
                tvChannelDescription.text = channel.description
                tvChannelCategory.text = channel.category

                // Format price
                val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
                tvChannelPrice.text = "${formatter.format(channel.price)}/bulan"

                // Load channel logo
                if (channel.logoBase64.isNotEmpty()) {
                    val base64String = if (channel.logoBase64.startsWith("data:image")) {
                        channel.logoBase64
                    } else {
                        "data:image/jpeg;base64,${channel.logoBase64}"
                    }
                    Glide.with(binding.root.context)
                        .load(base64String)
                        .into(ivChannelLogo)
                } else if (channel.logoUrl.isNotEmpty()) {
                    Glide.with(binding.root.context)
                        .load(channel.logoUrl)
                        .into(ivChannelLogo)
                }

                // Set click listener
                root.setOnClickListener { onChannelClick(channel) }
                btnSubscribe.setOnClickListener { onChannelClick(channel) }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Channel>() {
        override fun areItemsTheSame(oldItem: Channel, newItem: Channel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Channel, newItem: Channel): Boolean {
            return oldItem == newItem
        }
    }
}