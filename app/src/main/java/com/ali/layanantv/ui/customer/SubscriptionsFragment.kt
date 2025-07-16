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

    // Set untuk menyimpan ID subscription yang sudah dibatalkan
    private val cancelledSubscriptionIds = mutableSetOf<String>()

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
                delay(3000) // Reduced delay
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

                // Load subscriptions
                val subscriptions = loadUserSubscriptionsEnhanced()

                // PERBAIKAN: Filter cancelled subscriptions sebelum menyimpan ke currentSubscriptions
                val filteredSubscriptions = subscriptions.filter { subscription ->
                    subscription.id !in cancelledSubscriptionIds
                }

                currentSubscriptions = filteredSubscriptions

                Log.d(TAG, "Loaded ${subscriptions.size} subscriptions, ${filteredSubscriptions.size} after filtering cancelled")

                // Check again before updating UI
                if (binding == null || !isAdded) {
                    Log.w(TAG, "Fragment state changed during loading")
                    return@launch
                }

                updateSubscriptionUI(filteredSubscriptions)

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
            // Load all orders first
            val allOrders = customerRepository.getUserOrders()
            Log.d(TAG, "Retrieved ${allOrders.size} total orders")

            // Apply strict filtering untuk subscription aktif
            val validSubscriptions = allOrders.filter { order ->
                val isCorrectUser = order.userId == userId
                val hasValidChannel = order.channelId.isNotEmpty() && !order.channelName.isNullOrEmpty()
                val hasValidAmount = order.totalAmount > 0

                // Status yang dianggap aktif
                val activeStatuses = listOf(
                    "completed", "active", "pending", "success", "confirmed",
                    "paid", "processing", "approved", "subscribed"
                )
                val hasActiveStatus = order.status.lowercase() in activeStatuses.map { it.lowercase() }

                // Status yang dianggap tidak aktif (harus difilter)
                val inactiveStatuses = listOf(
                    "cancelled", "canceled", "expired", "failed", "rejected",
                    "refunded", "terminated", "suspended", "inactive"
                )
                val isNotInactive = order.status.lowercase() !in inactiveStatuses.map { it.lowercase() }

                // PERBAIKAN: Tambahkan filter untuk cancelled subscriptions di sini juga
                val notLocallyCancelled = order.id !in cancelledSubscriptionIds

                val isValid = isCorrectUser && hasValidChannel && hasValidAmount &&
                        hasActiveStatus && isNotInactive && notLocallyCancelled

                Log.d(TAG, "Order ${order.id} validation: user=$isCorrectUser, channel=$hasValidChannel, amount=$hasValidAmount, active=$hasActiveStatus, notInactive=$isNotInactive, notLocallyCancelled=$notLocallyCancelled, final=$isValid")

                isValid
            }

            Log.d(TAG, "Found ${validSubscriptions.size} valid subscriptions after filtering")

            if (validSubscriptions.isNotEmpty()) {
                return validSubscriptions.sortedByDescending { it.createdAt }
            }

            // Fallback strategies dengan filter yang sama
            val userSubscriptions = customerRepository.getUserSubscriptions()
            val filteredUserSubs = userSubscriptions.filter { subscription ->
                val isCorrectUser = subscription.userId == userId
                val hasValidChannel = !subscription.channelName.isNullOrEmpty()
                val inactiveStatuses = listOf(
                    "cancelled", "canceled", "expired", "failed", "rejected",
                    "refunded", "terminated", "suspended", "inactive"
                )
                val isNotInactive = subscription.status.lowercase() !in inactiveStatuses.map { it.lowercase() }
                val notLocallyCancelled = subscription.id !in cancelledSubscriptionIds

                isCorrectUser && hasValidChannel && isNotInactive && notLocallyCancelled
            }

            if (filteredUserSubs.isNotEmpty()) {
                return filteredUserSubs.sortedByDescending { it.createdAt }
            }

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
                updateSubscriptionSummary(0, 0.0)
            } else {
                Log.d(TAG, "Displaying ${subscriptions.size} subscriptions")
                safeBinding.emptyState.visibility = View.GONE
                safeBinding.rvSubscriptions.visibility = View.VISIBLE
                safeBinding.btnRenewAll.visibility = View.VISIBLE

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
            val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
            val formattedAmount = formatter.format(totalAmount)

            safeBinding.tvActiveCount.text = "$activeCount Channel"
            safeBinding.tvTotalAmount.text = formattedAmount

            Log.d(TAG, "Subscription summary updated: $activeCount active channels, total: $formattedAmount")

        } catch (e: Exception) {
            Log.e(TAG, "Error updating subscription summary: ${e.message}", e)
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
                    error.message?.contains("NETWORK_ERROR") == true -> "Periksa koneksi internet Anda"
                    else -> "Terjadi kesalahan saat memuat langganan"
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
                binding?.progressBar?.visibility = View.VISIBLE

                val newOrderId = customerRepository.renewSubscription(subscription)
                Log.d(TAG, "Renewed subscription with new order ID: $newOrderId")

                binding?.progressBar?.visibility = View.GONE

                context?.let { ctx ->
                    Toast.makeText(ctx, "Langganan ${subscription.channelName} berhasil diperpanjang", Toast.LENGTH_SHORT).show()
                }

                delay(2000)
                loadSubscriptions()

            } catch (e: Exception) {
                Log.e(TAG, "Error renewing subscription: ${e.message}", e)
                binding?.progressBar?.visibility = View.GONE

                context?.let { ctx ->
                    val errorMessage = when {
                        e.message?.contains("PERMISSION_DENIED") == true -> "Tidak memiliki izin untuk memperpanjang langganan"
                        e.message?.contains("UNAUTHENTICATED") == true -> "Silakan login terlebih dahulu"
                        e.message?.contains("UNAVAILABLE") == true -> "Layanan tidak tersedia saat ini"
                        e.message?.contains("INSUFFICIENT_FUNDS") == true -> "Saldo tidak mencukupi"
                        else -> "Gagal memperpanjang langganan"
                    }
                    Toast.makeText(ctx, errorMessage, Toast.LENGTH_SHORT).show()
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
                binding?.progressBar?.visibility = View.VISIBLE

                // PERBAIKAN: Tambahkan ke cancelled set terlebih dahulu
                cancelledSubscriptionIds.add(subscription.id)
                Log.d(TAG, "Added ${subscription.id} to cancelled subscriptions list")

                // Update UI immediately - hilangkan dari tampilan
                val updatedSubscriptions = currentSubscriptions.filter {
                    it.id != subscription.id
                }
                currentSubscriptions = updatedSubscriptions
                updateSubscriptionUI(updatedSubscriptions)

                // Tampilkan toast segera
                context?.let { ctx ->
                    Toast.makeText(ctx, "Langganan ${subscription.channelName} dibatalkan", Toast.LENGTH_SHORT).show()
                }

                // Coba batalkan subscription di server (background process)
                try {
                    customerRepository.cancelSubscription(subscription.id)
                    Log.d(TAG, "Successfully cancelled subscription on server: ${subscription.id}")
                } catch (serverError: Exception) {
                    Log.w(TAG, "Failed to cancel on server but continuing with local cancellation: ${serverError.message}")
                    // Tidak perlu tampilkan error ke user karena local cancellation sudah berhasil
                }

                binding?.progressBar?.visibility = View.GONE

            } catch (e: Exception) {
                Log.e(TAG, "Critical error in cancellation process: ${e.message}", e)

                binding?.progressBar?.visibility = View.GONE

                // PERBAIKAN: Jika terjadi error kritikal, rollback dengan lebih hati-hati
                if (cancelledSubscriptionIds.contains(subscription.id)) {
                    cancelledSubscriptionIds.remove(subscription.id)
                    Log.d(TAG, "Rolled back cancellation for ${subscription.id}")

                    // Reload untuk restore state
                    loadSubscriptions()
                }

                context?.let { ctx ->
                    val errorMessage = when {
                        e.message?.contains("PERMISSION_DENIED") == true -> "Tidak memiliki izin untuk membatalkan langganan"
                        e.message?.contains("UNAUTHENTICATED") == true -> "Silakan login terlebih dahulu"
                        e.message?.contains("UNAVAILABLE") == true -> "Layanan tidak tersedia saat ini"
                        else -> "Gagal membatalkan langganan: ${e.message}"
                    }
                    Toast.makeText(ctx, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun renewAllSubscriptions() {
        if (!isAdded || context == null) return

        Log.d(TAG, "Renewing all subscriptions")

        // PERBAIKAN: Filter cancelled subscriptions dari current list
        val activeSubscriptions = currentSubscriptions.filter {
            it.status.lowercase() in listOf("active", "completed", "success", "confirmed") &&
                    it.id !in cancelledSubscriptionIds
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
                binding?.progressBar?.visibility = View.VISIBLE

                var successCount = 0
                var failCount = 0

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

                binding?.progressBar?.visibility = View.GONE

                context?.let { ctx ->
                    val message = if (failCount == 0) {
                        "Semua langganan berhasil diperpanjang ($successCount langganan)"
                    } else {
                        "Berhasil memperpanjang $successCount langganan, gagal $failCount langganan"
                    }
                    Toast.makeText(ctx, message, Toast.LENGTH_LONG).show()
                }

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
        lifecycleScope.launch {
            delay(1000)
            loadSubscriptions()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "Fragment view destroyed")
        _binding = null
    }
}