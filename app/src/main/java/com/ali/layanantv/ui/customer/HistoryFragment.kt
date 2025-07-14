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
        setupUI()
        loadPurchaseHistory()
    }

    private fun setupUI() {
        // Setup RecyclerView for purchase history
        historyAdapter = PurchaseHistoryAdapter(
            onItemClick = { order ->
                showOrderDetails(order)
            },
            onReorderClick = { order ->
                reorderSubscription(order)
            }
        )

        binding.rvPurchaseHistory.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = historyAdapter
        }

        // Setup filter buttons
        binding.btnFilterAll.setOnClickListener {
            currentFilter = "all"
            updateFilterUI()
            loadPurchaseHistory()
        }

        binding.btnFilterActive.setOnClickListener {
            currentFilter = "completed"
            updateFilterUI()
            loadPurchaseHistory()
        }

        binding.btnFilterExpired.setOnClickListener {
            currentFilter = "cancelled"
            updateFilterUI()
            loadPurchaseHistory()
        }

        // Setup swipe refresh
        binding.swipeRefresh.setOnRefreshListener {
            loadPurchaseHistory()
        }
    }

    private fun updateFilterUI() {
        // Reset all button styles
        binding.btnFilterAll.isSelected = currentFilter == "all"
        binding.btnFilterActive.isSelected = currentFilter == "completed"
        binding.btnFilterExpired.isSelected = currentFilter == "cancelled"
    }

    private fun loadPurchaseHistory() {
        lifecycleScope.launch {
            try {
                if (!binding.swipeRefresh.isRefreshing) {
                    binding.progressBar.visibility = View.VISIBLE
                }

                val allOrders = customerRepository.getUserOrders()
                val filteredOrders = when (currentFilter) {
                    "all" -> allOrders
                    else -> allOrders.filter { it.status == currentFilter }
                }

                if (filteredOrders.isEmpty()) {
                    binding.layoutEmpty.visibility = View.VISIBLE
                    binding.rvPurchaseHistory.visibility = View.GONE
                } else {
                    binding.layoutEmpty.visibility = View.GONE
                    binding.rvPurchaseHistory.visibility = View.VISIBLE
                    historyAdapter.submitList(filteredOrders)
                }

                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false

            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
                Toast.makeText(context, "Error loading history: ${e.message}", Toast.LENGTH_SHORT).show()
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
        // TODO: Implement reorder logic
        Toast.makeText(context, "Reordering ${order.channelName}", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}