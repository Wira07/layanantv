package com.ali.layanantv.ui.customer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ali.layanantv.data.repository.CustomerRepository
import com.ali.layanantv.data.model.Order
import com.ali.layanantv.databinding.FragmentHistoryBinding
import kotlinx.coroutines.launch

class HistoryFragment : Fragment() {
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var customerRepository: CustomerRepository
    private lateinit var historyAdapter: PurchaseHistoryAdapter
    private var currentFilter = "all"
    private var allOrders = listOf<Order>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        customerRepository = CustomerRepository()

        // Debug log
        android.util.Log.d("HistoryFragment", "onViewCreated - Setting up UI")

        setupUI()
        loadPurchaseHistory()
    }

    private fun setupUI() {
        // Debug log
        android.util.Log.d("HistoryFragment", "Setting up UI")

        // Setup RecyclerView for purchase history
        historyAdapter = PurchaseHistoryAdapter(
            onItemClick = { order ->
                android.util.Log.d("HistoryFragment", "Order clicked: ${order.channelName}")
                showOrderDetails(order)
            },
            onReorderClick = { order ->
                android.util.Log.d("HistoryFragment", "Reorder clicked: ${order.channelName}")
                reorderSubscription(order)
            }
        )

        binding.rvPurchaseHistory.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = historyAdapter
        }

        // Setup filter buttons
        binding.btnFilterAll.setOnClickListener {
            android.util.Log.d("HistoryFragment", "Filter All clicked")
            currentFilter = "all"
            updateFilterUI()
            applyFilter()
        }

        binding.btnFilterActive.setOnClickListener {
            android.util.Log.d("HistoryFragment", "Filter Active clicked")
            currentFilter = "completed"
            updateFilterUI()
            applyFilter()
        }

        binding.btnFilterExpired.setOnClickListener {
            android.util.Log.d("HistoryFragment", "Filter Expired clicked")
            currentFilter = "cancelled"
            updateFilterUI()
            applyFilter()
        }

        // Setup swipe refresh
        binding.swipeRefresh.setOnRefreshListener {
            android.util.Log.d("HistoryFragment", "Swipe refresh triggered")
            refreshData()
        }
    }

    private fun updateFilterUI() {
        // Check if binding is still valid
        _binding?.let { binding ->
            // Reset all button styles
            binding.btnFilterAll.isSelected = currentFilter == "all"
            binding.btnFilterActive.isSelected = currentFilter == "completed"
            binding.btnFilterExpired.isSelected = currentFilter == "cancelled"
        }
    }

    private fun loadPurchaseHistory() {
        lifecycleScope.launch {
            try {
                // Check if binding is still valid before accessing it
                _binding?.let { binding ->
                    if (!binding.swipeRefresh.isRefreshing) {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                }

                // Gunakan getOrderHistory langsung
                val orders = customerRepository.getOrderHistory()
                allOrders = orders

                // Debug log
                android.util.Log.d("HistoryFragment", "Loaded ${orders.size} orders")
                orders.forEach { order ->
                    android.util.Log.d("HistoryFragment", "Order: ${order.channelName} - ${order.status}")
                }

                applyFilter()

                // Check if binding is still valid before accessing it
                _binding?.let { binding ->
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                }

            } catch (e: Exception) {
                // Check if binding is still valid before accessing it
                _binding?.let { binding ->
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                }
                android.util.Log.e("HistoryFragment", "Error loading history", e)
                showError("Error loading history: ${e.message}")
            }
        }
    }

    private fun refreshData() {
        lifecycleScope.launch {
            try {
                // Load fresh data menggunakan getOrderHistory
                val orders = customerRepository.getOrderHistory()
                allOrders = orders

                // Debug log
                android.util.Log.d("HistoryFragment", "Refreshed ${orders.size} orders")

                applyFilter()

                // Check if binding is still valid before accessing it
                _binding?.let { binding ->
                    binding.swipeRefresh.isRefreshing = false
                }

            } catch (e: Exception) {
                // Check if binding is still valid before accessing it
                _binding?.let { binding ->
                    binding.swipeRefresh.isRefreshing = false
                }
                android.util.Log.e("HistoryFragment", "Error refreshing data", e)
                showError("Error refreshing data: ${e.message}")
            }
        }
    }

    private fun applyFilter() {
        // Check if binding is still valid before proceeding
        _binding?.let { binding ->
            val filteredOrders = when (currentFilter) {
                "all" -> allOrders
                else -> allOrders.filter { it.status == currentFilter }
            }

            // Debug log
            android.util.Log.d("HistoryFragment", "Applying filter: $currentFilter")
            android.util.Log.d("HistoryFragment", "Total orders: ${allOrders.size}, Filtered: ${filteredOrders.size}")

            if (filteredOrders.isEmpty()) {
                binding.layoutEmpty.visibility = View.VISIBLE
                binding.rvPurchaseHistory.visibility = View.GONE
                android.util.Log.d("HistoryFragment", "Showing empty state")
            } else {
                binding.layoutEmpty.visibility = View.GONE
                binding.rvPurchaseHistory.visibility = View.VISIBLE
                historyAdapter.submitList(filteredOrders)
                android.util.Log.d("HistoryFragment", "Showing ${filteredOrders.size} orders in RecyclerView")
            }
        }
    }

    private fun showOrderDetails(order: Order) {
        val paymentStatus = if (order.paymentVerified) "✅ Verified" else "⏳ Pending"
        val notes = if (order.notes.isNotEmpty()) order.notes else "Tidak ada catatan"

        context?.let { ctx ->
            AlertDialog.Builder(ctx)
                .setTitle("Detail Pembelian")
                .setMessage("Order ID: ${order.id}\n" +
                        "Status: ${order.status.uppercase()}\n" +
                        "Channel: ${order.channelName}\n" +
                        "Tipe: ${order.subscriptionType}\n" +
                        "Pembayaran: ${order.paymentMethod}\n" +
                        "Jumlah: Rp ${String.format("%,d", order.totalAmount.toInt())}\n" +
                        "Status Pembayaran: $paymentStatus\n" +
                        "Catatan: $notes")
                .setPositiveButton("Tutup", null)
                .show()
        }
    }

    private fun reorderSubscription(order: Order) {
        lifecycleScope.launch {
            try {
                // Check if binding is still valid before accessing it
                _binding?.let { binding ->
                    binding.progressBar.visibility = View.VISIBLE
                }

                android.util.Log.d("HistoryFragment", "Reordering subscription for: ${order.channelName}")

                // Use the renewSubscription function from repository
                val newOrderId = customerRepository.renewSubscription(order)

                // Check if binding is still valid before accessing it
                _binding?.let { binding ->
                    binding.progressBar.visibility = View.GONE
                }

                Toast.makeText(
                    context,
                    "Subscription berhasil diperpanjang untuk ${order.channelName}. Order ID: $newOrderId",
                    Toast.LENGTH_LONG
                ).show()

                // Refresh data to show the new order
                refreshData()

            } catch (e: Exception) {
                // Check if binding is still valid before accessing it
                _binding?.let { binding ->
                    binding.progressBar.visibility = View.GONE
                }
                android.util.Log.e("HistoryFragment", "Error reordering subscription", e)
                showError("Error reordering subscription: ${e.message}")
            }
        }
    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}