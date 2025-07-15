package com.ali.layanantv.ui.customer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ali.layanantv.data.model.Channel
import com.ali.layanantv.data.repository.CustomerRepository
import com.ali.layanantv.databinding.FragmentSubscriptionsBinding
import kotlinx.coroutines.launch
import com.ali.layanantv.data.model.Order
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.*

class SubscriptionsFragment : Fragment() {
    private var _binding: FragmentSubscriptionsBinding? = null
    private val binding: FragmentSubscriptionsBinding?
        get() = _binding

    private lateinit var customerRepository: CustomerRepository
    private lateinit var subscriptionsAdapter: SubscriptionsAdapter
    private var currentSubscriptions: List<Order> = emptyList()

    companion object {
        private const val TAG = "SubscriptionsFragment"
    }

    // Activity Result Launcher untuk menangani result dari ChannelBrowserActivity
    private val channelBrowserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            Log.d(TAG, "Channel browser returned success, reloading subscriptions")
            lifecycleScope.launch {
                // Berikan waktu lebih untuk database sync
                delay(5000) // Increase to 5 seconds
                loadSubscriptions()

                // Backup reload jika masih belum muncul
                delay(3000)
                loadSubscriptions()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSubscriptionsBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "SubscriptionsFragment view created")

        customerRepository = CustomerRepository()
        setupUI()
        loadSubscriptions()
    }

    private fun setupUI() {
        val safeBinding = binding ?: return
        Log.d(TAG, "Setting up UI")

        // Setup RecyclerView for subscriptions
        subscriptionsAdapter = SubscriptionsAdapter(
            onRenewClick = { subscription ->
                Log.d(TAG, "Renew clicked for: ${subscription.channelName}")
                renewSubscription(subscription)
            },
            onCancelClick = { subscription ->
                Log.d(TAG, "Cancel clicked for: ${subscription.channelName}")
                cancelSubscription(subscription)
            }
        )

        safeBinding.rvSubscriptions.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = subscriptionsAdapter
        }

        // Setup click listeners
        safeBinding.btnBrowseChannels.setOnClickListener {
            Log.d(TAG, "Browse channels button clicked")
            if (isAdded && context != null) {
                val intent = Intent(requireContext(), ChannelBrowserActivity::class.java)
                channelBrowserLauncher.launch(intent)
            }
        }

        safeBinding.btnRenewAll.setOnClickListener {
            Log.d(TAG, "Renew all button clicked")
            renewAllSubscriptions()
        }

        // Setup swipe refresh
        safeBinding.swipeRefresh.setOnRefreshListener {
            Log.d(TAG, "Swipe refresh triggered")
            loadSubscriptions()
        }

        Log.d(TAG, "UI setup completed")
    }

    private fun loadSubscriptions() {
        Log.d(TAG, "Loading subscriptions")

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val safeBinding = binding
                if (safeBinding == null || !isAdded) {
                    Log.w(TAG, "Fragment not attached or binding is null")
                    return@launch
                }

                // Check authentication first
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser == null) {
                    Log.w(TAG, "User not authenticated")
                    handleLoadError(Exception("User not authenticated"))
                    return@launch
                }

                Log.d(TAG, "Loading subscriptions for user: ${currentUser.uid}")

                // Show loading
                if (!safeBinding.swipeRefresh.isRefreshing) {
                    safeBinding.progressBar.visibility = View.VISIBLE
                }

                // Load subscriptions using enhanced loading strategy
                val subscriptions = loadUserSubscriptionsEnhanced()
                currentSubscriptions = subscriptions

                Log.d(TAG, "Loaded ${subscriptions.size} subscriptions")

                // Log each subscription for debugging
                subscriptions.forEachIndexed { index, subscription ->
                    Log.d(TAG, "Subscription $index: ID=${subscription.id}, Channel=${subscription.channelName}, Status=${subscription.status}, UserId=${subscription.userId}, Amount=${subscription.totalAmount}")
                }

                // Check again before updating UI
                if (binding == null || !isAdded) {
                    Log.w(TAG, "Fragment state changed during loading")
                    return@launch
                }

                updateSubscriptionUI(subscriptions)

            } catch (e: Exception) {
                Log.e(TAG, "Error loading subscriptions: ${e.message}", e)
                handleLoadError(e)
            }
        }
    }

    private suspend fun loadUserSubscriptionsEnhanced(): List<Order> {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return emptyList()
        val userId = currentUser.uid

        Log.d(TAG, "Starting enhanced subscription loading for user: $userId")

        try {
            // Strategy 1: Load all orders and apply comprehensive filtering
            Log.d(TAG, "Loading all user orders...")
            val allOrders = customerRepository.getUserOrders()
            Log.d(TAG, "Retrieved ${allOrders.size} total orders")

            // Debug: Log all orders
            allOrders.forEachIndexed { index, order ->
                Log.d(TAG, "Order $index: ID=${order.id}, User=${order.userId}, Channel=${order.channelName}, Status=${order.status}, Amount=${order.totalAmount}")
            }

            // Enhanced filtering with more inclusive criteria
            val validSubscriptions = allOrders.filter { order ->
                val isCorrectUser = order.userId == userId
                val hasValidChannel = order.channelId.isNotEmpty() && !order.channelName.isNullOrEmpty()
                val hasValidAmount = order.totalAmount > 0

                // More inclusive status filtering - include more statuses
                val validStatuses = listOf(
                    "completed", "active", "pending", "success", "confirmed",
                    "paid", "processing", "approved", "subscribed"
                )
                val hasValidStatus = order.status.lowercase() in validStatuses.map { it.lowercase() }

                // Additional check: ensure order is not explicitly cancelled or expired
                val isNotCancelled = order.status.lowercase() !in listOf("cancelled", "canceled", "expired", "failed", "rejected")

                val isValid = isCorrectUser && hasValidChannel && hasValidAmount && hasValidStatus && isNotCancelled

                Log.d(TAG, "Order ${order.id} validation: user=$isCorrectUser, channel=$hasValidChannel, amount=$hasValidAmount, status=$hasValidStatus, notCancelled=$isNotCancelled, final=$isValid")

                isValid
            }

            Log.d(TAG, "Found ${validSubscriptions.size} valid subscriptions after filtering")

            if (validSubscriptions.isNotEmpty()) {
                // Sort by creation date (newest first)
                val sortedSubscriptions = validSubscriptions.sortedByDescending { it.createdAt }
                Log.d(TAG, "Returning ${sortedSubscriptions.size} subscriptions from orders")
                return sortedSubscriptions
            }

            // Strategy 2: Try getUserSubscriptions method as fallback
            Log.d(TAG, "No valid subscriptions from orders, trying getUserSubscriptions...")
            val userSubscriptions = customerRepository.getUserSubscriptions()
            Log.d(TAG, "Retrieved ${userSubscriptions.size} user subscriptions")

            val filteredUserSubs = userSubscriptions.filter { subscription ->
                subscription.userId == userId &&
                        !subscription.channelName.isNullOrEmpty() &&
                        subscription.status.lowercase() !in listOf("cancelled", "canceled", "expired", "failed")
            }

            if (filteredUserSubs.isNotEmpty()) {
                val sortedSubs = filteredUserSubs.sortedByDescending { it.createdAt }
                Log.d(TAG, "Returning ${sortedSubs.size} subscriptions from getUserSubscriptions")
                return sortedSubs
            }

            // Strategy 3: Try subscription history as last resort
            Log.d(TAG, "No subscriptions found, trying subscription history...")
            val subscriptionHistory = customerRepository.getSubscriptionHistory()
            Log.d(TAG, "Retrieved ${subscriptionHistory.size} subscription history records")

            val validHistory = subscriptionHistory.filter { subscription ->
                subscription.userId == userId &&
                        !subscription.channelName.isNullOrEmpty() &&
                        subscription.status.lowercase() in listOf("active", "completed", "pending", "success", "confirmed")
            }

            if (validHistory.isNotEmpty()) {
                val sortedHistory = validHistory.sortedByDescending { it.createdAt }
                Log.d(TAG, "Returning ${sortedHistory.size} subscriptions from history")
                return sortedHistory
            }

            // Strategy 4: Load orders with minimal filtering (for debugging)
            Log.d(TAG, "Trying minimal filtering for debugging...")
            val minimalFilteredOrders = allOrders.filter { order ->
                order.userId == userId && !order.channelName.isNullOrEmpty()
            }

            if (minimalFilteredOrders.isNotEmpty()) {
                Log.d(TAG, "Found ${minimalFilteredOrders.size} orders with minimal filtering")
                minimalFilteredOrders.forEach { order ->
                    Log.d(TAG, "Minimal filter order: ${order.channelName} - ${order.status} - ${order.totalAmount}")
                }
                return minimalFilteredOrders.sortedByDescending { it.createdAt }
            }

            Log.d(TAG, "No subscriptions found after all strategies")
            return emptyList()

        } catch (e: Exception) {
            Log.e(TAG, "Error in enhanced subscription loading: ${e.message}", e)
            return emptyList()
        }
    }

    private fun updateSubscriptionUI(subscriptions: List<Order>) {
        val safeBinding = binding ?: return

        try {
            // Hide loading
            safeBinding.progressBar.visibility = View.GONE
            safeBinding.swipeRefresh.isRefreshing = false

            if (subscriptions.isEmpty()) {
                Log.d(TAG, "No subscriptions found, showing empty state")
                safeBinding.emptyState.visibility = View.VISIBLE
                safeBinding.rvSubscriptions.visibility = View.GONE
                safeBinding.btnRenewAll.visibility = View.GONE

                // Update summary untuk empty state
                updateSubscriptionSummary(0, 0.0)
            } else {
                Log.d(TAG, "Displaying ${subscriptions.size} subscriptions")
                safeBinding.emptyState.visibility = View.GONE
                safeBinding.rvSubscriptions.visibility = View.VISIBLE
                safeBinding.btnRenewAll.visibility = View.VISIBLE

                // Log subscriptions for debugging
                subscriptions.forEachIndexed { index, subscription ->
                    Log.d(TAG, "Displaying subscription $index: ${subscription.channelName} - ${subscription.status} - Rp${subscription.totalAmount}")
                }

                subscriptionsAdapter.submitList(subscriptions)

                // Update summary
                val activeSubscriptions = subscriptions.filter {
                    it.status.lowercase() in listOf("active", "completed", "success", "confirmed")
                }
                val totalAmount = activeSubscriptions.sumOf { it.totalAmount }
                updateSubscriptionSummary(activeSubscriptions.size, totalAmount)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating subscription UI: ${e.message}", e)
        }
    }

    private fun updateSubscriptionSummary(activeCount: Int, totalAmount: Double) {
        val safeBinding = binding ?: return

        try {
            // Format currency
            val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
            val formattedAmount = formatter.format(totalAmount)

            // Update the summary TextViews
            safeBinding.tvActiveCount.text = "$activeCount Channel"
            safeBinding.tvTotalAmount.text = formattedAmount

            Log.d(TAG, "Subscription summary updated: $activeCount active channels, total: $formattedAmount")

        } catch (e: Exception) {
            Log.e(TAG, "Error updating subscription summary: ${e.message}", e)
            // Set default values if error occurs
            safeBinding.tvActiveCount.text = "0 Channel"
            safeBinding.tvTotalAmount.text = "Rp 0"
        }
    }

    private fun handleLoadError(error: Exception) {
        if (isAdded && binding != null) {
            binding?.let { b ->
                b.progressBar.visibility = View.GONE
                b.swipeRefresh.isRefreshing = false
                b.emptyState.visibility = View.VISIBLE
                b.rvSubscriptions.visibility = View.GONE
            }

            context?.let { ctx ->
                val errorMessage = when {
                    error.message?.contains("PERMISSION_DENIED") == true -> "Tidak memiliki izin untuk mengakses data langganan"
                    error.message?.contains("UNAUTHENTICATED") == true -> "Silakan login terlebih dahulu"
                    error.message?.contains("UNAVAILABLE") == true -> "Layanan tidak tersedia saat ini"
                    else -> "Terjadi kesalahan saat memuat langganan: ${error.message}"
                }
                Toast.makeText(ctx, errorMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun renewSubscription(subscription: Order) {
        if (!isAdded || context == null) return

        Log.d(TAG, "Renewing subscription for: ${subscription.channelName}")

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Show loading
                binding?.progressBar?.visibility = View.VISIBLE

                // Use renewSubscription method from repository
                val newOrderId = customerRepository.renewSubscription(subscription)
                Log.d(TAG, "Renewed subscription with new order ID: $newOrderId")

                // Hide loading
                binding?.progressBar?.visibility = View.GONE

                context?.let { ctx ->
                    Toast.makeText(ctx, "Langganan ${subscription.channelName} berhasil diperpanjang", Toast.LENGTH_SHORT).show()
                }

                // Reload subscriptions with delay
                delay(2000)
                loadSubscriptions()

            } catch (e: Exception) {
                Log.e(TAG, "Error renewing subscription: ${e.message}", e)

                binding?.progressBar?.visibility = View.GONE

                context?.let { ctx ->
                    Toast.makeText(ctx, "Gagal memperpanjang langganan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun cancelSubscription(subscription: Order) {
        if (!isAdded || context == null) return

        Log.d(TAG, "Cancelling subscription for: ${subscription.channelName}")

        context?.let { ctx ->
            androidx.appcompat.app.AlertDialog.Builder(ctx)
                .setTitle("Batalkan Langganan")
                .setMessage("Apakah Anda yakin ingin membatalkan langganan ${subscription.channelName}?")
                .setPositiveButton("Ya") { _, _ ->
                    performCancelSubscription(subscription)
                }
                .setNegativeButton("Tidak", null)
                .show()
        }
    }

    private fun performCancelSubscription(subscription: Order) {
        if (!isAdded || context == null) return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Show loading
                binding?.progressBar?.visibility = View.VISIBLE

                // Use cancelSubscription method from repository
                customerRepository.cancelSubscription(subscription.id)
                Log.d(TAG, "Cancelled subscription: ${subscription.id}")

                // Hide loading
                binding?.progressBar?.visibility = View.GONE

                context?.let { ctx ->
                    Toast.makeText(ctx, "Langganan ${subscription.channelName} berhasil dibatalkan", Toast.LENGTH_SHORT).show()
                }

                // Reload subscriptions with delay
                delay(2000)
                loadSubscriptions()

            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling subscription: ${e.message}", e)

                binding?.progressBar?.visibility = View.GONE

                context?.let { ctx ->
                    Toast.makeText(ctx, "Gagal membatalkan langganan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun renewAllSubscriptions() {
        if (!isAdded || context == null) return

        Log.d(TAG, "Renewing all subscriptions")

        val activeSubscriptions = currentSubscriptions.filter {
            it.status.lowercase() in listOf("active", "completed", "success", "confirmed")
        }

        if (activeSubscriptions.isEmpty()) {
            context?.let { ctx ->
                Toast.makeText(ctx, "Tidak ada langganan aktif yang dapat diperpanjang", Toast.LENGTH_SHORT).show()
            }
            return
        }

        context?.let { ctx ->
            androidx.appcompat.app.AlertDialog.Builder(ctx)
                .setTitle("Perpanjang Semua Langganan")
                .setMessage("Apakah Anda yakin ingin memperpanjang ${activeSubscriptions.size} langganan aktif?")
                .setPositiveButton("Ya") { _, _ ->
                    performRenewAllSubscriptions(activeSubscriptions)
                }
                .setNegativeButton("Tidak", null)
                .show()
        }
    }

    private fun performRenewAllSubscriptions(subscriptions: List<Order>) {
        if (!isAdded || context == null) return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Show loading
                binding?.progressBar?.visibility = View.VISIBLE

                var successCount = 0
                var failCount = 0

                // Renew each subscription
                subscriptions.forEach { subscription ->
                    try {
                        val newOrderId = customerRepository.renewSubscription(subscription)
                        Log.d(TAG, "Renewed subscription ${subscription.channelName} with order ID: $newOrderId")
                        successCount++
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to renew subscription ${subscription.channelName}: ${e.message}", e)
                        failCount++
                    }
                }

                // Hide loading
                binding?.progressBar?.visibility = View.GONE

                // Show result
                context?.let { ctx ->
                    val message = if (failCount == 0) {
                        "Semua langganan berhasil diperpanjang ($successCount langganan)"
                    } else {
                        "Berhasil memperpanjang $successCount langganan, gagal $failCount langganan"
                    }
                    Toast.makeText(ctx, message, Toast.LENGTH_LONG).show()
                }

                // Reload subscriptions with delay
                delay(2000)
                loadSubscriptions()

            } catch (e: Exception) {
                Log.e(TAG, "Error renewing all subscriptions: ${e.message}", e)

                binding?.progressBar?.visibility = View.GONE

                context?.let { ctx ->
                    Toast.makeText(ctx, "Gagal memperpanjang langganan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "Fragment resumed, reloading subscriptions")

        // Add delay to ensure any background operations are complete
        lifecycleScope.launch {
            delay(1000) // Increase delay
            loadSubscriptions()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "Fragment view destroyed")
        _binding = null
    }
}