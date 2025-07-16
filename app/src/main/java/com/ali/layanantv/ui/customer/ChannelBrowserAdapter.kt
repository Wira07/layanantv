package com.ali.layanantv.ui.customer

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ali.layanantv.data.model.Channel
import com.ali.layanantv.databinding.ItemChannelBrowserBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import java.text.NumberFormat
import java.util.*

class ChannelBrowserAdapter(
    private val onChannelClick: (Channel) -> Unit
) : ListAdapter<Channel, ChannelBrowserAdapter.ViewHolder>(DiffCallback()) {

    companion object {
        private const val TAG = "ChannelBrowserAdapter"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemChannelBrowserBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val channel = getItem(position)
        Log.d(TAG, "Binding channel at position $position: ${channel.name}")
        holder.bind(channel)
    }

    inner class ViewHolder(private val binding: ItemChannelBrowserBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(channel: Channel) {
            Log.d(TAG, "Binding channel: ${channel.name} (ID: ${channel.id})")

            binding.apply {
                // Set basic channel info
                tvChannelName.text = channel.name
                tvChannelDescription.text = channel.description
                tvChannelCategory.text = channel.category

                // Format price
                val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
                tvChannelPrice.text = "${formatter.format(channel.price)}/bulan"

                // Load channel logo with improved error handling
                loadChannelLogo(channel)

                // Set click listener for card (for viewing details)
                root.setOnClickListener {
                    Log.d(TAG, "Channel card clicked: ${channel.name}")
                    onChannelClick(channel)
                }

                // Set click listener for subscribe button (direct to payment)
                btnSubscribe.setOnClickListener {
                    Log.d(TAG, "Subscribe button clicked: ${channel.name}")
                    navigateToPayment(channel)
                }
            }
        }

        private fun navigateToPayment(channel: Channel) {
            try {
                val context = binding.root.context
                val intent = Intent(context, PaymentActivity::class.java).apply {
                    putExtra(PaymentActivity.EXTRA_CHANNEL_ID, channel.id)
                    putExtra(PaymentActivity.EXTRA_SUBSCRIPTION_TYPE, "1_month") // Default subscription type
                }
                context.startActivity(intent)
                Log.d(TAG, "Navigating to PaymentActivity for channel: ${channel.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Error navigating to PaymentActivity: ${e.message}", e)
            }
        }

        private fun loadChannelLogo(channel: Channel) {
            try {
                val context = binding.root.context

                // Create Glide request options
                val requestOptions = RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery) // Default placeholder
                    .error(android.R.drawable.ic_menu_gallery) // Error placeholder

                when {
                    // Priority 1: Base64 image
                    channel.logoBase64.isNotEmpty() -> {
                        Log.d(TAG, "Loading Base64 logo for channel: ${channel.name}")

                        val base64String = if (channel.logoBase64.startsWith("data:image")) {
                            channel.logoBase64
                        } else {
                            "data:image/jpeg;base64,${channel.logoBase64}"
                        }

                        Glide.with(context)
                            .load(base64String)
                            .apply(requestOptions)
                            .into(binding.ivChannelLogo)
                    }

                    // Priority 2: URL image
                    channel.logoUrl.isNotEmpty() -> {
                        Log.d(TAG, "Loading URL logo for channel: ${channel.name}")

                        Glide.with(context)
                            .load(channel.logoUrl)
                            .apply(requestOptions)
                            .into(binding.ivChannelLogo)
                    }

                    // Priority 3: Default image
                    else -> {
                        Log.d(TAG, "Using default logo for channel: ${channel.name}")

                        Glide.with(context)
                            .load(android.R.drawable.ic_menu_gallery)
                            .apply(requestOptions)
                            .into(binding.ivChannelLogo)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading channel logo for ${channel.name}: ${e.message}", e)

                // Fallback to default image
                try {
                    Glide.with(binding.root.context)
                        .load(android.R.drawable.ic_menu_gallery)
                        .into(binding.ivChannelLogo)
                } catch (ex: Exception) {
                    Log.e(TAG, "Error loading default image: ${ex.message}", ex)
                }
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

    override fun submitList(list: List<Channel>?) {
        Log.d(TAG, "Submitting list with ${list?.size ?: 0} channels")

        // Log each channel for debugging
        list?.forEachIndexed { index, channel ->
            Log.d(TAG, "Channel $index: ${channel.name} (ID: ${channel.id}, Active: ${channel.isActive}, Price: ${channel.price})")
        }

        super.submitList(list)
    }

    override fun getItemCount(): Int {
        val count = super.getItemCount()
        Log.d(TAG, "Current item count: $count")
        return count
    }

    // Helper function to refresh a specific channel
    fun updateChannel(updatedChannel: Channel) {
        val currentList = currentList.toMutableList()
        val index = currentList.indexOfFirst { it.id == updatedChannel.id }

        if (index != -1) {
            currentList[index] = updatedChannel
            submitList(currentList)
            Log.d(TAG, "Updated channel: ${updatedChannel.name} at position $index")
        } else {
            Log.w(TAG, "Channel not found for update: ${updatedChannel.name}")
        }
    }

    // Helper function to remove a channel
    fun removeChannel(channelId: String) {
        val currentList = currentList.toMutableList()
        val index = currentList.indexOfFirst { it.id == channelId }

        if (index != -1) {
            val removedChannel = currentList.removeAt(index)
            submitList(currentList)
            Log.d(TAG, "Removed channel: ${removedChannel.name} from position $index")
        } else {
            Log.w(TAG, "Channel not found for removal: $channelId")
        }
    }

    // Helper function to add a channel
    fun addChannel(newChannel: Channel) {
        val currentList = currentList.toMutableList()
        currentList.add(newChannel)
        submitList(currentList)
        Log.d(TAG, "Added new channel: ${newChannel.name}")
    }

    // Helper function to filter channels by category
    fun filterByCategory(category: String, allChannels: List<Channel>) {
        val filteredChannels = if (category.isEmpty() || category == "All") {
            allChannels
        } else {
            allChannels.filter { it.category.equals(category, ignoreCase = true) }
        }

        Log.d(TAG, "Filtering channels by category '$category': ${filteredChannels.size} results")
        submitList(filteredChannels)
    }

    // Helper function to filter channels by price range
    fun filterByPriceRange(minPrice: Double, maxPrice: Double, allChannels: List<Channel>) {
        val filteredChannels = allChannels.filter {
            it.price >= minPrice && it.price <= maxPrice
        }

        Log.d(TAG, "Filtering channels by price range $minPrice-$maxPrice: ${filteredChannels.size} results")
        submitList(filteredChannels)
    }

    // Helper function to search channels by name
    fun searchChannels(query: String, allChannels: List<Channel>) {
        val filteredChannels = if (query.isEmpty()) {
            allChannels
        } else {
            allChannels.filter { channel ->
                channel.name.contains(query, ignoreCase = true) ||
                        channel.description.contains(query, ignoreCase = true) ||
                        channel.category.contains(query, ignoreCase = true)
            }
        }

        Log.d(TAG, "Searching channels with query '$query': ${filteredChannels.size} results")
        submitList(filteredChannels)
    }

    // Helper function to sort channels
    fun sortChannels(sortBy: SortType, allChannels: List<Channel>) {
        val sortedChannels = when (sortBy) {
            SortType.NAME_ASC -> allChannels.sortedBy { it.name }
            SortType.NAME_DESC -> allChannels.sortedByDescending { it.name }
            SortType.PRICE_ASC -> allChannels.sortedBy { it.price }
            SortType.PRICE_DESC -> allChannels.sortedByDescending { it.price }
            SortType.CATEGORY -> allChannels.sortedBy { it.category }
            SortType.NEWEST -> allChannels.sortedByDescending { it.createdAt }
            SortType.OLDEST -> allChannels.sortedBy { it.createdAt }
        }

        Log.d(TAG, "Sorting channels by $sortBy")
        submitList(sortedChannels)
    }

    enum class SortType {
        NAME_ASC,
        NAME_DESC,
        PRICE_ASC,
        PRICE_DESC,
        CATEGORY,
        NEWEST,
        OLDEST
    }
}