package com.ali.layanantv.ui.customer

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.ali.layanantv.data.model.Channel
import com.ali.layanantv.data.model.Order
import com.ali.layanantv.data.repository.CustomerRepository
import com.ali.layanantv.databinding.ActivityChannelBrowserBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch

class ChannelBrowserActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChannelBrowserBinding
    private lateinit var customerRepository: CustomerRepository
    private lateinit var channelBrowserAdapter: ChannelBrowserAdapter
    private var allChannels: List<Channel> = emptyList()
    private var selectedCategory: String? = null

    companion object {
        private const val TAG = "ChannelBrowserActivity"
        const val EXTRA_CATEGORY = "extra_category"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChannelBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d(TAG, "ChannelBrowserActivity created")

        // Get category from intent if provided
        selectedCategory = intent.getStringExtra(EXTRA_CATEGORY)
        Log.d(TAG, "Selected category: $selectedCategory")

        customerRepository = CustomerRepository()
        setupUI()
        loadChannels()
    }

    private fun setupUI() {
        Log.d(TAG, "Setting up UI")

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (selectedCategory != null) {
            "Channel - $selectedCategory"
        } else {
            "Pilih Channel"
        }

        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        // Setup RecyclerView
        channelBrowserAdapter = ChannelBrowserAdapter { channel ->
            Log.d(TAG, "Channel clicked: ${channel.name}")
//            subscribeToChannel(channel)
        }

        binding.rvChannels.apply {
            layoutManager = GridLayoutManager(this@ChannelBrowserActivity, 2)
            adapter = channelBrowserAdapter
        }

        // Add refresh button
        binding.swipeRefresh?.setOnRefreshListener {
            refreshChannels()
        }

        Log.d(TAG, "UI setup completed")
    }

    private fun loadChannels() {
        Log.d(TAG, "Starting to load channels")

        lifecycleScope.launch {
            try {
                showLoading(true)

                Log.d(TAG, "Calling customerRepository methods...")

                // Enhanced channel loading with multiple strategies
                val channels = when {
                    selectedCategory != null -> {
                        Log.d(TAG, "Loading channels by category: $selectedCategory")
                        loadChannelsByCategory(selectedCategory!!)
                    }
                    else -> {
                        Log.d(TAG, "Loading all active channels")
                        loadAllActiveChannels()
                    }
                }

                // Store channels for later use
                allChannels = channels
                Log.d(TAG, "Final channel count: ${channels.size}")

                // Update UI based on results
                showLoading(false)
                displayChannels(channels)

            } catch (e: Exception) {
                Log.e(TAG, "Critical error in loadChannels(): ${e.message}", e)
                showLoading(false)
                showError("Terjadi kesalahan saat memuat channel")
            }
        }
    }

    private suspend fun loadAllActiveChannels(): List<Channel> {
        var channels: List<Channel> = emptyList()

        try {
            // Strategy 1: Try getActiveChannels()
            Log.d(TAG, "Strategy 1: Calling getActiveChannels()")
            channels = customerRepository.getActiveChannels()
            Log.d(TAG, "Got ${channels.size} channels from getActiveChannels()")

            if (channels.isNotEmpty()) {
                return channels
            }

            // Strategy 2: Try getAvailableChannels()
            Log.d(TAG, "Strategy 2: Calling getAvailableChannels()")
            channels = customerRepository.getAvailableChannels()
            Log.d(TAG, "Got ${channels.size} channels from getAvailableChannels()")

            if (channels.isNotEmpty()) {
                return channels
            }

            // Strategy 3: Try refreshChannels()
            Log.d(TAG, "Strategy 3: Calling refreshChannels()")
            channels = customerRepository.refreshChannels().filter { it.isActive }
            Log.d(TAG, "Got ${channels.size} active channels from refreshChannels()")

            if (channels.isNotEmpty()) {
                return channels
            }

            // Strategy 4: Try getAllChannels() and filter
            Log.d(TAG, "Strategy 4: Calling getAllChannels() and filtering")
            val allChannels = customerRepository.getAllChannels()
            channels = allChannels.filter { it.isActive }
            Log.d(TAG, "Got ${channels.size} active channels from getAllChannels()")

        } catch (e: Exception) {
            Log.e(TAG, "Error in loadAllActiveChannels(): ${e.message}", e)
        }

        return channels
    }

    private suspend fun loadChannelsByCategory(category: String): List<Channel> {
        var channels: List<Channel> = emptyList()

        try {
            // Strategy 1: Try getChannelsByCategory()
            Log.d(TAG, "Strategy 1: Calling getChannelsByCategory($category)")
            channels = customerRepository.getChannelsByCategory(category)
            Log.d(TAG, "Got ${channels.size} channels from getChannelsByCategory()")

            if (channels.isNotEmpty()) {
                return channels
            }

            // Strategy 2: Get all active channels and filter by category
            Log.d(TAG, "Strategy 2: Getting all active channels and filtering by category")
            val allActiveChannels = loadAllActiveChannels()
            channels = allActiveChannels.filter { it.category == category }
            Log.d(TAG, "Got ${channels.size} channels filtered by category")

        } catch (e: Exception) {
            Log.e(TAG, "Error in loadChannelsByCategory(): ${e.message}", e)
        }

        return channels
    }

    private fun refreshChannels() {
        Log.d(TAG, "Refreshing channels")

        lifecycleScope.launch {
            try {
                binding.swipeRefresh?.isRefreshing = true

                // Use refreshChannels() method from repository
                val refreshedChannels = customerRepository.refreshChannels()

                val activeChannels = if (selectedCategory != null) {
                    refreshedChannels.filter { it.isActive && it.category == selectedCategory }
                } else {
                    refreshedChannels.filter { it.isActive }
                }

                allChannels = activeChannels
                Log.d(TAG, "Refreshed ${activeChannels.size} channels")

                binding.swipeRefresh?.isRefreshing = false
                displayChannels(activeChannels)

            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing channels: ${e.message}", e)
                binding.swipeRefresh?.isRefreshing = false
                showError("Gagal memperbarui channel")
            }
        }
    }

    private fun displayChannels(channels: List<Channel>) {
        if (channels.isEmpty()) {
            Log.w(TAG, "No channels available")
            binding.layoutEmpty.visibility = View.VISIBLE
            binding.rvChannels.visibility = View.GONE

            val message = if (selectedCategory != null) {
                "Tidak ada channel tersedia untuk kategori $selectedCategory"
            } else {
                "Tidak ada channel tersedia saat ini"
            }

            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        } else {
            Log.d(TAG, "Displaying ${channels.size} channels")
            binding.layoutEmpty.visibility = View.GONE
            binding.rvChannels.visibility = View.VISIBLE

            // Log channels for debugging
            channels.forEachIndexed { index, channel ->
                Log.d(TAG, "Channel $index: ${channel.name} - ${channel.price} (Category: ${channel.category})")
            }

            channelBrowserAdapter.submitList(channels)
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.progressBar.visibility = View.VISIBLE
            binding.layoutEmpty.visibility = View.GONE
            binding.rvChannels.visibility = View.GONE
        } else {
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun showError(message: String) {
        binding.layoutEmpty.visibility = View.VISIBLE
        binding.rvChannels.visibility = View.GONE
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "Activity resumed, refreshing channels")
        loadChannels()
    }

    private fun checkAuthState() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.w(TAG, "User not authenticated, finishing activity")
            Toast.makeText(this, "Silakan login terlebih dahulu", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        checkAuthState()
    }
}