package com.ali.layanantv.ui.customer

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.ali.layanantv.data.model.Channel
import com.ali.layanantv.data.repository.CustomerRepository
import com.ali.layanantv.databinding.ActivityChannelBrowserBinding
import kotlinx.coroutines.launch

class ChannelBrowserActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChannelBrowserBinding
    private lateinit var customerRepository: CustomerRepository
    private lateinit var channelBrowserAdapter: ChannelBrowserAdapter
    private var allChannels: List<Channel> = emptyList()

    companion object {
        private const val TAG = "ChannelBrowserActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChannelBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d(TAG, "ChannelBrowserActivity created")

        customerRepository = CustomerRepository()
        setupUI()
        loadChannels()
    }

    private fun setupUI() {
        Log.d(TAG, "Setting up UI")

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Pilih Channel"

        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        // Setup RecyclerView
        channelBrowserAdapter = ChannelBrowserAdapter { channel ->
            Log.d(TAG, "Channel clicked: ${channel.name}")
            subscribeToChannel(channel)
        }

        binding.rvChannels.apply {
            layoutManager = GridLayoutManager(this@ChannelBrowserActivity, 2)
            adapter = channelBrowserAdapter
        }

        Log.d(TAG, "UI setup completed")
    }

    private fun loadChannels() {
        Log.d(TAG, "Starting to load channels")

        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                binding.layoutEmpty.visibility = View.GONE
                binding.rvChannels.visibility = View.GONE

                Log.d(TAG, "Calling customerRepository.getAvailableChannels()")

                // PERBAIKAN: Mencoba beberapa metode untuk mendapatkan channel
                val channels = try {
                    // Pertama, coba method utama
                    var channelList = customerRepository.getAvailableChannels()

                    // Jika kosong, coba refresh
                    if (channelList.isEmpty()) {
                        Log.w(TAG, "No channels from getAvailableChannels(), trying refresh...")
                        channelList = customerRepository.refreshChannels().filter { it.isActive }
                    }

                    // Jika masih kosong, coba getAllChannels untuk debug
                    if (channelList.isEmpty()) {
                        Log.w(TAG, "Still no channels, trying getAllChannels for debugging...")
                        val allChannels = customerRepository.getAllChannels()
                        Log.d(TAG, "Total channels in database: ${allChannels.size}")
                        allChannels.forEach { channel ->
                            Log.d(TAG, "DB Channel: ${channel.name} (Active: ${channel.isActive})")
                        }
                        channelList = allChannels.filter { it.isActive }
                    }

                    channelList
                } catch (e: Exception) {
                    Log.e(TAG, "Error in channel loading methods: ${e.message}", e)
                    emptyList()
                }

                // Simpan channels untuk filter/search nanti
                allChannels = channels

                Log.d(TAG, "Received ${channels.size} channels from repository")

                if (channels.isEmpty()) {
                    Log.w(TAG, "No channels available, showing empty state")
                    binding.layoutEmpty.visibility = View.VISIBLE
                    binding.rvChannels.visibility = View.GONE

                    // Untuk debugging, coba tampilkan pesan yang lebih informatif
                    Toast.makeText(
                        this@ChannelBrowserActivity,
                        "Tidak ada channel tersedia. Silakan hubungi admin untuk menambahkan channel.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Log.d(TAG, "Showing ${channels.size} channels")
                    binding.layoutEmpty.visibility = View.GONE
                    binding.rvChannels.visibility = View.VISIBLE

                    // Log each channel for debugging
                    channels.forEachIndexed { index, channel ->
                        Log.d(TAG, "Channel $index: ${channel.name} (ID: ${channel.id}, Active: ${channel.isActive}, Price: ${channel.price})")
                    }

                    channelBrowserAdapter.submitList(channels)
                }

                binding.progressBar.visibility = View.GONE

            } catch (e: Exception) {
                Log.e(TAG, "Error loading channels: ${e.message}", e)
                binding.progressBar.visibility = View.GONE
                binding.layoutEmpty.visibility = View.VISIBLE
                binding.rvChannels.visibility = View.GONE

                Toast.makeText(
                    this@ChannelBrowserActivity,
                    "Error loading channels: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun subscribeToChannel(channel: Channel) {
        Log.d(TAG, "Subscribe to channel: ${channel.name} (ID: ${channel.id})")

        lifecycleScope.launch {
            try {
                // Check if user is already subscribed
                val isSubscribed = customerRepository.isUserSubscribedToChannel(channel.id)

                if (isSubscribed) {
                    Toast.makeText(
                        this@ChannelBrowserActivity,
                        "Anda sudah berlangganan ${channel.name}",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                // TODO: Implement subscription logic
                // For now, just show a message
                Toast.makeText(
                    this@ChannelBrowserActivity,
                    "Berlangganan ${channel.name} - Fitur akan segera tersedia",
                    Toast.LENGTH_SHORT
                ).show()

                // Example of how to implement subscription:
                /*
                val order = Order(
                    userId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                    channelId = channel.id,
                    channelName = channel.name,
                    subscriptionType = "monthly",
                    status = "pending",
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now()
                )

                val orderId = customerRepository.createOrder(order)
                Log.d(TAG, "Order created with ID: $orderId")

                // Navigate to payment or show success message
                */

            } catch (e: Exception) {
                Log.e(TAG, "Error subscribing to channel: ${e.message}", e)
                Toast.makeText(
                    this@ChannelBrowserActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "Activity resumed, refreshing channels")
        loadChannels()
    }

    // Method untuk refresh manual jika diperlukan
    private fun refreshChannels() {
        Log.d(TAG, "Manual refresh triggered")
        loadChannels()
    }

    // Method untuk debugging - bisa dipanggil dari UI jika perlu
    private fun debugChannels() {
        lifecycleScope.launch {
            try {
                val allChannels = customerRepository.getAllChannels()
                Log.d(TAG, "=== DEBUGGING CHANNELS ===")
                Log.d(TAG, "Total channels in database: ${allChannels.size}")
                allChannels.forEach { channel ->
                    Log.d(TAG, "Channel: ${channel.name}")
                    Log.d(TAG, "  - ID: ${channel.id}")
                    Log.d(TAG, "  - Active: ${channel.isActive}")
                    Log.d(TAG, "  - Price: ${channel.price}")
                    Log.d(TAG, "  - Category: ${channel.category}")
                    Log.d(TAG, "  - Description: ${channel.description}")
                    Log.d(TAG, "  - CreatedAt: ${channel.createdAt}")
                    Log.d(TAG, "  - UpdatedAt: ${channel.updatedAt}")
                }
                Log.d(TAG, "=========================")
            } catch (e: Exception) {
                Log.e(TAG, "Error debugging channels: ${e.message}", e)
            }
        }
    }
}